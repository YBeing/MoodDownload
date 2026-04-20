import { createWriteStream } from "node:fs";
import { cp, mkdir, readdir, rm, stat } from "node:fs/promises";
import http from "node:http";
import https from "node:https";
import os from "node:os";
import path from "node:path";
import { pipeline } from "node:stream/promises";
import { spawn } from "node:child_process";

const projectDir = process.cwd();
const repoRoot = path.resolve(projectDir, "..");
const desktopRuntimeDir = path.join(projectDir, "resources", "runtime");
const localServiceDir = path.join(repoRoot, "local-service");
const localServiceTargetDir = path.join(localServiceDir, "target");
const localServiceOutputDir = path.join(desktopRuntimeDir, "local-service");
const windowsRuntimeDir = path.join(desktopRuntimeDir, "windows");
const windowsJreDir = path.join(windowsRuntimeDir, "jre");
const windowsAria2Dir = path.join(windowsRuntimeDir, "aria2");
const localMavenRepo = path.join(repoRoot, ".m2");
const isServiceOnly = process.argv.includes("--service-only");

const mvnExecutable = process.platform === "win32" ? "mvn.cmd" : "mvn";

const WINDOWS_JRE_URL =
  "https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u482-b08/OpenJDK8U-jre_x64_windows_hotspot_8u482b08.zip";
const WINDOWS_ARIA2_URL =
  "https://github.com/aria2/aria2/releases/download/release-1.37.0/aria2-1.37.0-win-64bit-build1.zip";

/**
 * 执行外部命令并继承终端输出，供桌面端打包脚本串联前后端构建。
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

/**
 * 递归查找目标文件路径，用于从压缩包解压目录中定位 JRE 或 aria2 根目录。
 *
 * @param {string} searchDir 搜索目录
 * @param {string} targetFileName 目标文件名
 * @returns {Promise<string | null>}
 */
async function findFilePath(searchDir, targetFileName) {
  const entries = await readdir(searchDir, { withFileTypes: true });
  for (const entry of entries) {
    const absolutePath = path.join(searchDir, entry.name);
    if (entry.isFile() && entry.name === targetFileName) {
      return absolutePath;
    }
    if (entry.isDirectory()) {
      const nestedPath = await findFilePath(absolutePath, targetFileName);
      if (nestedPath) {
        return nestedPath;
      }
    }
  }
  return null;
}

/**
 * 下载官方压缩包到临时目录，兼容 GitHub 重定向。
 *
 * @param {string} downloadUrl 下载地址
 * @param {string} outputFile 输出文件
 * @returns {Promise<void>}
 */
async function downloadFile(downloadUrl, outputFile) {
  const client = downloadUrl.startsWith("https:") ? https : http;
  await new Promise((resolve, reject) => {
    const request = client.get(downloadUrl, (response) => {
      if (
        response.statusCode
        && response.statusCode >= 300
        && response.statusCode < 400
        && response.headers.location
      ) {
        response.resume();
        downloadFile(response.headers.location, outputFile).then(resolve).catch(reject);
        return;
      }

      if (response.statusCode !== 200) {
        reject(new Error(`下载失败: ${downloadUrl}, status=${response.statusCode}`));
        return;
      }

      pipeline(response, createWriteStream(outputFile)).then(resolve).catch(reject);
    });
    request.on("error", reject);
  });
}

/**
 * 解压 zip 压缩包。Windows 下走 PowerShell，其余平台走 unzip。
 *
 * @param {string} zipFile 压缩包路径
 * @param {string} targetDir 解压目录
 * @returns {Promise<void>}
 */
async function extractZip(zipFile, targetDir) {
  await mkdir(targetDir, { recursive: true });
  if (process.platform === "win32") {
    await runCommand("powershell.exe", [
      "-NoProfile",
      "-Command",
      `Expand-Archive -Force -Path '${zipFile.replace(/'/g, "''")}' -DestinationPath '${targetDir.replace(/'/g, "''")}'`
    ]);
    return;
  }

  await runCommand("unzip", ["-oq", zipFile, "-d", targetDir]);
}

/**
 * 构建 Spring Boot fat jar，并复制到桌面安装包运行时目录。
 *
 * @returns {Promise<void>}
 */
