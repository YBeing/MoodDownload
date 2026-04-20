import path from "node:path";
import { spawn } from "node:child_process";

const projectDir = process.cwd();
const npmExecutable = process.platform === "win32" ? "npm.cmd" : "npm";
const nodeExecutable = process.execPath;
const electronBuilderExecutable = path.join(
  projectDir,
  "node_modules",
  ".bin",
  process.platform === "win32" ? "electron-builder.cmd" : "electron-builder"
);

/**
 * 串行执行桌面端打包命令，统一控制构建失败退出码。
 *
 * @param {string} command 命令名
 * @param {string[]} args 命令参数
 * @param {import("node:child_process").SpawnOptions} [options] 扩展参数
 * @returns {Promise<void>}
 */
function runCommand(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const childProcess = spawn(command, args, {
      cwd: projectDir,
      stdio: "inherit",
      ...options
    });
    childProcess.on("exit", (code) => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(`命令执行失败: ${command} ${args.join(" ")}`));
    });
    childProcess.on("error", reject);
  });
}

async function main() {
  console.info("[dist-win] 开始构建前端静态资源");
  await runCommand(npmExecutable, ["run", "build"]);

  console.info("[dist-win] 开始准备 Windows 运行时资源");
  await runCommand(nodeExecutable, ["./scripts/prepare-win-runtime.mjs"]);

  console.info("[dist-win] 开始生成 Windows 安装包");
  await runCommand(electronBuilderExecutable, ["--win", "nsis", "--x64"], {
    env: {
      ...process.env,
      CSC_IDENTITY_AUTO_DISCOVERY: "false"
    }
  });
}

main().catch((error) => {
  console.error("[dist-win] 打包失败", error);
  process.exit(1);
});
