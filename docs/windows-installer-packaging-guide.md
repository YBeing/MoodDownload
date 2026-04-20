# MoodDownload Windows 安装包打包说明

## 1. 文档目的

本文档用于沉淀 `MoodDownload` 当前已经跑通的 `Windows 10/11` 安装包打包链路，覆盖以下内容：

- `desktop-app` 如何生成 `NSIS` 安装包
- 安装包内如何携带 `local-service.jar`、`Windows JRE`、`aria2c.exe`
- 打包时 `aria2` 默认启动参数如何与当前文档约定保持一致
- 黑屏、`aria2 RPC 调用失败` 等常见问题如何排查

适用场景：

- 后续再次生成新的 `Windows` 安装包
- 调整 `desktop-app` 打包配置后做回归验证
- 在新机器上复现当前打包流程

## 2. 当前打包方案

当前项目的 `Windows` 安装包采用以下方案：

- 桌面壳：`Electron`
- 前端构建：`React + Vite`
- 安装包工具：`electron-builder + NSIS`
- 本地服务：`Spring Boot fat jar`
- Java 运行时：安装包内置 `Windows JRE`
- 下载引擎：安装包内置 `aria2c.exe`

当前打包后的应用运行模式为：

1. 安装包启动 `Electron`
2. `Electron main` 自动拉起内置 `aria2`
3. `Electron main` 轮询 `aria2 RPC` 就绪
4. `Electron main` 再拉起内置 `local-service`
5. `local-service` 健康检查通过后，主窗口再加载前端界面

这条链路已经在当前工程内通过命令行验证通过。

## 3. 相关文件

### 3.1 打包配置

- [desktop-app/package.json](/Users/lying/IdeaProjects/moodDownload/desktop-app/package.json)
- [desktop-app/vite.config.ts](/Users/lying/IdeaProjects/moodDownload/desktop-app/vite.config.ts)

### 3.2 打包脚本

- [desktop-app/scripts/prepare-win-runtime.mjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/scripts/prepare-win-runtime.mjs)
- [desktop-app/scripts/dist-win.mjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/scripts/dist-win.mjs)

### 3.3 Electron 托管运行时

- [desktop-app/electron/main.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/main.cjs)
- [desktop-app/electron/preload.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/preload.cjs)

### 3.4 关联文档

- [architecture.md](/Users/lying/IdeaProjects/moodDownload/docs/architecture.md)
- [local-development-startup.md](/Users/lying/IdeaProjects/moodDownload/docs/local-development-startup.md)
- [local-aria2-bt-optimization.md](/Users/lying/IdeaProjects/moodDownload/docs/local-aria2-bt-optimization.md)

## 4. 打包前提

执行打包前，当前机器至少需要具备：

- `Node.js + npm`
- `Java 8`
- `Maven`
- `unzip`

说明：

- `electron-builder`、前端依赖会通过 `npm install` 安装到 `desktop-app/node_modules`
- `prepare-win-runtime.mjs` 会调用本机 `mvn` 打出 `local-service.jar`
- `prepare-win-runtime.mjs` 会从官方地址下载 `Windows JRE` 和 `Windows aria2`

## 5. 当前脚本职责

### 5.1 `npm run build`

作用：

- 执行 `TypeScript` 检查
- 构建 `desktop-app/dist`

### 5.2 `npm run build:local-service`

作用：

- 只构建 `local-service` fat jar
- 复制到 `desktop-app/resources/runtime/local-service/local-service.jar`

适合场景：

- 只改了后端
- 不想重复下载 `Windows JRE` 和 `aria2`

### 5.3 `npm run prepare:win-runtime`

作用：

1. 构建 `local-service` fat jar
2. 下载 `Windows JRE`
3. 下载 `Windows aria2`
4. 将它们整理到 `desktop-app/resources/runtime`

目录结构如下：

```text
desktop-app/resources/runtime/
  local-service/
    local-service.jar
  windows/
    jre/
      bin/java.exe
    aria2/
      aria2c.exe
```

### 5.4 `npm run dist:win`

