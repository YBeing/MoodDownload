import { useTaskEvents } from "@/app/providers/TaskEventsProvider";
import { formatSpeed } from "@/shared/utils/formatters";

interface TaskSummaryBarProps {
  totalTasks: number;
  visibleTasks: number;
  activeTasks: number;
  totalSpeedBps: number;
}

export function TaskSummaryBar(props: TaskSummaryBarProps) {
  const { status, statusMessage } = useTaskEvents();

  return (
    <div className="summary-grid">
      <div className="summary-card">
        <span>总任务数</span>
        <strong>{props.totalTasks}</strong>
      </div>
      <div className="summary-card">
        <span>当前页任务</span>
        <strong>{props.visibleTasks}</strong>
      </div>
      <div className="summary-card">
        <span>活跃任务</span>
        <strong>{props.activeTasks}</strong>
      </div>
      <div className="summary-card">
        <span>{status === "connected" ? "总下载速度" : "SSE 状态"}</span>
        <strong>{status === "connected" ? formatSpeed(props.totalSpeedBps) : statusMessage}</strong>
      </div>
    </div>
  );
}
