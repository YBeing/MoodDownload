import { useState } from "react";
import { useTaskEvents } from "@/app/providers/TaskEventsProvider";
import { TaskDeleteDialog } from "@/domains/task/components/TaskDeleteDialog";
import { TaskSummaryBar } from "@/domains/task/components/TaskSummaryBar";
import { TaskTable } from "@/domains/task/components/TaskTable";
import { useTaskCollection } from "@/domains/task/hooks/useTaskCollection";
import { useShell } from "@/domains/shell/hooks/useShell";
import type { TaskDeleteMode, TaskListItem } from "@/domains/task/models/task";
import type { TaskRouteKey } from "@/shared/constants/navigation";

interface TaskRouteViewProps {
  viewKey: TaskRouteKey;
  title: string;
  description: string;
}

export function TaskRouteView(props: TaskRouteViewProps) {
  const [keyword, setKeyword] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<TaskListItem | null>(null);
  const { openCreateTask } = useShell();
  const { status, statusMessage } = useTaskEvents();
  const taskCollection = useTaskCollection(props.viewKey);

  const realtimeLabelMap = {
    connected: "实时同步正常",
    connecting: "正在建立连接",
    reconnecting: "连接恢复中",
    idle: "等待连接",
    error: "连接异常"
  } as const;

  const realtimeToneClass = status === "connected"
    ? "header-pill header-pill--success"
    : ["reconnecting", "error"].includes(status)
      ? "header-pill header-pill--warning"
      : "header-pill";

  const filteredTasks = taskCollection.visibleTasks.filter((task) => {
    if (!keyword.trim()) {
      return true;
    }
    return `${task.displayName} ${task.taskCode}`.toLowerCase().includes(keyword.trim().toLowerCase());
  });

  return (
    <>
      <section className="page-header">
        <div>
          <h1>{props.title}</h1>
          <p>{props.description}</p>
        </div>
        <div className="capture-card page-header-card">
          <strong>任务面板</strong>
          <div className="page-header-pills">
            <span className="header-pill">{props.title}</span>
            <span className={realtimeToneClass}>{realtimeLabelMap[status]}</span>
          </div>
          <span>{statusMessage}</span>
        </div>
      </section>

      {["reconnecting", "error"].includes(status) ? (
        <div className="realtime-banner">
          <strong>实时连接波动</strong>
          <span>{statusMessage}，列表仍可使用，系统会自动完成重连和静默对账。</span>
        </div>
      ) : null}

      <TaskSummaryBar {...taskCollection.summary} />

      <section className="task-list-panel">
        <div className="panel-header" style={{ marginBottom: 16 }}>
          <div>
            <h2>{props.title}视图</h2>
          </div>
          {taskCollection.refreshing ? <span className="caption-text">后台对账中...</span> : null}
        </div>

        <div className="task-toolbar">
          <input
            className="field"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索任务名称或任务编码"
            value={keyword}
          />
          <button className="button-ghost" onClick={() => void taskCollection.reloadTasks()} type="button">
            刷新
          </button>
          <button className="button-ghost" onClick={() => openCreateTask()} type="button">
            新建任务
          </button>
        </div>

        {taskCollection.loading ? (
          <div className="empty-state">正在读取任务列表...</div>
        ) : null}

        {!taskCollection.loading && taskCollection.errorMessage ? (
          <div className="error-state">
              <div>
                <strong>任务列表加载失败</strong>
                <p>{taskCollection.errorMessage}</p>
              </div>
              <div className="panel-actions">
                <button className="button" onClick={() => void taskCollection.reloadTasks()} type="button">
                  重试
                </button>
              </div>
            </div>
          ) : null}

        {!taskCollection.loading && !taskCollection.errorMessage && filteredTasks.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state__content">
              <strong>当前分组暂无任务</strong>
              <p>{props.description}</p>
              <div className="panel-actions">
                <button className="button" onClick={() => openCreateTask()} type="button">
                  新建任务
                </button>
                <button className="button-ghost" onClick={() => void taskCollection.reloadTasks()} type="button">
                  重新拉取
                </button>
              </div>
            </div>
          </div>
        ) : null}

        {!taskCollection.loading && !taskCollection.errorMessage && filteredTasks.length > 0 ? (
          <TaskTable
            actionFeedbacks={taskCollection.actionFeedbacks}
            busyTaskId={taskCollection.busyTaskId}
            items={filteredTasks}
            onDelete={(task) => setDeleteTarget(task)}
            onOpenFolder={taskCollection.openTaskFolder}
            onOpenDetail={taskCollection.openTaskDetail}
            onPrimaryAction={taskCollection.runPrimaryAction}
            resolvePrimaryActionLabel={taskCollection.resolvePrimaryActionLabel}
          />
        ) : null}
      </section>

      <TaskDeleteDialog
        busy={taskCollection.busyTaskId === deleteTarget?.taskId}
        onClose={() => setDeleteTarget(null)}
        onConfirm={async (deleteMode: TaskDeleteMode) => {
          if (!deleteTarget) {
            return;
          }
          await taskCollection.removeTask(deleteTarget, deleteMode);
          setDeleteTarget(null);
        }}
        open={Boolean(deleteTarget)}
        taskDisplayName={deleteTarget?.displayName || ""}
        taskId={deleteTarget?.taskId || null}
      />
    </>
  );
}
