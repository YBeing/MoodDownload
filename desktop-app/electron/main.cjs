const { app, BrowserWindow, Menu, Tray, nativeImage, ipcMain, dialog } = require("electron");
const { spawn } = require("node:child_process");
const crypto = require("node:crypto");
const fs = require("node:fs");
const http = require("node:http");
const net = require("node:net");
const path = require("node:path");
const process = require("node:process");

const APP_WINDOW_MIN_WIDTH = 1240;
const APP_WINDOW_MIN_HEIGHT = 760;
const WINDOW_STATE_EVENT_CHANNEL = "window:stateChanged";
const DEFAULT_LOCAL_SERVICE_HOST = "127.0.0.1";
const DEFAULT_LOCAL_SERVICE_PORT = 18080;
const DEFAULT_ARIA2_RPC_PORT = 6800;
const MANAGED_RUNTIME_TIMEOUT_MS = 30000;
const MANAGED_RUNTIME_FLAG = "MOODDOWNLOAD_MANAGED_RUNTIME";
const ARIA2_TRACKER_TEMPLATE =
  "udp://tracker.opentrackr.org:1337/announce,udp://tracker.torrent.eu.org:451/announce,udp://open.stealth.si:80/announce,udp://tracker.tryhackx.org:6969/announce,udp://tracker.qu.ax:6969/announce,https://tracker.opentrackr.org:443/announce";
const TRAY_ICON_DATA_URL =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAM1BMVEVHcEzj8P8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jx/8jm5v9wHyxqAAAAEHRSTlMAAQQJDhMaJ0hdaX2MofjWSsW1hwAAAF9JREFUGNNjYMAFJiYmBkYGZgYGBjA2NhYWFoYgYGRkYGRg4OLiYGBg4ObmZuZmZmFgYGJgAAKMDIyMDAwsLIwsrCxsLAxMTHBw8PLx8fDw8fHx8fDwMDAAAAUWQSHn0SiCcAAAAASUVORK5CYII=";

/** @type {BrowserWindow | null} */
let mainWindow = null;
/** @type {Tray | null} */
let appTray = null;
/** @type {import("node:child_process").ChildProcess | null} */
let managedLocalServiceProcess = null;
/** @type {import("node:child_process").ChildProcess | null} */
let managedAria2Process = null;
let runtimeConfig = buildRuntimeConfig();
let appQuitting = false;

/**
 * 解析打包模式下的运行时日志文件路径，便于排查内置子进程启动失败。
 *
 * @param {string} label 日志标签
 * @returns {string}
 */
function resolveManagedLogPath(label) {
  const logsDir = app.getPath("logs");
  ensureDirectory(logsDir);
  return path.join(logsDir, `${label}.log`);
}

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
 * 构造安装包运行态配置，使用应用内拉起的本地服务地址与动态令牌。
 *
 * @param {number} localServicePort 本地服务端口
 * @param {string} localToken 本地访问令牌
 * @returns {{serviceUrl: string, localToken: string, clientType: string, platform: string}}
 */
function buildManagedRuntimeConfig(localServicePort, localToken) {
  return {
    serviceUrl: `http://${DEFAULT_LOCAL_SERVICE_HOST}:${localServicePort}`,
    localToken,
    clientType: "desktop-app",
    platform: process.platform
  };
}

/**
 * 同步运行时环境变量，确保 preload 与渲染层能读取到最新的本地服务地址和令牌。
 *
 * @param {{serviceUrl: string, localToken: string}} nextRuntimeConfig 最新运行时配置
 */
function syncRuntimeEnv(nextRuntimeConfig) {
  process.env.LOCAL_SERVICE_URL = nextRuntimeConfig.serviceUrl;
  process.env.VITE_LOCAL_SERVICE_URL = nextRuntimeConfig.serviceUrl;
  process.env.LOCAL_SERVICE_TOKEN = nextRuntimeConfig.localToken;
  process.env.VITE_LOCAL_SERVICE_TOKEN = nextRuntimeConfig.localToken;
}

