# MoodDownload 本地联调启动说明

## 1. 文档目的

本文档用于说明 `MoodDownload` 在本地开发环境下的最小启动方式，覆盖以下模块：

- `aria2` 下载引擎
- `local-service` 本地后端服务
- `desktop-app` Electron 桌面端前端

适用场景：

- 启动桌面端查看真实任务数据
- 开展 `M6` 主链路联调与验收
- 排查任务创建、列表刷新、详情操作、设置保存、剪贴板提示等问题

## 2. 启动前提

本地需提前具备以下环境：

- `Node.js + npm`
- `Java 8`
- `Maven`
- `aria2c`

当前工程的关键约束：

- `local-service` 基于 `Spring Boot 2.7.x + Java 8`
- `desktop-app` 基于 `Electron + React + Vite`
- 前端默认访问本地服务地址 `http://127.0.0.1:18080`
- 开发联调默认本地令牌为 `dev-local-token`

## 3. 默认联调参数

| 模块 | 默认地址 / 端口 | 说明 |
| --- | --- | --- |
| `aria2 RPC` | `http://127.0.0.1:6800/jsonrpc` | 真实创建下载任务依赖该 RPC 服务 |
| `local-service` | `http://127.0.0.1:18080` | 桌面端 REST + SSE 默认接入地址 |
| `desktop-app renderer` | `http://127.0.0.1:5173` | Vite 开发服务 |
| 本地鉴权令牌 | `dev-local-token` | `local-service` 未显式指定 profile 时默认走 `dev` |

## 4. 推荐启动顺序

建议使用 3 个终端窗口，按以下顺序启动：

1. 启动 `aria2`
2. 启动 `local-service`
3. 启动 `desktop-app`

这样可以避免桌面端启动后立即出现创建任务失败、SSE 重连或列表请求鉴权失败。

## 5. 具体启动命令

### 5.1 启动 aria2

```bash
aria2c \
  --enable-rpc \
  --rpc-listen-all=false \
  --rpc-listen-port=6800
```

说明：

- 当前后端默认连接 `127.0.0.1:6800`
- 本地开发默认不传 `rpc-secret`
- 若你后续显式配置了 `rpc-secret`，需要同步设置后端的 `ARIA2_RPC_SECRET`
- 如果只想先看界面、不验证真实下载链路，可以不启动，但创建任务时会失败

### 5.2 启动 local-service

```bash
cd /Users/lying/IdeaProjects/moodDownload/local-service
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home \
mvn spring-boot:run
```

说明：

- 未显式指定 profile 时默认加载 `application-dev.yml`
- 本地联调令牌为 `dev-local-token`
- 默认监听地址为 `127.0.0.1:18080`
- 如需覆盖其他环境，可显式指定 `SPRING_PROFILES_ACTIVE` 或 `-Dspring-boot.run.profiles`

如果你的 `Java 8` 路径不同，请替换 `JAVA_HOME`。

### 5.3 启动 desktop-app

```bash
cd /Users/lying/IdeaProjects/moodDownload/desktop-app
npm install
npm run dev
```

说明：

- `npm run dev` 会同时拉起 `Vite` 和 `Electron`
- 如仅需单独启动渲染层，可执行 `npm run dev:renderer`
- 如仅需单独启动 Electron 壳层，可执行 `npm run dev:electron`

## 6. 可选环境变量

如果你的本地联调地址或令牌不是默认值，可按模块覆盖。

### 6.1 desktop-app 可用变量

- `VITE_LOCAL_SERVICE_URL`
- `VITE_LOCAL_SERVICE_TOKEN`
- `LOCAL_SERVICE_URL`
- `LOCAL_SERVICE_TOKEN`

### 6.2 local-service 可用变量

- `LOCAL_SERVICE_PORT`
- `LOCAL_SERVICE_TOKEN`
- `ARIA2_RPC_URL`
- `ARIA2_RPC_SECRET`

## 7. 最小验证方式

全部启动后，建议至少验证以下内容：

1. 桌面端可以正常打开，不是空白页
2. 底栏本地服务地址显示为 `http://127.0.0.1:18080`
3. 顶部 `SSE` 状态可进入已连接
4. 设置页可以正常加载
5. 新建任务弹窗可以正常打开
6. 若 `aria2` 已启动，创建 URL 或磁力任务后列表能回流新任务

## 8. 常见问题

### 8.1 前端能打开，但列表和设置全报错

通常是 `local-service` 没启动，或前端指向了错误地址。

优先检查：

- `local-service` 是否已启动
- 端口是否仍为 `18080`
- 前端环境变量是否覆盖成了其他地址

### 8.2 请求返回鉴权失败

通常是前后端 token 不一致，或后端没有按本地联调方式加载 `dev` 配置。

建议检查：

- 后端是否被显式指定成了非 `dev` profile
- `LOCAL_SERVICE_TOKEN` / `VITE_LOCAL_SERVICE_TOKEN` 是否被自定义覆盖

### 8.3 页面能加载，但创建任务失败

通常是 `aria2` 未启动，或 RPC 地址 / 密钥不匹配。

优先检查：

- `aria2c` 是否已启动并开启 RPC
- 后端 `ARIA2_RPC_URL` 是否仍为 `http://127.0.0.1:6800/jsonrpc`
- `ARIA2_RPC_SECRET` 是否与 `aria2` 实际配置一致

### 8.4 只想开发前端界面，不想起整条链路

可以只启动：

- `desktop-app`

或者启动：

- `local-service`
- `desktop-app`

此时前者适合纯静态界面开发，后者适合联调列表、设置和基础 REST/SSE，但不适合验证真实下载创建链路。

## 9. 相关文档

- [local-aria2-bt-optimization.md](/Users/lying/IdeaProjects/moodDownload/docs/local-aria2-bt-optimization.md)
- [architecture.md](/Users/lying/IdeaProjects/moodDownload/docs/architecture.md)
- [backend-detailed-design.md](/Users/lying/IdeaProjects/moodDownload/docs/backend-detailed-design.md)
- [frontend-detailed-design.md](/Users/lying/IdeaProjects/moodDownload/docs/frontend-detailed-design.md)
- [implementation-iteration-plan.md](/Users/lying/IdeaProjects/moodDownload/docs/implementation-iteration-plan.md)
