# MoodDownload Windows 安装包打包说明

## 1. 文档目的

本文档用于沉淀 `MoodDownload` 当前已经跑通的 `Windows 10/11` 安装包打包链路，覆盖以下内容：

- `desktop-app` 如何生成 `NSIS` 安装包
- 安装包内如何携带 `local-service.jar`、`Windows JRE`、`aria2c.exe`
- 打包时 `aria2` 默认启动参数如何与当前文档约定保持一致
- 浏览器扩展如何安装、配置并与桌面端协同
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
- [browser-extension/README.md](/Users/lying/IdeaProjects/moodDownload/browser-extension/README.md)

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

### 5.5 `browser-extension`

作用：

- Chrome / Edge Manifest V3 浏览器扩展
- 拦截磁力链、`.torrent`、常见下载按钮左键点击
- 支持右键“发送到 MoodDownload”
- 支持 Popup 检测本地服务状态

说明：

- 浏览器插件当前不打进 `Windows exe` 安装包
- 插件仍以 [browser-extension](/Users/lying/IdeaProjects/moodDownload/browser-extension) 目录的“加载已解压扩展程序”方式安装
- 修改插件脚本后，不需要重新打 `Windows exe`，只需要在浏览器扩展页点击“重新加载”

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

补充说明：

- 浏览器扩展不是 `exe` 产物的一部分
- 如果本次迭代同时改了 `desktop-app / electron / local-service` 与 `browser-extension`
  - `desktop-app / electron / local-service` 需要重新打 `Windows exe`
  - `browser-extension` 需要单独在浏览器中“重新加载”

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

### 9.5 浏览器插件 Popup 一直显示“正在检测本地服务...”

现象：

- 插件已安装
- 点击 `刷新状态` 后，Popup 长时间停留在“正在检测本地服务...”

根因：

- [browser-extension/background.js](/Users/lying/IdeaProjects/moodDownload/browser-extension/background.js) 早期有两个 `chrome.runtime.onMessage` 监听
- 其中“下载接管”监听器会错误拦截非 `capture-download` 消息，导致 `ping-local-service / get-config / get-last-result` 无法正确返回

当前修复：

- 非 `capture-download` 消息直接返回 `false`
- `popup.js` 与 `options.js` 已补充空响应和异常提示

后续规则：

- 浏览器插件如果继续拆分消息类型，必须保证“下载接管消息”和“状态查询消息”不会互相拦截

### 9.6 浏览器插件接管失败报 `仅允许 native-host 或 browser-extension 调用扩展接管接口`

现象：

- 插件与本地服务已连通
- 但右键或左键接管时返回 `UNAUTHORIZED`

根因：

- [local-service/src/main/java/com/mooddownload/local/service/capture/ExtensionCaptureService.java](/Users/lying/IdeaProjects/moodDownload/local-service/src/main/java/com/mooddownload/local/service/capture/ExtensionCaptureService.java) 早期只放行 `native-host`
- 浏览器插件默认发送的 `X-Client-Type` 是 `browser-extension`

当前修复：

- 现已同时放行：
  - `native-host`
  - `browser-extension`

后续规则：

- 插件配置页中的“调用方类型”默认保持 `browser-extension`
- 不要再把浏览器插件误配置为 `desktop-app`

### 9.7 浏览器接管成功但桌面端不切换到下载器窗口

现象：

- 插件接管成功
- 任务已创建
- 但桌面端没有自动切回当前下载器窗口，或没有自动打开任务详情

高概率原因：

- 桌面端只在任务页初始化任务快照，导致停留在设置页等非任务页面时无法及时识别“新外部任务”
- 浏览器接管成功后，主窗口恢复与聚焦链路不完整

当前修复：

- [desktop-app/src/domains/capture/store/capture-context.tsx](/Users/lying/IdeaProjects/moodDownload/desktop-app/src/domains/capture/store/capture-context.tsx) 已在应用启动时主动初始化任务快照
- [desktop-app/electron/main.cjs](/Users/lying/IdeaProjects/moodDownload/desktop-app/electron/main.cjs) 已补充 `window:showAndFocus`
- 接管成功后，桌面端会：
  - 恢复并聚焦主窗口
  - 直接打开新任务详情抽屉

当前限制：

- 仅覆盖“桌面端已经在运行”的场景
- 如果桌面端进程根本没有启动，浏览器扩展当前仍无法直接冷启动应用

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
- 浏览器插件显示已连通，但右键 / 左键接管持续失败

### 10.3 推荐排查顺序

1. 先看 `aria2-stderr.log`
2. 再看 `local-service-stderr.log`
3. 如果是前端白屏或界面异常，再结合 `Electron` 控制台或重新本地跑 `npm run dev`

补充说明：

- `Windows 11` 上如果弹出 `The argument 'stdio' is invalid`，优先回看是否误把 `WriteStream` 直接传回了 `spawn(..., { stdio })`
- 如果弹出 `等待 aria2 RPC 启动超时`，优先检查 `aria2-stderr.log` 中是否有 `input-file`、session 文件不存在、端口监听失败等信息
- 如果浏览器插件接管失败，优先检查 `local-service-stderr.log` 中是否有 `UNAUTHORIZED`、`CAPTURE_DISABLED`、`COMMON_PARAM_INVALID` 等错误

## 11. 浏览器插件安装与配置

当前浏览器插件位于：

- [browser-extension](/Users/lying/IdeaProjects/moodDownload/browser-extension)

当前能力：

