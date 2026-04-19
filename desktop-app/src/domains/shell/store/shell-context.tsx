import { createContext, useContext, useEffect, useRef, useState, type PropsWithChildren } from "react";
import { createLogger } from "@/shared/utils/logger";

export type ToastTone = "info" | "success" | "warning" | "danger";
export type TaskCreateDraftMode = "url" | "magnet" | "torrent";
export type TaskCreateDraftSource = "manual" | "clipboard";
const logger = createLogger("shell-context");
const TOAST_AUTO_DISMISS_MS = 4200;
const TOAST_DEDUP_WINDOW_MS = 2200;

export interface ShellWindowState {
  isFocused: boolean;
  isMaximized: boolean;
  isMinimized: boolean;
  isVisible: boolean;
  trayAvailable: boolean;
}

export interface TaskCreateDraft {
  mode?: TaskCreateDraftMode;
  sourceUri?: string;
  source?: TaskCreateDraftSource;
}

export interface ToastInput {
  title: string;
  message?: string;
  tone?: ToastTone;
  key?: string;
  durationMs?: number;
}

export interface ToastItem {
  id: string;
  title: string;
  message?: string;
  tone: ToastTone;
  key: string;
}

interface ShellContextValue {
  isCreateTaskOpen: boolean;
  createTaskDraft: TaskCreateDraft | null;
  detailTaskId: number | null;
  toasts: ToastItem[];
  windowState: ShellWindowState;
  openCreateTask: (draft?: TaskCreateDraft) => void;
  closeCreateTask: () => void;
  openTaskDetail: (taskId: number) => void;
  closeTaskDetail: () => void;
  pushToast: (input: string | ToastInput, tone?: ToastTone) => void;
  dismissToast: (toastId: string) => void;
  minimizeWindow: () => Promise<void>;
  toggleWindowMaximize: () => Promise<boolean>;
  closeWindow: () => Promise<void>;
  minimizeToTray: () => Promise<void>;
}

const initialWindowState: ShellWindowState = {
  isFocused: true,
  isMaximized: false,
  isMinimized: false,
  isVisible: true,
  trayAvailable: false
};

/**
 * 将字符串形式的旧 toast 调用归一化，统一为标题 / 副文案结构。
 *
 * @param input 旧字符串或新对象格式
 * @param fallbackTone 兼容旧签名的默认提示语气
 * @returns 标准化后的 toast 配置
 */
function normalizeToastInput(input: string | ToastInput, fallbackTone: ToastTone): Required<ToastInput> {
  if (typeof input === "string") {
    return {
      title: input,
      message: "",
      tone: fallbackTone,
      key: `${fallbackTone}:${input}`,
      durationMs: TOAST_AUTO_DISMISS_MS
    };
  }

  return {
    title: input.title,
    message: input.message || "",
    tone: input.tone || fallbackTone,
    key: input.key || `${input.tone || fallbackTone}:${input.title}:${input.message || ""}`,
    durationMs: input.durationMs || TOAST_AUTO_DISMISS_MS
  };
}

const ShellContext = createContext<ShellContextValue>({
  isCreateTaskOpen: false,
  createTaskDraft: null,
  detailTaskId: null,
  toasts: [],
  windowState: initialWindowState,
  openCreateTask() {},
  closeCreateTask() {},
  openTaskDetail() {},
  closeTaskDetail() {},
  pushToast() {},
  dismissToast() {},
  minimizeWindow: async () => {},
  toggleWindowMaximize: async () => false,
  closeWindow: async () => {},
  minimizeToTray: async () => {}
});

