# Writing Tools API 102 migration status

Based on Akshayykadam/PixelBoard (GPL-3.0), especially
`GboardAiWritingToolsRuntime`, `GboardAiWritingToolsDeviceSupport`,
`GboardAiWritingToolsFlagValuePatch`.

## Implemented (source only; no build)
- `WritingToolsFlagPolicy`: typed and feature-scoped subset of upstream legacy
  Writing Helper/Proofread flags and V2 flags.
- `PixelBoardModule`: shares the existing verified Gboard flag getter binding,
  applies Writing Tools policy separately, then the existing Rambler policy.
- `writing_tools` remote preference remains independent and defaults OFF.
- V2 override only for eligible Google Pixel models, with stock values otherwise.
- No backend, AICore, consent, endpoint, minor/age or model-download spoofing.
- Existing `WritingToolsBindings` is a descriptor inventory, NOT installed hooks.
- `WritingToolsPolicy` remains read-only inventory retained for reference.

- `WritingToolsDiagnostics`: independent read-only resolution of upstream 18.0.3 AutoFix, GenAI/SmartEdit and refresh method signatures. No method interception or invocation.\n\n## Upstream functionality not yet ported
PixelBoard also uses DEX bytecode edits for signal gates, official preferences,
GenAI/SmartEdit initialization, client refresh, AUTO_FIX route construction and
ambiguity acceptance; flag getters alone do not substitute for these edits.
Methods are obfuscated and version-specific. 18.0.3 DEX bindings are not a safe
assumption for 18.3.1 / 18.4.1.

## Future checks
- Verify unchanged Rambler behavior first with Writing Tools OFF.
- Enable Writing Tools separately; check `PixelBoardLS` logs, toolbar entry,
  selected-text actions and real proofread/stylization.
- If missing, locate exact Gboard version-specific controller / signal / init
  methods before building additional hooks.
- Do not infer success from a saved toggle.

No GitHub Actions build or release was triggered.

## Notes from upstream source inspection
- Signal identities are `gvu#a`, `lvi#b`, `gqd#a`; the upstream code forces only these signal objects, not arbitrary signal results. No equivalent signal gate method signature has been verified in this project, so no signal Hook is installed.
- Upstream official preference resource IDs are `0x7f1409e7`, `0x7f140b0f`, `0x7f140d0b` for its target APK. Resource IDs must not be assumed stable across stock versions; do not apply these constants blindly.
- Upstream default preference seeding includes Writing Tools and screen context opt-in keys. This port deliberately does not synthesize user consent.
- Current flag interception still uses the Rambler selector profile for the shared getter. The diagnostics and typed flag getter now resolve independently from the voice selector. If multiple candidates match, the resolver skips installation instead of guessing.

## Runtime isolation update
- A single typed flag getter hook now applies independent Rambler and Writing Tools policies.
- The independent getter resolver runs even when the voice selector cannot be matched; it skips ambiguous symbol families.
- The voice selector hooks are installed only for Rambler/diagnostics, never just because Writing Tools is switched on.
- Signal gate and settings controller method signatures remain unknown; no speculative hooks added.

## Signal identity diagnostic extension
- `WritingToolsDiagnostics.probeSignals` inspects the declaring classes and
  static field types for PixelBoard's `gvu#a`, `lvi#b`, and `gqd#a`.
- It deliberately does **not** read the static field values (which could
  initialize Gboard classes), mutate signal state, or install a signal-result hook.
- Finding these descriptors alone does not establish the consumer method,
  branch semantics, or Writing Tools toolbar eligibility.
- The upstream 18.0.3 signal/resource identities must be revalidated for the
  actual Gboard build before safely applying runtime result changes.

## Upstream provenance review
- The published PixelBoard Writing Tools patch registration invokes the shared
  flag-family patch and 18.0.3 AutoFix / initialization bytecode patches.
- The upstream runtime **contains** signal and Settings Controller helper methods,
  but finding helper methods does **not** establish that they are reachable from
  stock Gboard. The inspected WritingToolsFlagValuePatch has no matching injected
  call sites for `applySignalResult`, `enterSettingsControllerScope`, or
  `shouldBypassSettingsRemoval`; they are NOT treated as verified fixes.
- The first practical prerequisite is proof that a typed flag getter maps to the
  actual installed Gboard version. Diagnostics now report unique getter resolution.
- To port a settings-removal bypass or signal override, derive the concrete
  caller/target from a matching APK's method graph before installing a hook.

## Server-backend entry experiment
- On non-Pixel devices, enabling the separate Writing Tools switch now opts into
  `writing_tools`, `writing_helper`, `config_proofread`, and three narrowly
  scoped V2 entry flags (`enable_writing_tools_v2`,
  `enable_writing_tools_v2_on_toolbar`, `writing_tools_v2`).
- For the typed backend flag `writing_tools_v2_backend_type`, the experimental
  policy requests upstream's server backend value `Long(1)`. This is **not**
  proof of server entitlement or successful generation.
- On-device AICore/My Style/suggest-style/consent/resource flags stay untouched.
- The shared Hook logs at most 40 Writing Tools flag encounters per process,
  recording names, value types and whether changed; it never logs typed text.
- If the toolbar still does not appear, exact controller and eligibility callers
  must be identified. Blindly forcing all signals or fabricating consent is
  not an acceptable substitute.
- Source only, uncompiled, not verified. Prior notes describing non-Pixel V2
  as always stock are superseded by this opt-in server entry experiment.
