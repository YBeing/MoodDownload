interface Aria2RestartConfirmDialogProps {
  busy: boolean;
  open: boolean;
  onCancel: () => void;
  onConfirm: () => Promise<void>;
}

export function Aria2RestartConfirmDialog(props: Aria2RestartConfirmDialogProps) {
  if (!props.open) {
    return null;
  }

  return (
    <div aria-hidden="true" className="modal-backdrop" onClick={props.busy ? undefined : props.onCancel}>
      <div
        className="modal-card"
        onClick={(event) => {
          event.stopPropagation();
        }}
        style={{ width: "min(440px, calc(100vw - 32px))", padding: 18 }}
      >
        <div className="modal-head" style={{ marginBottom: 14 }}>
          <div>
            <h2>重启 aria2 引擎</h2>
            <p>aria2 配置已经保存，是否现在重启 aria2 引擎？</p>
          </div>
          <button
            aria-label="关闭重启确认弹窗"
            className="drawer-close"
            disabled={props.busy}
            onClick={props.onCancel}
            type="button"
          >
            ×
          </button>
        </div>

        <div className="inline-banner inline-banner--warning">
          <strong>如果现在重启，下载连接会短暂中断</strong>
          <span>选择仅保存则保持当前引擎继续运行，新的启动命令会在下次重启后生效。</span>
        </div>

        <div className="drawer-actions" style={{ marginTop: 16 }}>
          <button className="button-ghost" disabled={props.busy} onClick={props.onCancel} type="button">
            仅保存
          </button>
          <button className="button" disabled={props.busy} onClick={() => void props.onConfirm()} type="button">
            {props.busy ? "重启中..." : "立即重启"}
          </button>
        </div>
      </div>
    </div>
  );
}
