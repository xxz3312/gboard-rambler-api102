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

## Exact user APK analysis: Gboard 18.4.1.985164140 arm64-v8a

Analyzed supplied 85 MB APK with DEX string/type/proto/method/field table
inspection, not assumptions from 18.0.3. The APK has four DEX files.

Verified:
- `Lodx;->g()Ljava/lang/Object;` is present (classes.dex).
- `Lodx;->a:Ljava/lang/String;` is present (classes.dex), matching the
  existing typed flag-name getter mapping for Gboard 18.4.1.
- Strings include `enable_writing_tools_v2`,
  `enable_writing_tools_v2_on_toolbar`, `writing_helper`,
  `config_proofread`.
- In contrast, the upstream 18.0.3 AUTO_FIX and GenAI patch signatures
  cannot be presumed valid for the installed 18.4.1 build.
- Upstream candidate signal descriptor `Llvi;->b` is `long` in this APK
  (classes2.dex); it is **not** a safe Writing Tools signal object to force.
- `Lgvu;->a` exists but has type `Lwfr;`; its meaning and identity cannot
  be inferred from the name alone.
- `Lgqd;->a` was not among resolved fields in the inspected DEX field
  inventory. Avoid using upstream 18.0.3 signal forcing.

Consequences:
- Keep the verified typed flag hook for 18.4.1, but stop treating upstream
  18.0.3 signal specs as valid runtime patch targets.
- Next identify 18.4.1's actual Writing Tools toolbar/policy conditions via
  cross-references from DEX code, not names or 18.0.3 obfuscation alone.
- No verified UI-controller bypass or equivalent AutoFix instruction-level
  hook has yet been established.

## 18.4.1 direct DEX instruction cross-reference (confirmed)
- `enable_writing_tools_v2_on_toolbar` is constructed in
  `Lplx;-><clinit>()V` and stored in static field `Lplx;->n`.
- `Lpmb;->n` reads `Lplx;->n` (confirmed DEX `sget` reference).
- The named field is not itself a Boolean: it is a typed flag object.
  The value computation and disabled-state/UI construction still require
  method-level control-flow inspection, including `Lpmb;->n` and callers.
- **Do not hook `Lpmb;->n` to unconditional true:** this would ignore
  arguments/return type/feature eligibility and could damage keyboard.
- The earlier 18.0.3 signal candidate `lvi#b` was invalid; the direct
  18.4.1 toolbar flag xref is a more reliable path.

Further parsed exact DEX descriptors: `Lplx;->n:Lodr;` and `Lpmb;->n(Lnsn;)Ljava/lang/String;`. Thus `pmb.n` is **not a Boolean availability gate**, despite reading the toolbar flag; it may use the flag in string-producing logic. Added read-only, exact signature check and will not force its return value.

## 18.4.1 passive toolbar-consumer observation

Installed an opt-in, read-only API 102 interceptor for verified
`Lpmb;->n(Lnsn;)Ljava/lang/String;` on the staging branch.
It calls `chain.proceed()` and returns the original result unchanged,
logging only bounded invocation counts and whether the result was null.
It never logs text, arguments, or returned strings.

An observed invocation will establish that the method is active in the
user's runtime, **not** that it is a UI eligibility/visibility gate.
The existing APK signature maps it to a string-producing consumer of
`Lplx;->n:Lodr;`. No value/eligibility force is performed.

The short supplied log from 2026-10-18 only contained repeated
`enable_writing_tools_v2` observations; it did not show toolbar gate
reads. It predates this observer and cannot prove non-execution.
