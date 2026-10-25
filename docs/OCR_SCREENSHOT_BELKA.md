# OCR、截图建议与 Belka 移植

以已验证的 0.1.30 发布提交 202ea62 为基础；本次不发布 Release。
来源为 Akshayykadam/PixelBoard 1.3.2 的 GboardAiWritingToolsRuntime 与 GboardRoundedKeysSettings，GPL-3.0。

三个开关均默认关闭，独立通过 LSPosed remote preferences 同步；修改后重启 Gboard。
仅在唯一共享 flag getter 解析成功时启用。不依赖 Rambler 或 AI 写作工具开关。

- OCR：enable_ocr、config_ocr、enable_camera_permission_overlay；字符串 enabled_ocr_language_tags 为 *。
- 截图：enable_clipboard_image_suggestion、enable_screenshot_notification_flow、enable_screenshot_chip、enable_clipboard_recent_screenshot、clipboard_screenshot_enabled_at_least_once、enable_screenshot_in_clipboard。
- Belka：belka_rounded_keyboard、belka_ui、belka_background、more_pill_keys、enable_rounded_key_by_default、enable_rounded_keys。另临时覆盖 Gboard 进程 enable_rounded_keys 的布尔偏好读取，避免永久写入原设置。

保持返回值类型；未知类型和关闭的功能返回原值。保留原生 ocr_password_allowlist 策略。
不修改官方 Gboard APK，不补权限，不复制 CameraX/OCR 模型，不注册额外的相机或截图服务。
Gboard 的 CAMERA、READ_MEDIA_IMAGES（旧系统 READ_EXTERNAL_STORAGE）声明/授权情况可在模块中查看；未声明的权限无法靠本模块开关增加。照片的有限授权不保证能发现全部截图。
截图建议还受 Gboard 剪贴板开关、截图时间、输入框是否支持图片以及系统后台访问限制影响。

## 验证

CI 执行新策略测试、已有浅色/深色启动与适配回归测试、R8 Release 构建和 APK 许可检查。实机未验证。

设备验收：三个开关逐个开启，且 Rambler/写作工具均关闭；确认共享 getter 安装。OCR 授权后从工具栏进入扫描文本，测试识别并插入光标处；截图后切到支持图片的普通输入框，确认横幅并粘贴；Belka 开关前后重启，比较按键外观。最后全部关闭并重启，确认恢复原行为。
