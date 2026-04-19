import { useEffect, useState } from "react";
import { useCapture } from "@/domains/capture/store/capture-context";
import { getDownloadConfig } from "@/domains/config/api/configApi";
import { createTask, importTorrentTask } from "@/domains/task/api/taskApi";
import { taskStore } from "@/domains/task/store/task-store";
import { useShell } from "@/domains/shell/hooks/useShell";
import type { TaskSourceType } from "@/domains/task/models/task";
import { createLogger } from "@/shared/utils/logger";

type CreateTaskMode = "url" | "magnet" | "torrent";

interface CreateTaskFormState {
  sourceUri: string;
  saveDir: string;
  displayName: string;
  torrentFile: File | null;
}

const logger = createLogger("create-task-modal");
const initialFormState: CreateTaskFormState = {
  sourceUri: "",
  saveDir: "",
  displayName: "",
  torrentFile: null
};

function createClientRequestId() {
  return globalThis.crypto?.randomUUID?.() || `task-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}

/**
 * 将链接输入解析为后端创建任务所需的来源类型。
 *
 * @param sourceUri 用户输入的下载地址
 * @returns 对应来源类型，无法识别时返回 null
 */
function resolveLinkSourceType(sourceUri: string): TaskSourceType | null {
  const normalizedSourceUri = sourceUri.trim();
  if (!normalizedSourceUri) {
    return null;
  }
  if (normalizedSourceUri.startsWith("magnet:?")) {
    return "MAGNET";
  }

  try {
    const sourceUrl = new URL(normalizedSourceUri);
    if (sourceUrl.protocol === "http:") {
      return "HTTP";
    }
    if (sourceUrl.protocol === "https:") {
      return "HTTPS";
    }
    return null;
  } catch (error) {
    return null;
  }
}

function resolveValidationMessage(mode: CreateTaskMode, formState: CreateTaskFormState) {
  if (mode === "torrent") {
    if (!formState.torrentFile) {
      return "请先选择 .torrent 文件";
    }
    return "";
  }

  if (!formState.sourceUri.trim()) {
    return mode === "magnet" ? "磁力链接不能为空" : "下载地址不能为空";
  }

  const sourceType = resolveLinkSourceType(formState.sourceUri);
  if (mode === "url" && !["HTTP", "HTTPS"].includes(sourceType || "")) {
    return "请输入有效的 HTTP 或 HTTPS 下载地址";
  }
  if (mode === "magnet" && sourceType !== "MAGNET") {
    return "请输入有效的磁力链接";
  }
  return "";
}

export function TaskCreateModal() {
  const { isCreateTaskOpen, createTaskDraft, closeCreateTask, pushToast } = useShell();
  const { markTaskAsLocallyInitiated } = useCapture();
  const [mode, setMode] = useState<CreateTaskMode>("url");
  const [formState, setFormState] = useState<CreateTaskFormState>(initialFormState);
  const [submitting, setSubmitting] = useState(false);
  const [loadingDefaultDir, setLoadingDefaultDir] = useState(false);
  const [formError, setFormError] = useState("");

  useEffect(() => {
    if (!isCreateTaskOpen) {
      return;
    }

    let disposed = false;
    const nextMode = createTaskDraft?.mode || "url";
    setMode(nextMode);
    setFormState({
      ...initialFormState,
      sourceUri: createTaskDraft?.sourceUri || ""
    });
    setSubmitting(false);
    setFormError("");
    setLoadingDefaultDir(true);

    async function loadDefaultSaveDir() {
      try {
        const downloadConfig = await getDownloadConfig();
        if (!disposed) {
          setFormState((currentFormState) =>
            currentFormState.saveDir.trim()
              ? currentFormState
              : {
                  ...currentFormState,
                  saveDir: downloadConfig.defaultSaveDir || ""
                }
          );
        }
      } catch (error) {
        logger.warn("读取默认下载目录失败，将回退为空目录输入", error);
      } finally {
        if (!disposed) {
          setLoadingDefaultDir(false);
        }
      }
    }

    void loadDefaultSaveDir();
    return () => {
      disposed = true;
    };
  }, [createTaskDraft?.mode, createTaskDraft?.sourceUri, isCreateTaskOpen]);

  function updateFormState<K extends keyof CreateTaskFormState>(key: K, value: CreateTaskFormState[K]) {
    setFormError("");
    setFormState((currentFormState) => ({
      ...currentFormState,
      [key]: value
    }));
  }

  async function submitTask() {
    const validationMessage = resolveValidationMessage(mode, formState);
    if (validationMessage) {
      setFormError(validationMessage);
      pushToast(validationMessage, "warning");
      return;
    }

    try {
      setSubmitting(true);
      setFormError("");
      const clientRequestId = createClientRequestId();
      const normalizedSaveDir = formState.saveDir.trim();

      const createdTask =
        mode === "torrent"
          ? await importTorrentTask({
              clientRequestId,
              torrentFile: formState.torrentFile as File,
              saveDir: normalizedSaveDir || undefined
            })
          : await createTask({
              clientRequestId,
              sourceType: resolveLinkSourceType(formState.sourceUri) as TaskSourceType,
              sourceUri: formState.sourceUri.trim(),
              saveDir: normalizedSaveDir || undefined,
              displayName: formState.displayName.trim() || undefined
            });

      closeCreateTask();
      markTaskAsLocallyInitiated(createdTask.taskId);
      void taskStore.reloadTasks({
        silent: true,
        reason: "create-task"
      });
      pushToast(
        `已创建任务：${createdTask.displayName || formState.displayName.trim() || formState.torrentFile?.name || "新任务"}`,
        "success"
      );
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "创建任务失败";
      setFormError(errorMessage);
      pushToast(errorMessage, "danger");
    } finally {
      setSubmitting(false);
    }
  }

  if (!isCreateTaskOpen) {
    return null;
  }

  return (
    <div className="modal-backdrop">
      <div className="modal-card">
        <div className="modal-head">
          <div>
            <h2>新建任务</h2>
            <p>
              {createTaskDraft?.source === "clipboard"
                ? "当前内容来自剪贴板识别，可确认后直接创建或继续调整。"
                : "支持 URL、磁力和种子三种入口，提交成功后任务会自动回流到列表上下文。"}
            </p>
          </div>
          <button className="button-ghost" disabled={submitting} onClick={closeCreateTask} type="button">
            关闭
          </button>
        </div>

        <div className="modal-grid">
          <div className="panel-actions">
            <button
              className={mode === "url" ? "button" : "button-ghost"}
              onClick={() => setMode("url")}
              type="button"
            >
              URL
            </button>
            <button
              className={mode === "magnet" ? "button" : "button-ghost"}
              onClick={() => setMode("magnet")}
              type="button"
            >
              磁力
            </button>
            <button
              className={mode === "torrent" ? "button" : "button-ghost"}
              onClick={() => setMode("torrent")}
              type="button"
            >
              种子
            </button>
          </div>

          {formError ? (
            <div className="inline-banner inline-banner--danger">
              <strong>提交失败</strong>
              <span>{formError}</span>
            </div>
          ) : null}

          {createTaskDraft?.source === "clipboard" ? (
            <div className="inline-banner inline-banner--info">
              <strong>来自剪贴板</strong>
              <span>系统已识别下载地址并自动带入表单，你可以直接创建或先调整名称、目录后再提交。</span>
            </div>
          ) : null}

          {mode === "torrent" ? (
            <label className="settings-field">
              <span>种子文件</span>
              <input
                accept=".torrent,application/x-bittorrent"
                className="field"
                onChange={(event) => updateFormState("torrentFile", event.target.files?.[0] || null)}
                type="file"
              />
              <span>{formState.torrentFile ? `已选择：${formState.torrentFile.name}` : "请选择 .torrent 文件"}</span>
            </label>
          ) : (
            <>
              <label className="settings-field">
                <span>{mode === "magnet" ? "磁力链接" : "来源地址"}</span>
                <input
                  className="field"
                  onChange={(event) => updateFormState("sourceUri", event.target.value)}
                  placeholder={
                    mode === "magnet"
                      ? "magnet:?xt=urn:btih:..."
                      : "https://example.com/archive/demo.zip"
                  }
                  value={formState.sourceUri}
                />
              </label>

              <label className="settings-field">
                <span>自定义名称</span>
                <input
                  className="field"
                  onChange={(event) => updateFormState("displayName", event.target.value)}
                  placeholder="可选，自定义任务名称"
                  value={formState.displayName}
                />
              </label>
            </>
          )}

          <label className="settings-field">
            <span>保存目录</span>
            <input
              className="field"
              onChange={(event) => updateFormState("saveDir", event.target.value)}
              placeholder={loadingDefaultDir ? "正在读取默认下载目录..." : "留空则使用默认下载目录"}
              value={formState.saveDir}
            />
            <span>{loadingDefaultDir ? "正在从设置页同步默认下载目录..." : "留空时由后端回退到默认目录"}</span>
          </label>

          <div className="capture-card">
            <strong>当前阶段说明</strong>
            <span>
              {mode === "torrent"
                ? "种子模式会走 multipart/form-data 上传链路；若文件非法，后端会返回解析失败提示。"
                : "链接模式会先做本地校验，再根据协议映射为 HTTP / HTTPS / MAGNET 创建请求。"}
            </span>
          </div>
        </div>

        <div className="modal-actions" style={{ marginTop: 18 }}>
          <button className="button-ghost" disabled={submitting} onClick={closeCreateTask} type="button">
            取消
          </button>
          <button className="button" disabled={submitting} onClick={() => void submitTask()} type="button">
            {submitting ? "创建中..." : "创建任务"}
          </button>
        </div>
      </div>
    </div>
  );
}
