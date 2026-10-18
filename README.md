# Gboard Rambler — LSPosed API 102

Experimental runtime port based on [PixelBoard](https://github.com/Akshayykadam/PixelBoard),
GPL-3.0. Rambler and the basic Writing Tools entry/English proofreading result
have been confirmed on the user's setup.

The `writing-tools-upstream-port` branch retains 0.1.7. The experimental branch
`writing-tools-v2-experiment` builds **0.1.11-private-status**, with AICore /
Astrea selection, binding guards and an in-app runtime report. Optional AICore
local proofreading now defaults off and is separate from private rewriting.

Actual V2 AICore selection has been observed; successful private inference has
not. The runtime report distinguishes private Feature 703 availability, optional
local proofreading, aggregate status, and actual responses.

[V2 implementation and test limits](docs/WRITING_TOOLS_V2.md).
V2 is scoped to Gboard 18.4.1.985164140-release-arm64-v8a / 176027078.
Restart affected processes after configuration changes. Android 34+ can copy
runtime events directly from the module home screen.
