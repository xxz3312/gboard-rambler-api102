# 上游功能跟进：0.2

对照 Akshayykadam/PixelBoard 的 main 提交 `65c21b61c4b41542417522530152895a828ab1c6`（1.3.2），本模块继续采用 LSPosed API 102 运行时策略，不修改官方 Gboard APK。

## 本次移植范围

以下 7 个开关均默认关闭，单独保存和同步。无需开启 Rambler 或 AI 写作工具；依赖唯一匹配的共享 flag getter。修改后同步配置并重启 Gboard，关闭后恢复原有 flag 返回值，不持久改写 Gboard 偏好。

| 开关 | 移植内容 | 实际效果的限制 |
| --- | --- | --- |
| 表情面板 V2 | emoji_palette_v2 与最近/常用切换入口 | 面板实现依赖 Gboard 版本 |
| 常用表情优先 | enable_frequent_emoji_recent_Tab，注意上游名称中的大写 T | 新版 Gboard 可能用常用页替换最近页，而非同时显示两个页签；关闭后遵循 Gboard 原值 |
| 表情大小入口 | emoji_scale_supported | 仅开放能力开关，不重建缺失的设置资源或滑块 |
| 快速插入面板 | super_insert、super_insert_vk、动态视图与快捷键 | 不强制开启联系人、浏览历史、个人上下文等额外数据源；入口仍受原生组件与权限影响 |
| 行内智能补全 | Smart Compose 及 inline completion 相关开关；smart_compose_status=1L | 不提供模型，语言和应用支持取决于 Gboard；不改写输入框排除列表 |
| 加快词汇学习 | 历史 n-gram 与动态语言模型相关开关；新词阈值 2L、前缀长度 2L、待学习词写入等待 500L、历史候选阈值 -5.0d | 使用上游实际 Runtime 常量，而非 README 中旧的“阈值 1”；保留原有语言范围与禁用自动纠错的字段策略，不保证输入两字母就出现候选 |
| 输入时联想表情 | concept prediction、emoji shortcut、config_expression 等开关 | 需用户开启 Gboard 表情建议，未覆盖此偏好；依赖语言资源 |

来源：`pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsRuntime.java` 与 `settings/GboardEmojiSettings.java`，GPL-3.0。字符串、proto、未知类型及未启用分组均保持原值；数值严格匹配 Long/Double 类型。

## 0.1.34 继续移植

PR #2 已合并到 main。本轮新增滑行输入、自动纠错、手动无痕入口、Emogen 入口与候选表情完整色彩五个独立开关，并新增剪贴板保留时长选择。详见 [通用 Hook 与验收](UNIVERSAL_HOOKS.md)。这些能力尚未实机验证。

## 剩余差异

| 上游能力 | 当前状态 / 后续所需工作 |
| --- | --- |
| 扫描文本、截图建议、Belka 圆角键 | 0.1.32 已接入独立开关；仍需实机验收，OCR/图片访问需官方 APK 具备组件与权限 |
| 剪贴板 1 小时 / 24 小时 / 7 天 / 30 天 / 永久 | 0.1.34 模块提供时长选项，以限定作用域的原生偏好读取和唯一时限计算匹配适配；无匹配时不覆盖；实际清理行为待实机验证 |
| Samsung / Vivo 剪贴板历史同步 | 0.2 已添加独立输入焦点同步开关，使用 Android 原生监听器与输入法生命周期，无混淆字段；需设备验证 |
| 候选表情全不透明渲染 | 0.1.34 候选控件绘制作用域内修正，使用 Unicode 属性排除扩展汉字、普通符号与混合文本；待实机验证 |
| 滑行输入和自动纠错增强 | 0.1.34 已接入独立开关；按语言实机比较尚待完成 |
| 手动无痕入口 / Emogen | 0.1.34 已提供独立入口开关；资源与后端可用性待验证 |
| 无重启即时生效 | 已监听远程配置并更新运行时策略，0.2 增加 CONFIG_APPLIED 回执；Gboard 缓存的界面/模型仍需原生重载或重启，不伪造无条件免重启 |
| Writing Tools V2 / AICore / Astrea | 稳定策略未开启，不能仅靠界面 flag 保证本机后端可用 |
| 系统拼写检查改造 | 独立实验项此前已移除，未重新启用 |
| 多语言设置与 TalkBack 焦点恢复 | 0.2 添加模块设置页 TalkBack 操作返回焦点恢复；完整多语言及 Gboard 私有菜单焦点恢复仍未移植 |
| 改包共存、签名处理、改包后的密码管理器集成 | 上游修改 APK 的专用能力，本模块运行在官方 Gboard 中，不适用 |

## 验证与验收

CI 运行开关隔离、关闭还原、数值类型保护、未知 flag 保留、浅色/深色设置页启动、既有适配回归、Debug 构建和 R8 APK 检查。编译及 Hook 安装成功不代表宿主能力实际可用，所有新增功能仍待实机验收。

逐项开启并重启，在模块运行详情确认 `UPSTREAM` 中的分组和 getter 状态；打开诊断日志后实际触发功能，检查 `EXTRA_FLAG` 的 flag 读取事件。其他开关全部关闭时也应可独立加载。逐项关闭并重启，再比较原有键盘效果。0.2 发布正式版本；功能状态以设备实际效果及运行事件为准。
