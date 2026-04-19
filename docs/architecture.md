# MoodDownload 架构文档

## 1. 项目概述

`MoodDownload` 是一个面向 `Windows 10/11` 个人用户的本地下载软件，目标是首版提供接近 `Motrix` 的核心下载能力，并支持浏览器扩展接管下载。

首版能力范围：

- 支持 `HTTP/HTTPS`
- 支持 `BT`
- 支持 `磁力链接`
- 支持浏览器下载接管
- 支持 `暂停/继续`
- 支持 `断点续传`
- 支持 `并发下载`
- 支持 `限速`
- 支持 `队列管理`
- 支持 `失败重试`
- 支持 `下载目录管理`
- 支持 `剪贴板监听`
- 提供 `Windows NSIS` 安装包

首版明确不做：

- `thunder://`
- 登录账号
- 云同步
- 国际化
- 远程下发
- 计划任务
- BT 做种、分享率、高级端口配置
- 自动更新
- Redis、MQ、搜索服务等额外基础设施

## 2. 总体架构

项目采用 `桌面端 + 本地服务 + 下载内核 + 浏览器扩展` 的模块化本地架构。

### 2.1 架构分层

1. `Electron Desktop App`
   负责桌面 UI、系统托盘、设置页、任务列表、安装包集成，与本地服务通信。

2. `Spring Boot Local Service`
   负责业务编排、任务调度、配置管理、aria2 进程管理、SQLite 持久化，并对前端提供本地 `REST API` 和 `SSE`。

3. `aria2 Download Engine`
   作为下载执行内核，通过 `JSON-RPC` 供本地服务统一调度，承担 `HTTP/HTTPS`、`BT`、`Magnet` 下载。

4. `Browser Extension`
   负责浏览器侧下载拦截和下载请求上报，通过 `Native Messaging` 与本地程序通信。

5. `SQLite`
   用于本地持久化任务元数据、用户配置、下载历史、失败记录。

### 2.2 选型结论

- 前端：`React + Electron + Vite`
- 后端：`Spring Boot`
- 数据库：`SQLite`
- 本地 API：`REST + SSE`
- 下载引擎：`aria2`
- 浏览器接管：`Browser Extension + Native Messaging`
- 安装包：`electron-builder + NSIS`

## 3. 模块设计

### 3.1 桌面端模块

- `dashboard`
  展示下载中、已完成、已暂停、失败任务列表。
- `task-detail`
  展示任务详情、进度、速度、保存路径、错误信息。
- `create-task`
  支持输入 URL、磁力链接、导入种子文件。
- `settings`
  配置默认下载目录、全局限速、并发数、剪贴板监听、浏览器接管开关。
- `tray`
  提供托盘菜单、显示主窗口、退出程序等能力。

### 3.2 本地服务模块

- `task-api`
  提供任务增删改查、暂停、继续、重试接口。
- `task-scheduler`
  负责下载队列、并发控制、失败重试策略。
- `aria2-adapter`
  封装 aria2 RPC，管理下载状态同步、子进程生命周期。
- `extension-gateway`
  接收浏览器扩展发来的下载请求。
- `clipboard-monitor`
  监听剪贴板并识别 URL、磁力链接。
- `config-service`
  管理用户设置与运行参数。
- `event-stream`
  通过 `SSE` 向桌面端推送任务状态变化。
- `persistence`
  基于 `SQLite` 存储任务、配置、历史记录。

### 3.3 浏览器扩展模块

- `download-capture`
  拦截浏览器下载请求。
- `service-worker`
  负责消息桥接和 Native Messaging 调用。
- `options`
  提供扩展启用状态与接管配置。

## 4. 核心流程

### 4.1 浏览器下载接管

1. 用户在浏览器中点击下载链接。
2. 浏览器扩展识别可接管下载请求。
3. 扩展通过 `Native Messaging` 将下载信息发送给本地程序。
4. 本地服务创建下载任务并写入 `SQLite`。
5. 本地服务调用 aria2 发起下载。
6. 桌面端通过 `SSE` 获取任务状态并实时刷新界面。

### 4.2 手动新增任务

1. 用户在桌面端输入 URL / 磁力链接，或导入种子文件。
2. 桌面端调用本地服务创建任务。
3. 本地服务校验参数并保存任务。
4. aria2 开始执行下载。
5. 前端订阅任务状态更新并展示结果。

