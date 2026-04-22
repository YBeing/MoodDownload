import { useEffect, useRef, useState } from "react";
import { deleteTask, getTaskDetail, getTaskOpenContext, pauseTask, resumeTask, retryTask } from "@/domains/task/api/taskApi";
import { useTaskEvents } from "@/app/providers/TaskEventsProvider";
import { taskStore } from "@/domains/task/store/task-store";
import { useShell } from "@/domains/shell/hooks/useShell";
import { formatBytes, formatDateTime, formatProgress, formatSpeed } from "@/shared/utils/formatters";
import type { TaskDeleteMode, TaskDetail, TaskEngineDetail, TaskTorrentFile } from "@/domains/task/models/task";

type DetailActionType = "pause" | "resume" | "retry" | "delete" | "refresh";
type DetailNoticeTone = "success" | "warning" | "danger";

interface DetailActionNotice {
  title: string;
  message: string;
  tone: DetailNoticeTone;
}

interface DeleteDialogState {
  deleteMode: TaskDeleteMode;
  open: boolean;
}

interface ProgressSnapshotProps {
  title: string;
  progress: number;
  summary: string;
  detail?: string;
  compact?: boolean;
}

function resolveBadgeClass(status: string) {
  if (status === "RUNNING") {
    return "badge badge--running";
  }
  if (status === "COMPLETED") {
    return "badge badge--completed";
  }
  if (status === "PAUSED") {
    return "badge badge--paused";
  }
  if (status === "FAILED") {
    return "badge badge--failed";
  }
  return "badge badge--pending";
}

function resolvePrimaryAction(status: string) {
  if (["PENDING", "RUNNING", "DISPATCHING"].includes(status)) {
    return {
      type: "pause" as const,
      label: "暂停任务"
    };
  }
  if (status === "PAUSED") {
    return {
      type: "resume" as const,
      label: "继续任务"
    };
  }
  if (status === "FAILED") {
    return {
      type: "retry" as const,
      label: "重试任务"
    };
  }
  return null;
}

function formatTorrentFileLabel(torrentFile: TaskTorrentFile) {
  if (torrentFile.fileIndex === null || torrentFile.fileIndex === undefined) {
    return torrentFile.filePath;
  }
  return `#${torrentFile.fileIndex} ${torrentFile.filePath}`;
}

function resolveEngineTaskTitle(engineTask: TaskEngineDetail, index: number) {
  const prefix = engineTask.metadataOnly ? "Metadata 子任务" : "真实下载子任务";
  return `${prefix} ${index + 1}`;
}

function normalizeProgress(progress: number) {
  if (!Number.isFinite(progress)) {
    return 0;
  }
  return Math.min(1, Math.max(0, progress));
}

function ProgressSnapshot({ title, progress, summary, detail, compact = false }: ProgressSnapshotProps) {
  const normalizedProgress = normalizeProgress(progress);

  return (
    <div className={compact ? "progress-snapshot progress-snapshot--compact" : "progress-snapshot"}>
      <div className="progress-snapshot__head">
        <span>{title}</span>
        {detail ? <span className="caption-text">{detail}</span> : null}
      </div>
      <div className="progress-snapshot__body">
        <strong className="progress-snapshot__value mono-text">{formatProgress(normalizedProgress)}</strong>
        <div className="progress-snapshot__meter">
          <div className="progress-snapshot__track">
            <div className="progress-snapshot__fill" style={{ width: formatProgress(normalizedProgress) }} />
          </div>
          <span className="progress-snapshot__summary">{summary}</span>
        </div>
      </div>
    </div>
  );
}

