# MoodDownload 本地开发 aria2 BT 优化操作文档

## 1. 文档目的

本文档用于指导 `MoodDownload` 在本地开发环境下优化 `aria2` 的 BT / 磁力下载能力，重点解决以下问题：

- `magnet` 或 `.torrent` 任务能创建，但下载速度长期为 `0`
- 任务详情已经能看到文件列表，但一直拿不到有效 peers
- `aria2` 只开启了 RPC，未启用 DHT / PEX / LPD / tracker 注入
- 本地联调时希望尽量复现真实 BT 下载行为

本文档仅面向本地开发和联调，不涉及最终桌面安装包内置 `aria2` 的产品化托管方案。

## 2. 典型现象

如果出现下面这些现象，基本都可以按本文档处理：

- 任务创建成功，状态为 `RUNNING`，但 `downloadSpeedBps = 0`
- 磁力任务长时间只有元数据，没有实际下载速度
- 相同资源在其他 BT 客户端有速度，但当前本地联调环境没有速度
- 热门公开种子也长期为 `0`

## 3. 原因判断

### 3.1 最常见原因

- `aria2` 启动参数过少，只开了 RPC，没有启用 DHT / PEX / LPD
- `tracker` 太少、失效或不可访问
- 本机和路由器没有放行 BT 监听端口，导致入站连接能力弱
- 当前网络限制 P2P 或限制 UDP
- 资源本身做种少，或者私有种子缺少有效 tracker / passkey

### 3.2 结论先说

如果你现在本地只是这样启动：

```bash
aria2c \
  --enable-rpc \
  --rpc-listen-all=false \
  --rpc-listen-port=6800
```

那只能保证 RPC 可调用，不能保证 BT 下载质量。

## 4. 推荐本地开发启动方式

本地联调时，建议至少使用下面这组参数启动 `aria2`：

```bash
aria2c \
  --enable-rpc \
  --rpc-listen-all=false \
  --rpc-listen-port=6800 \
  --enable-dht=true \
  --enable-dht6=true \
  --bt-enable-lpd=true \
  --enable-peer-exchange=true \
  --listen-port=51413 \
  --dht-listen-port=51413 \
  --seed-time=0 \
  --follow-torrent=true \
  --follow-metalink=true
```

参数说明：

- 本地开发默认不传 `rpc-secret`
- `--enable-dht=true`：启用 IPv4 DHT，磁力链路几乎必备
- `--enable-dht6=true`：启用 IPv6 DHT
- `--bt-enable-lpd=true`：局域网 Peer Discovery，本地网络可辅助发现 peers
- `--enable-peer-exchange=true`：启用 Peer Exchange
- `--listen-port=51413`：BT 监听端口
- `--dht-listen-port=51413`：DHT 监听端口
- `--seed-time=0`：下载完成后不继续做种，适合本地开发
- `--follow-torrent=true`：允许自动跟随种子任务
- `--follow-metalink=true`：保留对 metalink 的默认支持

## 5. tracker 优化建议

### 5.1 为什么要补 tracker

单靠 DHT 并不总是够用，尤其是：

- 新磁力链接
- 冷门资源
- 某些网络环境对 DHT / UDP 不友好

所以建议显式注入一批公共 tracker。下面这组值可以直接作为当前项目的默认模板使用。

### 5.2 生产默认模板

可以在启动参数里追加：

```bash
--bt-tracker="udp://tracker.opentrackr.org:1337/announce,udp://tracker.torrent.eu.org:451/announce,udp://open.stealth.si:80/announce,udp://tracker.tryhackx.org:6969/announce,udp://tracker.qu.ax:6969/announce,https://tracker.opentrackr.org:443/announce"
```

完整示例：

```bash
aria2c \
  --enable-rpc \
  --rpc-listen-all=false \
  --rpc-listen-port=6800 \
  --enable-dht=true \
  --enable-dht6=true \
  --bt-enable-lpd=true \
  --enable-peer-exchange=true \
  --listen-port=51413 \
  --dht-listen-port=51413 \
  --seed-time=0 \
  --follow-torrent=true \
  --follow-metalink=true \
  --allow-overwrite=true \
  --bt-tracker="udp://tracker.opentrackr.org:1337/announce,udp://tracker.torrent.eu.org:451/announce,udp://open.stealth.si:80/announce,udp://tracker.tryhackx.org:6969/announce,udp://tracker.qu.ax:6969/announce,https://tracker.opentrackr.org:443/announce"
```

说明：

- 这组值的目标不是“尽可能多”，而是“来源明确、当前可用、适合做默认值”
- 公共 tracker 可能随时间失效，需要定期替换
- 私有种子不要混用公共 tracker；私有站点一般要求使用资源自带 tracker
- 公共 tracker 没有 SLA，这里给的是“当前可直接作为生产默认值的稳定组合”，不是永久固定名单

### 5.3 当前默认值来源

