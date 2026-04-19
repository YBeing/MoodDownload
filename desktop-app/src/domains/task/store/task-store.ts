import { useSyncExternalStore } from "react";
import { listTasks } from "@/domains/task/api/taskApi";
import type { TaskCommandResult, TaskListItem, TaskListPage, TaskSseEvent } from "@/domains/task/models/task";
import { createLogger } from "@/shared/utils/logger";

const logger = createLogger("task-store");
const TASK_LIST_PAGE_SIZE = 200;
const SILENT_RELOAD_DELAY_MS = 360;

interface TaskStoreState {
  items: TaskListItem[];
  total: number;
  pageNo: number;
  pageSize: number;
  loading: boolean;
  refreshing: boolean;
  initialized: boolean;
  errorMessage: string;
  lastEventTimestamp: number;
}

type Listener = () => void;

const listeners = new Set<Listener>();
let silentReloadTimer: number | null = null;
let activeRequestId = 0;

let state: TaskStoreState = {
  items: [],
  total: 0,
  pageNo: 1,
  pageSize: TASK_LIST_PAGE_SIZE,
  loading: false,
  refreshing: false,
  initialized: false,
  errorMessage: "",
  lastEventTimestamp: 0
};

function emitChange() {
  listeners.forEach((listener) => listener());
}

function setState(nextState: TaskStoreState) {
  state = nextState;
  emitChange();
}

function updateState(updater: (currentState: TaskStoreState) => TaskStoreState) {
  setState(updater(state));
}

function sortTaskItems(taskItems: TaskListItem[]) {
  return [...taskItems].sort((left, right) => {
    if (right.updatedAt !== left.updatedAt) {
      return right.updatedAt - left.updatedAt;
    }
    return right.taskId - left.taskId;
  });
}

function normalizeTaskPage(taskPage: TaskListPage): TaskStoreState {
  return {
    ...state,
    items: sortTaskItems(taskPage.items || []),
    total: taskPage.total ?? taskPage.items?.length ?? 0,
    pageNo: taskPage.pageNo || 1,
    pageSize: taskPage.pageSize || TASK_LIST_PAGE_SIZE,
    loading: false,
    refreshing: false,
    initialized: true,
    errorMessage: ""
  };
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "任务列表加载失败";
}

/**
 * 统一拉取任务列表快照，供四个任务页共享，避免路由切换后重复发起首屏请求。
 *
 * @param options 请求控制项
 */
async function reloadTasks(options: { silent?: boolean; reason?: string } = {}) {
  const { silent = false, reason = "manual" } = options;
  const requestId = ++activeRequestId;

  updateState((currentState) => ({
    ...currentState,
    loading: silent ? currentState.loading : true,
    refreshing: silent,
    errorMessage: silent ? currentState.errorMessage : ""
  }));

  try {
    const taskPage = await listTasks({
      pageNo: 1,
      pageSize: TASK_LIST_PAGE_SIZE
    });
    if (requestId !== activeRequestId) {
      return;
    }
    setState(normalizeTaskPage(taskPage));
  } catch (error) {
    if (requestId !== activeRequestId) {
      return;
    }

    logger.warn("任务列表刷新失败", {
      reason,
      silent,
      error
    });

    updateState((currentState) => ({
      ...currentState,
      loading: false,
      refreshing: false,
      initialized: true,
      errorMessage: silent ? currentState.errorMessage : resolveErrorMessage(error)
    }));
  }
}

function mutateTask(taskId: number, updater: (task: TaskListItem) => TaskListItem | null) {
  updateState((currentState) => {
    const taskIndex = currentState.items.findIndex((task) => task.taskId === taskId);
    if (taskIndex < 0) {
      return currentState;
    }

    const nextItems = [...currentState.items];
    const nextTask = updater(nextItems[taskIndex]);
    if (!nextTask) {
      nextItems.splice(taskIndex, 1);
      return {
        ...currentState,
        items: nextItems,
        total: Math.max(currentState.total - 1, 0)
      };
    }

    nextItems[taskIndex] = nextTask;
    return {
      ...currentState,
      items: sortTaskItems(nextItems)
    };
  });
}

/**
 * 使用 SSE 事件对已存在的任务做轻量补丁；如果本地没有该任务，则触发静默对账。
 *
 * @param taskEvent 后端推送的任务更新事件
 */
function applyTaskEvent(taskEvent: TaskSseEvent) {
  if (!taskEvent || taskEvent.timestamp <= state.lastEventTimestamp) {
    return;
  }

  const targetTask = state.items.find((task) => task.taskId === taskEvent.taskId);
  if (!targetTask) {
    scheduleSilentReload("missing-task-snapshot");
    updateState((currentState) => ({
      ...currentState,
      lastEventTimestamp: taskEvent.timestamp
    }));
    return;
  }

  mutateTask(taskEvent.taskId, (task) => ({
    ...task,
    domainStatus: taskEvent.domainStatus,
    engineStatus: taskEvent.engineStatus,
    progress: taskEvent.progress,
    downloadSpeedBps: taskEvent.downloadSpeedBps,
    updatedAt: taskEvent.timestamp
  }));

  updateState((currentState) => ({
    ...currentState,
    lastEventTimestamp: taskEvent.timestamp
  }));
}

function scheduleSilentReload(reason: string) {
  if (silentReloadTimer) {
    return;
  }

  silentReloadTimer = window.setTimeout(() => {
    silentReloadTimer = null;
    void reloadTasks({
      silent: true,
      reason
    });
  }, SILENT_RELOAD_DELAY_MS);
}

/**
 * 用命令接口的返回值更新本地任务快照，缩短按钮操作后的 UI 反馈链路。
 *
 * @param taskId 任务 ID
 * @param commandResult 命令接口返回值
 */
function applyCommandResult(taskId: number, commandResult: TaskCommandResult) {
  mutateTask(taskId, (task) => ({
    ...task,
    domainStatus: commandResult.domainStatus,
    updatedAt: Date.now()
  }));
  scheduleSilentReload("command-reconcile");
}

function removeTask(taskId: number) {
  mutateTask(taskId, () => null);
}

async function ensureHydrated() {
  if (state.initialized || state.loading) {
    return;
  }
  await reloadTasks({
    reason: "initial-hydration"
  });
}

function subscribe(listener: Listener) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

function getSnapshot() {
  return state;
}

export const taskStore = {
  subscribe,
  getSnapshot,
  ensureHydrated,
  reloadTasks,
  applyTaskEvent,
  applyCommandResult,
  removeTask,
  scheduleSilentReload
};

export function useTaskStore() {
  return useSyncExternalStore(taskStore.subscribe, taskStore.getSnapshot, taskStore.getSnapshot);
}