/**
 * 判断当前是否应该由桌面端托管 local-service 与 aria2。
 *
 * @returns {boolean}
 */
function shouldUseManagedRuntime() {
  if (process.env.LOCAL_SERVICE_URL || process.env.VITE_LOCAL_SERVICE_URL) {
    return process.env[MANAGED_RUNTIME_FLAG] === "1";
  }
  return app.isPackaged || process.env[MANAGED_RUNTIME_FLAG] === "1";
}

/**
 * 解析安装包或本地调试模式下的运行时资源目录。
 *
 * @param {...string} segments 资源路径片段
 * @returns {string}
 */
function resolveRuntimeResourcePath(...segments) {
  const baseDir = app.isPackaged
    ? path.join(process.resourcesPath, "runtime")
    : path.join(__dirname, "..", "resources", "runtime");
  return path.join(baseDir, ...segments);
}

/**
 * 构建桌面窗口选项，优先复用系统原生标题栏与窗口控制按钮。
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
    frame: true,
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

/**
 * 确保目录存在，供内置数据库、下载目录和 aria2 session 使用。
 *
 * @param {string} directoryPath 目录路径
 */
function ensureDirectory(directoryPath) {
  fs.mkdirSync(directoryPath, { recursive: true });
}

/**
 * 生成高熵密钥，供本地服务令牌和 aria2 rpc-secret 使用。
 *
 * @returns {string}
 */
function createSecret() {
  return crypto.randomBytes(24).toString("hex");
}

/**
 * 获取一个可用本地端口，优先尝试推荐值，被占用时自动回退随机端口。
 *
 * @param {number} preferredPort 优先端口
 * @returns {Promise<number>}
 */
function findAvailablePort(preferredPort) {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.unref();
    server.on("error", () => {
      const fallbackServer = net.createServer();
      fallbackServer.unref();
      fallbackServer.on("error", reject);
      fallbackServer.listen(0, DEFAULT_LOCAL_SERVICE_HOST, () => {
        const address = fallbackServer.address();
        const nextPort = typeof address === "object" && address ? address.port : preferredPort;
        fallbackServer.close(() => resolve(nextPort));
      });
    });
    server.listen(preferredPort, DEFAULT_LOCAL_SERVICE_HOST, () => {
      const address = server.address();
      const nextPort = typeof address === "object" && address ? address.port : preferredPort;
      server.close(() => resolve(nextPort));
    });
  });
}

/**
 * 轮询本地服务健康接口，避免渲染层在后端未就绪前直接报错。
 *
 * @param {string} serviceUrl 本地服务地址
 * @param {number} timeoutMillis 超时时间
 * @returns {Promise<void>}
 */
async function waitForLocalServiceReady(serviceUrl, timeoutMillis) {
  const deadline = Date.now() + timeoutMillis;
  while (Date.now() < deadline) {
    const ready = await new Promise((resolve) => {
      const healthUrl = new URL("/actuator/health", serviceUrl);
      const request = http.get(healthUrl, (response) => {
        response.resume();
        resolve(response.statusCode === 200);
      });
      request.on("error", () => resolve(false));
      request.setTimeout(1500, () => {
        request.destroy();
        resolve(false);
      });
    });

    if (ready) {
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, 600));
  }

  throw new Error("等待本地服务启动超时");
}

/**
 * 轮询 aria2 JSON-RPC，就绪后再允许 local-service 接入，避免首个下载请求命中未启动窗口。
 *
 * @param {number} aria2RpcPort aria2 RPC 端口
 * @param {string} aria2Secret aria2 RPC 密钥
 * @param {number} timeoutMillis 超时时间
 * @returns {Promise<void>}
 */
