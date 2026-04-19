# MoodDownload Desktop App

`desktop-app` 是 MoodDownload 的桌面端前端工程，当前由 `Electron + React + Vite + TypeScript` 组成。

## 启动方式

1. 安装依赖：`npm install`
2. 一键启动开发模式：`npm run dev`
3. 仅启动渲染层：`npm run dev:renderer`
4. 仅启动 Electron 壳层：`npm run dev:electron`
5. 构建前端静态产物：`npm run build`

完整的前后端联调启动顺序、`aria2` 前置要求和常见问题见：

- [local-development-startup.md](/Users/lying/IdeaProjects/moodDownload/docs/local-development-startup.md)

## 环境变量

可复制 `.env.example` 作为本地环境配置：

- `VITE_LOCAL_SERVICE_URL`
- `VITE_LOCAL_SERVICE_TOKEN`

Electron 预加载脚本也支持：

- `LOCAL_SERVICE_URL`
- `LOCAL_SERVICE_TOKEN`

默认连接地址为 `http://127.0.0.1:18080`，默认令牌为 `dev-local-token`。

## 当前阶段范围

F1 已完成内容：

- Electron 主窗口、预加载桥接与托盘最小化入口
- React 路由、桌面壳层、主题与全局弹窗承载
- 本地 REST Client 与自定义 Header SSE Client
- 任务页基础视图、设置页基础表单、任务详情抽屉骨架

后续阶段继续实现：

- F2：任务列表体验与实时刷新打磨
- F3：新建任务完整创建链路与详情抽屉操作
- F4：接入提示、剪贴板提示与系统联动
