# 0.1.24-cloud-stable

基于实机验证过的 0.1.7-cloud-entry（32908286d9cae924ef637a988c291211d8bfff73）整理的稳定功能快照。

- 新增 Material 3 界面、深浅色与 Android 12+ 动态配色，新增隐藏桌面图标开关；保留 LSPosed 设置入口与 ADB 恢复方式。
- 保留原版 Rambler 选择与特性开关逻辑。
- 保留基础云端 Writing Tools；用户已确认入口与英语校对返回文本。
- 此版明确关闭 Writing Tools V2 私有后端、混合推理和端侧校对。
- 不包含 AICore/PCS Hook、Pixel 伪装、模型准备或下载实验。
- 移除未使用的 AICore 检测组件和 ML Kit SDK；补齐 GPL-3.0 许可、来源署名与第三方声明。
- Release 构建启用 R8 与资源裁剪；运行单元测试并验证 APK 签名及 API 102 入口资源。

## 安装
需要支持 libxposed API 102 的 LSPosed 框架。Writing Tools 针对 Gboard
18.4.1.985164140-release-arm64-v8a（176027078）适配；其他版本不承诺可用。
仅勾选 Gboard 作用域。分别开启 Rambler 和 AI 写作工具，然后强制停止并重新唤起 Gboard。
在普通便签中用英语测试，并完成 Gboard 原有同意流程；部分应用（包括 ChatGPT）受原应用排除策略限制。
云端服务可能受网络、语言、地区、账号和 Google 服务端策略影响。

## 签名与验证范围
本次 APK 使用 GitHub Actions 构建时生成的测试签名，不是固定生产签名。
与已有测试 APK 签名不保证一致；若 Android 提示签名冲突，需要卸载旧模块再安装，
卸载会清除该模块设置，不需要卸载 Gboard。后续长期更新应配置固定私有签名。
发布构建的 R8/打包结果经过 CI 检查；R8 版本尚需实机复验，不能把 CI 通过视为云端服务保证。

## 来源
上游 PixelBoard：https://github.com/Akshayykadam/PixelBoard
作者与许可见 LICENSE、NOTICE、THIRD_PARTY_NOTICES.md。
本仓库为独立运行时移植；不是 Google 官方组件。
