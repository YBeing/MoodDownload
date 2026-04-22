import { createContext, useContext, useEffect, useRef, useState, type PropsWithChildren } from "react";
import { getDownloadConfig } from "@/domains/config/api/configApi";
import type { DownloadConfig } from "@/domains/config/models/config";
import { taskStore } from "@/domains/task/store/task-store";
import type { TaskListItem } from "@/domains/task/models/task";
import { useShell } from "@/domains/shell/hooks/useShell";
import { createLogger } from "@/shared/utils/logger";

const logger = createLogger("capture-context");
const CLIPBOARD_POLL_INTERVAL_MS = 2800;
const DOWNLOAD_URI_PATTERN = /(magnet:\?\S+|https?:\/\/\S+|bt:\/\/\S+)/i;

interface ClipboardPromptState {
  visible: boolean;
  clipboardText: string;
  detectedUrl: string;
}

interface ExternalCaptureNotice {
  visible: boolean;
  taskId: number;
  displayName: string;
  taskCode: string;
  sourceType: string;
}

interface CaptureContextValue {
  browserCaptureEnabled: boolean;
  clipboardMonitorEnabled: boolean;
  clipboardBridgeAvailable: boolean;
  clipboardPollingActive: boolean;
  clipboardPrompt: ClipboardPromptState;
  externalCaptureNotice: ExternalCaptureNotice | null;
  lastDetectedClipboardUrl: string;
  lastExternalTaskName: string;
  syncConfig: (config: DownloadConfig) => void;
  markTaskAsLocallyInitiated: (taskId: number) => void;
  confirmClipboardPrompt: () => void;
  dismissClipboardPrompt: () => void;
  openExternalCaptureNotice: () => void;
  dismissExternalCaptureNotice: () => void;
}

const CaptureContext = createContext<CaptureContextValue>({
  browserCaptureEnabled: false,
  clipboardMonitorEnabled: false,
  clipboardBridgeAvailable: false,
  clipboardPollingActive: false,
  clipboardPrompt: {
    visible: false,
    clipboardText: "",
    detectedUrl: ""
  },
  externalCaptureNotice: null,
  lastDetectedClipboardUrl: "",
  lastExternalTaskName: "",
  syncConfig() {},
  markTaskAsLocallyInitiated() {},
  confirmClipboardPrompt() {},
  dismissClipboardPrompt() {},
  openExternalCaptureNotice() {},
  dismissExternalCaptureNotice() {}
});

function trimTrailingDelimiters(candidate: string) {
  let normalizedCandidate = candidate;
  while (/[.,;\])]+$/.test(normalizedCandidate)) {
    normalizedCandidate = normalizedCandidate.slice(0, -1);
  }
  return normalizedCandidate;
}

function extractDownloadUrl(clipboardText: string) {
  const matchedValue = clipboardText.match(DOWNLOAD_URI_PATTERN)?.[1] || "";
  return matchedValue ? trimTrailingDelimiters(matchedValue) : "";
}

function resolveDraftMode(detectedUrl: string) {
  return detectedUrl.startsWith("magnet:?") ? "magnet" : "url";
}

function isBrowserCaptureLikeTask(task: TaskListItem) {
  return ["HTTP", "HTTPS", "MAGNET", "BT"].includes(task.sourceType);
}

/**
 * 浏览器接管成功后，主动恢复并聚焦桌面主窗口，便于用户立即看到新任务。
 *
 * @returns Promise<void>
 */
async function revealDesktopWindowForCapture() {
  if (!window.moodDownloadBridge?.window.showAndFocus) {
    return;
  }
  try {
    await window.moodDownloadBridge.window.showAndFocus();
  } catch (error) {
    logger.warn("恢复主窗口失败", error);
  }
}