### 4.3 应用重启恢复

1. 本地服务启动后读取 `SQLite` 中未完成任务。
2. 恢复任务状态并重新建立 aria2 管理关系。
3. 对处于待执行状态的任务重新入队。
4. 桌面端加载后展示恢复结果。

## 5. 通信设计

### 5.1 通信方式

- `Electron -> Spring Boot`：`REST`
- `Spring Boot -> Electron`：`SSE`
- `Spring Boot -> aria2`：`JSON-RPC`
- `Browser Extension -> Native Host`：`Native Messaging`

### 5.2 设计说明

- `REST` 负责命令型操作，例如创建、暂停、继续、删除任务。
- `SSE` 负责状态推送，例如下载速度、进度、状态变更。
- 首版不引入 `WebSocket`，降低实现复杂度。
- 本地服务默认监听 `127.0.0.1`，避免暴露到局域网。

## 6. 数据模型设计

### 6.1 下载任务表 `t_download_task`

- `id`
- `gid`
- `type`
- `source`
- `torrent_file_path`
- `name`
- `status`
- `save_dir`
- `file_size`
- `completed_size`
- `download_speed`
- `upload_speed`
- `error_code`
- `error_message`
- `retry_count`
- `created_at`
- `updated_at`

### 6.2 配置表 `t_download_config`

- `id`
- `default_save_dir`
- `max_concurrent_downloads`
- `max_global_download_speed`
- `max_global_upload_speed`
- `clipboard_monitor_enabled`
- `browser_capture_enabled`
- `auto_start_enabled`

### 6.3 历史记录表 `t_download_history`

- `id`
- `task_id`
- `final_status`
- `completed_at`

## 7. API 草案

- `POST /api/tasks`
  创建下载任务
- `GET /api/tasks`
  查询任务列表
- `GET /api/tasks/{id}`
  查询任务详情
- `POST /api/tasks/{id}/pause`
  暂停任务
- `POST /api/tasks/{id}/resume`
  继续任务
- `POST /api/tasks/{id}/retry`
  重试任务
- `DELETE /api/tasks/{id}`
  删除任务
- `GET /api/config`
  获取配置
- `PUT /api/config`
  更新配置
- `GET /api/events/tasks`
  订阅任务状态 `SSE`
- `POST /api/extension/capture`
  浏览器扩展发起下载请求

## 8. 目录结构建议

```text
project-root/
  desktop-app/
  local-service/
  browser-extension/
  installer/
  docs/
```

建议继续细化为：

```text
desktop-app/
  src/
  electron/
  resources/

local-service/
  src/main/java/
  src/main/resources/

browser-extension/
  src/
  public/
```

## 9. 两周开发节奏建议

### 第 1-3 天

- 搭建 `Electron`、`Spring Boot`、`SQLite`、`aria2` 基础骨架
- 打通桌面端与本地服务通信

### 第 4-6 天

- 实现任务创建、任务列表、状态同步
- 完成暂停、继续、删除、重试等基础能力

### 第 7-9 天

- 实现浏览器扩展与 `Native Messaging`
- 完成浏览器下载接管与剪贴板监听

### 第 10-12 天

- 实现设置页、限速、并发、失败重试、恢复逻辑
- 完成托盘交互和主要 UI 收尾

### 第 13-14 天

- 联调与问题修复
- 生成 `NSIS` 安装包
- 完成基础验收

## 10. 风险与约束

- `Native Messaging` 的安装、扩展 ID、注册表配置是首版高风险点。
- aria2 与本地数据库状态同步必须避免状态漂移。
- 两周周期较紧，首版应优先保证主链路稳定，不建议追加复杂 BT 功能。
- 自动更新、远程控制、多端同步应留到后续版本。

## 11. 参考依据

- aria2 官方手册：<https://aria2.github.io/manual/en/html/README.html>
- Chrome Native Messaging 官方文档：<https://developer.chrome.com/docs/extensions/develop/concepts/native-messaging>
- Edge Native Messaging 官方文档：<https://learn.microsoft.com/en-us/microsoft-edge/extensions/developer-guide/native-messaging>
- electron-builder Auto Update 文档：<https://www.electron.build/auto-update.html>
- Spring Boot 官方项目页：<https://spring.io/projects/spring-boot>
- SQLite 官方文档：<https://www.sqlite.org/about.html>
