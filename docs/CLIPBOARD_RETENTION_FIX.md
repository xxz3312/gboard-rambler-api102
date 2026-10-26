# 0.2.1：剪贴板保留时长适配修正

0.2.1 已完成构建验证；用户日志进一步确认 Gboard 18.4.4 的匹配与 7 天配置读取。

## 已确认的根因

使用用户此前上传的 Gboard 18.4.1.985164140-release-arm64-v8a.apk 进行本地静态审计，APK SHA-256：`3310fd5d99de638b3350b0bfe7634ff303372f05ecae6d296a6568417be8e6ad`。

该原版 APK 的 DEX 不包含此前假设的 `clipboard_history_retention` 或 `clipboard_history_duration_ms` 两个键。上游设置页说明虽提及后者，但其当前运行时代码没有对应覆盖，不能将注释当作已实现的路径。

原版实际使用资源 ID 读取小时字符串，读取后的原生 getter 是 `static long (Context)`，以 `j$.time.Duration.ofHours(long).toMillis()` 换算。其调用方再减去该时长形成查询或清理的时间戳。因此，旧解析器的偏好键 + SharedPreferences.getString + TimeUnit.toMillis 同方法约束无法匹配，不代表此功能缺失。

## 修复方式

优先联合匹配以下稳定语义证据，而非版本号、混淆类名、方法名或资源 ID：

- 日志模块来源 `com/google/android/apps/inputmethod/libs/clipboard/ClipboardContentProviderUtils`。
- 逻辑名称 `getUnpinnedItemTimeLimitInMilliSeconds`。
- 解析失败说明 `Failed to parse retention hours`。
- 静态 `(android.content.Context) -> long` 的具体方法。
- 同一 Duration 类型的 ofHours(long) 与 toMillis()；支持 `j$.time.Duration` 及 `java.time.Duration`。
- 排除直接含 System.currentTimeMillis、SystemClock 或 Instant 时间戳运算的候选；唯一匹配才安装。

在已审计的 18.4.1 APK 中得到唯一结果，位于 classes2.dex。已确认至少有三个查询/清理相关调用方使用该时长；混淆描述符仅作为本地审计结果，不进入运行时匹配规则。APK 本体及反编译代码不进入仓库。

旧偏好读取路径保留为兼容回退，但只限定偏好读取作用域，不再依据返回 long 就覆盖整个方法结果，防止将时长写进绝对时间戳。缓存修订升级，旧的未匹配负缓存会失效。

## 即时配置和诊断

每次原生 getter 调用都读取当前远程配置快照。选择 1 小时/24 小时/7 天/30 天/100 年时覆盖毫秒时长；选“遵循 Gboard”恢复原生结果。安装新模块仍需重启 Gboard 加载 Hook；之后该时长策略在下一次原生 getter 调用时更新，不强行调用界面或模型重载，不修改宿主设置或数据库，也不能恢复已清理的历史。

开启诊断后，运行详情新增 `RETENTION_READ`，显示识别策略、选择值、原生毫秒数与生效毫秒数，不记录剪贴板内容。`RUNTIME_HOOKS` 中的新路径应为 `retentionCalculation=installed(native-duration)`；解析失败保留简短原因。

## 验证范围

- 本地真实 18.4.1 APK 静态审计：唯一匹配及调用关系检查。
- CI：两种 Duration 实现、部分证据/时间戳候选拒绝、重命名方法、参数及返回值验证、原值恢复、7 天/30 天/100 年长整数换算及已有回归。
- 可选本地审计脚本 `.github/scripts/audit-gboard-retention.py` 使用 androguard 读取指定 APK，仅输出元数据，不上传 APK。
- 2026-10-26 16:17 的用户日志确认 18.4.4：retentionCalculation=installed(native-duration)，selected=168，nativeMs=604800000，effectiveMs=604800000；焦点同步有 callbacks=2，无当前初始化错误。原生与生效值本次相同，不能据此证明发生了数值覆盖；已确认实际路径与 7 天配置读取，跨天清理行为仍需观察。