当前文档里的默认 tracker 组合来自两类来源：

- 官方明确公开可用地址的站点
  - OpenTrackr: https://opentrackr.org/
  - torrent.eu.org: https://torrent.eu.org/
- 持续监控可用性的公共 tracker 状态页
  - newTrackon: https://newtrackon.com/

选型策略：

- 优先保留来源明确的 `OpenTrackr` 和 `torrent.eu.org`
- 再补少量 `newTrackon` 上近期 uptime 较高、延迟和可达性较好的公共 tracker
- 不再把 `open.demonii.com` 作为默认推荐项

建议把这组默认值沉淀到你后续的桌面端内置 `aria2` 启动参数里，而不是让用户手工维护。

### 5.4 后续维护建议

如果要把这组参数用于长期生产默认值，建议这样维护：

1. 固定保留 `OpenTrackr` 和 `torrent.eu.org`
2. 每隔一段时间从 `newTrackon` 或 `trackerslist` 刷新补充项
3. 把默认列表控制在 4 到 8 条，不要无限堆积
4. 私有种子场景禁用公共 tracker 注入，避免破坏私有站规则
5. 每次调整默认列表前，先用一个热门公开种子做联调回归，确认 peers 和速度没有明显退化

## 6. 端口与网络要求

如果要让本地 BT 下载表现稳定，建议同时检查本机和路由器。

### 6.1 本机侧

- 确保防火墙未拦截 `51413/TCP`
- 确保防火墙未拦截 `51413/UDP`
- 确保本机网络允许 P2P 出站

### 6.2 路由器侧

- 若在家庭网络下，建议把 `51413` 做端口映射到当前开发机
- 如果不做映射，BT 仍可能能下，但 peers 数通常更差

### 6.3 公司网络 / 校园网

如果你在公司网、校园网、云桌面环境里联调：

- 很可能会遇到 UDP 被限
- 很可能会遇到 DHT 不可用
- 很可能会遇到 NAT 严格导致长期无速度

这种情况下，本地代码正常也可能跑不出速度。

## 7. 推荐联调步骤

建议按下面顺序验证，而不是直接用冷门资源试：

1. 用上文推荐参数启动 `aria2`
2. 启动 `local-service`
3. 启动 `desktop-app`
4. 先导入一个热门公开 `magnet` 或 `.torrent`
5. 观察任务详情中的文件列表、进度、速度是否开始刷新

### 7.1 推荐验证样本

如果你当前主要是想验证“`magnet` 能否识别、文件列表能否解析、下载速度能否起来”，建议优先使用下面这个样本：

- 样本：`Debian 13.4 amd64 netinst`
- 体积：约 `754 MB`
- 适用原因：体积相对较小，公开来源明确，做种数通常明显高于冷门 Linux 发行版镜像，更适合国内网络联调

推荐 `magnet`：

```text
magnet:?xt=urn:btih:3b1de9cb7011350fa152ec47419620aa153e19e7&dn=debian-13.4.0-amd64-netinst.iso&tr=http%3A%2F%2Fbttracker.debian.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Ftracker.torrent.eu.org%3A451%2Fannounce&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.tryhackx.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.qu.ax%3A6969%2Fannounce&tr=https%3A%2F%2Ftracker.opentrackr.org%3A443%2Fannounce
```

说明：

- 这个样本自带 Debian 官方 `bttracker.debian.org`
- 额外补入了本文推荐的公共 tracker，便于验证你的默认配置是否生效
- 如果这个样本也长期 `0`，优先怀疑本机网络、端口、防火墙、DHT/UDP，而不是资源本身

判断标准：

- 热门种子有速度：说明 BT 主链路基本正常
- 热门种子长期 `0`：优先查 `aria2` 启动参数、端口、防火墙、网络环境
- 热门种子正常，冷门种子 `0`：大概率是资源本身没人做种，不是系统 bug

## 8. 本地排查顺序

### 8.1 第一步：确认不是前端显示问题

当前系统已经会把 aria2 的速度、进度、文件列表同步到本地任务表。

所以如果桌面端详情页显示 `0`：

- 先看后端日志里有没有同步成功
- 再查本地 SQLite 中任务的 `download_speed_bps`

如果数据库里也是 `0`，那就是 aria2 本身确实没有拿到速度。

### 8.2 第二步：确认磁力是否拿到了元数据

对于 `magnet`：

- 能看到文件列表，说明元数据已经拿到了
- 拿到元数据后仍长期 `0`，就不是“磁力未解析”的问题，而是 peers / tracker 问题

### 8.3 第三步：用热门资源做对照

不要只用冷门资源排查。

建议至少准备两类测试样本：

- 热门公开资源
- 你当前实际业务资源

如果热门资源正常、业务资源异常，优先看资源可用性，不要先怀疑代码。

### 8.4 第四步：观察 aria2 自身日志

重点看：

