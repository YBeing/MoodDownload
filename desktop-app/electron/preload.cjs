const { clipboard, contextBridge, ipcRenderer } = require("electron");
const process = require("node:process");
const WINDOW_STATE_EVENT_CHANNEL = "window:stateChanged";

const runtimeConfig = {
  serviceUrl: process.env.LOCAL_SERVICE_URL || process.env.VITE_LOCAL_SERVICE_URL || "http://127.0.0.1:18080",
  localToken: process.env.LOCAL_SERVICE_TOKEN || process.env.VITE_LOCAL_SERVICE_TOKEN || "dev-local-token",
  clientType: "desktop-app",
  platform: process.platform
};

contextBridge.exposeInMainWorld("moodDownloadBridge", {
  runtime: runtimeConfig,
  app: {
    getRuntimeConfig: () => ipcRenderer.invoke("app:getRuntimeConfig"),
    pickDirectory: (defaultPath) => ipcRenderer.invoke("app:pickDirectory", defaultPath)
  },
  clipboard: {
    readText: () => clipboard.readText()
  },
  window: {
    minimize: () => ipcRenderer.invoke("window:minimize"),
    toggleMaximize: () => ipcRenderer.invoke("window:toggleMaximize"),
    close: () => ipcRenderer.invoke("window:close"),
    minimizeToTray: () => ipcRenderer.invoke("window:minimizeToTray"),
    isMaximized: () => ipcRenderer.invoke("window:isMaximized"),
    getState: () => ipcRenderer.invoke("window:getState"),
    onStateChange: (listener) => {
      const wrappedListener = (_event, payload) => listener(payload);
      ipcRenderer.on(WINDOW_STATE_EVENT_CHANNEL, wrappedListener);
      return () => {
        ipcRenderer.removeListener(WINDOW_STATE_EVENT_CHANNEL, wrappedListener);
      };
    }
  }
});
