export interface DownloadConfig {
  defaultSaveDir: string;
  maxConcurrentDownloads: number;
  maxGlobalDownloadSpeed: number;
  maxGlobalUploadSpeed: number;
  browserCaptureEnabled: boolean;
  clipboardMonitorEnabled: boolean;
  activeEngineProfileCode?: string;
  deleteToRecycleBinEnabled?: boolean;
  updatedAt: number;
}

export interface UpdateDownloadConfigPayload {
  defaultSaveDir: string;
  maxConcurrentDownloads: number;
  maxGlobalDownloadSpeed: number;
  maxGlobalUploadSpeed: number;
  browserCaptureEnabled: boolean;
  clipboardMonitorEnabled: boolean;
  activeEngineProfileCode?: string;
  deleteToRecycleBinEnabled?: boolean;
}

export interface EngineRuntimeProfileItem {
  profileCode: string;
  profileName: string;
  trackerSetCode?: string | null;
  profileJson?: string | null;
  isDefault: boolean;
  enabled: boolean;
}

export interface EngineRuntimeSnapshot {
  activeProfileCode: string;
  profiles: EngineRuntimeProfileItem[];
}

export interface UpdateEngineRuntimeProfilePayload {
  profileCode: string;
  profileJson: string;
}

export interface BtTrackerSet {
  trackerSetCode: string;
  trackerSetName: string;
  trackerListText: string;
  sourceUrl?: string | null;
}

export interface UpdateBtTrackerSetPayload {
  trackerSetName: string;
  trackerListText: string;
  sourceUrl?: string;
}