- tracker announce 是否成功
- 是否拿到 peers
- 是否报 `timeout`
- 是否报 `DHT is disabled`
- 是否报端口监听失败

## 9. 当前项目建议

结合当前代码结构，本地开发阶段建议这样执行：

### 9.1 先保持后端连接参数不变

当前后端默认连接：

- `ARIA2_RPC_URL=http://127.0.0.1:6800/jsonrpc`
- `ARIA2_RPC_SECRET=空`

本地优化阶段只改 `aria2c` 启动方式，不必先改 `local-service` 配置。

### 9.2 把优化后的启动命令写进你的本地脚本

建议你在本地维护一个启动脚本，例如：

```bash
#!/usr/bin/env bash

aria2c \
  --enable-rpc \
  --rpc-listen-all=false \
  --rpc-listen-port=6800 \
  --enable-dht=true \
  --enable-dht6=true \
  --bt-enable-lpd=true \
  --enable-peer-exchange=true \
  --listen-port=51413 \
  --dht-listen-port=51413 \
  --seed-time=0 \
  --follow-torrent=true \
  --follow-metalink=true \
  --allow-overwrite=true \
  --bt-tracker="udp://tracker.opentrackr.org:1337/announce,udp://tracker.torrent.eu.org:451/announce,udp://open.stealth.si:80/announce,udp://tracker.tryhackx.org:6969/announce,udp://tracker.qu.ax:6969/announce,https://tracker.opentrackr.org:443/announce"
```

这样后续联调时不需要每次手敲参数。

## 10. 验收标准

你可以按下面标准判断“本地开发优化”是否达标：

- `magnet` 任务可以正常创建
- `magnet` 任务能够解析并展示文件列表
- 热门公开 BT 资源能出现非 `0` 下载速度
- 删除重建相同 BT 任务时，不会因重复 infohash 导致异常体验
- 后端日志中不再长期重复刷“重复注册”或“无速度但无任何排查入口”的问题

## 11. 后续演进建议

本文档解决的是本地开发联调。

如果后续做正式桌面安装包，建议进一步演进为：

- 桌面端内置并托管 `aria2c`
- 应用自动生成 `rpc-secret`
- 应用自动注入 DHT / tracker / 监听端口参数
- 设置页暴露少量高层配置，而不是暴露底层命令行参数
- 任务详情页增加 BT 诊断信息，例如 peers 数、tracker 状态、metadata 状态

## 12. Windows 打包说明

如果后续前后端和下载引擎一起打包成 Windows 桌面软件，推荐做法不是要求用户自行安装 `aria2`，而是由应用内置并托管 `aria2c.exe`。

推荐形态：

- 安装包内直接附带 `aria2c.exe`
- 桌面程序启动时自动拉起 `aria2c.exe`
- 桌面程序退出时自动关闭 `aria2c.exe`
- `rpc-secret`、端口、DHT、tracker、监听端口统一由程序自动注入

结论：

- 如果你做的是“内置托管”方案，那么 Windows 端即使没有单独安装 `aria2`，软件也可以正常工作
- 如果你只是把前后端打包，但没有随包分发 `aria2c.exe`，那用户机器没装 `aria2` 时，下载链路仍然不可用

## 13. 本地重启与关闭说明

### 13.1 什么时候需要先关闭

如果你只是阅读文档或查看当前配置，不需要先关闭服务。

如果你准备：

- 用新的 BT 优化参数重启 `aria2`
- 重启 `local-service`
- 避免端口冲突

那就应该先停掉当前旧进程。

### 13.2 最简单的关闭方式

如果服务是直接在当前终端前台运行的，优先使用：

```bash
Ctrl + C
```

### 13.3 按端口关闭 local-service

先查占用 `18080` 端口的进程：

```bash
lsof -i :18080
```

再优雅关闭：

```bash
kill -15 <PID>
```

如果没有正常退出，再强制关闭：

```bash
kill -9 <PID>
```

### 13.4 按端口关闭 aria2

先查占用 `6800` 端口的进程：

```bash
lsof -i :6800
```

再优雅关闭：

```bash
kill -15 <PID>
```

如果没有正常退出，再强制关闭：

```bash
kill -9 <PID>
```

### 13.5 推荐本地操作顺序

当你准备切换到新的 BT 优化参数时，建议按下面顺序执行：

1. 先关闭当前 `aria2`
2. 用优化参数重新启动 `aria2`
3. 如有需要，再重启 `local-service`
4. 最后打开 `desktop-app` 验证任务速度和文件列表

## 14. 相关文档

- [local-development-startup.md](/Users/lying/IdeaProjects/moodDownload/docs/local-development-startup.md)
- [backend-detailed-design.md](/Users/lying/IdeaProjects/moodDownload/docs/backend-detailed-design.md)
- [implementation-iteration-plan.md](/Users/lying/IdeaProjects/moodDownload/docs/implementation-iteration-plan.md)
