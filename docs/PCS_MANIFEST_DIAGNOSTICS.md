# 0.1.27 PCS 清单请求诊断（实验版）

本版用于确认 AICore 清单下载失败的阶段，不宣称模型可用。

- 保留 0.1.26 的四种 Prompt 配置查询。
- 新增 PCS 149829 最终远程 GetManifestConfig 请求观察，过滤到已分析的 AICore 清单客户端。
- 记录转换后 build_id、device_tier、client_group、variant、APK 版本，以及校验模式和证明/公钥字节长度。
- 记录原始异步调用的最终 RPC 状态。错误描述仅分类，不输出原文、响应体、令牌、API key、密钥或 trailers 内容。
- 不改写清单标签、证明、RPC 返回或模型可用性。不修改设备身份或校验流程。
- 精确校验 PCS 版本与字段/方法类型；不匹配则输出 MANIFEST_OBSERVER_UNSUPPORTED 并保留原行为。每个进程最多观察 16 次目标请求。

测试：保留 KSU 统一模块 0.3.0，覆盖安装此 LSPosed APK；重启手机以重新加载 PCS/AICore 中的模块代码，再执行一次四种配置查询。

需要的日志为 PCS_MANIFEST_WIRE、PCS_MANIFEST_RESULT 和 stage=pcs_remote 的安装结果。PCS 到模块的后台事件通道可能受应用关联策略限制，因此原始 PixelBoardLS logcat 也会保留这几项。

PCS 149829 字节码中的模式映射为：att_mode=1 表示获取令牌路径返回；2 表示未请求令牌路径；4 表示获取令牌路径异常后的结果。它们均不证明服务器接受了证明。

这是诊断版本，尚未在目标手机验证观察点。KSU 模块权限已授权与实际清单下载/推理成功是不同状态。
