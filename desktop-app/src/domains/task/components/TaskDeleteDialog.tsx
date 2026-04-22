import { useEffect, useState } from "react";
import type { TaskDeleteMode } from "@/domains/task/models/task";

const DELETE_MODE_OPTIONS: Array<{ mode: TaskDeleteMode; label: string; description: string }> = [
  {
    mode: "TASK_ONLY",
    label: "仅删记录",
    description: "只删除下载记录，保留源文件和其他关联文件。"
  },
  {
    mode: "TASK_AND_OUTPUT",
    label: "删除记录和源文件",
    description: "删除下载记录和源文件，保留种子等其他关联文件。"
  },
  {
    mode: "TASK_AND_ALL_ARTIFACTS",
    label: "彻底删除所有相关文件",
    description: "删除下载记录、源文件以及其他相关文件。"
  }
];

interface TaskDeleteDialogProps {
  busy: boolean;
  open: boolean;
  taskDisplayName: string;
  taskId: number | null;
  onClose: () => void;
  onConfirm: (deleteMode: TaskDeleteMode) => Promise<void>;
}

export function TaskDeleteDialog(props: TaskDeleteDialogProps) {
  const [deleteMode, setDeleteMode] = useState<TaskDeleteMode>("TASK_ONLY");

  useEffect(() => {
    if (!props.open) {
      setDeleteMode("TASK_ONLY");
    }
  }, [props.open, props.taskId]);

  if (!props.open || !props.taskId) {
    return null;
  }

  return (
    <div aria-hidden="true" className="modal-backdrop" onClick={props.busy ? undefined : props.onClose}>
      <div
        className="modal-card"
        onClick={(event) => {
          event.stopPropagation();
        }}
        style={{ width: "min(420px, calc(100vw - 32px))", padding: 18 }}
      >
        <div className="modal-head" style={{ marginBottom: 14 }}>
          <div>
            <h2>删除任务</h2>
            <p>为“{props.taskDisplayName}”选择删除策略。</p>
          </div>
          <button
            aria-label="关闭删除弹窗"
            className="drawer-close"
            disabled={props.busy}
            onClick={props.onClose}
            type="button"
          >
            ×
          </button>
        </div>

        <div style={{ display: "grid", gap: 10 }}>
          {DELETE_MODE_OPTIONS.map((option) => (
            <button
              className={deleteMode === option.mode ? "button" : "button-ghost"}
              disabled={props.busy}
              key={option.mode}
              onClick={() => setDeleteMode(option.mode)}
              style={{ justifyContent: "flex-start", textAlign: "left", padding: "12px 14px" }}
              type="button"
            >
              {option.label}
            </button>
          ))}
          <span className="muted-text">{DELETE_MODE_OPTIONS.find((option) => option.mode === deleteMode)?.description}</span>
        </div>

        <div className="drawer-actions" style={{ marginTop: 16 }}>
          <button className="button-ghost" disabled={props.busy} onClick={props.onClose} type="button">
            取消
          </button>
          <button
            className="button-danger"
            disabled={props.busy}
            onClick={() => void props.onConfirm(deleteMode)}
            type="button"
          >
            {props.busy ? "删除中..." : "确认删除"}
          </button>
        </div>
      </div>
    </div>
  );
}
