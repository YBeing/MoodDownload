# MoodDownload Browser Extension

Chrome / Edge Manifest V3 插件 MVP。

当前能力：
- 自动拦截磁力链点击
- 自动拦截 `.torrent` 链接点击
- 自动拦截明确的文件下载链接左键点击
- 右键“发送到 MoodDownload”
- Popup 查看本地服务连通性与最近一次接管结果
- Options 配置本地服务地址、令牌和接管开关

安装方式：
1. 打开 Chrome 或 Edge 扩展管理页
2. 开启“开发者模式”
3. 选择“加载已解压的扩展程序”
4. 选择当前目录 `browser-extension`

使用前需要确认：
- 本地 `local-service` 已启动
- 插件里的 `serviceUrl` 和 `localToken` 与本地服务一致
