# Gboard Rambler — LSPosed API 102

将部分 PixelBoard 功能移植为 LSPosed 运行时模块，无需修改 Gboard 安装包。维护：Kaede。

基于 [Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard) 的选择策略、开关策略与版本适配研究，采用 GPL-3.0。完整声明见 [LICENSE](LICENSE)、[NOTICE](NOTICE) 和 [第三方声明](THIRD_PARTY_NOTICES.md)。

## 下载

[前往 Release 下载](https://github.com/xxz3312/gboard-rambler-api102/releases/latest)

0.1.30 APK 的 SHA-256：

```text
9d548c25f57133666b15f87a830b85c3e0d6390e5d81a505c5fb1e0d8c5f25fb
```

## 功能范围

| 功能 | 当前情况 |
| --- | --- |
| Rambler 语音输入 | 保留原有选择与开关策略；历史版本已在用户设备验证 |
| AI 写作工具 | 基础云端校对与改写；可用性受网络、语言及服务端策略影响 |
| Gboard 适配 | 根据稳定字符串、调用关系和完整方法签名定位，保留已知签名兼容路径 |
| 不支持提示 | 找不到唯一 Hook 点时，模块界面红字显示“不支持该版本 Gboard” |
| 设置界面 | Material 3 分组列表与双行开关，支持深浅色及 Android 12+ 动态配色 |
| 隐藏桌面图标 | 可在设置中开启，也可从 LSPosed 模块页恢复 |
| 行内拼写检查实验项 | 已移除独立开关及 Hook，并清理旧设置 |

本正式版不包含 Writing Tools V2 私有后端、AICore/PCS 模型准备、Pixel 伪装或独立本地模型。无需为使用本版安装这些实验组件。本模块不提供 Rambler 离线能力保证；基础云端写作工具需要联网。

## 验证状态

0.1.30 已通过浅色、深色启动回归测试、单元测试及 R8 构建检查，设置界面已获用户实机确认。适配成功不保证覆盖所有 Gboard 版本，写作工具仍受 Google 的网络、语言、账号及服务端限制。

## 安装与使用

1. 准备官方 Gboard 和支持 **libxposed API 102** 的 LSPosed 兼容框架。
2. 安装 APK，在框架中启用本模块，作用域只勾选 Gboard（`com.google.android.inputmethod.latin`）。
3. 打开模块设置，按需开启 Rambler 或 AI 写作工具，确认配置同步状态。
4. 强制停止并重新唤起 Gboard；可使用系统应用设置，或模块中的“Root 重启 Gboard”按钮。
5. 先在普通便签中使用英语测试写作工具，并完成 Gboard 原有同意流程。Gboard 的应用排除策略仍然有效。

模块本身无需单独授予 Root；“Root 重启 Gboard”是可选操作，只在点击并确认后请求执行。框架本身的运行要求另行满足。

当前 APK 使用 CI 测试签名，不能保证与旧 APK 签名相同。若提示签名冲突，需要卸载旧模块后安装，模块设置会清除；无需清除 Gboard 数据。

## 适配与运行状态

设置页分别显示 Rambler 和写作工具的定位结果、Hook 安装结果、配置同步结果与初始化错误。开启开关、配置同步成功、定位成功，均不等于功能已成功运行。

首次适配可能进行 DEX 扫描，缓存随 APK 更新失效，并重新验证方法描述符。候选为空或有歧义时，不安装对应功能 Hook。

Android 14+ 可在设置页查看经系统发送者身份验证的 Gboard 运行元数据；更低系统版本请通过 Logcat 的 `PixelBoardLS` 标签检查。尚未收到本版本运行事件时，不直接判定 Gboard 不受支持。

## 隐藏或恢复桌面图标

“隐藏桌面图标”仅禁用 Launcher 别名，设置 Activity 与模块仍保持启用。可从 LSPosed 模块页进入设置并关闭开关。也可通过 ADB 打开设置或恢复图标：

```sh
adb shell am start -n dev.kaede.pixelboard/.SettingsActivity
adb shell pm enable dev.kaede.pixelboard/.Launcher
```

## 构建

环境：JDK 17、Gradle 9.4.1、Android SDK 37.0、Build Tools 36.0.0。

```sh
gradle :app:testDebugUnitTest :app:assembleRelease
```

Release 构建启用 R8 和资源裁剪。

## 许可与来源

GPL-3.0。DexKit 等依赖的许可和来源说明随 APK 附带，见 `assets/licenses` 和仓库中的 [第三方声明](THIRD_PARTY_NOTICES.md)。仓库及发布附件不包含 Gboard、AICore、PCS 安装包或 Google 模型权重。

Google、Gboard、Pixel 等商标属于其权利人。本项目与 Google、上游作者及框架无隶属或背书关系。
