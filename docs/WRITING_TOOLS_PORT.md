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

## Upstream functionality not yet ported
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
