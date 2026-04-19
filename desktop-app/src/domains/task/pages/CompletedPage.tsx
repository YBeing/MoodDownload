import { TaskRouteView } from "@/domains/task/components/TaskRouteView";

export function CompletedPage() {
  return (
    <TaskRouteView
      description="更安静的历史视图，强调完成结果、保存目录与回看效率。"
      title="已完成"
      viewKey="completed"
    />
  );
}