async function waitForAria2Ready(aria2RpcPort, aria2Secret, timeoutMillis) {
  const deadline = Date.now() + timeoutMillis;
  while (Date.now() < deadline) {
    const ready = await new Promise((resolve) => {
      const requestBody = JSON.stringify({
        id: `aria2-ready-${Date.now()}`,
        jsonrpc: "2.0",
        method: "aria2.getVersion",
        params: [`token:${aria2Secret}`]
      });
      const request = http.request(
        {
          host: DEFAULT_LOCAL_SERVICE_HOST,
          port: aria2RpcPort,
          path: "/jsonrpc",
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Content-Length": Buffer.byteLength(requestBody)
          }
        },
        (response) => {
          let responseBody = "";
          response.setEncoding("utf8");
          response.on("data", (chunk) => {
            responseBody += chunk;
          });
          response.on("end", () => {
            if (response.statusCode !== 200) {
              resolve(false);
              return;
            }
            try {
              const payload = JSON.parse(responseBody);
              resolve(Boolean(payload?.result));
            } catch (error) {
              void error;
              resolve(false);
            }
          });
        }
      );
      request.on("error", () => resolve(false));
      request.setTimeout(1500, () => {
        request.destroy();
        resolve(false);
      });
      request.write(requestBody);
      request.end();
    });

    if (ready) {
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, 500));
  }

  throw new Error("等待 aria2 RPC 启动超时");
}

/**
 * 解析 JRE 可执行文件。优先走安装包内置 JRE，不存在时回退系统 PATH。
 *
 * @returns {string}
 */
function resolveJavaExecutable() {
  const bundledJava = resolveRuntimeResourcePath("windows", "jre", "bin", "java.exe");
  if (process.platform === "win32" && fs.existsSync(bundledJava)) {
    return bundledJava;
  }
  return process.env.MOODDOWNLOAD_JAVA_BIN || (process.platform === "win32" ? "java.exe" : "java");
}

/**
 * 解析 aria2 可执行文件。优先走安装包内置 aria2c.exe，不存在时回退系统 PATH。
 *
 * @returns {string}
 */
function resolveAria2Executable() {
  const bundledAria2 = resolveRuntimeResourcePath("windows", "aria2", "aria2c.exe");
  if (process.platform === "win32" && fs.existsSync(bundledAria2)) {
    return bundledAria2;
  }
  return process.env.MOODDOWNLOAD_ARIA2_BIN || (process.platform === "win32" ? "aria2c.exe" : "aria2c");
}

/**
 * 解析 local-service fat jar 路径。
 *
 * @returns {string}
 */
function resolveLocalServiceJarPath() {
  return resolveRuntimeResourcePath("local-service", "local-service.jar");
}

/**
 * 统一启动受托管子进程，打包后隐藏黑窗，开发态保留终端输出便于排查。
 *
 * @param {string} command 可执行文件
 * @param {string[]} args 参数列表
 * @param {NodeJS.ProcessEnv} env 环境变量
 * @param {string} label 进程标签
 * @returns {import("node:child_process").ChildProcess}
 */
function startManagedProcess(command, args, env, label) {
  const packagedStdout = app.isPackaged ? fs.createWriteStream(resolveManagedLogPath(`${label}-stdout`), { flags: "a" }) : null;
  const packagedStderr = app.isPackaged ? fs.createWriteStream(resolveManagedLogPath(`${label}-stderr`), { flags: "a" }) : null;
  const childProcess = spawn(command, args, {
    env,
    cwd: app.isPackaged ? app.getPath("userData") : process.cwd(),
    windowsHide: true,
    stdio: app.isPackaged ? ["ignore", packagedStdout, packagedStderr] : "inherit"
  });
  childProcess.on("exit", (code, signal) => {
    packagedStdout?.end();
    packagedStderr?.end();
    if (!appQuitting) {
      console.warn(`[managed-runtime] ${label} 已退出`, { code, signal });
    }
  });
  return childProcess;
}

