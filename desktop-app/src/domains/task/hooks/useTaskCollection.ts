import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { deleteTask, getTaskOpenContext, pauseTask, resumeTask, retryTask } from "@/domains/task/api/taskApi";
import { useTaskEvents } from "@/app/providers/TaskEventsProvider";
import { useShell } from "@/domains/shell/hooks/useShell";
import type { TaskDeleteMode, TaskDomainStatus, TaskListItem } from "@/domains/task/models/task";
import { taskStore, useTaskStore } from "@/domains/task/store/task-store";
import type { TaskRouteKey } from "@/shared/constants/navigation";

const ACTION_FEEDBACK_DURATION_MS = 3600;

interface TaskActionFeedbackItem {
  message: string;
  tone: "success" | "warning" | "danger";
}

function isTaskVisible(task: TaskListItem, viewKey: TaskRouteKey) {
  if (viewKey === "running") {
    return ["PENDING", "DISPATCHING", "RUNNING"].includes(task.domainStatus);
  }
  if (viewKey === "completed") {
    return task.domainStatus === "COMPLETED";
  }
  if (viewKey === "paused") {
    return task.domainStatus === "PAUSED";
  }
  return task.domainStatus === "FAILED";
}

function resolvePrimaryActionLabel(status: TaskDomainStatus | string) {
  if (["RUNNING", "PENDING", "DISPATCHING"].includes(status)) {
    return "暂停";
  }
  if (status === "PAUSED") {
    return "继续";
  }
  if (status === "FAILED") {
    return "重试";
  }
  return null;
}

