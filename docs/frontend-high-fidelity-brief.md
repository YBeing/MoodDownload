# MoodDownload 高保真设计稿 Brief

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 文档名称 | `MoodDownload 高保真设计稿 Brief` |
| 版本 | `v0.1-draft` |
| 输出模式 | `静态 mockup / 设计稿 brief` |
| 主要依据 | [frontend-detailed-design.md](/Users/lying/IdeaProjects/moodDownload/docs/frontend-detailed-design.md) |
| 原型依据 | [frontend-prototype-spec.md](/Users/lying/IdeaProjects/moodDownload/docs/frontend-prototype-spec.md) |
| 接口约束来源 | [backend-detailed-design.md](/Users/lying/IdeaProjects/moodDownload/docs/backend-detailed-design.md) |
| 适用范围 | 桌面端高保真视觉稿、页面视觉统一、状态稿说明 |
| 不在本文覆盖范围 | 生产级代码实现、浏览器扩展页面、安装包物料 |

## 2. 设计目标

本次高保真设计稿要解决的不是“页面有没有”，而是“桌面下载器是否看起来像一个专业、稳定、偏技术流的下载中枢”。

核心目标：

- 保持 `Motrix` 式下载器的熟悉布局
- 拉高桌面端的精致感和极客感
- 让状态信息高密度但不混乱
- 让高频操作足够近、足够明确
- 让失败、暂停、实时速度等下载器特征信息成为视觉重点

## 3. 全局视觉方向

### 3.1 关键词

- 极客风
- 冷静、理性、系统化
- 高信息密度
- 桌面工具感
- 精准、不花哨
- 接近 `Motrix`，但更硬核一点

### 3.2 视觉气质

不要做成通用 SaaS 后台，也不要做成娱乐型产品。  
目标是“专业下载控制台”，像本地下载调度中心，而不是普通文件列表。

建议整体呈现：

- 深色主界面
- 冷灰底
- 青蓝或电蓝作为高亮色
- 成功、暂停、失败、运行状态拥有明确状态色
- 较强的分区线、卡片边界和列表节奏

## 4. 视觉系统提案

以下内容是本次高保真稿的建议方向，可直接作为设计稿统一规范。

### 4.1 色彩系统

#### 主色建议

- 主背景：`#0B0F14`
- 二级背景：`#111823`
- 面板背景：`#151E2B`
- 分割线：`#233044`
- 主高亮：`#23C7FF`
- 次高亮：`#4A8DFF`

#### 文字颜色

- 主文字：`#EAF2FF`
- 次文字：`#9CB0C9`
- 弱文字：`#6F8198`

#### 状态色

- 运行中：`#23C7FF`
- 已完成：`#27D980`
- 已暂停：`#F7B84B`
- 失败：`#FF5D73`
- 待调度：`#8EA3BD`

### 4.2 字体与排版

字体方向建议：

- 中文：`HarmonyOS Sans SC` 或 `Noto Sans SC`
- 英文 / 数字：`IBM Plex Sans` 或 `JetBrains Mono` 用于速度、大小、状态码等局部信息

排版建议：

- 页面标题偏硬朗，字重中高
- 任务名称使用中等偏高字重
- 速度、大小、时间适合使用等宽数字风格
- 列表信息层次要明确，避免所有文本一个灰度

### 4.3 组件风格

- 按钮：
  - 主按钮使用青蓝高亮
  - 次按钮用描边或低对比实体
- 输入框：
  - 深色填充
  - 边框细而清晰
  - 聚焦时出现高亮描边
- 开关：
  - 开启时使用电蓝或青蓝
- 列表：
  - 行高适中
  - hover 有轻微亮度抬升
  - 当前选中任务行要有更明显的背景差
- 进度条：
  - 深色轨道 + 高亮填充
  - 运行中可加入极轻的流动感

### 4.4 图标方向

- 线性图标为主
- 细线、略科技感
- 统一圆角风格
- 不使用过重拟物图标

图标重点场景：

- 下载状态
- 新建任务
- 设置
- 目录选择
- 重试
- 托盘最小化

## 5. 布局系统

### 5.1 窗口整体布局

