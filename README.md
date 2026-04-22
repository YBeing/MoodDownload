# MoodDownload

一个面向 `Windows 10/11` 的桌面下载工具，基于 `Electron + Spring Boot + aria2` 构建，支持常规下载、`BT / Magnet / .torrent`、浏览器下载接管，以及独立的 Windows 安装包交付。

## Features

- 支持 `HTTP / HTTPS`
- 支持 `BT / Magnet / .torrent`
- 支持 `Chrome / Edge` 浏览器下载接管
- 支持右键“发送到 MoodDownload”
- 支持 Windows 安装包分发
- 支持独立浏览器插件安装与配置

## Repository Layout

```text
MoodDownload/
├── desktop-app/         # Electron 桌面端
├── local-service/       # Spring Boot 本地服务
├── browser-extension/   # Chrome / Edge 扩展
└── docs/                # 设计、打包、安装说明
```

核心目录：

- [desktop-app](./desktop-app)：桌面端壳层与前端界面
- [local-service](./local-service)：本地后端服务
- [browser-extension](./browser-extension)：浏览器扩展

## Windows Distribution

当前 Windows 交付分为两部分：

1. 桌面端安装包
   - `MoodDownload-Setup-0.0.1-x64.exe`
2. 浏览器插件
   - `browser-extension.zip`
   - 或 [browser-extension](./browser-extension) 目录

注意：

- `exe` 安装包**不包含**浏览器插件
- 浏览器插件需要单独安装
- 浏览器插件当前不能在桌面端未启动时冷启动应用，所以首次使用前需要先打开 `MoodDownload`

更详细的用户安装说明：

- [docs/windows-user-installation-guide.md](./docs/windows-user-installation-guide.md)

更详细的打包说明：

- [docs/windows-installer-packaging-guide.md](./docs/windows-installer-packaging-guide.md)

## Browser Extension

当前浏览器插件支持：

- `Google Chrome`
- `Microsoft Edge`

安装方式：

1. 解压 `browser-extension.zip`，或直接使用 [browser-extension](./browser-extension) 目录
2. 打开扩展管理页
   - `chrome://extensions/`
   - `edge://extensions/`
3. 开启“开发者模式”
4. 点击“加载已解压的扩展程序”
5. 选择 `browser-extension` 文件夹

插件安装完成后，建议固定到浏览器工具栏，便于查看 `Popup` 状态和进入 `Options` 页面。

插件能力包括：

- 自动接管 `magnet`
- 自动接管 `.torrent`
- 自动接管明确的文件下载链接
- 右键“发送到 MoodDownload”
- `Popup` 检测本地服务状态

当前行为说明：

- 接管成功后，如果桌面端已经在运行，会自动切回 `MoodDownload` 窗口
- 接管成功后，会自动打开新任务详情
- 纯页面跳转型“下载”按钮不会默认拦截

## Browser Extension Config

插件安装后，需要在 `Options / 设置` 页面填写本地服务配置。

### Windows 安装包模式

如果你运行的是正式安装包版 `MoodDownload`，请使用下面这组固定值：

```text
serviceUrl=http://127.0.0.1:18080
localToken=mooddownload-packaged-local-token
clientType=browser-extension
autoCaptureMagnet=true
autoCaptureTorrent=true
autoCaptureDownloadButton=true
captureViaContextMenu=true
```

### 本地开发模式

如果你在本机以开发模式启动项目，请使用下面这组值：

```text
serviceUrl=http://127.0.0.1:18080
localToken=dev-local-token
clientType=browser-extension
autoCaptureMagnet=true
autoCaptureTorrent=true
autoCaptureDownloadButton=true
captureViaContextMenu=true
```

### 参数说明

| 参数 | 说明 |
| --- | --- |
| `serviceUrl` | 本地服务地址 |
| `localToken` | 本地服务访问令牌 |
| `clientType` | 固定填写 `browser-extension` |
| `autoCaptureMagnet` | 自动接管磁力链 |
| `autoCaptureTorrent` | 自动接管 `.torrent` 链接 |
| `autoCaptureDownloadButton` | 自动接管明确的文件下载链接左键点击 |
| `captureViaContextMenu` | 启用右键菜单“发送到 MoodDownload” |

不要使用插件里的占位值：

```text
localToken=change-me-in-prod
```

它不是正式可用配置。

## Build Windows Installer

在 [desktop-app](./desktop-app) 目录执行：

```bash
cd desktop-app
npm install
npm run dist:win
```

安装包默认输出到：

```text
desktop-app/release/MoodDownload-Setup-0.0.1-x64.exe
```

如果只是修改浏览器插件脚本，不需要重新打 `exe`，只需要在浏览器扩展页点击“重新加载”。

## Local Development

本地联调建议先看：

- [docs/local-development-startup.md](./docs/local-development-startup.md)

常用命令：

### 启动本地服务

```bash
cd local-service
mvn spring-boot:run
```

### 启动桌面端

```bash
cd desktop-app
npm install
npm run dev
```

## Docs

- [docs/architecture.md](./docs/architecture.md)
- [docs/windows-installer-packaging-guide.md](./docs/windows-installer-packaging-guide.md)
- [docs/windows-user-installation-guide.md](./docs/windows-user-installation-guide.md)
- [browser-extension/README.md](./browser-extension/README.md)

## Known Limitations

- 浏览器插件不会随 `exe` 自动安装
- 浏览器插件当前仍需手工加载
- 桌面端未启动时，浏览器插件当前不能直接冷启动应用
- 如果本机 `18080` 端口被占用，安装包模式可能无法正常启动本地服务
