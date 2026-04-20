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
  };
  clipboard: {
    readText: () => string;
  };
  window: {
    minimize: () => Promise<void>;
    toggleMaximize: () => Promise<boolean>;
    close: () => Promise<void>;
    minimizeToTray: () => Promise<void>;
    isMaximized: () => Promise<boolean>;
    getState: () => Promise<MoodDownloadWindowState>;
    onStateChange: (listener: (state: MoodDownloadWindowState) => void) => () => void;
  };
}

interface Window {
  moodDownloadBridge?: MoodDownloadBridge;
}
