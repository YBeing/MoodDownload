import { useTaskEvents } from "@/app/providers/TaskEventsProvider";
import { useShell } from "@/domains/shell/hooks/useShell";
import { getRuntimeConfig } from "@/shared/api/httpClient";

export function StatusFooter() {
  const { status, statusMessage, lastEvent } = useTaskEvents();
  const { windowState } = useShell();
  const runtimeConfig = getRuntimeConfig();

  return (
    <footer className="status-footer">
      <div className="status-footer__cluster">
        <span className={`status-dot status-dot--${status}`}>{statusMessage}</span>
        <span>本地服务：{runtimeConfig.serviceUrl}</span>
      </div>
      <div className="status-footer__cluster mono-text">
        <span>窗口：{windowState.isVisible ? (windowState.isFocused ? "前台" : "可见") : "托盘后台"}</span>
        <span>托盘：{windowState.trayAvailable ? "已就绪" : "未启用"}</span>
        <span>平台：{runtimeConfig.platform}</span>
        <span>最近事件：{lastEvent ? `${lastEvent.eventType} #${lastEvent.taskId}` : "暂无"}</span>
      </div>
    </footer>
  );
}