```text
┌──────────────────────────────────────────────────────────────────────┐
│ 自定义标题栏 / 新建任务 / 窗口控制 / 状态提示                       │
├───────────────┬──────────────────────────────────────────────────────┤
│ 左侧导航      │ 页面头部 + 数据摘要 + 操作区                        │
│               ├──────────────────────────────────────────────────────┤
│               │ 核心内容区：任务列表 / 表单 / 设置面板              │
│               │                                                      │
│               │                                                      │
├───────────────┴──────────────────────────────────────────────────────┤
│ 底部轻提示 / 实时连接状态                                            │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.2 左侧导航

设计目标：

- 像下载器，不像网站
- 紧凑、稳定、可长期盯着看

设计建议：

- 宽度偏窄，约 `88-112px`
- 使用图标 + 短标签
- 当前选中项有高亮底板或侧边高亮条
- 底部单独放 `设置`

### 5.3 主内容区

主内容区建议分为三层：

1. 页面标题层
2. 状态摘要 / 操作层
3. 列表或表单内容层

不要把标题、筛选、统计和内容挤在同一层里。

## 6. 页面级设计 Brief

下面每一页都给出高保真设计要点，可直接用于出图或继续转图像生成 prompt。

### 6.1 下载中页

#### 页面定位

这是整个产品的主工作台，必须最强。

#### 视觉重点

- 页面头部有“正在下载”的控制台感
- 中上部展示全局下载摘要
- 任务列表是绝对主角
- 进度条、速度、状态标签必须清晰

#### 布局建议

```text
顶部：标题 + 新建任务按钮
第二层：总任务数 / 活跃任务 / 总速度 / SSE 状态
第三层：搜索 / 筛选 / 排序
主体：任务列表
```

#### 任务行设计要求

- 左侧：任务类型图标 + 文件名
- 中间：进度条 + 状态信息
- 右侧：速度 / 大小 / 保存目录 / 操作按钮
- hover 时显露更多行级操作
- 当前活动任务有轻微发光或高亮描边

#### 高保真 prompt

```text
Design a high-fidelity desktop downloader dashboard for the "Running" page of MoodDownload. Dark geek-style interface, close to Motrix layout but more technical and sharper. Narrow left sidebar with icons and labels, large main content area, top custom title bar with a primary "New Task" button. In the main area, show a compact status summary row with active tasks, total tasks, download speed, and connection status. Below it, show a dense but clean task list with each row containing file icon, filename, progress bar, status badge, speed, size, save path, and action icons. Use deep graphite backgrounds, cyan-blue accent lighting, precise separators, subtle glow on active rows, and strong information hierarchy. No marketing style, no playful cards, no mobile UI.
```

### 6.2 已完成页

#### 页面定位

这是结果回看页，比下载中页更安静、更整洁。

#### 视觉重点

- 强调“完成”状态的可靠感
- 列表更偏历史记录浏览
- 减少动态感，增强秩序感

#### 设计建议

- 完成状态标签使用绿色
- 完成时间、文件大小、保存路径应清晰
- 列表行比下载中页略简洁

#### 高保真 prompt

```text
Design a high-fidelity desktop downloader "Completed" page for MoodDownload. Maintain the same dark technical visual system as the main dashboard, with a narrow left navigation and large content panel. The page should feel calmer and more archival than the running page. Show a clean completed-task list with filename, completion time, file size, save path, and quick actions like view details and delete. Use green completion indicators, muted secondary text, tight spacing, and strong alignment. Keep it tool-like, efficient, and desktop-native.
```

### 6.3 已暂停页

#### 页面定位

- 强调“可恢复”
- 让用户一眼能看出哪些任务停住了

#### 设计建议

- 暂停状态色为黄
- 恢复按钮视觉权重大于删除按钮
- 列表顶部可有“全部继续”操作位，但首版是否展示记为 `TBD`

#### 高保真 prompt

```text
Design a high-fidelity desktop downloader "Paused" page for MoodDownload. Use the same dark cyber-technical system, with yellow pause indicators and clear resume actions. Layout should match the app shell: left sidebar, page header, action row, and structured task list. The list should highlight that these tasks are recoverable, with resume buttons visually prioritized over delete. Dense, practical, sharp, and close to professional system tooling.
```

### 6.4 失败页

#### 页面定位

- 强调故障诊断感
- 失败原因必须视觉上可读

#### 设计建议

- 失败标签用红色
- 错误摘要要比其他辅助信息更醒目
- 重试按钮要清晰但不喧宾夺主

#### 高保真 prompt

```text
Design a high-fidelity desktop downloader "Failed" page for MoodDownload. Keep the same dark geek-style shell, but emphasize error diagnosis and recovery. Show task rows with filename, failure reason, last failure time, and actions like retry, delete, and view details. Use controlled red accents for error states, not overwhelming. Make the interface feel like a professional troubleshooting console rather than a generic admin table.
```

### 6.5 设置页

#### 页面定位

- 这是唯一一个偏表单型页面
- 要体现“系统设置面板”的稳定感

#### 设计建议

- 表单采用分组布局：
  - 下载目录
  - 并发与限速
  - 接管开关
- 每组有标题和说明文案
- 保存按钮明确，成功反馈克制

#### 高保真 prompt

```text
Design a high-fidelity desktop downloader settings page for MoodDownload. The interface should use the same dark technical design language, but shift to a stable control-panel feel. Use grouped settings sections for default save directory, concurrency, bandwidth limits, browser capture, and clipboard monitoring. Show clear labels, concise helper text, dark form controls with cyan focus states, and a prominent save button. It should feel like a desktop control center, not a web form.
```

### 6.6 新建任务弹窗

#### 页面定位

- 高使用频率弹窗
- 需要快速、直接、低认知成本

#### 设计建议

- 弹窗不要过宽
- 顶部有明确标题和关闭动作
- URL / 磁力 / 种子三类入口要有清晰切换
- 创建按钮为主按钮

#### 高保真 prompt

```text
Design a high-fidelity modal dialog for creating a new download task in MoodDownload. Dark, compact, technical desktop style. Show tabs or segmented switches for URL, magnet link, and torrent file input. Include a directory selection field, optional custom name area, and a strong primary "Create" button. Make the modal feel fast and utilitarian, with clean spacing, thin borders, cyan focus accents, and no consumer-style fluff.
```

### 6.7 任务详情抽屉

#### 页面定位

- 辅助信息面板
- 用于看单任务的完整上下文

#### 设计建议

- 右侧滑出抽屉
- 顶部显示任务名和状态
- 中部展示原始链接、目录、大小、速度、错误信息
- 底部保留操作按钮

#### 高保真 prompt

```text
Design a high-fidelity right-side task detail drawer for MoodDownload. Use a dark technical style consistent with the main app. The drawer should show task name, status badge, source URL, save path, progress information, speed metrics, file size details, and error information when present. Keep the layout dense but readable, with strong section separation and action buttons at the bottom for pause, resume, retry, and delete.
```

### 6.8 剪贴板提示条 / 弹窗

#### 页面定位

- 轻量确认，不要喧宾夺主

#### 设计建议

- 更像桌面工具的轻通知，不像营销弹层
- 出现后不遮挡主内容
- 动作只有“忽略 / 创建”

#### 高保真 prompt

```text
Design a small high-fidelity desktop notification panel for MoodDownload that appears when a downloadable link is detected in the clipboard. It should feel lightweight, technical, and system-native. Dark compact panel, short link preview, and two actions: ignore and create task. Use subtle cyan accents and minimal visual noise.
```

## 7. 状态稿要求

高保真设计稿不只出正常态，至少每类核心页面要补这些状态稿：

| 页面 | 必出状态 |
| --- | --- |
| 下载中 | 正常态 / loading / empty / SSE 断线提示态 |
| 已完成 | 正常态 / empty |
| 已暂停 | 正常态 / empty |
| 失败 | 正常态 / empty / 错误信息强调态 |
| 设置页 | 正常态 / saving / success / error |
| 新建任务弹窗 | 正常态 / submitting / 表单错误态 |
| 任务详情抽屉 | 正常态 / loading / 错误态 |

## 8. 动效方向

高保真稿中可以表现但不要过量：

- 页面切换：轻微淡入 + 位移
- 抽屉展开：右侧滑入
- 弹窗出现：轻缩放 + 淡入
- 进度条：轻微流动感
- 顶部状态提示：短时滑入

原则：

- 动效服务于工具感
- 不做夸张弹跳
- 不做移动端风格的卡片飞行动画

## 9. 设计交付建议

如果后续交给设计师或继续生成视觉稿，建议交付顺序：

1. 全局壳层
2. 下载中页正常态
3. 新建任务弹窗
4. 任务详情抽屉
5. 失败页
6. 设置页
7. 其他状态稿

## 10. 假设与 TBD

### 当前采用的设计假设

- 主界面采用深色基调
- 用青蓝作为主高亮色
- 桌面端风格优先于网页感
- 整体接近 `Motrix` 的列表型框架

### TBD

- 是否首版同时输出浅色方案
- 是否输出“关于 / 日志查看”页设计稿
- 是否把批量操作区放进列表页
- 是否在下载中页加入更强的动态波形或速度图

## 11. 下一步建议

从这份高保真 brief 往后走，可以继续做两类事情：

1. 基于这份 brief 生成静态设计稿图像  
   适合继续调用 `imagegen`

2. 基于这份 brief 和低保真原型，生成可运行的 Electron / React 高保真原型代码  
   适合直接进入前端原型实现
