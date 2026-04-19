import { formatDateTime, formatProgress, formatSpeed } from "@/shared/utils/formatters";
import type { TaskListItem } from "@/domains/task/models/task";

interface TaskTableProps {
  actionFeedbacks: Record<number, { message: string; tone: "success" | "warning" | "danger" }>;
  items: TaskListItem[];
  busyTaskId: number | null;
  onOpenDetail: (taskId: number) => void;
  onPrimaryAction: (task: TaskListItem) => void;
  onDelete: (task: TaskListItem) => void;
  resolvePrimaryActionLabel: (status: string) => string | null;
}

function resolveBadgeClass(task: TaskListItem) {
  if (task.domainStatus === "RUNNING") {
    return "badge badge--running";
  }
  if (task.domainStatus === "COMPLETED") {
    return "badge badge--completed";
  }
  if (task.domainStatus === "PAUSED") {
    return "badge badge--paused";
  }
  if (task.domainStatus === "FAILED") {
    return "badge badge--failed";
  }
  return "badge badge--pending";
}

export function TaskTable(props: TaskTableProps) {
  if (props.items.length === 0) {
    return (
      <div className="empty-state">
        <div>
          <strong>当前分组暂无任务</strong>
          <p>壳层、路由和数据接入已经就位，后续阶段会继续补强列表体验。</p>
        </div>
      </div>
    );
  }

  return (
    <div className="task-list">
      {props.items.map((task) => {
        const isActive = ["PENDING", "RUNNING", "DISPATCHING"].includes(task.domainStatus);
        const isBusy = props.busyTaskId === task.taskId;
        const actionFeedback = props.actionFeedbacks[task.taskId];
        const primaryActionLabel = props.resolvePrimaryActionLabel(task.domainStatus);
        return (
          <div className={isActive ? "task-row task-row--active" : "task-row"} key={task.taskId}>
            <button className="task-name button-ghost" onClick={() => props.onOpenDetail(task.taskId)} type="button">
              <span className="task-name__icon">{task.sourceType.startsWith("TORRENT") ? "🧲" : "⇩"}</span>
              <span className="task-name__meta">
                <strong>{task.displayName}</strong>
                <span>{task.saveDir}</span>
              </span>
            </button>

            <div className="task-kv">
              <span className={resolveBadgeClass(task)}>{task.domainStatus}</span>
              <div className="progress-track">
                <div className="progress-fill" style={{ width: formatProgress(task.progress) }} />
              </div>
              <span>{formatProgress(task.progress)} / {task.engineStatus || "UNKNOWN"}</span>
            </div>

            <div className="task-kv">
              <strong>{formatSpeed(task.downloadSpeedBps)}</strong>
              <span>最后刷新：{formatDateTime(task.updatedAt)}</span>
            </div>

            <div className="task-row__action-stack">
              <div className="task-row__actions">
                {primaryActionLabel ? (
                  <button
                    className="button-ghost"
                    disabled={isBusy}
                    onClick={() => props.onPrimaryAction(task)}
                    type="button"
                  >
                    {primaryActionLabel}
                  </button>
                ) : null}
                <button className="button-ghost" onClick={() => props.onOpenDetail(task.taskId)} type="button">
                  详情
                </button>
                <button
                  className="button-danger"
                  disabled={isBusy}
                  onClick={() => props.onDelete(task)}
                  type="button"
                >
                  删除
                </button>
              </div>
              {actionFeedback ? (
                <span className={`task-action-feedback task-action-feedback--${actionFeedback.tone}`}>
                  {actionFeedback.message}
                </span>
              ) : null}
            </div>
          </div>
        );
      })}
    </div>
  );
}
