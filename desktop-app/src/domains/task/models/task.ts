export type TaskDomainStatus =
  | "PENDING"
  | "DISPATCHING"
  | "RUNNING"
  | "COMPLETED"
  | "PAUSED"
  | "FAILED"
  | "CANCELLED";

export type TaskSourceType = "HTTP" | "HTTPS" | "MAGNET" | "BT" | "TORRENT";

export interface TaskListItem {
  taskId: number;
  taskCode: string;
  displayName: string;
  sourceType: string;
  domainStatus: TaskDomainStatus | string;
  engineStatus: string;
  progress: number;
  downloadSpeedBps: number;
  saveDir: string;
  updatedAt: number;
}

export interface TaskListPage {
  pageNo: number;
  pageSize: number;
  total: number;
  items: TaskListItem[];
}

export interface TaskListQuery {
  status?: string;
  keyword?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface TaskCreateResult {
  taskId: number;
  taskCode: string;
  domainStatus: TaskDomainStatus | string;
  engineStatus: string;
  displayName?: string;
  createdAt: number;
}

export interface TaskTorrentFile {
  fileIndex?: number | null;
  filePath: string;
  fileSizeBytes: number;
  selected?: boolean | null;
}

export interface TaskEngineDetail {
  engineGid: string;
  parentEngineGid?: string | null;
  engineStatus: string;
  metadataOnly?: boolean | null;
  totalSizeBytes: number;
  completedSizeBytes: number;
  downloadSpeedBps: number;
  uploadSpeedBps: number;
  errorCode?: string | null;
  errorMessage?: string | null;
}

export interface TaskDetail {
  taskId: number;
  taskCode: string;
  displayName: string;
  sourceType: string;
  sourceUri: string;
  domainStatus: TaskDomainStatus | string;
  engineStatus: string;
  progress: number;
  totalSizeBytes: number;
  completedSizeBytes: number;
  downloadSpeedBps: number;
  uploadSpeedBps: number;
  saveDir: string;
  retryCount: number;
  errorCode?: string | null;
  errorMessage?: string | null;
  torrentFiles: TaskTorrentFile[];
  engineTasks: TaskEngineDetail[];
  torrentMetadataReady?: boolean;
  createdAt: number;
  updatedAt: number;
}

export type TaskDeleteMode = "TASK_ONLY" | "TASK_AND_OUTPUT" | "TASK_AND_ALL_ARTIFACTS";

export interface TaskDeletionPreview {
  taskId: number;
  deleteMode: TaskDeleteMode;
  removable: boolean;
  targets: string[];
  warnings: string[];
}

export interface TaskOpenContext {
  taskId: number;
  openFolderPath?: string | null;
  primaryFilePath?: string | null;
  canOpen: boolean;
  reason?: string | null;
}

export interface TaskCommandResult {
  taskId: number;
  domainStatus: TaskDomainStatus | string;
  retryCount?: number;
  operationApplied?: boolean;
}

export interface TaskCreatePayload {
  clientRequestId: string;
  sourceType: TaskSourceType;
  sourceUri: string;
  saveDir?: string;
  displayName?: string;
}

export interface TaskTorrentCreatePayload {
  clientRequestId: string;
  torrentFile: File;
  saveDir?: string;
}

export interface TaskDeleteResult {
  taskId: number;
  removed: boolean;
  deleteMode: TaskDeleteMode;
  outputRemoved: boolean;
  artifactRemoved: boolean;
  partialSuccess: boolean;
  message: string;
}

export interface TaskSseEvent {
  eventType: string;
  taskId: number;
  taskCode: string;
  domainStatus: TaskDomainStatus | string;
  engineStatus: string;
  progress: number;
  downloadSpeedBps: number;
  timestamp: number;
}