作用：

1. 构建前端静态资源
2. 准备 `Windows` 运行时资源
3. 调用 `electron-builder` 生成 `NSIS` 安装包

这是后续默认使用的完整出包命令。

## 6. 打包操作步骤

在项目根下的 `desktop-app` 目录执行。

### 6.1 首次或依赖变更后安装依赖

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm install
```

### 6.2 日常构建检查

```bash
npm run build
```

### 6.3 准备 Windows 运行时

```bash
npm run prepare:win-runtime
```

### 6.4 生成安装包

```bash
npm run dist:win
```

## 7. 安装包产物位置

打包完成后，默认产物位于：

- `desktop-app/release/MoodDownload-Setup-0.0.1-x64.exe`
- `desktop-app/release/MoodDownload-Setup-0.0.1-x64.exe.blockmap`
- `desktop-app/release/win-unpacked/`

当前真正给测试机安装时，通常只需要：

- `MoodDownload-Setup-0.0.1-x64.exe`

## 8. aria2 打包运行参数

安装包运行时的 `aria2` 启动参数由 [desktop-app/electron/main.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/main.cjs) 中的 `buildAria2Args(...)` 统一生成。

其中 BT 相关核心参数，当前强约束为：

- 必须沿用 [local-aria2-bt-optimization.md](/Users/lying/IdeaProjects/moodDownload/docs/local-aria2-bt-optimization.md) 中 `5.2 生产默认模板` 的完整示例
- 不能在未确认前随意替换 `bt-tracker` 组合

当前实际包含的核心参数为：

```text
--enable-rpc
--rpc-listen-all=false
--rpc-listen-port=<动态端口>
--rpc-secret=<动态密钥>
--enable-dht=true
--enable-dht6=true
--bt-enable-lpd=true
--enable-peer-exchange=true
--listen-port=51413
--dht-listen-port=51413
--seed-time=0
--follow-torrent=true
--follow-metalink=true
--allow-overwrite=true
--bt-tracker=<文档模板中的 tracker 组合>
```

除此之外，安装包运行还额外补了以下必要参数：

```text
--dir=<应用下载目录>
--input-file=<session 文件>
--save-session=<session 文件>
--save-session-interval=30
--continue=true
--auto-file-renaming=false
```

这部分属于安装包运行态要求，不是对文档模板的偏离。

## 9. 已处理过的关键问题

### 9.1 安装包启动黑屏

根因：

- `Vite` 默认把资源写成绝对路径 `/assets/...`
- 安装包内 `index.html` 通过 `file://` 打开时，脚本和样式无法从磁盘根目录加载

当前修复：

- [desktop-app/vite.config.ts](/Users/lying/IdeaProjects/moodDownload/desktop-app/vite.config.ts) 已设置 `base: "./"`

后续规则：

- 不要删除这个配置
- 如果后续改回绝对资源路径，安装包大概率再次黑屏

### 9.2 安装后点击下载报 `aria2 RPC 调用失败`

高概率原因：

- `local-service` 已启动，但 `aria2 RPC` 还没 ready
- 首个下载请求打到了未就绪的 `aria2`

当前修复：

- `Electron main` 先拉起 `aria2`
- 轮询 `aria2.getVersion`
- `aria2 RPC` ready 后才拉起 `local-service`

对应代码见：

- [desktop-app/electron/main.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/main.cjs)

### 9.3 Windows 11 安装后启动即报 `The argument 'stdio' is invalid`

现象：

- 安装完成后首次启动，立即弹窗报错
- 报错信息包含 `The argument 'stdio' is invalid. Received WriteStream`

根因：

- 打包模式下，`Electron main` 曾直接将 `fs.createWriteStream(...)` 传入 `spawn(..., { stdio })`
- 在 `Windows 11 + Electron/Node` 的安装包运行场景中，这种写法会被判定为非法 `stdio` 参数

当前修复：

- [desktop-app/electron/main.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/main.cjs) 已改为 `stdio: ["ignore", "pipe", "pipe"]`
- 子进程 `stdout/stderr` 改为启动后再手动 `pipe` 到日志文件

