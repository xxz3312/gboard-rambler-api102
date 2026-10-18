# Gboard Rambler — LSPosed API 102

Experimental runtime port based on [PixelBoard](https://github.com/Akshayykadam/PixelBoard),
GPL-3.0.

Rambler works on the user's tested setup. The 0.1.7 screenshot confirms the basic
Writing Tools entry and English proofreading result.

The `writing-tools-upstream-port` branch retains 0.1.7. The separate
`writing-tools-v2-experiment` branch builds **0.1.10-runtime-status**:
an independent V2 switch, AICore / Astrea backend selection, binding guards and
an in-app runtime report. V2 defaults off; inference remains unverified.

[Basic cloud test](docs/BASIC_CLOUD_TEST.md) ·
[V2 implementation, runtime report and limits](docs/WRITING_TOOLS_V2.md)

Changes require restarting affected processes. V2 is guarded to Gboard
18.4.1.985164140-release-arm64-v8a / 176027078. Android 34+ can copy the latest
runtime events from module settings; older versions retain framework logging.
Build success and a successful binding check do not establish inference availability.