async function prepareLocalServiceJar() {
  console.info("[prepare-win-runtime] 开始构建 local-service fat jar");
  await runCommand(mvnExecutable, ["-q", "-DskipTests", `-Dmaven.repo.local=${localMavenRepo}`, "package"], {
    cwd: localServiceDir,
    env: process.env
  });

  const jarCandidates = (await readdir(localServiceTargetDir))
    .filter((fileName) => fileName.endsWith(".jar") && !fileName.endsWith(".original"))
    .map((fileName) => path.join(localServiceTargetDir, fileName));

  if (jarCandidates.length === 0) {
    throw new Error("未找到 local-service fat jar，请检查 Maven 打包结果");
  }

  let selectedJar = jarCandidates[0];
  let latestMtimeMs = 0;
  for (const jarFile of jarCandidates) {
    const jarStat = await stat(jarFile);
    if (jarStat.mtimeMs >= latestMtimeMs) {
      latestMtimeMs = jarStat.mtimeMs;
      selectedJar = jarFile;
    }
  }

  await rm(localServiceOutputDir, { recursive: true, force: true });
  await mkdir(localServiceOutputDir, { recursive: true });
  await cp(selectedJar, path.join(localServiceOutputDir, "local-service.jar"));
  console.info("[prepare-win-runtime] local-service fat jar 已复制完成");
}

/**
 * 下载并准备 Windows JRE 运行时。
 *
 * @returns {Promise<void>}
 */
async function prepareWindowsJre() {
  const bundledJava = path.join(windowsJreDir, "bin", "java.exe");
  try {
    await stat(bundledJava);
    console.info("[prepare-win-runtime] 已存在 Windows JRE，跳过下载");
    return;
  } catch (error) {
    void error;
  }

  const tempRootDir = path.join(os.tmpdir(), `mooddownload-jre-${Date.now()}`);
  const zipFile = path.join(tempRootDir, "temurin-jre.zip");
  const extractDir = path.join(tempRootDir, "extract");

  await mkdir(tempRootDir, { recursive: true });
  console.info("[prepare-win-runtime] 开始下载 Windows JRE");
  await downloadFile(WINDOWS_JRE_URL, zipFile);
  await extractZip(zipFile, extractDir);

  const javaFile = await findFilePath(extractDir, "java.exe");
  if (!javaFile) {
    throw new Error("Windows JRE 解压完成后未找到 java.exe");
  }

  const jreRootDir = path.dirname(path.dirname(javaFile));
  await rm(windowsJreDir, { recursive: true, force: true });
  await mkdir(path.dirname(windowsJreDir), { recursive: true });
  await cp(jreRootDir, windowsJreDir, { recursive: true });
  await rm(tempRootDir, { recursive: true, force: true });
  console.info("[prepare-win-runtime] Windows JRE 已准备完成");
}

/**
 * 下载并准备 Windows aria2 二进制。
 *
 * @returns {Promise<void>}
 */
async function prepareWindowsAria2() {
  const bundledAria2 = path.join(windowsAria2Dir, "aria2c.exe");
  try {
    await stat(bundledAria2);
    console.info("[prepare-win-runtime] 已存在 Windows aria2，跳过下载");
    return;
  } catch (error) {
    void error;
  }

  const tempRootDir = path.join(os.tmpdir(), `mooddownload-aria2-${Date.now()}`);
  const zipFile = path.join(tempRootDir, "aria2.zip");
  const extractDir = path.join(tempRootDir, "extract");

  await mkdir(tempRootDir, { recursive: true });
  console.info("[prepare-win-runtime] 开始下载 Windows aria2");
  await downloadFile(WINDOWS_ARIA2_URL, zipFile);
  await extractZip(zipFile, extractDir);

  const aria2File = await findFilePath(extractDir, "aria2c.exe");
  if (!aria2File) {
    throw new Error("Windows aria2 解压完成后未找到 aria2c.exe");
  }

  const aria2RootDir = path.dirname(aria2File);
  await rm(windowsAria2Dir, { recursive: true, force: true });
  await mkdir(path.dirname(windowsAria2Dir), { recursive: true });
  await cp(aria2RootDir, windowsAria2Dir, { recursive: true });
  await rm(tempRootDir, { recursive: true, force: true });
  console.info("[prepare-win-runtime] Windows aria2 已准备完成");
}

async function main() {
  await mkdir(desktopRuntimeDir, { recursive: true });
  await prepareLocalServiceJar();

  if (isServiceOnly) {
    return;
  }

  await Promise.all([prepareWindowsJre(), prepareWindowsAria2()]);
  console.info("[prepare-win-runtime] Windows 运行时资源全部准备完成");
}

main().catch((error) => {
  console.error("[prepare-win-runtime] 运行失败", error);
  process.exit(1);
});
