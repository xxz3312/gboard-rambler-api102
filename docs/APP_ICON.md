# Gboard Rambler 应用图标

原创图像标识：深青色语音气泡、三个留白声波柱与一枚小星芒。通过内置图像生成工具生成透明前景，再做 Android 资源尺寸导出；没有使用 Google 标志。

资源：`app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.webp`。432×432 px 无损透明 WebP 对应 108×108 dp，重要图形包含在居中 66 dp 直径安全区内（导出后所有非透明像素最大半径约 26.3 dp）。背景为独立的 #DDEFF4 色层，不预先添加遮罩、圆角或阴影。API 26+ 使用 adaptive-icon，API 33+ 增加 monochrome，以同一前景的 alpha 轮廓供系统主题着色。应用最低 API 29，因此无需旧版非自适应图标资源。

Manifest 的 icon / roundIcon 引用同一个自适应图标；桌面入口 alias 继承应用图标。已检查圆形、圆角方形、浅色和深色主题预览。

设计依据：https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

生成提示词：原创 Android 自适应启动图标的透明前景；深青色圆角语音气泡，内部三个透明留白圆头声波柱，中间最高，右上连接一个克制的四角星；单一清晰轮廓，无文字、无谷歌标志、无背景底板、无阴影或透视，保证小尺寸辨识。主色 #164E63，星芒辅色 #0E7490，全部主体居中并预留自适应裁切边距。

应用显示版本仍为 0.2.5，versionCode 递增至 44。
