# PixelBoard-equivalent Writing Tools port

Branch: `writing-tools-upstream-port`. This is a staging branch, separate from
the currently working Rambler module. No APK compiled/released from this branch.

## Root cause of earlier attempt

Flag overrides alone only proved that `enable_writing_tools_v2` was read.
On the user's device the Gboard Settings Writing Tools switch remains greyed out
and no writing toolbar action appears. Additional flags are not evidence of a
working implementation.

## Upstream reference (Akshayykadam/PixelBoard, GPL-3.0)

The published `gboardAiWritingToolsPatch` depends on:
1. `gboardPatchesSettingsPatch`
2. `gboardAiWritingToolsFeatureMarkerPatch`
3. `gboardAiWritingToolsFlagValuePatch` (typed flag result delegate)
4. `gboardAiWritingTools1803AutoFixRoutePatch`
5. `gboardAiWritingTools1803AutoFixAcceptancePatch`
6. `gboardAiWritingTools1803GenAiInitPatch`
7. `gboardAiWritingTools1803GenAiRefreshPatch`
8. `gboardAiWritingTools1803SmartEditInitPatch`

The route/acceptance patches insert DEX instructions inside existing Gboard
methods; the GenAI/SmartEdit/refresh patches insert delegate invocations at
precise instruction offsets. These are **not** equivalent to toggling Boolean
flags or blindly intercepting the outer method return.

## Plan for original Gboard under LSPosed API 102

- Match the user's exact Gboard APK `versionName`, `versionCode` and
  method descriptors, then identify the equivalent code branches / call sites.
- Implement narrowly scoped runtime interception only where it yields
  behavior equivalent to the upstream DEX changes.
- For inside-method list insertion / branch edits that cannot be reproduced by
  a safe API 102 method-level hook, explicitly mark the unsupported behavior and
  evaluate whether the user accepts an APK-patch-based build instead.
- Keep all new code gated behind the independent Writing Tools toggle.
- Preserve Rambler bindings, scope and policy unchanged.
- Fail closed on unknown or ambiguous obfuscation profiles; do not override
  arbitrary signals, user consent, age/minor restrictions, or signatures.
- Only build a Debug APK after target descriptors are verified; run Rambler
  regression checks before considering a Release.

## Needed for exact adaptation

A copy of the exact installed Gboard base APK (and split APKs if the installation
uses splits), or an equivalent complete class/method dump from that same build.
A settings screenshot alone does not reveal the obfuscated internal entry methods.

Upstream file paths:
- `pixelboard-patches/patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/registry/GboardPatchRegistry.kt`
- `pixelboard-patches/patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/writingtools/GboardAiWritingToolsFlagValuePatch.kt`
- `pixelboard-patches/extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsRuntime.java`
