import { useEffect, useState } from "react";
import { useCapture } from "@/domains/capture/store/capture-context";
import {
  getDownloadConfig,
  getEngineRuntimeSnapshot,
  listTrackerSets,
  updateDownloadConfig,
  updateEngineRuntimeProfile,
  updateTrackerSet
} from "@/domains/config/api/configApi";
import { Aria2RestartConfirmDialog } from "@/domains/config/components/Aria2RestartConfirmDialog";
import { Aria2SettingsSection } from "@/domains/config/components/Aria2SettingsSection";
import { BasicSettingsSection } from "@/domains/config/components/BasicSettingsSection";
import type { BtTrackerSet, DownloadConfig, EngineRuntimeSnapshot, UpdateDownloadConfigPayload } from "@/domains/config/models/config";
import { getDefaultAria2Command, buildAria2Command, parseAria2Command } from "@/domains/config/utils/aria2Command";
import { useShell } from "@/domains/shell/hooks/useShell";

type SettingsTabKey = "basic" | "aria2";

interface PendingAria2RestartPayload {
  profileJson: string;
  trackerListText: string;
}

const SETTINGS_TABS: Array<{ key: SettingsTabKey; label: string; description: string }> = [
  {
    key: "basic",
    label: "基础配置",
    description: "下载目录、并发限制、浏览器接管和剪贴板监听。"
  },
  {
    key: "aria2",
    label: "aria2 配置",
    description: "只保留 aria2 启动命令，并支持保存后按需重启引擎。"
  }
];

function buildPayload(config: DownloadConfig): UpdateDownloadConfigPayload {
  return {
    defaultSaveDir: config.defaultSaveDir,
    maxConcurrentDownloads: Number(config.maxConcurrentDownloads),
    maxGlobalDownloadSpeed: Number(config.maxGlobalDownloadSpeed),
    maxGlobalUploadSpeed: Number(config.maxGlobalUploadSpeed),
    browserCaptureEnabled: Boolean(config.browserCaptureEnabled),
    clipboardMonitorEnabled: Boolean(config.clipboardMonitorEnabled),
    activeEngineProfileCode: config.activeEngineProfileCode,
    deleteToRecycleBinEnabled: Boolean(config.deleteToRecycleBinEnabled)
  };
}

function resolveTrackerSetCode(snapshot: EngineRuntimeSnapshot | null, trackerSets: BtTrackerSet[], profileCode: string) {
  const activeProfile = snapshot?.profiles.find((profile) => profile.profileCode === profileCode);
  return activeProfile?.trackerSetCode || trackerSets[0]?.trackerSetCode || "builtin-default";
}

function buildTrackerPayload(trackerSets: BtTrackerSet[], trackerSetCode: string, trackerListText: string) {
  const matchedTrackerSet = trackerSets.find((trackerSet) => trackerSet.trackerSetCode === trackerSetCode);
  return {
    trackerSetName: matchedTrackerSet?.trackerSetName || "内置默认 Tracker",
    trackerListText,
    sourceUrl: matchedTrackerSet?.sourceUrl || undefined
  };
}

