/**
 * 将字节数格式化为桌面下载器场景更易读的容量文本。
 *
 * @param value 原始字节数
 * @returns 格式化结果
 */
export function formatBytes(value?: number | null) {
  if (!value || value <= 0) {
    return "0 B";
  }
  const units = ["B", "KB", "MB", "GB", "TB"];
  let current = value;
  let unitIndex = 0;
  while (current >= 1024 && unitIndex < units.length - 1) {
    current /= 1024;
    unitIndex += 1;
  }
  return `${current.toFixed(current >= 100 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

export function formatSpeed(value?: number | null) {
  return `${formatBytes(value)}/s`;
}

export function formatDateTime(timestamp?: number | null) {
  if (!timestamp) {
    return "暂无";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(timestamp);
}

export function formatProgress(progress?: number | null) {
  if (!progress || progress <= 0) {
    return "0.0%";
  }
  return `${(progress * 100).toFixed(1)}%`;
}