- 自动拦截磁力链点击
- 自动拦截 `.torrent` 链接点击
- 自动拦截常见下载按钮左键点击
- 右键“发送到 MoodDownload”
- Popup 查看本地服务状态和最近一次接管结果

### 11.1 安装步骤

以 `Chrome / Edge` 为例：

1. 打开扩展管理页
   - `chrome://extensions/`
   - `edge://extensions/`
2. 打开“开发者模式”
3. 点击“加载已解压的扩展程序”
4. 选择目录：

```text
/Users/lying/IdeaProjects/moodDownload/browser-extension
```

5. 安装完成后，建议固定到浏览器工具栏，便于查看 Popup 状态

### 11.2 插件配置项说明

插件 `Options` 页面当前包含以下字段：

- `serviceUrl`
  - 本地服务地址
  - 开发模式通常为：`http://127.0.0.1:18080`
- `localToken`
  - 本地访问令牌
  - 开发模式通常为：`dev-local-token`
  - 不能使用占位值 `change-me-in-prod`
- `clientType`
  - 固定填：`browser-extension`
- `autoCaptureMagnet`
  - 是否自动拦截磁力链
- `autoCaptureTorrent`
  - 是否自动拦截 `.torrent`
- `autoCaptureDownloadButton`
  - 是否自动拦截常见下载按钮左键点击
- `captureViaContextMenu`
  - 是否启用右键“发送到 MoodDownload”

### 11.3 开发模式推荐配置

如果你当前运行的是源码开发模式，浏览器插件推荐直接使用：

```text
serviceUrl=http://127.0.0.1:18080
localToken=dev-local-token
clientType=browser-extension
autoCaptureMagnet=true
autoCaptureTorrent=true
autoCaptureDownloadButton=true
captureViaContextMenu=true
```

### 11.4 Windows 安装包模式固定配置

如果你当前运行的是 `Windows exe` 安装包，浏览器插件当前固定使用以下配置：

```text
serviceUrl=http://127.0.0.1:18080
localToken=mooddownload-packaged-local-token
clientType=browser-extension
autoCaptureMagnet=true
autoCaptureTorrent=true
autoCaptureDownloadButton=true
captureViaContextMenu=true
```

对应说明：

- `serviceUrl`
  - 安装包模式固定为：`http://127.0.0.1:18080`
- `localToken`
  - 安装包模式固定为：`mooddownload-packaged-local-token`
- `clientType`
  - 固定填：`browser-extension`

### 11.5 Windows 安装包模式下的注意事项

如果你当前运行的是 `Windows exe` 安装包，需要特别注意：

- 必须使用上面的固定配置
- 不能继续使用插件默认占位值：
  - `localToken=change-me-in-prod`

当前已知限制：

- 浏览器插件的安装仍然是“手工加载目录”，不是安装包自动安装
- 如果 `18080` 端口已经被其他程序占用，安装包会直接启动失败，而不会再自动回退随机端口

### 11.6 接管后的窗口行为

当前已实现：

- 浏览器左键 / 右键接管成功后
- 如果桌面端已经在运行，会自动：
  - 切回桌面端主窗口
  - 聚焦窗口
  - 打开当前新任务详情

当前未实现：

- 如果桌面端未启动，浏览器插件无法直接冷启动应用
- 因此测试“自动切回下载器窗口”前，必须先确保桌面端进程已经启动

## 12. 常见命令组合

### 12.1 仅更新后端并重打包

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run build:local-service
npm run dist:win
```

### 12.2 改了前端或 Electron 壳层后重打包

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run build
npm run dist:win
```

### 12.3 想重新准备干净的运行时资源后再打包

直接重新执行：

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run prepare:win-runtime
npm run dist:win
```

### 12.4 只改了浏览器插件，不重打 Windows 包

```text
1. 修改 browser-extension 目录下脚本或页面
2. 在 Chrome / Edge 扩展管理页点击“重新加载”
3. 无需执行 npm run dist:win
```

### 12.5 改了安装包模式固定地址或固定 token 后重打包

如果改了以下任意一项：

- `desktop-app/electron/main.cjs` 中的固定 `local-service` 端口
- `desktop-app/electron/main.cjs` 中的固定 `localToken`

则必须重新执行：

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm run build
npm run dist:win
```

## 13. 后续维护约束

后续如果继续改打包链路，建议优先遵守以下约束：

1. 不要拆掉 `Electron main` 对 `aria2 + local-service` 的托管启动顺序。
2. 不要删除 `vite.config.ts` 里的相对资源路径配置。
3. 不要绕过 `prepare-win-runtime.mjs` 手工塞资源，除非同步回写脚本。
4. 不要擅自修改 `aria2` 的生产默认参数来源，必须继续以 `5.2 生产默认模板` 为准。
5. 如果改了安装包运行目录、日志目录、资源目录，必须同步更新本文档。
6. 如果调整受托管子进程日志方案，不要把 `WriteStream` 直接传给 `spawn(..., { stdio })`。
7. 如果保留 `aria2` 的 `--input-file` / `--save-session` 参数，必须同步保留 session 文件预创建逻辑。
8. 浏览器插件改动默认不进入 `Windows exe`，除非后续明确把插件纳入安装包分发流程。
9. 浏览器插件的 `clientType` 默认保持 `browser-extension`，不要改成 `desktop-app`。
10. 如果要保证“浏览器接管成功后切回下载器窗口”，必须确保桌面端进程已先启动。

## 14. 当前文档对应的已验证结果

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