export function useTaskCollection(viewKey: TaskRouteKey) {
  const navigate = useNavigate();
  const { lastEvent } = useTaskEvents();
  const { pushToast, openTaskDetail } = useShell();
  const taskState = useTaskStore();
  const [busyTaskId, setBusyTaskId] = useState<number | null>(null);
  const [actionFeedbacks, setActionFeedbacks] = useState<Record<number, TaskActionFeedbackItem>>({});
  const feedbackTimersRef = useRef<Record<number, number>>({});
  const completionNavigationTimestampRef = useRef(lastEvent?.timestamp || 0);

  async function reloadTasks(silent = false) {
    await taskStore.reloadTasks({
      silent,
      reason: silent ? "route-silent-refresh" : "route-manual-refresh"
    });
  }

  useEffect(() => {
    void taskStore.ensureHydrated();
  }, []);

  useEffect(() => {
    if (!lastEvent) {
      return;
    }
    taskStore.applyTaskEvent(lastEvent);
    if (
      lastEvent.domainStatus === "COMPLETED"
      && lastEvent.timestamp > completionNavigationTimestampRef.current
      && viewKey !== "completed"
    ) {
      completionNavigationTimestampRef.current = lastEvent.timestamp;
      navigateToTaskView("completed");
      return;
    }
    completionNavigationTimestampRef.current = Math.max(completionNavigationTimestampRef.current, lastEvent.timestamp);
  }, [lastEvent?.timestamp]);

  useEffect(() => {
    return () => {
      Object.values(feedbackTimersRef.current).forEach((timerId) => window.clearTimeout(timerId));
    };
  }, []);

  /**
   * 在任务行内展示短时操作反馈，减少暂停 / 继续 / 重试后频繁弹出全局 toast 的噪音。
   *
   * @param taskId 任务 ID
   * @param feedback 反馈文案与语气
   */
  function showActionFeedback(taskId: number, feedback: TaskActionFeedbackItem) {
    if (feedbackTimersRef.current[taskId]) {
      window.clearTimeout(feedbackTimersRef.current[taskId]);
    }

    setActionFeedbacks((currentFeedbacks) => ({
      ...currentFeedbacks,
      [taskId]: feedback
    }));

    feedbackTimersRef.current[taskId] = window.setTimeout(() => {
      setActionFeedbacks((currentFeedbacks) => {
        const nextFeedbacks = { ...currentFeedbacks };
        delete nextFeedbacks[taskId];
        return nextFeedbacks;
      });
      delete feedbackTimersRef.current[taskId];
    }, ACTION_FEEDBACK_DURATION_MS);
  }

  /**
   * 根据任务操作结果切换到目标分组页面，避免用户在当前分组看到任务瞬间消失后无感知。
   *
   * @param targetViewKey 目标任务分组
   */
  function navigateToTaskView(targetViewKey: TaskRouteKey) {
    if (viewKey === targetViewKey) {
      return;
    }
    navigate(`/tasks/${targetViewKey}`);
  }

  async function runPrimaryAction(task: TaskListItem) {
    if (!["RUNNING", "PENDING", "DISPATCHING", "PAUSED", "FAILED"].includes(task.domainStatus)) {
      openTaskDetail(task.taskId);
      return;
    }

    try {
      setBusyTaskId(task.taskId);
      if (["RUNNING", "PENDING", "DISPATCHING"].includes(task.domainStatus)) {
        const commandResult = await pauseTask(task.taskId);
        taskStore.applyCommandResult(task.taskId, commandResult);
        showActionFeedback(task.taskId, {
          message: "暂停请求已提交，任务会在下一次同步后更新状态。",
          tone: "warning"
        });
        if (viewKey === "running") {
          navigateToTaskView("paused");
        }
      } else if (task.domainStatus === "PAUSED") {
        const commandResult = await resumeTask(task.taskId);
        taskStore.applyCommandResult(task.taskId, commandResult);
        showActionFeedback(task.taskId, {
          message: "继续请求已提交，任务会回到下载队列。",
          tone: "success"
        });
        if (viewKey === "paused") {
          navigateToTaskView("running");
        }
      } else if (task.domainStatus === "FAILED") {
        const commandResult = await retryTask(task.taskId);
        taskStore.applyCommandResult(task.taskId, commandResult);
        showActionFeedback(task.taskId, {
          message: "重试请求已提交，系统会重新拉起下载。",
          tone: "success"
        });
      }
    } catch (error) {
      showActionFeedback(task.taskId, {
        message: "操作失败，请稍后重试。",
        tone: "danger"
      });
      pushToast(error instanceof Error ? error.message : "任务操作失败", "danger");
    } finally {
      setBusyTaskId(null);
    }
  }

  async function openTaskFolder(task: TaskListItem) {
    try {
      const openContext = await getTaskOpenContext(task.taskId);
      if (!openContext.canOpen || !openContext.openFolderPath) {
        pushToast(openContext.reason || "当前任务没有可打开的目录", "warning");
        return;
      }
      if (!window.moodDownloadBridge?.app.openPath) {
        pushToast("当前环境不支持打开本地目录", "warning");
        return;
      }
      const openResult = await window.moodDownloadBridge.app.openPath(openContext.openFolderPath);
      if (openResult) {
        pushToast(openResult, "danger");
        return;
      }
      showActionFeedback(task.taskId, {
        message: "已打开下载目录。",
        tone: "success"
      });
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "打开文件夹失败", "danger");
    }
  }

  async function removeTask(task: TaskListItem, deleteMode: TaskDeleteMode) {
    try {
      setBusyTaskId(task.taskId);
      const deleteResult = await deleteTask(task.taskId, deleteMode);
      taskStore.removeTask(task.taskId);
      taskStore.scheduleSilentReload("delete-reconcile");
      pushToast(
        deleteResult.partialSuccess
          ? deleteResult.message || `任务已删除，但部分文件清理失败：${task.displayName}`
          : deleteResult.message || `已删除任务：${task.displayName}`,
        deleteResult.partialSuccess ? "warning" : "success"
      );
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "删除任务失败", "danger");
    } finally {
      setBusyTaskId(null);
    }
  }

  const visibleTasks = useMemo(
    () => taskState.items.filter((task) => isTaskVisible(task, viewKey)),
    [taskState.items, viewKey]
  );
  const summary = {
    totalTasks: taskState.total,
    visibleTasks: visibleTasks.length,
    activeTasks: taskState.items.filter((task) => ["PENDING", "DISPATCHING", "RUNNING"].includes(task.domainStatus))
      .length,
    totalSpeedBps: taskState.items.reduce((speed, task) => speed + (task.downloadSpeedBps || 0), 0)
  };

  return {
    taskItems: taskState.items,
    visibleTasks,
    loading: taskState.loading || !taskState.initialized,
    refreshing: taskState.refreshing,
    errorMessage: taskState.errorMessage,
    busyTaskId,
    actionFeedbacks,
    summary,
    reloadTasks,
    runPrimaryAction,
    openTaskFolder,
    removeTask,
    openTaskDetail,
    resolvePrimaryActionLabel
  };
}
