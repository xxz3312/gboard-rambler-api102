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
- Current flag interception still uses the Rambler selector profile for the shared getter. The new diagnostics run independently; the Writing Tools getter is **not yet independently bound** when selector profile matching fails.
