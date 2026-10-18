# Gboard Rambler — LSPosed API 102

Experimental runtime port based on [PixelBoard](https://github.com/Akshayykadam/PixelBoard),
GPL-3.0.

Rambler works on the user's tested setup. The 0.1.7 screenshot confirms the
basic Writing Tools entry and English proofreading result.

The `writing-tools-upstream-port` branch retains 0.1.7.
The separate `writing-tools-v2-experiment` branch builds **0.1.8-v2-backends**,
with an independent V2 switch and AICore / Astrea backend selection.
V2 defaults off and its inference results remain unverified.

[Basic cloud test](docs/BASIC_CLOUD_TEST.md) ·
[V2 implementation, device test and limits](docs/WRITING_TOOLS_V2.md) ·
[Upstream adaptation notes](docs/WRITING_TOOLS_UPSTREAM_PORT.md)

Changes require restarting the affected app processes. V2 activation is guarded
to the inspected Gboard 18.4.1.985164140-release-arm64-v8a / 176027078 profile.
Logcat tag: `PixelBoardLS`. Build success is not proof of backend availability.
