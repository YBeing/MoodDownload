import { useEffect, useState } from "react";
import { useCapture } from "@/domains/capture/store/capture-context";
import { getDownloadConfig, updateDownloadConfig } from "@/domains/config/api/configApi";
import type { DownloadConfig, UpdateDownloadConfigPayload } from "@/domains/config/models/config";
import { CaptureReadinessPanel } from "@/domains/capture/components/CaptureReadinessPanel";
import { useShell } from "@/domains/shell/hooks/useShell";
import { formatDateTime } from "@/shared/utils/formatters";

function buildPayload(config: DownloadConfig): UpdateDownloadConfigPayload {
  return {
    defaultSaveDir: config.defaultSaveDir,
    maxConcurrentDownloads: Number(config.maxConcurrentDownloads),
    maxGlobalDownloadSpeed: Number(config.maxGlobalDownloadSpeed),
    maxGlobalUploadSpeed: Number(config.maxGlobalUploadSpeed),
    browserCaptureEnabled: Boolean(config.browserCaptureEnabled),
    clipboardMonitorEnabled: Boolean(config.clipboardMonitorEnabled)
  };
}

export function SettingsPage() {
  const { pushToast } = useShell();
  const { syncConfig } = useCapture();
  const [config, setConfig] = useState<DownloadConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  useEffect(() => {
    let disposed = false;

    async function loadConfig() {
      try {
        setLoading(true);
        setErrorMessage("");
        setSuccessMessage("");
        const nextConfig = await getDownloadConfig();
        if (!disposed) {
          setConfig(nextConfig);
          syncConfig(nextConfig);
        }
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
  }, []);

  function updateField<K extends keyof DownloadConfig>(key: K, value: DownloadConfig[K]) {
    setSuccessMessage("");
    setConfig((currentConfig) => (currentConfig ? { ...currentConfig, [key]: value } : currentConfig));
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
      setSuccessMessage("配置已保存，运行态提示已同步更新。");
      pushToast("设置已保存", "success");
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "设置保存失败", "danger");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <section className="page-header">
        <div>
          <h1>设置</h1>
          <p>调整本地下载目录、并发限额、全局限速与接管行为，保存后立即生效。</p>
        </div>
        <div className="capture-card page-header-card">
          <strong>运行参数基线</strong>
          <div className="page-header-pills">
            <span className="header-pill">本地配置</span>
            <span className="header-pill header-pill--success">保存即生效</span>
          </div>
          <span>配置更新后无需重启应用，当前页面展示的都是运行时实际生效的参数。</span>
        </div>
      </section>

      <div className="settings-grid">
        <section className="settings-card">
          {loading ? <div className="empty-state">正在读取本地配置...</div> : null}

          {!loading && errorMessage ? (
            <div className="error-state">
              <div>
                <strong>配置加载失败</strong>
                <p>{errorMessage}</p>
              </div>
            </div>
          ) : null}

          {!loading && config ? (
            <>
              {successMessage ? (
                <div className="inline-banner inline-banner--success">
                  <strong>保存成功</strong>
                  <span>{successMessage}</span>
                </div>
              ) : null}

              <div className="settings-form">
                <label className="settings-field">
                  <span>默认下载目录</span>
                  <input
                    className="field"
                    onChange={(event) => updateField("defaultSaveDir", event.target.value)}
                    value={config.defaultSaveDir}
                  />
                </label>

                <label className="settings-field">
                  <span>最大并发下载数</span>
                  <input
                    className="field"
                    min={1}
                    onChange={(event) => updateField("maxConcurrentDownloads", Number(event.target.value))}
                    type="number"
                    value={config.maxConcurrentDownloads}
                  />
                </label>

                <label className="settings-field">
                  <span>全局下载限速（B/s）</span>
                  <input
                    className="field"
                    min={0}
                    onChange={(event) => updateField("maxGlobalDownloadSpeed", Number(event.target.value))}
                    type="number"
                    value={config.maxGlobalDownloadSpeed}
                  />
                </label>

                <label className="settings-field">
                  <span>全局上传限速（B/s）</span>
                  <input
                    className="field"
                    min={0}
                    onChange={(event) => updateField("maxGlobalUploadSpeed", Number(event.target.value))}
                    type="number"
                    value={config.maxGlobalUploadSpeed}
                  />
                </label>

                <div className="toggle-row">
                  <div>
                    <strong>浏览器接管</strong>
                    <span>开启后会对新入队的外部 HTTP / HTTPS 任务给出浏览器接管结果提示。</span>
                  </div>
                  <button
                    className={config.browserCaptureEnabled ? "toggle toggle--checked" : "toggle"}
                    onClick={() => updateField("browserCaptureEnabled", !config.browserCaptureEnabled)}
                    type="button"
                  />
                </div>

                <div className="toggle-row">
                  <div>
                    <strong>剪贴板监听</strong>
                    <span>开启后会轮询系统剪贴板中的下载地址，并可直接带入新建任务弹窗确认。</span>
                  </div>
                  <button
                    className={config.clipboardMonitorEnabled ? "toggle toggle--checked" : "toggle"}
                    onClick={() => updateField("clipboardMonitorEnabled", !config.clipboardMonitorEnabled)}
                    type="button"
                  />
                </div>
              </div>

              <div className="settings-actions">
                <span className="muted-text">最近更新时间：{formatDateTime(config.updatedAt)}</span>
                <button className="button" disabled={saving} onClick={() => void submitConfig()} type="button">
                  {saving ? "保存中..." : "保存设置"}
                </button>
              </div>
            </>
          ) : null}
        </section>

        <div className="settings-form">
          <CaptureReadinessPanel />
        </div>
      </div>
    </>
  );
}