/**
 * 构造打包模式下的运行时状态。
 *
 * @returns {Promise<{
 *   userDataDir: string,
 *   downloadDir: string,
 *   aria2WorkDir: string,
 *   aria2SessionFile: string,
 *   dbPath: string,
 *   localServicePort: number,
 *   aria2RpcPort: number,
 *   localToken: string,
 *   aria2Secret: string
 * }>}
 */
async function buildManagedRuntimeState() {
  const userDataDir = app.getPath("userData");
  const downloadDir = path.join(userDataDir, "downloads");
  const aria2WorkDir = path.join(userDataDir, "aria2");

  ensureDirectory(userDataDir);
  ensureDirectory(downloadDir);
  ensureDirectory(aria2WorkDir);

  return {
    userDataDir,
    downloadDir,
    aria2WorkDir,
    aria2SessionFile: path.join(aria2WorkDir, "session.txt"),
    dbPath: path.join(userDataDir, "mooddownload-local.db"),
    localServicePort: await findAvailablePort(DEFAULT_LOCAL_SERVICE_PORT),
    aria2RpcPort: await findAvailablePort(DEFAULT_ARIA2_RPC_PORT),
    localToken: createSecret(),
    aria2Secret: createSecret()
  };
}

/**
 * 构造 aria2 启动参数。核心 BT 参数直接沿用文档 5.2「生产默认模板」的完整示例。
 *
 * @param {{
 *   downloadDir: string,
 *   aria2SessionFile: string,
 *   aria2RpcPort: number,
 *   aria2Secret: string
 * }} managedRuntimeState 运行时状态
 * @returns {string[]}
 */
function buildAria2Args(managedRuntimeState) {
  return [
    "--enable-rpc",
    "--rpc-listen-all=false",
    `--rpc-listen-port=${managedRuntimeState.aria2RpcPort}`,
    `--rpc-secret=${managedRuntimeState.aria2Secret}`,
    "--enable-dht=true",
    "--enable-dht6=true",
    "--bt-enable-lpd=true",
    "--enable-peer-exchange=true",
    "--listen-port=51413",
    "--dht-listen-port=51413",
    "--seed-time=0",
    "--follow-torrent=true",
    "--follow-metalink=true",
    "--allow-overwrite=true",
    `--bt-tracker=${ARIA2_TRACKER_TEMPLATE}`,
    `--dir=${managedRuntimeState.downloadDir}`,
    `--input-file=${managedRuntimeState.aria2SessionFile}`,
    `--save-session=${managedRuntimeState.aria2SessionFile}`,
    "--save-session-interval=30",
    "--continue=true",
    "--auto-file-renaming=false"
  ];
}

/**
 * 启动内置 aria2 进程。
 *
 * @param {{
 *   downloadDir: string,
 *   aria2SessionFile: string,
 *   aria2RpcPort: number,
 *   aria2Secret: string
 * }} managedRuntimeState 运行时状态
 */
function startManagedAria2(managedRuntimeState) {
  if (process.platform === "win32") {
    const bundledAria2 = resolveRuntimeResourcePath("windows", "aria2", "aria2c.exe");
    if (app.isPackaged && !fs.existsSync(bundledAria2)) {
      throw new Error("安装包缺少内置 aria2c.exe，请先准备 Windows 运行时资源");
    }
  }

  managedAria2Process = startManagedProcess(
    resolveAria2Executable(),
    buildAria2Args(managedRuntimeState),
    process.env,
    "aria2"
  );
}

/**
 * 启动内置 local-service 进程，并将本地 token、SQLite 和 aria2 RPC 参数注入到进程环境。
 *
 * @param {{
 *   dbPath: string,
 *   localServicePort: number,
 *   aria2RpcPort: number,
 *   localToken: string,
 *   aria2Secret: string
 * }} managedRuntimeState 运行时状态
 */
