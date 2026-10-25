# 0.1.34 通用 Hook 与验收

基于 PixelBoard 1.3.2（65c21b61），仅提供 Actions 测试包，不发布 Release。PR #2 已合并到 main。本轮未在手机上验证，不能将 Hook 安装成功视为功能生效。

## 新增功能

- 滑行输入：手势流修正与两阶段解码，独立开关。
- 自动纠错：空间模型、后置纠错、提交等待 150ms，保留原有应用/语言排除策略。
- 手动无痕入口：只开放原生入口，不自动切换无痕状态。
- Emogen：只开放原生入口，不绕过账号、地区或模型资格。
- 候选表情色彩：只处理 AutoCenterScaleTextView 绘制期间的 TextPaint，Unicode Emoji_Presentation / Emoji + VS16 / keycap 判定；普通符号、扩展汉字、混合文字不处理。按线程保存作用域，宿主绘制结束或异常均恢复最新原生 alpha。
- 剪贴板保留：遵循 Gboard / 1 小时 / 24 小时 / 7 天 / 30 天 / 100 年。100 年对应上游“Never”，不会阻止手动删除、清除数据、系统或存储策略清理。默认遵循 Gboard。

## 兼容策略

1. 共享 flag getter 继续按语义字符串、构造签名和字段类型唯一匹配；覆盖值必须与原始值同类型。新增所有布尔开关默认关闭。
2. HostDex 联合扫描基础 APK 和所有 split APK；同一描述符去重，匹配不唯一时跳过。基础包或 split 的路径、大小、时间及解析器修订变化使缓存失效；缓存命中后重新核对描述符。
3. 剪贴板优先使用 Android SharedPreferences.getString 的原生稳定键 clipboard_history_retention，仅在已验证的运行时计算作用域内只读覆盖并开启 extended flag，不写入 Gboard 偏好、数据库或滑块字段。模块设置页提供选择，避免 K/L/b/d/ap 等混淆字段依赖。
4. 时限计算必须同时具备原生偏好键、getString(String,String) 和 TimeUnit.toMillis(long)。优先唯一的无参数 long 返回且不直接调用 System.currentTimeMillis 的时长 getter，覆盖毫秒结果；否则尝试唯一的运行时计算调用，将原生偏好覆盖限定在此调用内部，保留其原始返回结果，绝不将时长赋给时间戳。原生设置页读取不进入该作用域，防止滑块读到越界值。无法定位时保留 Gboard 原样，RUNTIME_HOOKS 明示 calculation=skipped；负结果同样按安装指纹缓存。**这不保证未来版本没有隐藏上限或其他清理路径，真实清理周期必须实测。**
5. 候选控件采用未混淆的公开控件类名与 Android onDraw(Canvas) 契约，并校验 TextView 继承关系。类名或绘制契约变化则独立跳过，不扩大到所有 TextView、Canvas 或所有表情字符。Paint 的短暂 Hook 仅在该控件的线程局部绘制作用域内修改 TextPaint，退出作用域恢复 alpha。
6. 所有可解析的策略 Hook 在启动时安装，开关全关也能随后启用。远程 SharedPreferences 监听器强引用保存；每次回调从 getAll 生成不可变快照，运行时不进行跨进程配置查询。同步后新一次 getter/绘制会读取新策略；**已被宿主缓存的 flag、界面、模型和清理任务可能仍需重启**。未反射调用未知界面重载函数。
7. 每个可选 Hook 独立捕获解析/安装失败；运行详情新增 RUNTIME_HOOKS，区分原生偏好、毫秒计算、候选绘制安装情况。不以 Gboard 版本号放宽匹配，不承诺所有未来版本都兼容。

## 验收

CI：既有回归、开关隔离、数值类型、长整型时长、Unicode 排除、作用域嵌套和异常恢复、配置刷新及设置页启动；Debug 与 R8 构建，APK 入口/签名/许可证校验。

实机：全关启动后单开每个开关并同步，重新打开键盘，对比关闭还原；缓存界面需重启后比较。剪贴板至少验证 1 小时/24 小时界限、固定项目与手动删除，检查原生滑块是否仍做截断，切回“遵循 Gboard”后恢复原生策略。表情比较浅深色候选、扩展汉字、数字、混合文本与 Emoji ZWJ 序列；发生崩溃时收集异常和 RUNTIME_HOOKS，不能仅靠设置页开关推断成功。

仍未移植：Samsung/Vivo 剪贴板焦点同步（上游依赖 handler 和 observer 的混淆调用，需可靠定位）；完整免重启界面重载；独立系统拼写检查改造；上游完整设置页与 TalkBack 焦点增强。AICore/Astrea 本机服务资格仍不由通用 Hook 保证。
