import { spawn } from "node:child_process";
import process from "node:process";
import { setTimeout as delay } from "node:timers/promises";

const devServerUrl = "http://127.0.0.1:5173";
const npmExecutable = process.platform === "win32" ? "npm.cmd" : "npm";

/**
 * 启动子进程并继承终端输出，确保桌面端开发脚本只维护一套入口。
 *
 * @param {string[]} args 命令参数
 * @param {NodeJS.ProcessEnv} [env] 环境变量
 * @returns {import("node:child_process").ChildProcess}
 */
function startProcess(args, env = process.env) {
  return spawn(npmExecutable, args, {
    cwd: process.cwd(),
    stdio: "inherit",
    env
  });
}

/**
 * 等待 Vite 开发服务器就绪，避免 Electron 提前启动导致空白页。
 */
async function waitForDevServer() {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      const response = await fetch(devServerUrl);
      if (response.ok) {
        return;
      }
    } catch (error) {
      void error;
    }
    await delay(1000);
  }
  throw new Error("等待 Vite 开发服务超时");
}

const rendererProcess = startProcess(["run", "dev:renderer"]);
let electronProcess = null;

const cleanup = () => {
  if (electronProcess && !electronProcess.killed) {
    electronProcess.kill("SIGTERM");
  }
  if (!rendererProcess.killed) {
    rendererProcess.kill("SIGTERM");
  }
};

process.on("SIGINT", () => {
  cleanup();
  process.exit(0);
});
process.on("SIGTERM", () => {
  cleanup();
  process.exit(0);
});

rendererProcess.on("exit", (code) => {
  if (!electronProcess) {
    process.exit(code ?? 1);
  }
});

try {
  await waitForDevServer();
  electronProcess = startProcess(["run", "dev:electron"], {
    ...process.env,
    VITE_DEV_SERVER_URL: devServerUrl
  });
  electronProcess.on("exit", (code) => {
    cleanup();
    process.exit(code ?? 0);
  });
} catch (error) {
  console.error("[desktop-app] 启动开发环境失败", error);
  cleanup();
  process.exit(1);
}
