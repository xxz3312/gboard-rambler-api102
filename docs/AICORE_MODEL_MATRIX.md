# 0.1.26-model-matrix (experimental)
仅用于 AICore 模型配置和清单请求诊断，尚未验证 SM8850 推理成功。

- 独立前台 Prompt 探针新增 STABLE/FULL、STABLE/FAST、PREVIEW/FULL、PREVIEW/FAST 顺序查询。
- 每项独立客户端；状态等待最多 20 秒，名称最多 10 秒；错误继续下一项。查询不会主动下载或推理。
- 可手动选择配置后查询、下载、校对或正式改写；不自动切换配置或回退云端。
- 对精确验证的 AICore 513248 记录进入 PCS RPC 前的转换后 build ID、tier/client/group/variant 枚举整数。
- 观察器不修改参数，不读取密钥、令牌、证书或辅助字符串正文；该日志不能证明服务端收到相同参数。
- 补全清单观察器运行状态事件，失败时保留原请求。

使用：保留已验证生效的 0.2.0 特权模块。打开本实验版的“AICore 端侧执行”，保持前台，点击“查询四种模型配置”。V2 可以保持关闭；启用模型下载准备后重启 AICore/PCS，再查询一次并查看 MANIFEST_WIRE。
公开云端 RC2 和本实验版使用同一应用包名，只能安装其中一个。此包是 debug 测试签名，与旧包签名可能不一致；冲突时卸载旧模块再安装并重新配置，勿清除 Gboard/AICore/PCS 数据。
UNAVAILABLE 仍可能表示设备不支持或配置尚未获取，PREVIEW 需要相应资格，AVAILABLE 也不等于推理成功。
本版不修改设备身份、清单标签、系统权限或 root 模块。

许可：GPL-3.0。LICENSE、NOTICE、THIRD_PARTY_NOTICES.md 和 APK 内 assets/licenses 随包提供。
