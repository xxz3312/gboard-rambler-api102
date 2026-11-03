# Gboard 18.5.6 适配

输入 APK：`18.5.6.993608717-release-arm64-v8a`，versionCode `176049814`。
SHA-256：`1a2b40379f323c34d903817364ac55c1409b16492f92a08379c3709aebb941de`。
仅记录自有适配代码与方法元数据，不分发 Google APK 或反汇编正文。

## 问题与修正

2026-11-03 21:10 的设备日志确认：模块已注入，`resolver=cache`、`rambler=false`、
`writing=true`；原生偏好 `nativeSelected=true`，共享 getter 和 Rambler UI Hook 已安装。
因此这次入口消失的直接原因是缓存的语音选择器失败结果，不能用旧进程的成功记录判断。
首次语义扫描失败的具体步骤未被旧版记录，暂不能确定其底层原因。

- 提升解析缓存修订号，安装后不再使用旧缓存。
- 只有语音选择器完整解析成功才复用缓存；失败后下次进程启动会重新扫描。
- 保留语义扫描优先，增加经本 APK 六方法逐一核对的完整签名备选。
  所有描述符仍在运行时通过反射复核，多个备选同时成立则拒绝匹配。
- 新增 `RESOLVER` 事件：记录实际选择器来源与扫描异常；唯一性失败注明角色与数量。
- 保持对用户原生选择的尊重，不写入 `enable_jetson` 或自动切换语音类型。
- 测试包继续显示 0.2.5，内部 versionCode 45；不改写已发布的 0.2.5 Release。

## 静态核对

| 角色 | 本 APK 方法 |
| --- | --- |
| 布局 | VoiceSettingsFragment.aC()int |
| 设置初始化 | VoiceSettingsFragment.ac()void |
| 创建 | VoiceSettingsFragment.f(Bundle)void |
| 原生选择写入 | VoiceSettingsFragment.aE(boolean,qvt,Selector,Selector)void |
| 原生选择读取 | nca.a(Context)boolean |
| 默认选择作用域 | fhv.hU()void |
| 共享标志 getter | oki.g()Object，名称字段 oki.a:String |

`.github/scripts/audit-gboard-selector.py` 独立核对六个语义角色与完整备选签名。
`.github/scripts/audit-gboard-controls.py` 核对十二个原生按钮方法与两组本地化资源；
本版本仍可匹配，未新增固定按钮类名。

静态 APK 审计不等于设备上的 DexKit、LSPosed 安装或云端语音服务验证。
安装测试包并重启 Gboard 后，需要设备确认设置入口与实际 Rambler 输入。
