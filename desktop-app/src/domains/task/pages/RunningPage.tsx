import { TaskRouteView } from "@/domains/task/components/TaskRouteView";

export function RunningPage() {
  return (
    <TaskRouteView
      description="默认工作台，承载运行中、待调度与实时刷新的任务概览。"
      title="下载中"
      viewKey="running"
    />
  );
}
