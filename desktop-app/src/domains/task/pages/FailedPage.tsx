import { TaskRouteView } from "@/domains/task/components/TaskRouteView";

export function FailedPage() {
  return (
    <TaskRouteView
      description="强调错误可见性与恢复入口，为后续故障诊断体验留出空间。"
      title="失败"
      viewKey="failed"
    />
  );
}
