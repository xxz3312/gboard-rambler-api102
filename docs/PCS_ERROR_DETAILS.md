# 0.1.28 PCS 错误详情诊断（实验版）

0.1.27 已在目标手机确认：PCS 最终清单请求携带生成的令牌和公钥，但远程返回 INVALID_ARGUMENT。不能据此认定证明已被服务端接受，或认定 SM8850 无法运行模型。

本版保留原请求观察器，增加 PCS_MANIFEST_DETAILS：

- 只读取 grpc-status-details-bin 这一项 trailers。
- 对 google.rpc.Status 中的 BadRequest、ErrorInfo、PreconditionFailure 做有界解析。
- 只记录受限字段路径、错误原因代码及数量；不记录 Status.message、违规描述、subject、域名、任意 metadata 值、令牌或密钥。
- 原请求、证明、返回值和异步完成状态均不改写。
- 详情不存在、编码不支持、输入过大或解析失败都会明确标注。trailers 存在不等于服务器提供了结构化详情。
- 精确适配 PCS 149829；每进程仍最多观察 16 个目标清单请求。

保持 KSU 统一模块 0.3.0。覆盖安装此 LSPosed APK 后，重启目标 Gboard/AICore/PCS 进程，使新代码加载；整机重启也可。开始 logcat 抓取后执行 Root 重载，再运行一次四种配置查询。

主要观察 PCS_MANIFEST_WIRE、PCS_MANIFEST_RESULT、PCS_MANIFEST_DETAILS。若详情为 ABSENT，下一步需要已支持设备的正常清单请求作对照，而不能继续把同一通用错误当成更多证据。

本版用于诊断，尚未实机验证新的 trailers 读取点；不承诺清单下载或模型推理成功。
