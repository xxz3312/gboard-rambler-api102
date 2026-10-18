# Rambler runtime migration notes

Upstream (GPL-3.0): `extensions/extension/.../rambler/GboardRambler1803OfficialSelectionRuntime.java`
and `GboardRambler1803StockPolicy.java`, author Akshay Kadam.

## Ported as independent policy classes

- `RamblerSelection`: maintain last verified official selection, check `enable_jetson` preference, preserve voice/default-selection scope precedence; never write preferences.
- `RamblerFlagPolicy`: explicit allowlist of 18 feature identifiers, plus dict settings and activation type with strict boxed-type checking.
- Gating on `FeatureConfig.ENABLE_RAMBLER`, which remains **false**.
- The policy is **not attached** to a Gboard flag reader. No functionality is enabled by this commit.

## Missing integration

1. Recover Gboard typed flag-reader signature and name/argument extraction from upstream flag-family implementation.
2. Validate the exact reader against a real official Gboard APK for each version.
3. Connect `RamblerSelection.observe` to the version-verified formal selector read/write hooks. Lifecycle wrappers currently observe only.
4. Wire policy to a narrowly targeted flag-read hook only after strict verification.
5. Unit-test scope precedence, disabled state, and boxed value type preservation.
6. Verify on-device behavior, without assuming server eligibility.

Do not globally intercept SharedPreferences or force `enable_jetson` to true; such changes are liable to leak beyond Gboard's official selector.
