import { useCapture } from "@/domains/capture/store/capture-context";

export function CapturePromptViewport() {
  const {
    clipboardPrompt,
    externalCaptureNotice,
    confirmClipboardPrompt,
    dismissClipboardPrompt,
    openExternalCaptureNotice,
    dismissExternalCaptureNotice
  } = useCapture();

  if (!clipboardPrompt.visible && !externalCaptureNotice?.visible) {
    return null;
  }

  return (
    <div className="capture-viewport">
      {clipboardPrompt.visible ? (
        <section className="capture-notice capture-notice--clipboard">
          <div className="capture-notice__content">
            <strong>检测到剪贴板中的下载地址</strong>
            <span className="mono-text">{clipboardPrompt.detectedUrl}</span>
            <span>可直接复用新建任务弹窗确认或调整后创建。</span>
          </div>
          <div className="panel-actions">
            <button className="button" onClick={confirmClipboardPrompt} type="button">
              去创建
            </button>
            <button className="button-ghost" onClick={dismissClipboardPrompt} type="button">
              忽略
            </button>
          </div>
        </section>
      ) : null}

      {externalCaptureNotice?.visible ? (
        <section className="capture-notice capture-notice--browser">
          <div className="capture-notice__content">
            <strong>检测到外部接入的新任务</strong>
            <span>
              {externalCaptureNotice.displayName} · {externalCaptureNotice.taskCode}
            </span>
            <span>来源类型：{externalCaptureNotice.sourceType}，可打开详情继续查看状态。</span>
          </div>
          <div className="panel-actions">
            <button className="button" onClick={openExternalCaptureNotice} type="button">
              查看详情
            </button>
            <button className="button-ghost" onClick={dismissExternalCaptureNotice} type="button">
              关闭
            </button>
          </div>
        </section>
      ) : null}
    </div>
  );
}
