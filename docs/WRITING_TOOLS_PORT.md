# AI Writing Tools migration (source-only, not enabled)

Sources: Akshayykadam/PixelBoard (GPL-3.0):
- `extensions/extension/src/main/java/com/akshaykadam/pixelboard/extension/writingtools/GboardAiWritingToolsRuntime.java`
- `patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/features/writingtools/GboardAiWritingToolsFlagValuePatch.kt`
- `patches/src/main/kotlin/com/akshaykadam/pixelboard/patches/gboard/shared/GboardFlagFamilyComposer.kt`

Implemented:
- `WritingToolsPolicy` conservatively inventories named Boolean vs String flags; never overrides them.
- `WritingToolsBindings` stores 18.0.3 symbolic method descriptor candidates, without installing any hooks.
- Shared getter family identified in upstream (`odx.g()`, `obw.g()`, `acps.g()`; `nxw.g()` has a generated name field, not yet verified locally).

Not implemented:
- Verified, exact target DEX bindings or meaningful Writing Tools feature activation.
- Flag value overrides, model routing, preference consent, client initialization compatibility.
- Bytecode-internal `INTENT_AUTO_FIX` list injection and acceptance branch, which cannot be transparently substituted by an ordinary method-result hook.
- Any change to the target app's manifest, resources, APK, or static DEX.

Policy: do not override backend hosts, consent bits, minor/age settings, or model downloads. Preserve stock values until a real Gboard build is verified.
