import { useCapture } from "@/domains/capture/store/capture-context";

export function CaptureReadinessPanel() {
  const {
    browserCaptureEnabled,
    clipboardMonitorEnabled,
    clipboardBridgeAvailable,
    clipboardPollingActive,
    lastDetectedClipboardUrl,
    lastExternalTaskName
  } = useCapture();

  return (
    <section className="capture-card">
      <strong>接入提示运行态</strong>
      <div className="capture-readiness">
        <div className="drawer-kv">
          <span>浏览器接管</span>
          <strong>{browserCaptureEnabled ? "已开启" : "已关闭"}</strong>
        </div>
        <div className="drawer-kv">
          <span>剪贴板监听</span>
          <strong>{clipboardMonitorEnabled ? "已开启" : "已关闭"}</strong>
        </div>
        <div className="drawer-kv">
          <span>剪贴板桥接</span>
          <strong>{clipboardBridgeAvailable ? "可用" : "不可用"}</strong>
        </div>
        <div className="drawer-kv">
          <span>剪贴板轮询</span>
          <strong>{clipboardPollingActive ? "运行中" : "未运行"}</strong>
        </div>
        <div className="drawer-kv">
          <span>最近识别链接</span>
          <strong>{lastDetectedClipboardUrl || "暂无"}</strong>
        </div>
        <div className="drawer-kv">
          <span>最近外部接入</span>
          <strong>{lastExternalTaskName || "暂无"}</strong>
        </div>
      </div>
    </section>
  );
}