后续规则：

- 不要再把 `WriteStream` 实例直接传给 `spawn` 的 `stdio`
- 如果后续继续调整日志落盘方案，必须保留“先 `pipe`，再写文件”的实现方式

### 9.4 Windows 11 启动时报 `等待 aria2 RPC 启动超时`

现象：

- 安装包启动时弹出 `等待 aria2 RPC 启动超时`
- 界面尚未进入主页面，应用直接停在启动失败阶段

高概率原因：

- 首次安装时 `aria2/session.txt` 尚未创建
- 启动参数中又包含 `--input-file=<session 文件>`，导致 `aria2` 进程提前退出
- 上层只看到 RPC 一直未 ready，最终表现为超时

当前修复：

- [desktop-app/electron/main.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/main.cjs) 在构造运行时状态时，已先确保 `aria2/session.txt` 存在
- `waitForAria2Ready(...)` 已补充“`aria2` 提前退出”检测，避免继续以泛化超时掩盖真实原因

后续规则：

- 只要保留 `--input-file` / `--save-session`，就必须同步保证 session 文件在首次启动前已创建
- 如果后续修改 `aria2` 工作目录或 session 文件名，必须同步更新初始化逻辑和本文档

## 10. 运行日志定位

打包模式下，`Electron` 已经把内置子进程日志落盘。

### 10.1 关注的日志文件

应用 `logs` 目录下会生成：

- `aria2-stdout.log`
- `aria2-stderr.log`
- `local-service-stdout.log`
- `local-service-stderr.log`

### 10.2 适用场景

如果安装包内出现以下问题，优先看这些日志：

- 应用能打开，但创建任务时报 `aria2 RPC 调用失败`
- 启动时弹出“内置运行时启动失败”
- 启动时弹出 `The argument 'stdio' is invalid`
- 启动时弹出 `等待 aria2 RPC 启动超时`
- 设置能打开，但下载相关接口全部失败

### 10.3 推荐排查顺序

1. 先看 `aria2-stderr.log`
2. 再看 `local-service-stderr.log`
3. 如果是前端白屏或界面异常，再结合 `Electron` 控制台或重新本地跑 `npm run dev`

补充说明：

- `Windows 11` 上如果弹出 `The argument 'stdio' is invalid`，优先回看是否误把 `WriteStream` 直接传回了 `spawn(..., { stdio })`
- 如果弹出 `等待 aria2 RPC 启动超时`，优先检查 `aria2-stderr.log` 中是否有 `input-file`、session 文件不存在、端口监听失败等信息

## 11. 常见命令组合

### 11.1 仅更新后端并重打包

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run build:local-service
npm run dist:win
```

### 11.2 改了前端或 Electron 壳层后重打包

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run build
npm run dist:win
```

### 11.3 想重新准备干净的运行时资源后再打包

直接重新执行：

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run prepare:win-runtime
npm run dist:win
```

## 12. 后续维护约束

后续如果继续改打包链路，建议优先遵守以下约束：

1. 不要拆掉 `Electron main` 对 `aria2 + local-service` 的托管启动顺序。
2. 不要删除 `vite.config.ts` 里的相对资源路径配置。
3. 不要绕过 `prepare-win-runtime.mjs` 手工塞资源，除非同步回写脚本。
4. 不要擅自修改 `aria2` 的生产默认参数来源，必须继续以 `5.2 生产默认模板` 为准。
5. 如果改了安装包运行目录、日志目录、资源目录，必须同步更新本文档。
6. 如果调整受托管子进程日志方案，不要把 `WriteStream` 直接传给 `spawn(..., { stdio })`。
7. 如果保留 `aria2` 的 `--input-file` / `--save-session` 参数，必须同步保留 session 文件预创建逻辑。

## 13. 当前文档对应的已验证结果

以下命令已在当前仓库实际执行通过：

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm install
npm run build
npm run prepare:win-runtime
npm run dist:win
```

并已成功生成：

- `release/MoodDownload-Setup-0.0.1-x64.exe`