function startManagedLocalService(managedRuntimeState) {
  const localServiceJar = resolveLocalServiceJarPath();
  if (!fs.existsSync(localServiceJar)) {
    throw new Error("安装包缺少 local-service.jar，请先执行 Windows 运行时准备脚本");
  }

  if (process.platform === "win32") {
    const bundledJava = resolveRuntimeResourcePath("windows", "jre", "bin", "java.exe");
    if (app.isPackaged && !fs.existsSync(bundledJava)) {
      throw new Error("安装包缺少内置 JRE，请先准备 Windows JRE 运行时资源");
    }
  }

  managedLocalServiceProcess = startManagedProcess(
    resolveJavaExecutable(),
    ["-Dfile.encoding=UTF-8", "-jar", localServiceJar],
    {
      ...process.env,
      LOCAL_SERVICE_PORT: String(managedRuntimeState.localServicePort),
      LOCAL_SERVICE_TOKEN: managedRuntimeState.localToken,
      ARIA2_RPC_URL: `http://${DEFAULT_LOCAL_SERVICE_HOST}:${managedRuntimeState.aria2RpcPort}/jsonrpc`,
      ARIA2_RPC_SECRET: managedRuntimeState.aria2Secret,
      MOODDOWNLOAD_DB_PATH: managedRuntimeState.dbPath,
      SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || "dev"
    },
    "local-service"
  );
}

/**
 * 打包模式下自动拉起 aria2 与 local-service，并等待后端健康检查通过后再加载界面。
 */
async function ensureManagedRuntime() {
  const managedRuntimeState = await buildManagedRuntimeState();
  runtimeConfig = buildManagedRuntimeConfig(managedRuntimeState.localServicePort, managedRuntimeState.localToken);
  syncRuntimeEnv(runtimeConfig);
  startManagedAria2(managedRuntimeState);
  await waitForAria2Ready(
    managedRuntimeState.aria2RpcPort,
    managedRuntimeState.aria2Secret,
    MANAGED_RUNTIME_TIMEOUT_MS
  );
  startManagedLocalService(managedRuntimeState);
  await waitForLocalServiceReady(runtimeConfig.serviceUrl, MANAGED_RUNTIME_TIMEOUT_MS);
}

/**
 * 打开系统目录选择器，供设置页与新建任务弹窗复用。
 *
 * @param {string | undefined} defaultPath 默认目录
 * @returns {Promise<string | null>}
 */
async function pickDirectory(defaultPath) {
  const dialogResult = await dialog.showOpenDialog(mainWindow || undefined, {
    title: "选择下载目录",
    defaultPath: defaultPath || app.getPath("downloads"),
    properties: ["openDirectory", "createDirectory"]
  });
  if (dialogResult.canceled || !dialogResult.filePaths?.length) {
    return null;
  }
  return dialogResult.filePaths[0];
}

/**
 * 停止受托管的本地进程，避免应用退出后残留后台 Java 或 aria2。
 */
function stopManagedRuntime() {
  [managedLocalServiceProcess, managedAria2Process].forEach((childProcess) => {
    if (!childProcess || childProcess.killed) {
      return;
    }
    try {
      childProcess.kill();
    } catch (error) {
      console.warn("[managed-runtime] 停止子进程失败", error);
    }
  });
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

ipcMain.handle("app:getRuntimeConfig", () => runtimeConfig);
ipcMain.handle("app:pickDirectory", (_event, defaultPath) => pickDirectory(defaultPath));

app.whenReady().then(async () => {
  app.setAppUserModelId("com.mooddownload.desktop");

  try {
    if (shouldUseManagedRuntime()) {
      await ensureManagedRuntime();
    } else {
      runtimeConfig = buildRuntimeConfig();
      syncRuntimeEnv(runtimeConfig);
    }
  } catch (error) {
    console.error("[managed-runtime] 启动失败", error);
    dialog.showErrorBox(
      "MoodDownload 启动失败",
      error instanceof Error ? error.message : "内置运行时启动失败，请检查打包资源是否完整"
    );
    app.quit();
    return;
  }

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

app.on("before-quit", () => {
  appQuitting = true;
  stopManagedRuntime();
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
