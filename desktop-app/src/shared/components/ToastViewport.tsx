import { useShell } from "@/domains/shell/hooks/useShell";

export function ToastViewport() {
  const { toasts, dismissToast } = useShell();

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="toast-viewport">
      {toasts.map((toast) => (
        <div className={`toast-item toast-item--${toast.tone}`} key={toast.id}>
          <div className="toast-item__head">
            <strong>{toast.title}</strong>
            <button className="button-ghost" onClick={() => dismissToast(toast.id)} type="button">
              关闭
            </button>
          </div>
          {toast.message ? <span className="toast-item__body">{toast.message}</span> : null}
        </div>
      ))}
    </div>
  );
}