export function CaptureProvider({ children }: PropsWithChildren) {
  const { openCreateTask, openTaskDetail, pushToast } = useShell();
  const [browserCaptureEnabled, setBrowserCaptureEnabled] = useState(false);
  const [clipboardMonitorEnabled, setClipboardMonitorEnabled] = useState(false);
  const [clipboardPrompt, setClipboardPrompt] = useState<ClipboardPromptState>({
    visible: false,
    clipboardText: "",
    detectedUrl: ""
  });
  const [externalCaptureNotice, setExternalCaptureNotice] = useState<ExternalCaptureNotice | null>(null);
  const [lastDetectedClipboardUrl, setLastDetectedClipboardUrl] = useState("");
  const [lastExternalTaskName, setLastExternalTaskName] = useState("");
  const [clipboardPollingActive, setClipboardPollingActive] = useState(false);

  const knownTaskIdsRef = useRef<Set<number>>(new Set());
  const localTaskIdsRef = useRef<Set<number>>(new Set());
  const dismissedClipboardUrlRef = useRef("");
  const hasHydratedTaskIdsRef = useRef(false);

  const clipboardBridgeAvailable = typeof window.moodDownloadBridge?.clipboard.readText === "function";

  useEffect(() => {
    void taskStore.ensureHydrated();
  }, []);

  useEffect(() => {
    let disposed = false;

    async function loadCaptureConfig() {
      try {
        const downloadConfig = await getDownloadConfig();
        if (disposed) {
          return;
        }
        setBrowserCaptureEnabled(downloadConfig.browserCaptureEnabled);
        setClipboardMonitorEnabled(downloadConfig.clipboardMonitorEnabled);
      } catch (error) {
        logger.warn("初始化 capture 配置失败，将保持默认关闭态", error);
      }
    }

    void loadCaptureConfig();
    return () => {
      disposed = true;
    };
  }, []);

  useEffect(() => {
    const unsubscribe = taskStore.subscribe(() => {
      const taskState = taskStore.getSnapshot();
      if (!taskState.initialized) {
        return;
      }

      const currentTaskIds = new Set(taskState.items.map((task) => task.taskId));
      if (!hasHydratedTaskIdsRef.current) {
        knownTaskIdsRef.current = currentTaskIds;
        hasHydratedTaskIdsRef.current = true;
        return;
      }

      const newExternalTask = taskState.items.find((task) => {
        if (knownTaskIdsRef.current.has(task.taskId)) {
          return false;
        }
        if (localTaskIdsRef.current.has(task.taskId)) {
          localTaskIdsRef.current.delete(task.taskId);
          return false;
        }
        return isBrowserCaptureLikeTask(task);
      });

      knownTaskIdsRef.current = currentTaskIds;

      if (!newExternalTask || !browserCaptureEnabled) {
        return;
      }

      void revealDesktopWindowForCapture();
      openTaskDetail(newExternalTask.taskId);
      setLastExternalTaskName(newExternalTask.displayName);
      setExternalCaptureNotice({
        visible: true,
        taskId: newExternalTask.taskId,
        displayName: newExternalTask.displayName,
        taskCode: newExternalTask.taskCode,
        sourceType: newExternalTask.sourceType
      });
    });

    return unsubscribe;
  }, [browserCaptureEnabled]);

  useEffect(() => {
    if (!clipboardMonitorEnabled || !clipboardBridgeAvailable) {
      setClipboardPollingActive(false);
      return;
    }

    let disposed = false;

    async function scanClipboard() {
      try {
        const clipboardText = window.moodDownloadBridge?.clipboard.readText() || "";
        if (!clipboardText.trim()) {
          return;
        }
        const detectedUrl = extractDownloadUrl(clipboardText);
        if (!detectedUrl || dismissedClipboardUrlRef.current === detectedUrl) {
          return;
        }
        if (document.hidden || !document.hasFocus()) {
          return;
        }

        setLastDetectedClipboardUrl(detectedUrl);
        setClipboardPrompt((currentPrompt) => {
          if (currentPrompt.detectedUrl === detectedUrl && currentPrompt.visible) {
            return currentPrompt;
          }
          return {
            visible: true,
            clipboardText,
            detectedUrl
          };
        });
      } catch (error) {
        if (!disposed) {
          logger.warn("读取系统剪贴板失败", error);
        }
      }
    }

    setClipboardPollingActive(true);
    void scanClipboard();
    const timerId = window.setInterval(() => {
      void scanClipboard();
    }, CLIPBOARD_POLL_INTERVAL_MS);

    return () => {
      disposed = true;
      setClipboardPollingActive(false);
      window.clearInterval(timerId);
    };
  }, [clipboardBridgeAvailable, clipboardMonitorEnabled]);

  function syncConfig(config: DownloadConfig) {
    setBrowserCaptureEnabled(config.browserCaptureEnabled);
    setClipboardMonitorEnabled(config.clipboardMonitorEnabled);
    if (!config.browserCaptureEnabled) {
      setExternalCaptureNotice(null);
    }
    if (!config.clipboardMonitorEnabled) {
      setClipboardPrompt({
        visible: false,
        clipboardText: "",
        detectedUrl: ""
      });
    }
  }

  function markTaskAsLocallyInitiated(taskId: number) {
    localTaskIdsRef.current.add(taskId);
  }

  function dismissClipboardPrompt() {
    if (clipboardPrompt.detectedUrl) {
      dismissedClipboardUrlRef.current = clipboardPrompt.detectedUrl;
    }
    setClipboardPrompt({
      visible: false,
      clipboardText: "",
      detectedUrl: ""
    });
  }

  function confirmClipboardPrompt() {
    if (!clipboardPrompt.detectedUrl) {
      return;
    }
    dismissedClipboardUrlRef.current = clipboardPrompt.detectedUrl;
    openCreateTask({
      mode: resolveDraftMode(clipboardPrompt.detectedUrl),
      source: "clipboard",
      sourceUri: clipboardPrompt.detectedUrl
    });
    pushToast("已将剪贴板中的下载地址带入新建任务弹窗", "info");
    dismissClipboardPrompt();
  }

  function dismissExternalCaptureNotice() {
    setExternalCaptureNotice(null);
  }

  function openExternalCaptureNotice() {
    if (!externalCaptureNotice) {
      return;
    }
    openTaskDetail(externalCaptureNotice.taskId);
    dismissExternalCaptureNotice();
  }

  return (
    <CaptureContext.Provider
      value={{
        browserCaptureEnabled,
        clipboardMonitorEnabled,
        clipboardBridgeAvailable,
        clipboardPollingActive,
        clipboardPrompt,
        externalCaptureNotice,
        lastDetectedClipboardUrl,
        lastExternalTaskName,
        syncConfig,
        markTaskAsLocallyInitiated,
        confirmClipboardPrompt,
        dismissClipboardPrompt,
        openExternalCaptureNotice,
        dismissExternalCaptureNotice
      }}
    >
      {children}
    </CaptureContext.Provider>
  );
}

export function useCapture() {
  return useContext(CaptureContext);
}