export function SettingsPage() {
  const { pushToast } = useShell();
  const { syncConfig } = useCapture();
  const [config, setConfig] = useState<DownloadConfig | null>(null);
  const [runtimeSnapshot, setRuntimeSnapshot] = useState<EngineRuntimeSnapshot | null>(null);
  const [trackerSets, setTrackerSets] = useState<BtTrackerSet[]>([]);
  const [selectedProfileCode, setSelectedProfileCode] = useState("");
  const [aria2Command, setAria2Command] = useState(getDefaultAria2Command());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [activeTab, setActiveTab] = useState<SettingsTabKey>("basic");
  const [restartConfirmOpen, setRestartConfirmOpen] = useState(false);
  const [restartingAria2Engine, setRestartingAria2Engine] = useState(false);
  const [pendingRestartPayload, setPendingRestartPayload] = useState<PendingAria2RestartPayload | null>(null);

  function hydrateAria2Editor(snapshot: EngineRuntimeSnapshot, nextTrackerSets: BtTrackerSet[]) {
    const profileCode = snapshot.activeProfileCode || "default";
    const matchedProfile = snapshot.profiles.find((profile) => profile.profileCode === profileCode);
    const matchedTrackerSet =
      nextTrackerSets.find((trackerSet) => trackerSet.trackerSetCode === matchedProfile?.trackerSetCode) || nextTrackerSets[0];
    setSelectedProfileCode(profileCode);
    setAria2Command(buildAria2Command(matchedProfile?.profileJson, matchedTrackerSet?.trackerListText));
  }

  useEffect(() => {
    let disposed = false;

    async function loadConfig() {
      try {
        setLoading(true);
        setErrorMessage("");
        const [nextConfig, nextRuntimeSnapshot, nextTrackerSets] = await Promise.all([
          getDownloadConfig(),
          getEngineRuntimeSnapshot(),
          listTrackerSets()
        ]);
        if (disposed) {
          return;
        }
        setConfig(nextConfig);
        syncConfig(nextConfig);
        setRuntimeSnapshot(nextRuntimeSnapshot);
        setTrackerSets(nextTrackerSets);
        hydrateAria2Editor(nextRuntimeSnapshot, nextTrackerSets);
      } catch (error) {
        if (!disposed) {
          setErrorMessage(error instanceof Error ? error.message : "配置加载失败");
        }
      } finally {
        if (!disposed) {
          setLoading(false);
        }
      }
    }

    void loadConfig();
    return () => {
      disposed = true;
    };
  }, [syncConfig]);

  function updateField<K extends keyof DownloadConfig>(key: K, value: DownloadConfig[K]) {
    setSuccessMessage("");
    setConfig((currentConfig) => (currentConfig ? { ...currentConfig, [key]: value } : currentConfig));
  }

  /**
   * 重新拉取 aria2 运行配置快照，并回填当前命令输入框。
   *
   * @returns Promise<void>
   */
  async function reloadRuntimeConfigs() {
    const [nextRuntimeSnapshot, nextTrackerSets] = await Promise.all([getEngineRuntimeSnapshot(), listTrackerSets()]);
    setRuntimeSnapshot(nextRuntimeSnapshot);
    setTrackerSets(nextTrackerSets);
    hydrateAria2Editor(nextRuntimeSnapshot, nextTrackerSets);
  }

  async function selectDefaultSaveDir() {
    if (!config) {
      return;
    }
    if (!window.moodDownloadBridge?.app.pickDirectory) {
      pushToast("当前环境不支持目录选择器", "warning");
      return;
    }
    try {
      const selectedDirectory = await window.moodDownloadBridge.app.pickDirectory(config.defaultSaveDir || undefined);
      if (selectedDirectory) {
        updateField("defaultSaveDir", selectedDirectory);
      }
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "打开目录选择器失败", "danger");
    }
  }

  async function submitConfig() {
    if (!config) {
      return;
    }
    if (!config.defaultSaveDir.trim()) {
      pushToast("默认保存目录不能为空", "warning");
      return;
    }
    if (config.maxConcurrentDownloads <= 0) {
      pushToast("最大并发下载数必须大于 0", "warning");
      return;
    }

    try {
      setSaving(true);
      const updatedConfig = await updateDownloadConfig(buildPayload(config));
      setConfig(updatedConfig);
      syncConfig(updatedConfig);
      setSuccessMessage("基础配置已保存。");
      pushToast("设置已保存", "success");
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "设置保存失败", "danger");
    } finally {
      setSaving(false);
    }
  }

  /**
   * 保存 aria2 启动命令，并在保存成功后询问用户是否立即重启引擎。
   *
   * @returns Promise<void>
   */
  async function submitAria2Command() {
    const effectiveProfileCode = selectedProfileCode.trim() || runtimeSnapshot?.activeProfileCode || "default";

    try {
      setSaving(true);
      const parsedCommand = parseAria2Command(aria2Command);
      const nextSnapshot = await updateEngineRuntimeProfile({
        profileCode: effectiveProfileCode,
        profileJson: parsedCommand.profileJson
      });
      const trackerSetCode = resolveTrackerSetCode(nextSnapshot, trackerSets, effectiveProfileCode);
      await updateTrackerSet(trackerSetCode, buildTrackerPayload(trackerSets, trackerSetCode, parsedCommand.trackerListText));
      await reloadRuntimeConfigs();
      setPendingRestartPayload(parsedCommand);
      setRestartConfirmOpen(true);
      pushToast("aria2 配置已保存，请确认是否立即重启引擎", "success");
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "aria2 配置保存失败", "danger");
    } finally {
      setSaving(false);
    }
  }

  function closeRestartConfirm() {
    setRestartConfirmOpen(false);
    setPendingRestartPayload(null);
    pushToast("aria2 配置已保存，当前未重启引擎", "warning");
  }

  /**
   * 调用 Electron 主进程重启托管 aria2，并等待新的启动命令重新生效。
   *
   * @returns Promise<void>
   */
  async function restartAria2Engine() {
    if (!pendingRestartPayload) {
      pushToast("当前没有可重启的 aria2 配置", "warning");
      return;
    }
    if (!window.moodDownloadBridge?.app.restartAria2Engine) {
      pushToast("当前环境不支持重启 aria2 引擎", "warning");
      return;
    }

    try {
      setRestartingAria2Engine(true);
      const restartResult = await window.moodDownloadBridge.app.restartAria2Engine(pendingRestartPayload);
      if (restartResult) {
        pushToast(restartResult, "danger");
        return;
      }
      await reloadRuntimeConfigs();
      setRestartConfirmOpen(false);
      setPendingRestartPayload(null);
      pushToast("aria2 引擎已重启，新的启动命令已生效", "success");
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "重启 aria2 引擎失败", "danger");
    } finally {
      setRestartingAria2Engine(false);
    }
  }

  return (
    <>
      <section className="page-header">
        <div>
          <h1>设置</h1>
          <p>配置按基础设置和 aria2 启动命令拆开，减少不同配置域混在一起。</p>
        </div>
        <div className="capture-card page-header-card">
          <strong>运行参数基线</strong>
          <div className="page-header-pills">
            <span className="header-pill">本地配置</span>
            <span className="header-pill header-pill--success">aria2 启动命令</span>
          </div>
          <span>aria2 页签现在只保留启动命令输入框，保存后再决定是否立即重启引擎。</span>
        </div>
      </section>

      <div aria-label="设置页签" className="settings-tabs" role="tablist">
        {SETTINGS_TABS.map((tab) => (
          <button
            key={tab.key}
            aria-selected={activeTab === tab.key}
            className={activeTab === tab.key ? "settings-tab settings-tab--active" : "settings-tab"}
            onClick={() => setActiveTab(tab.key)}
            role="tab"
            type="button"
          >
            <strong>{tab.label}</strong>
            <span>{tab.description}</span>
          </button>
        ))}
      </div>

      <div className="settings-tab-panel">
        {activeTab === "basic" ? (
          <BasicSettingsSection
            config={config}
            errorMessage={errorMessage}
            loading={loading}
            onSelectDefaultSaveDir={selectDefaultSaveDir}
            onSubmit={submitConfig}
            onUpdateField={updateField}
            saving={saving}
            successMessage={successMessage}
          />
        ) : null}

        {activeTab === "aria2" ? (
          <Aria2SettingsSection
            aria2Command={aria2Command}
            onChangeCommand={setAria2Command}
            onSubmit={submitAria2Command}
            runtimeSnapshot={runtimeSnapshot}
            saving={saving}
          />
        ) : null}
      </div>

      <Aria2RestartConfirmDialog
        busy={restartingAria2Engine}
        onCancel={closeRestartConfirm}
        onConfirm={restartAria2Engine}
        open={restartConfirmOpen}
      />
    </>
  );
}
