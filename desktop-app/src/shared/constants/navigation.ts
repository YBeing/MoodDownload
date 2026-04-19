export type TaskRouteKey = "running" | "completed" | "paused" | "failed";

export interface NavigationItem {
  key: TaskRouteKey | "settings";
  label: string;
  icon: string;
  path: string;
}

export const navigationItems: NavigationItem[] = [
  {
    key: "running",
    label: "下载中",
    icon: "⬇",
    path: "/tasks/running"
  },
  {
    key: "completed",
    label: "已完成",
    icon: "✔",
    path: "/tasks/completed"
  },
  {
    key: "paused",
    label: "已暂停",
    icon: "⏸",
    path: "/tasks/paused"
  },
  {
    key: "failed",
    label: "失败",
    icon: "⚠",
    path: "/tasks/failed"
  },
  {
    key: "settings",
    label: "设置",
    icon: "⚙",
    path: "/settings"
  }
];