export function TaskDetailDrawer() {
  const { detailTaskId, closeTaskDetail, pushToast } = useShell();
  const { lastEvent } = useTaskEvents();
  const [taskDetail, setTaskDetail] = useState<TaskDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [acting, setActing] = useState<DetailActionType | null>(null);
  const [actionNotice, setActionNotice] = useState<DetailActionNotice | null>(null);
  const [deleteDialogState, setDeleteDialogState] = useState<DeleteDialogState>({
    deleteMode: "TASK_ONLY",
    open: false
  });
  const activeTaskIdRef = useRef<number | null>(null);

  /**
   * 拉取抽屉详情数据；静默刷新时不打断已展示内容，仅更新局部状态。
   *
   * @param taskId 任务 ID
   * @param silent 是否静默刷新
   */
  async function loadTaskDetail(taskId: number, silent = false) {
    try {
      if (silent) {
        setRefreshing(true);
      } else {
        setLoading(true);
        setErrorMessage("");
      }

      const nextDetail = await getTaskDetail(taskId);
      if (activeTaskIdRef.current !== taskId) {
        return;
      }
      setTaskDetail(nextDetail);
      if (!silent) {
        setErrorMessage("");
      }
    } catch (error) {
      if (activeTaskIdRef.current !== taskId) {
        return;
      }
      const nextErrorMessage = error instanceof Error ? error.message : "任务详情加载失败";
      setErrorMessage(nextErrorMessage);
      if (!silent) {
        pushToast(nextErrorMessage, "danger");
      }
    } finally {
      if (activeTaskIdRef.current !== taskId) {
        return;
      }
      if (silent) {
        setRefreshing(false);
      } else {
        setLoading(false);
      }
    }
  }

  useEffect(() => {
    activeTaskIdRef.current = detailTaskId;
    if (!detailTaskId) {
      setTaskDetail(null);
      setLoading(false);
      setErrorMessage("");
      setRefreshing(false);
      setActing(null);
      setActionNotice(null);
      setDeleteDialogState({
        deleteMode: "TASK_ONLY",
        open: false
      });
      return;
    }
    void loadTaskDetail(detailTaskId);
  }, [detailTaskId]);

  useEffect(() => {
    if (!detailTaskId || !lastEvent || lastEvent.taskId !== detailTaskId) {
      return;
    }
    void loadTaskDetail(detailTaskId, true);
  }, [detailTaskId, lastEvent?.timestamp]);

  async function handlePrimaryAction() {
    if (!taskDetail) {
      return;
    }

    const primaryAction = resolvePrimaryAction(taskDetail.domainStatus);
    if (!primaryAction) {
      return;
    }

    try {
      setActing(primaryAction.type);
      setActionNotice(null);
      if (primaryAction.type === "pause") {
        const commandResult = await pauseTask(taskDetail.taskId);
        taskStore.applyCommandResult(taskDetail.taskId, commandResult);
        setActionNotice({
          title: "暂停请求已提交",
          message: "任务详情会继续跟随 SSE 和静默对账同步最新状态。",
          tone: "warning"
        });
      } else if (primaryAction.type === "resume") {
        const commandResult = await resumeTask(taskDetail.taskId);
        taskStore.applyCommandResult(taskDetail.taskId, commandResult);
        setActionNotice({
          title: "继续请求已提交",
          message: "任务已重新进入执行队列，请关注速度和进度变化。",
          tone: "success"
        });
      } else if (primaryAction.type === "retry") {
        const commandResult = await retryTask(taskDetail.taskId);
        taskStore.applyCommandResult(taskDetail.taskId, commandResult);
        setActionNotice({
          title: "重试请求已提交",
          message: "系统会重新拉起下载任务，失败信息会在详情区持续刷新。",
          tone: "success"
        });
      }
      await loadTaskDetail(taskDetail.taskId, true);
    } catch (error) {
      setActionNotice({
        title: "任务操作失败",
        message: "请稍后重试，或刷新详情后确认当前状态。",
        tone: "danger"
      });
      pushToast(error instanceof Error ? error.message : "任务操作失败", "danger");
    } finally {
      setActing(null);
    }
  }

  async function handleOpenFolder() {
    if (!taskDetail) {
      return;
    }
    try {
      const openContext = await getTaskOpenContext(taskDetail.taskId);
      if (!openContext.canOpen || !openContext.openFolderPath) {
        pushToast(openContext.reason || "当前任务没有可打开的目录", "warning");
        return;
      }
      const openResult = await window.moodDownloadBridge?.app.openPath?.(openContext.openFolderPath);
      if (openResult) {
        pushToast(openResult, "danger");
        return;
      }
      pushToast("已打开下载目录", "success");
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "打开文件夹失败", "danger");
    }
  }

  async function handleDeleteTask() {
    if (!taskDetail) {
      return;
    }
    setDeleteDialogState({
      deleteMode: "TASK_ONLY",
      open: true
    });
  }

  async function confirmDeleteTask() {
    if (!taskDetail) {
      return;
    }

    try {
      setActing("delete");
      const deleteResult = await deleteTask(taskDetail.taskId, deleteDialogState.deleteMode);
      taskStore.removeTask(taskDetail.taskId);
      taskStore.scheduleSilentReload("detail-delete");
      closeTaskDetail();
      pushToast(
        deleteResult.partialSuccess ? deleteResult.message || "任务已删除，但部分文件清理失败" : `已删除任务：${taskDetail.displayName}`,
        deleteResult.partialSuccess ? "warning" : "success"
      );
    } catch (error) {
      pushToast(error instanceof Error ? error.message : "删除任务失败", "danger");
    } finally {
      setActing(null);
    }
  }

  async function handleRefreshTaskDetail() {
    if (!detailTaskId) {
      return;
    }

    try {
      setActing("refresh");
      await loadTaskDetail(detailTaskId, true);
    } finally {
      setActing(null);
    }
  }

  if (!detailTaskId) {
    return null;
  }

  const primaryAction = taskDetail ? resolvePrimaryAction(taskDetail.domainStatus) : null;
  const actionBusy = acting !== null;

  return (
    <div aria-hidden="true" className="drawer-backdrop" onClick={closeTaskDetail}>
      <aside
        aria-label="任务详情抽屉"
        className="drawer-card"
        onClick={(event) => {
          event.stopPropagation();
        }}
      >
        <div className="drawer-head">
          <div>
            <h2>任务详情</h2>
            <p>详情抽屉会跟随列表上下文刷新；对当前任务的命令操作会同步回流到共享任务状态。</p>
          </div>
          <button
            aria-label="关闭任务详情"
            className="drawer-close"
            onClick={closeTaskDetail}
            type="button"
          >
            ×
          </button>
        </div>

        <div className="drawer-scroll">
          {loading ? <div className="empty-state">正在读取任务详情...</div> : null}

          {!loading && actionNotice ? (
            <div className={`inline-banner inline-banner--${actionNotice.tone}`}>
              <strong>{actionNotice.title}</strong>
              <span>{actionNotice.message}</span>
            </div>
          ) : null}

          {!loading && errorMessage ? (
            <div className="error-state">
              <div>
                <strong>任务详情加载失败</strong>
                <p>{errorMessage}</p>
              </div>
              <div className="panel-actions">
                <button className="button" onClick={() => void loadTaskDetail(detailTaskId)} type="button">
                  重试
                </button>
              </div>
            </div>
          ) : null}

          {!loading && taskDetail ? (
            <>
              <div className="glass-panel" style={{ padding: 18 }}>
                <div className="drawer-meta">
                  <strong>{taskDetail.displayName}</strong>
                  <div className="panel-actions">
                    <span className={resolveBadgeClass(taskDetail.domainStatus)}>{taskDetail.domainStatus}</span>
                    {refreshing ? <span className="caption-text">同步中...</span> : null}
                  </div>
                  <span>任务编码：{taskDetail.taskCode}</span>
                  <span>来源类型：{taskDetail.sourceType}</span>
                  <span>来源：{taskDetail.sourceUri}</span>
                  <span>保存目录：{taskDetail.saveDir}</span>
                </div>
              </div>

              <div className="drawer-metrics">
                <div className="settings-card">
                  <ProgressSnapshot
                    detail="聚合当前任务下所有真实下载子任务"
                    progress={taskDetail.progress}
                    summary={`已下载 ${formatBytes(taskDetail.completedSizeBytes)} / ${formatBytes(taskDetail.totalSizeBytes)}`}
                    title="当前进度"
                  />
                </div>
                <div className="settings-card">
                  <span>当前速度</span>
                  <strong className="mono-text">{formatSpeed(taskDetail.downloadSpeedBps)}</strong>
                  <span>上传：{formatSpeed(taskDetail.uploadSpeedBps)}</span>
                </div>
              </div>

              <div className="settings-card">
                <div className="drawer-kv">
                  <span>创建时间</span>
                  <strong>{formatDateTime(taskDetail.createdAt)}</strong>
                </div>
                <div className="drawer-kv">
                  <span>总大小</span>
                  <strong>{formatBytes(taskDetail.totalSizeBytes)}</strong>
                </div>
                <div className="drawer-kv">
                  <span>已下载</span>
                  <strong>{formatBytes(taskDetail.completedSizeBytes)}</strong>
                </div>
                <div className="drawer-kv">
                  <span>重试次数</span>
                  <strong>{taskDetail.retryCount}</strong>
                </div>
                <div className="drawer-kv">
                  <span>最近更新时间</span>
                  <strong>{formatDateTime(taskDetail.updatedAt)}</strong>
                </div>
              </div>

              {taskDetail.engineTasks.length > 0 ? (
                <div className="settings-card">
                  <span>aria2 子任务列表</span>
                  <strong>{taskDetail.engineTasks.length} 个子任务</strong>
                  <div className="torrent-file-list">
                    {taskDetail.engineTasks.map((engineTask, index) => {
                      const totalSizeLabel = engineTask.metadataOnly
                        ? "metadata 任务"
                        : formatBytes(engineTask.totalSizeBytes);
                      const progress = engineTask.totalSizeBytes > 0
                        ? engineTask.completedSizeBytes / engineTask.totalSizeBytes
                        : 0;
                      return (
                        <div className="torrent-file-row" key={`${engineTask.engineGid}-${index}`}>
                          <strong className="mono-text">{resolveEngineTaskTitle(engineTask, index)}</strong>
                          <div className="drawer-kv">
                            <span>GID</span>
                            <strong className="mono-text">{engineTask.engineGid}</strong>
                          </div>
                          {engineTask.parentEngineGid ? (
                            <div className="drawer-kv">
                              <span>父级 GID</span>
                              <strong className="mono-text">{engineTask.parentEngineGid}</strong>
                            </div>
                          ) : null}
                          <div className="drawer-kv">
                            <span>引擎状态</span>
                            <strong>{engineTask.engineStatus || "UNKNOWN"}</strong>
                          </div>
                          {!engineTask.metadataOnly ? (
                            <ProgressSnapshot
                              compact
                              progress={progress}
                              summary={`${formatBytes(engineTask.completedSizeBytes)} / ${formatBytes(engineTask.totalSizeBytes)}`}
                              title="下载进度"
                            />
                          ) : (
                            <div className="drawer-kv">
                              <span>任务类型</span>
                              <strong>{totalSizeLabel}</strong>
                            </div>
                          )}
                          <div className="drawer-kv">
                            <span>当前速度</span>
                            <strong>{formatSpeed(engineTask.downloadSpeedBps)}</strong>
                          </div>
                          {engineTask.errorMessage ? (
                            <div className="drawer-kv">
                              <span>失败信息</span>
                              <strong>{engineTask.errorCode || "UNKNOWN"}</strong>
                            </div>
                          ) : null}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ) : null}

              {taskDetail.torrentFiles.length > 0 ? (
                <div className="settings-card">
                  <span>种子文件列表</span>
                  <strong>{taskDetail.torrentFiles.length} 个文件</strong>
                  <div className="torrent-file-list">
                    {taskDetail.torrentFiles.map((torrentFile) => (
                      <div
                        className={torrentFile.selected === false ? "torrent-file-row torrent-file-row--disabled" : "torrent-file-row"}
                        key={`${torrentFile.fileIndex ?? "file"}-${torrentFile.filePath}`}
                      >
                        <strong className="mono-text">{formatTorrentFileLabel(torrentFile)}</strong>
                        <div className="drawer-kv">
                          <span>文件大小</span>
                          <strong>{formatBytes(torrentFile.fileSizeBytes)}</strong>
                        </div>
                        <div className="drawer-kv">
                          <span>下载状态</span>
                          <strong>{torrentFile.selected === false ? "已跳过" : "已选中"}</strong>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : taskDetail.torrentMetadataReady === false ? (
                <div className="settings-card">
                  <span>种子文件列表</span>
                  <strong>正在解析种子信息</strong>
                  <span>当前只拿到了任务组概要，真实文件列表会在 metadata 解析完成后自动刷新。</span>
                </div>
              ) : null}

              {taskDetail.errorMessage ? (
                <div className="settings-card">
                  <span>失败信息</span>
                  <strong>{taskDetail.errorCode || "UNKNOWN"}</strong>
                  <span>{taskDetail.errorMessage}</span>
                </div>
              ) : null}
            </>
          ) : null}
        </div>

        <div className="drawer-actions" style={{ marginTop: 18 }}>
          <button
            className="button-ghost"
            disabled={loading || actionBusy}
            onClick={() => void handleRefreshTaskDetail()}
            type="button"
          >
            {refreshing || acting === "refresh" ? "刷新中..." : "刷新详情"}
          </button>
          {taskDetail?.domainStatus === "COMPLETED" ? (
            <button
              className="button-ghost"
              disabled={loading || actionBusy}
              onClick={() => void handleOpenFolder()}
              type="button"
            >
              打开文件夹
            </button>
          ) : null}
          {primaryAction ? (
            <button
              className="button"
              disabled={loading || actionBusy}
              onClick={() => void handlePrimaryAction()}
              type="button"
            >
              {acting === primaryAction.type ? "处理中..." : primaryAction.label}
            </button>
          ) : null}
          <button
            className="button-danger"
            disabled={loading || actionBusy || !taskDetail}
            onClick={() => void handleDeleteTask()}
            type="button"
          >
            {acting === "delete" ? "删除中..." : "删除任务"}
          </button>
        </div>

        {deleteDialogState.open ? (
          <div aria-hidden="true" className="modal-backdrop" onClick={acting === "delete" ? undefined : () => {
            setDeleteDialogState((currentState) => ({ ...currentState, open: false }));
          }}>
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
                  <p>为“{taskDetail?.displayName}”选择删除策略。</p>
                </div>
                <button
                  aria-label="关闭删除弹窗"
                  className="drawer-close"
                  disabled={acting === "delete"}
                  onClick={() => {
                    setDeleteDialogState((currentState) => ({ ...currentState, open: false }));
                  }}
                  type="button"
                >
                  ×
                </button>
              </div>

              <div style={{ display: "grid", gap: 10 }}>
                {[
                  {
                    mode: "TASK_ONLY" as TaskDeleteMode,
                    label: "仅删记录",
                    description: "只删除任务记录，保留已下载文件和关联工件。"
                  },
                  {
                    mode: "TASK_AND_OUTPUT" as TaskDeleteMode,
                    label: "删除记录和源文件",
                    description: "删除任务记录和已下载输出文件，保留种子等工件。"
                  },
                  {
                    mode: "TASK_AND_ALL_ARTIFACTS" as TaskDeleteMode,
                    label: "彻底删除所有相关文件",
                    description: "删除任务记录、输出文件以及种子等关联工件。"
                  }
                ].map((option) => (
                  <button
                    className={deleteDialogState.deleteMode === option.mode ? "button" : "button-ghost"}
                    disabled={acting === "delete"}
                    key={option.mode}
                    onClick={() => {
                      setDeleteDialogState((currentState) => ({ ...currentState, deleteMode: option.mode }));
                    }}
                    style={{ justifyContent: "flex-start", textAlign: "left", padding: "12px 14px" }}
                    type="button"
                  >
                    {option.label}
                  </button>
                ))}
                <span className="muted-text">
                  {
                    [
                      {
                        mode: "TASK_ONLY" as TaskDeleteMode,
                        description: "只删除下载记录，保留源文件和其他关联文件。"
                      },
                      {
                        mode: "TASK_AND_OUTPUT" as TaskDeleteMode,
                        description: "删除下载记录和源文件，保留种子等其他关联文件。"
                      },
                      {
                        mode: "TASK_AND_ALL_ARTIFACTS" as TaskDeleteMode,
                        description: "删除下载记录、源文件以及其他相关文件。"
                      }
                    ].find((option) => option.mode === deleteDialogState.deleteMode)?.description
                  }
                </span>
              </div>

              <div className="drawer-actions" style={{ marginTop: 16 }}>
                <button
                  className="button-ghost"
                  disabled={acting === "delete"}
                  onClick={() => {
                    setDeleteDialogState((currentState) => ({ ...currentState, open: false }));
                  }}
                  type="button"
                >
                  取消
                </button>
                <button
                  className="button-danger"
                  disabled={acting === "delete"}
                  onClick={() => void confirmDeleteTask()}
                  type="button"
                >
                  {acting === "delete" ? "删除中..." : "确认删除"}
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </aside>
    </div>
  );
}
