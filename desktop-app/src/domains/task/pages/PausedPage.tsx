import { TaskRouteView } from "@/domains/task/components/TaskRouteView";

export function PausedPage() {
  return (
    <TaskRouteView
      description="聚焦可恢复任务，继续操作优先于删除操作。"
      title="已暂停"
      viewKey="paused"
    />
  );
}
