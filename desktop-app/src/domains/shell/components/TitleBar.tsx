import { useShell } from "@/domains/shell/hooks/useShell";
import { useTaskEvents } from "@/app/providers/TaskEventsProvider";
import type { TaskStreamStatus } from "@/shared/api/task-event-stream";

function resolveStreamBadge(status: TaskStreamStatus) {
  if (status === "connected") {
    return {
      className: "badge badge--running",
      label: "SSE 已连接"
    };
  }
  if (status === "reconnecting") {
    return {
      className: "badge badge--paused",
      label: "SSE 重连中"
    };
  }
  if (status === "error") {
    return {
      className: "badge badge--failed",
      label: "SSE 异常"
    };
  }
  if (status === "idle") {
    return {
      className: "badge badge--pending",
      label: "SSE 未启动"
    };
  }
  return {
    className: "badge badge--pending",
    label: "SSE 连接中"
  };
}

export function TitleBar() {
  const { openCreateTask, windowState } = useShell();
  const { status } = useTaskEvents();
  const streamBadge = resolveStreamBadge(status);

  return (
    <header className="chrome-bar">
      <div className="chrome-brand">
        <span className="chrome-brand__badge">MD</span>
        <div className="chrome-brand__title">
          <strong>MoodDownload</strong>
        </div>
      </div>

      <div className="chrome-actions">
        <span className={streamBadge.className}>{streamBadge.label}</span>
        <span className={`badge badge--${windowState.isVisible ? "completed" : "paused"}`}>
          {windowState.isVisible ? (windowState.isFocused ? "窗口前台" : "窗口可见") : "托盘后台运行"}
        </span>
        {windowState.trayAvailable ? <span className="badge badge--pending">托盘已就绪</span> : null}
        <button className="button" onClick={() => openCreateTask()} type="button">
          新建任务
        </button>
      </div>
    </header>
  );
}
