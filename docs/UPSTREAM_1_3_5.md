# PixelBoard 1.3.4 / 1.3.5 同步（0.2.7）

审阅源：Akshayykadam/PixelBoard `4b4e48dd4486f29d1140a3b36144933d90a1b7c4`（2026-11-17），对照上次同步 `7e025660c974f3088549b55364b909a9b2b836c9`。上游 GPL-3.0 代码继续保留作者及许可证。本仓库是 LSPosed API102 运行时模块，不是上游的 Morphe APK 补丁包。

## 已移植的运行时变化

- Rambler 语言策略：关闭 proactive parallel ASR / seamless fallback，减少语言漂移；保留 PCM 缓存开关。尊重模块开关、原生选择、默认初始化作用域与返回值类型。此修改没有增加离线模型或完整离线 Rambler。
- 旋转宽限：尽早监听显示方向/尺寸；变化后 2 秒内，原生停用延迟返回 1500ms，其他时间为 0。原生重激活负责取消停止，不新增录音器。
- 语音结果保护：移植上游 DictationRuntime，保留真实的句中替换、选区操作、撤销和编辑命令；处理尾部误替换与听写结果回退。Hook 要求唯一匹配上游完整 callback 描述符、AgenticDictationExtension DEX 锚点及 protobuf 字段类型；未知构建报告 DICTATION_REPAIR skipped。额外保护：缺少完整编辑器上下文时不清除 diff。不记录输入、语音或转写内容。
- Writing Tools：新增 replace button 同义标志及 V2 tutorial 关闭；与 1.3.5 一致关闭 selected-text/modeless-smartedit 路径，继续使用 GBOARD_SERVER，V2、混合和本地校对关闭。保留用户独立控制的跨应用范围；不迁移改包专用服务地址、模型配置或同意状态。
- Direct Boot：用户未解锁时推迟需要凭据存储的初始化，收到 USER_UNLOCKED 后执行一次；覆盖注册期间发生解锁的竞争情形。
- 缓存修订和测试版号更新为 0.2.7；保留现有设置、剪贴板时长选择和原生重复设置检测。

## 未移植，不作为完成项

| 上游内容 | 本模块处理 |
| --- | --- |
| 最近剪贴板 5 → 100 条 | 上游在 3 个方法内部改 DEX 常量；现有 Java 方法 Hook 不能直接替代该修改。没有使用全局集合/数据库 Hook 绕过，尚未移植。 |
| 默认 24 小时、83 语言保留时长提示资源 | 保留“遵循 Gboard”和用户已选时长，不静默改默认值；新宿主字符串/资源打包尚未移植。现有时长 Hook 继续工作，不能声称提示已与时长一致。 |
| Gboard 18.6.3 APK 基础版、改包/签名/XML修复 | 模块不分发或重打包 Gboard。现有语义扫描会尝试当前宿主，不据发布说明宣称支持 18.6.3；需该原版 APK 和实机验证。 |
| JNI split readiness 补丁 | 尚未接入独立 Advanced Voice 功能；不强制返回 ready，保留原生结果。 |

## 验证与实机验收

CI 执行完整单元测试、Debug APK、R8/资源压缩 Release APK、入口/签名/许可证检查。实机需确认：旋转连续听写、结束/取消、原生 Standard 选择不受影响、预填/粘贴文本不重复、句中替换/撤销保留、指定语言不漂移、写作工具语气与校对正常、重启未解锁和解锁后的输入法可用。DICTATION_REPAIR=installed 只证明 Hook 装载；没有实机结果不声明行为已验证。

此次不创建或发布 GitHub Release。
