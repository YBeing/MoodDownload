export interface DownloadConfig {
  defaultSaveDir: string;
  maxConcurrentDownloads: number;
  maxGlobalDownloadSpeed: number;
  maxGlobalUploadSpeed: number;
  browserCaptureEnabled: boolean;
  clipboardMonitorEnabled: boolean;
  autoStartEnabled: boolean;
  updatedAt: number;
}

export interface UpdateDownloadConfigPayload {
  defaultSaveDir: string;
  maxConcurrentDownloads: number;
  maxGlobalDownloadSpeed: number;
  maxGlobalUploadSpeed: number;
  browserCaptureEnabled: boolean;
  clipboardMonitorEnabled: boolean;
}
