/// <reference types="vite/client" />

interface MoodDownloadRuntimeConfig {
  serviceUrl: string;
  localToken: string;
  clientType: string;
  platform: string;
}

interface MoodDownloadWindowState {
  isFocused: boolean;
  isMaximized: boolean;
  isMinimized: boolean;
  isVisible: boolean;
  trayAvailable: boolean;
  reason?: string;
  updatedAt?: number;
}

interface MoodDownloadBridge {
  runtime: MoodDownloadRuntimeConfig;
  app: {
    getRuntimeConfig: () => Promise<MoodDownloadRuntimeConfig>;
    pickDirectory: (defaultPath?: string) => Promise<string | null>;
    openPath: (targetPath: string) => Promise<string>;
    restartAria2Engine: (payload: { profileJson: string; trackerListText?: string }) => Promise<string>;
  };
  clipboard: {
    readText: () => string;
  };
  window: {
    minimize: () => Promise<void>;
    toggleMaximize: () => Promise<boolean>;
    close: () => Promise<void>;
    minimizeToTray: () => Promise<void>;
    showAndFocus: () => Promise<boolean>;
    isMaximized: () => Promise<boolean>;
    getState: () => Promise<MoodDownloadWindowState>;
    onStateChange: (listener: (state: MoodDownloadWindowState) => void) => () => void;
  };
}

interface Window {
  moodDownloadBridge?: MoodDownloadBridge;
}