export function ShellProvider({ children }: PropsWithChildren) {
  const [isCreateTaskOpen, setCreateTaskOpen] = useState(false);
  const [createTaskDraft, setCreateTaskDraft] = useState<TaskCreateDraft | null>(null);
  const [detailTaskId, setDetailTaskId] = useState<number | null>(null);
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const [windowState, setWindowState] = useState<ShellWindowState>(initialWindowState);
  const toastHistoryRef = useRef<Map<string, number>>(new Map());

  function dismissToast(toastId: string) {
    setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== toastId));
  }

  function pushToast(input: string | ToastInput, tone: ToastTone = "info") {
    const normalizedToast = normalizeToastInput(input, tone);
    const now = Date.now();
    const lastPushedAt = toastHistoryRef.current.get(normalizedToast.key) || 0;
    if (now - lastPushedAt < TOAST_DEDUP_WINDOW_MS) {
      return;
    }

    const toastId = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`;
    toastHistoryRef.current.set(normalizedToast.key, now);
    setToasts((currentToasts) => [
      ...currentToasts,
      {
        id: toastId,
        title: normalizedToast.title,
        message: normalizedToast.message,
        tone: normalizedToast.tone,
        key: normalizedToast.key
      }
    ]);
    window.setTimeout(() => {
      dismissToast(toastId);
    }, normalizedToast.durationMs);
  }

  useEffect(() => {
    if (!window.moodDownloadBridge?.window.getState) {
      return;
    }

    let disposed = false;

    /**
     * 同步 Electron 窗口状态，供标题栏与底栏统一消费。
     */
    async function syncWindowState() {
      try {
        const nextState = await window.moodDownloadBridge?.window.getState();
        if (!disposed && nextState) {
          setWindowState(nextState);
        }
      } catch (error) {
        logger.warn("读取窗口状态失败", error);
      }
    }

    const unsubscribe = window.moodDownloadBridge.window.onStateChange?.((nextState) => {
      if (disposed) {
        return;
      }

      setWindowState((currentState) => {
        if (!currentState.isVisible && nextState.isVisible && nextState.trayAvailable) {
          pushToast({
            title: "已恢复主窗口",
            message: "托盘后台任务继续保持运行，你可以继续查看任务状态。",
            tone: "info",
            key: "window-restored"
          });
        }
        return nextState;
      });
    });

    void syncWindowState();

    return () => {
      disposed = true;
      unsubscribe?.();
    };
  }, []);

  /**
   * 统一最小化调用，便于后续继续在壳层收口异常处理与统计埋点。
   */
  async function minimizeWindow() {
    try {
      await window.moodDownloadBridge?.window.minimize();
    } catch (error) {
      logger.warn("最小化窗口失败", error);
      pushToast({
        title: "最小化失败",
        message: error instanceof Error ? error.message : "请稍后重试",
        tone: "danger",
        key: "window-minimize-failed"
      });
    }
  }

  async function toggleWindowMaximize() {
    try {
      return (await window.moodDownloadBridge?.window.toggleMaximize()) || false;
    } catch (error) {
      logger.warn("切换窗口最大化状态失败", error);
      pushToast({
        title: "窗口操作失败",
        message: error instanceof Error ? error.message : "无法切换最大化状态",
        tone: "danger",
        key: "window-maximize-failed"
      });
      return false;
    }
  }

  async function closeWindow() {
    try {
      await window.moodDownloadBridge?.window.close();
    } catch (error) {
      logger.warn("关闭窗口失败", error);
      pushToast({
        title: "关闭窗口失败",
        message: error instanceof Error ? error.message : "请稍后重试",
        tone: "danger",
        key: "window-close-failed"
      });
    }
  }

  async function minimizeToTray() {
    try {
      await window.moodDownloadBridge?.window.minimizeToTray();
    } catch (error) {
      logger.warn("最小化到托盘失败", error);
      pushToast({
        title: "托盘收起失败",
        message: error instanceof Error ? error.message : "请确认当前环境支持系统托盘",
        tone: "danger",
        key: "window-tray-failed"
      });
    }
  }

  return (
    <ShellContext.Provider
      value={{
        isCreateTaskOpen,
        createTaskDraft,
        detailTaskId,
        toasts,
        windowState,
        openCreateTask(draft) {
          setCreateTaskDraft(draft || null);
          setCreateTaskOpen(true);
        },
        closeCreateTask() {
          setCreateTaskOpen(false);
          setCreateTaskDraft(null);
        },
        openTaskDetail(taskId: number) {
          setDetailTaskId(taskId);
        },
        closeTaskDetail() {
          setDetailTaskId(null);
        },
        pushToast,
        dismissToast,
        minimizeWindow,
        toggleWindowMaximize,
        closeWindow,
        minimizeToTray
      }}
    >
      {children}
    </ShellContext.Provider>
  );
}

export function useShellContext() {
  return useContext(ShellContext);
}
