# Rambler 按钮完整运行时移植（0.2.4 测试）

本轮补齐 PixelBoard 1.3.3 尚未移植的原生按钮重排和 TalkBack 标签。来源为上游 `GboardRamblerMinimizeKeyPatch.kt` / `GboardRamblerMinimizeKeyRuntime.java`，提交 `7e025660c974f3088549b55364b909a9b2b836c9`，GPL-3.0。没有复制上游 18.5.3 混淆名称或固定资源 ID。

## 实现

- 通过控制器静态初始化中的完整日志路径定位基类和键盘控制器，仅匹配 `<clinit>`，排除其他类引用相同日志文本的情况。
- 校验完整的键构建调用图：键盘/浮动栏按钮、通用 factory、builder、描述 setter、build、原生关闭键、关闭图标、销毁、槽位隐藏、原生添加/移除通知，共 12 个方法。方法签名、父类关系、参数类型和调用关系必须唯一。
- ThreadLocal 嵌套作用域区分“最小化”“返回键盘”和真实关闭键；真实关闭键调用不会被误标为最小化。只改写原生正在生成的键，不合成或替换语音操作 Runnable。
- 用原生 builder 设置本地化标签，保留原生键盘最小化/恢复动作。将最小化键发布到 `key_pos_header_end_extra_key`，原关闭槽位返回原生关闭键；语音结束时移除额外键，槽位隐藏后恢复其可见性。
- 放置状态通过控制器弱引用管理，旧控制器销毁不移除新控制器的键；无全局“已放置”布尔状态。不保留 Activity、键盘 View 或输入文本。
- 所有 Hook 成功后才统一启用；部分安装失败时已经注册的拦截器保持透传。自定义构建/通知失败时保留原生键，宿主异常正常传播。

## 混淆资源与缓存

提供的 APK 将字符串名称混淆为 `0_resource_name_obfuscated`。模块解析只读 `resources.arsc`，支持 UTF-8/UTF-16 池、普通/稀疏/16 位偏移和资源引用，从实际表中查找英文含义，再通过英文配置的 Android Resources 校验。描述仍由宿主在当前语言解析，不硬编码中文或英文到按钮；重复的同义英文标签可选用同义原生资源。未知 compact/complex 条目不猜测。

扫描读取限制 64 MiB，缓存按 base/split APK 路径、大小和时间指纹失效；每次缓存命中仍检查所有方法签名/关系和原生标签。无法完整匹配时只跳过按钮增强，不调用不确定方法。日志包含 `RAMBLER_UI` 安装结果和最多 16 条 `RAMBLER_UI_EVENT`，不包含语音或输入内容。

## 新 APK 验证

用户提供：Gboard `18.4.6.985164140-release-arm64-v8a`，versionCode `176027358`，SHA-256 `45e8a04cfce2fc214e560b2167eb82bfeac35483a543370dd5a2c25be2c9ae56`。

本地运行 `.github/scripts/audit-gboard-controls.py` 对新 APK 的原生图与标签做独立审计；直接编译运行生产资源索引解析器，也能找到两个“Minimize keyboard”原生标签和一个“Back to keyboard”标签。`.github/scripts/audit-gboard-retention.py` 确认原生保留时长 getter 仍唯一。

自动回归覆盖嵌套作用域/异常清理、关闭键用途隔离、跨控制器销毁、指定槽位显示恢复、改名方法的结构校验、无资源拒绝安装，以及资源表编码、引用、稀疏偏移和损坏边界。构建检查不等于实机 Hook 或云端验证；请在新 APK 上测试开始语音、最小化、浮动栏返回键盘、关闭、重新开始及 TalkBack 标签。

本轮仅提供测试版，不发布 Release。
