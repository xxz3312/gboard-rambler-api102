# Gboard Rambler — LSPosed API 102

独立的 PixelBoard 运行时移植。维护：Kaede。
基于 [Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard) 的选择策略、开关策略与版本适配研究，
采用 GPL-3.0；完整声明见 [LICENSE](LICENSE)、[NOTICE](NOTICE) 和 [第三方声明](THIRD_PARTY_NOTICES.md)。

## 下载与功能
当前源码版本 **0.1.29**。CI 仅提供 APK 构建附件，不自动发布 Release。
0.1.24 已收到写作工具不可用反馈，请勿将其视为实机验证通过的发布构建。
原始功能依据为
基于已实测的 0.1.7：Rambler 与基础云端写作工具。
Writing Tools V2 私有后端、AICore/PCS、本地模型下载与 Pixel 伪装不属于该发布版。
云端入口和英语校对在用户设备上实测可用；其他语言、功能与设备不承诺全部可用。
R8 构建仍需实机复验。本版使用语义自动定位并保留完整签名兼容路径；未知或歧义目标不安装 Hook。
界面会分别报告 Rambler 和写作工具的适配结果，无法定位时红字提示“不支持该版本 Gboard”。
未收到本版运行事件不会直接判定不支持。

## 使用
需要支持 libxposed API 102 的框架与官方 Gboard。
0.1.7 的已验证目标为 Gboard 18.4.1.985164140-release-arm64-v8a（176027078）。
安装模块，在 LSPosed 中只勾选 Gboard，按需开启 Rambler／AI 写作工具，强制停止并重新打开 Gboard。
先在普通便签中用英语测试；Gboard 的服务同意、账号限制和应用排除策略仍然有效。
不要把本模块当作无需联网的 Gemini Nano 移植。

## 分支
- `writing-tools-upstream-port`：云端稳定功能发布源码。
- `writing-tools-v2-experiment`：独立实验分支；模型准备、私有后端不保证可用，勿当稳定版发布。
- `main`：项目入口；发布源码以 Release 标签为准。

## 构建与来源
JDK 17、Gradle 9.4.1、Android SDK 37.0。
运行 `gradle :app:testDebugUnitTest :app:assembleRelease`。
Release 使用 R8 和资源裁剪。签名方式、兼容性和验证范围见 [发布说明](RELEASE_NOTES.md)。
仓库及发布附件不包含 Gboard/AICore/PCS APK 或 Google 模型权重。
Google/Gboard/Pixel 等商标属于其权利人，本项目与 Google、上游作者及框架无隶属或背书关系。

## 运行状态
已移除未验证的行内拼写检查 Hook 和开关，并在同步时清理遗留配置。
设置页显示 LSPosed 配置同步结果及经系统发送者身份验证的 Gboard 运行元数据。
“定位成功”只代表结构匹配，无法保证云端请求成功；不代表自动支持所有未来版本。
首次解析可能进行 DEX 扫描；缓存随 APK 更新失效。Android 14 以下请用 Logcat 验证。
Root 重启按钮仅在用户点击并确认后停止 Gboard，不自动申请 Root。

## 界面与桌面图标
Material 3 界面跟随系统深浅色，Android 12+ 使用动态配色。
设置中的“隐藏桌面图标”仅禁用 Launcher 别名；设置 Activity 和模块本身保持启用。
可从 LSPosed 模块页面打开设置并关闭此开关。若管理器不提供设置入口，可通过 ADB：

```sh
adb shell am start -n dev.kaede.pixelboard/.SettingsActivity
adb shell pm enable dev.kaede.pixelboard/.Launcher
```

