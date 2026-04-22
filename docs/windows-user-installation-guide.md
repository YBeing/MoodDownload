# MoodDownload Windows 用户安装说明

## 1. 文档目的

本文档用于给最终用户说明 `MoodDownload` 在 `Windows 10/11` 上的安装与使用方式，覆盖以下内容：

- 如何安装桌面端软件
- 如何安装浏览器插件
- 浏览器插件应该如何配置
- 如何验证浏览器下载接管是否生效

适用场景：

- 首次在 `Windows` 电脑上安装 `MoodDownload`
- 首次为 `Chrome / Edge` 安装浏览器插件
- 安装后验证“左键下载按钮 / 右键发送到 MoodDownload”是否正常工作

## 2. 需要准备的文件

给最终用户时，通常需要提供以下两个文件：

1. `Windows` 安装包
   - `MoodDownload-Setup-0.0.1-x64.exe`
2. 浏览器插件压缩包
   - `browser-extension.zip`

说明：

- `exe` 安装包不包含浏览器插件
- 浏览器插件需要单独安装

## 3. 安装桌面端软件

### 3.1 运行安装包

双击：

```text
MoodDownload-Setup-0.0.1-x64.exe
```

按安装向导完成安装。

### 3.2 首次启动

安装完成后，先启动一次 `MoodDownload`。

重要说明：

- 浏览器插件接管下载前，`MoodDownload` 桌面端必须已经启动
- 如果桌面端没有启动，浏览器插件当前无法直接把应用冷启动拉起

## 4. 安装浏览器插件

当前插件支持：

- `Google Chrome`
- `Microsoft Edge`

### 4.1 解压插件包

先解压：

```text
browser-extension.zip
```

解压后会得到一个 `browser-extension` 文件夹。

### 4.2 在浏览器中加载插件

以 `Chrome / Edge` 为例：

1. 打开扩展管理页
   - `chrome://extensions/`
   - `edge://extensions/`
2. 开启右上角“开发者模式”
3. 点击“加载已解压的扩展程序”
4. 选择刚才解压出来的 `browser-extension` 文件夹

安装完成后，建议把插件固定到浏览器工具栏。

## 5. 浏览器插件配置

安装完成后，打开插件的 `Options / 设置` 页面，填写以下内容。

### 5.1 Windows 安装包模式固定配置

如果你当前用的是正式安装包版 `MoodDownload`，请直接填写：

```text
serviceUrl=http://127.0.0.1:18080
localToken=mooddownload-packaged-local-token
clientType=browser-extension
autoCaptureMagnet=true
autoCaptureTorrent=true
autoCaptureDownloadButton=true
captureViaContextMenu=true
```

### 5.2 字段说明

- `serviceUrl`
  - 本地服务地址
  - 安装包模式固定为：`http://127.0.0.1:18080`
- `localToken`
  - 本地访问令牌
  - 安装包模式固定为：`mooddownload-packaged-local-token`
- `clientType`
  - 固定填写：`browser-extension`
- `autoCaptureMagnet`
  - 是否自动接管磁力链
- `autoCaptureTorrent`
  - 是否自动接管 `.torrent` 下载
- `autoCaptureDownloadButton`
  - 是否自动接管网页上的常见下载按钮左键点击
- `captureViaContextMenu`
  - 是否启用右键“发送到 MoodDownload”

### 5.3 不要使用的默认占位值

不要继续使用插件默认占位值：

```text
localToken=change-me-in-prod
```

这个值不是正式可用配置。

## 6. 安装后验证

建议按下面顺序验证：

### 6.1 验证桌面端已启动

先确认 `MoodDownload` 桌面端已经打开并处于运行状态。

### 6.2 验证插件连通性

1. 点击浏览器工具栏中的插件图标
2. 点击 `刷新状态`
3. 如果显示：

```text
本地服务可连接
```

说明插件已经能连接到桌面端本地服务。

### 6.3 验证右键接管

在任意下载链接上右键，点击：

```text
发送到 MoodDownload
```

正常结果：

- `MoodDownload` 桌面端窗口会切换到前台
- 会自动打开这条新任务的详情

### 6.4 验证左键下载按钮接管

在支持的站点中，直接左键点击下载按钮或下载链接。

当前支持优先覆盖：

- 磁力链
- `.torrent` 链接
- 常见文件下载链接
- 文案或属性明显是“下载”的按钮

正常结果：

- 浏览器会优先拦截
- 下载任务会进入 `MoodDownload`
- 桌面端窗口会切换到前台并打开任务详情

## 7. 常见问题

### 7.1 插件显示“本地服务不可连接”

优先检查：

- `MoodDownload` 桌面端是否已经启动
- 插件配置是否与下列固定值一致：

```text
serviceUrl=http://127.0.0.1:18080
localToken=mooddownload-packaged-local-token
clientType=browser-extension
```

### 7.2 插件安装成功，但点击下载没有反应

优先检查：

- 插件 `Options` 页面中相关开关是否开启
- 桌面端是否已经处于运行状态
- 点击插件 Popup 中的 `刷新状态` 后，是否显示“本地服务可连接”

### 7.3 浏览器接管成功，但没有切回桌面端窗口

当前前提是：

- `MoodDownload` 桌面端必须已经在运行

如果桌面端根本没有启动，浏览器插件当前无法直接把它冷启动拉起。

### 7.4 更新了插件后没有生效

如果更换了新的插件版本，需要：

1. 打开浏览器扩展管理页
2. 找到 `MoodDownload` 插件
3. 点击 `重新加载`

## 8. 当前已知限制

- 浏览器插件当前不随 `exe` 自动安装
- 用户仍需手工加载插件目录
- 浏览器插件当前不能在桌面端未启动时直接冷启动应用
- 如果本机 `18080` 端口被其他程序占用，桌面端可能无法正常启动

## 9. 建议交付方式

给最终用户时，建议一起提供：

1. `MoodDownload-Setup-0.0.1-x64.exe`
2. `browser-extension.zip`
3. 本文档
