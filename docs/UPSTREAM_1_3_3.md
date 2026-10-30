# PixelBoard 1.3.3 同步（0.2.3 测试构建）

来源：[Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard/tree/7e025660c974f3088549b55364b909a9b2b836c9)，提交 `7e025660c974f3088549b55364b909a9b2b836c9`，GPL-3.0。相较此前 `65c21b6`，已审核四个新增提交和实际 Java/Kotlin 差异。

## 已接入

| 能力 | 模块实现 |
| --- | --- |
| Rambler 最小化 | Rambler 选中时令 `enable_rambler_al_toolbar=false`，恢复原生折叠/浮动语音栏路径；保留各版本原生按钮位置。继续尊重原生关闭状态与默认初始化作用域。 |
| Google Sans Flex | 独立开关，覆盖 `google_sans_flex`、`platform_google_sans_flex`、`gm3_expressive` 布尔值；不分发字体。 |
| 撤销按钮 | 独立开关，接入主动撤销、自动纠错协同、chunk fusion、语音删除撤销；上游阈值 1L / 100L / 64L 与两个 stop 标志 false，清空撤销应用过滤模式。 |
| 快捷表情栏 | 独立开关，启用 `enable_fast_access_bar` 与应用通配配置。 |
| Magic Emoji | 独立开关，接入入口、语言与应用范围、TalkBack 场景、模式顺序 `1,2,3`，遵循上游 `enable_emojify_model=false` 路径。与 Emogen 生成入口分离。 |
| 校对按钮 | 独立开关，开放原生 chips 和 WebView chips；AI 写作工具主开关另加入 `enable_writing_tools_replace_button`。原有稳定入口与协作模式策略已具备，继续保留。 |
| 扩展校对应用范围 | 独立开关且必须同时开启 AI 写作工具，按上游将 `proofread_supported_apps` 字符串置空。默认保持原生过滤。 |

新开关自动进入模块设置、本地保存、远程同步与诊断配置报告。均默认关闭，关闭时返回原值；与上游默认开启部分能力的行为不同，以免升级时改变既有选择。

Hook 仍通过唯一语义定位的共享 flag getter 处理，校验返回值类型，不依赖 18.5.3 混淆名称。Google Sans、撤销、表情与校对开关不依赖 Rambler。

上游另外初始化快捷表情、Magic Emoji、校对的原生偏好。模块使用 `SharedPreferencesImpl.getBoolean` 通用 Hook，仅对 Gboard 默认偏好实例（设备保护/凭据保护存储）处理对应键，并返回已启用策略。不会写入宿主偏好、覆盖其他偏好文件或保存输入文本。事件 `NATIVE_PREF` 记录发生覆盖的键；安装情况在 `EXTRAS nativePreferences` 中显示。关闭后下一次读取恢复原值，宿主已缓存的界面仍可能需要重启。

## 保留的差异

- 上游最小化按钮重排及两个按钮的 TalkBack 标签补丁硬编码 `flz/flw/flb/mtm` 与 18.5.3 资源 ID。本模块没有复制这些映射，暂保留宿主原生按钮布局与标签；完整按钮重排/标签修复尚未移植，需要相应 APK 验证通用定位及 builder 调用关系。
- 不移植改包后的服务地址重写、未成年资格覆盖或偏好持久初始化；不因上游发布说明而声称云端稳定性已验证。
- “校对与纠错按钮”作用于 Gboard 原生界面，不重新启用此前移除的独立系统拼写检查实验。
- 语言资源、模型、服务资格、应用输入兼容性由 Gboard 决定，不保证所有应用或语言可用。

## 验收

自动回归覆盖新功能分组隔离、类型保护、关闭还原、校对范围双开关、原生偏好实例隔离与不落盘、Rambler 关闭/默认作用域/类型保护；原有设置页明暗主题与逐项开关持久测试继续运行。CI 还构建 Debug 和 R8 Release APK 并核验入口、签名与许可证。

实机请逐项启用新开关后重启 Gboard，比较键帽字体、纠错撤销、快捷表情/文字转表情、校对按钮；再逐项关闭并重启比较原生行为。Rambler 需验证连续识别、最小化、浮动栏返回键盘与结束会话。导出日志检查 `RAMBLER_FLAG`、`EXTRA_FLAG` 和 `NATIVE_PREF`。本轮没有实机测试结果，未发布正式 Release。
