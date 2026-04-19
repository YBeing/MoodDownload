const { app, BrowserWindow, Menu, Tray, nativeImage, ipcMain } = require("electron");
const path = require("node:path");
const process = require("node:process");

const APP_WINDOW_MIN_WIDTH = 1240;
const APP_WINDOW_MIN_HEIGHT = 760;
const WINDOW_STATE_EVENT_CHANNEL = "window:stateChanged";
const TRAY_ICON_DATA_URL =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAM1BMVEVHcEzj8P8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jm5v9wHyxqAAAAEHRSTlMAAQQJDhMaJ0hdaX2MofjWSsW1hwAAAF9JREFUGNNjYMAFJiYmBkYGZgYGBjA2NhYWFoYgYGRkYGRg4OLiYGBg4ObmZuZmZmFgYGJgAAKMDIyMDAwsLIwsrCxsLAxMTHBw8PLx8fDw8fHx8fDwMDAAAAUWQSHn0SiCcAAAAASUVORK5CYII=";

/** @type {BrowserWindow | null} */
let mainWindow = null;
/** @type {Tray | null} */
let appTray = null;

/**
 * 生成渲染进程运行时配置，统一桌面端与本地后端的连接参数。
 *
 * @returns {{serviceUrl: string, localToken: string, clientType: string, platform: string}}
 */
function buildRuntimeConfig() {
  return {
    serviceUrl: process.env.LOCAL_SERVICE_URL || process.env.VITE_LOCAL_SERVICE_URL || "http://127.0.0.1:18080",
    localToken: process.env.LOCAL_SERVICE_TOKEN || process.env.VITE_LOCAL_SERVICE_TOKEN || "dev-local-token",
    clientType: "desktop-app",
    platform: process.platform
  };
}

/**
 * 构建桌面窗口选项，预留自定义标题栏和后续托盘最小化能力。
 *
 * @returns {import("electron").BrowserWindowConstructorOptions}
 */
function buildWindowOptions() {
  return {
    width: 1440,
    height: 920,
    minWidth: APP_WINDOW_MIN_WIDTH,
    minHeight: APP_WINDOW_MIN_HEIGHT,
    backgroundColor: "#0b0f14",
    title: "MoodDownload",
    autoHideMenuBar: true,
    frame: false,
    titleBarStyle: process.platform === "darwin" ? "hiddenInset" : "hidden",
    titleBarOverlay:
      process.platform === "win32"
        ? {
            color: "#0b0f14",
            symbolColor: "#eaf2ff",
            height: 44
          }
        : false,
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  };
}

/**
 * 统一生成当前窗口状态快照，供渲染进程做标题栏和底栏状态同步。
 *
 * @returns {{isFocused: boolean, isMaximized: boolean, isMinimized: boolean, isVisible: boolean, trayAvailable: boolean}}
 */
function buildWindowState() {
  return {
    isFocused: Boolean(mainWindow?.isFocused()),
    isMaximized: Boolean(mainWindow?.isMaximized()),
    isMinimized: Boolean(mainWindow?.isMinimized()),
    isVisible: Boolean(mainWindow?.isVisible()),
    trayAvailable: Boolean(appTray)
  };
}

/**
 * 将窗口状态主动广播给渲染进程，避免标题栏只在首次加载时读取一次状态。
 *
 * @param {string} reason 状态变化原因
 */
function emitWindowState(reason) {
  if (!mainWindow || mainWindow.isDestroyed()) {
    return;
  }

  mainWindow.webContents.send(WINDOW_STATE_EVENT_CHANNEL, {
    ...buildWindowState(),
    reason,
    updatedAt: Date.now()
  });
}

/**
 * 绑定窗口生命周期事件，在最小化、隐藏、恢复时同步壳层状态。
 */
function bindWindowStateEvents() {
  if (!mainWindow) {
    return;
  }

  ["maximize", "unmaximize", "minimize", "restore", "show", "hide", "focus", "blur"].forEach((eventName) => {
    mainWindow.on(eventName, () => {
      emitWindowState(eventName);
    });
  });
}

/**
 * 创建主窗口并加载 Vite 开发服务或构建后的静态资源。
 */
async function createMainWindow() {
  mainWindow = new BrowserWindow(buildWindowOptions());
  bindWindowStateEvents();
  mainWindow.on("closed", () => {
    mainWindow = null;
  });

  const devServerUrl = process.env.VITE_DEV_SERVER_URL;
  if (devServerUrl) {
    await mainWindow.loadURL(devServerUrl);
    mainWindow.webContents.openDevTools({ mode: "detach" });
    emitWindowState("loaded");
    return;
  }

  await mainWindow.loadFile(path.join(__dirname, "..", "dist", "index.html"));
  emitWindowState("loaded");
}

/**
 * 初始化托盘，用于窗口最小化后的恢复与退出。
 */
function ensureTray() {
  if (appTray) {
    return appTray;
  }

  const trayIcon = nativeImage.createFromDataURL(TRAY_ICON_DATA_URL);
  appTray = new Tray(trayIcon);
  appTray.setToolTip("MoodDownload");
  appTray.setContextMenu(
    Menu.buildFromTemplate([
      {
        label: "显示主窗口",
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
            emitWindowState("tray-show");
          }
        }
      },
      {
        label: "退出",
        click: () => {
          app.quit();
        }
      }
    ])
  );
  appTray.on("click", () => {
    if (mainWindow) {
      if (mainWindow.isVisible()) {
        mainWindow.hide();
      } else {
        mainWindow.show();
        mainWindow.focus();
      }
      emitWindowState("tray-toggle");
    }
  });
  return appTray;
}

ipcMain.handle("window:minimize", () => {
  mainWindow?.minimize();
});

ipcMain.handle("window:toggleMaximize", () => {
  if (!mainWindow) {
    return false;
  }
  if (mainWindow.isMaximized()) {
    mainWindow.unmaximize();
    return false;
  }
  mainWindow.maximize();
  return true;
});

ipcMain.handle("window:close", () => {
  mainWindow?.close();
});

ipcMain.handle("window:minimizeToTray", () => {
  ensureTray();
  mainWindow?.hide();
  emitWindowState("minimize-to-tray");
});

ipcMain.handle("window:isMaximized", () => {
  return Boolean(mainWindow?.isMaximized());
});

ipcMain.handle("window:getState", () => {
  return buildWindowState();
});

ipcMain.handle("app:getRuntimeConfig", () => buildRuntimeConfig());

app.whenReady().then(async () => {
  await createMainWindow();
  ensureTray();
  emitWindowState("ready");
  app.on("activate", async () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      await createMainWindow();
      ensureTray();
      emitWindowState("activate");
      return;
    }
    mainWindow?.show();
    emitWindowState("activate");
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
