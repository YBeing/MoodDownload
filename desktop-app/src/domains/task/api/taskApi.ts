import { requestJson } from "@/shared/api/httpClient";
import type {
  TaskCommandResult,
  TaskCreatePayload,
  TaskCreateResult,
  TaskDeleteResult,
  TaskDeleteMode,
  TaskDeletionPreview,
  TaskDetail,
  TaskListQuery,
  TaskListPage,
  TaskOpenContext,
  TaskTorrentCreatePayload
} from "@/domains/task/models/task";

function buildTaskListQuery(query: TaskListQuery = {}) {
  const searchParams = new URLSearchParams();
  if (query.status) {
    searchParams.set("status", query.status);
  }
  if (query.keyword) {
    searchParams.set("keyword", query.keyword);
  }
  if (query.pageNo) {
    searchParams.set("pageNo", String(query.pageNo));
  }
  if (query.pageSize) {
    searchParams.set("pageSize", String(query.pageSize));
  }
  const queryString = searchParams.toString();
  return queryString ? `/api/tasks?${queryString}` : "/api/tasks";
}

export async function listTasks(query: TaskListQuery = {}) {
  return requestJson<TaskListPage>(buildTaskListQuery(query));
}

export async function getTaskDetail(taskId: number) {
  return requestJson<TaskDetail>(`/api/tasks/${taskId}`);
}

export async function getTaskOpenContext(taskId: number) {
  return requestJson<TaskOpenContext>(`/api/tasks/${taskId}/open-context`);
}

export async function previewTaskDeletion(taskId: number, deleteMode: TaskDeleteMode) {
  return requestJson<TaskDeletionPreview>(`/api/tasks/${taskId}/deletion-preview?deleteMode=${deleteMode}`);
}

export async function createTask(payload: TaskCreatePayload) {
  return requestJson<TaskCreateResult>("/api/tasks", {
    method: "POST",
    body: payload
  });
}

export async function importTorrentTask(payload: TaskTorrentCreatePayload) {
  const formData = new FormData();
  formData.append("clientRequestId", payload.clientRequestId);
  formData.append("torrentFile", payload.torrentFile);
  if (payload.saveDir) {
    formData.append("saveDir", payload.saveDir);
  }
  return requestJson<TaskCreateResult>("/api/tasks/torrent", {
    method: "POST",
    body: formData
  });
}

export async function pauseTask(taskId: number) {
  return requestJson<TaskCommandResult>(`/api/tasks/${taskId}/pause`, {
    method: "POST"
  });
}

export async function resumeTask(taskId: number) {
  return requestJson<TaskCommandResult>(`/api/tasks/${taskId}/resume`, {
    method: "POST"
  });
}

export async function retryTask(taskId: number) {
  return requestJson<TaskCommandResult>(`/api/tasks/${taskId}/retry`, {
    method: "POST"
  });
}

export async function deleteTask(taskId: number, deleteMode: TaskDeleteMode) {
  return requestJson<TaskDeleteResult>(`/api/tasks/${taskId}?deleteMode=${deleteMode}`, {
    method: "DELETE"
  });
}
