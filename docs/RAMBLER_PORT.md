# Rambler port (uncompiled, disabled)

GPL-3.0 source: Akshayykadam/PixelBoard, especially GboardFlagFamilyComposer and OfficialSelectionRuntime.

## Added

- Version-conditional selector lifecycle scopes with finalizer cleanup.
- Typed allowlisted flag policy and read-only `enable_jetson` preference.
- Gboard-only `ActivityThread.currentApplication()` Context lookup, no app Context caching.
- Getter receiver/field/getter signature validation for `odx` (18.4.1) and `obw` / `acps` (18.3.1).
- **18.0.3 flag name field intentionally not guessed**.
- Guarded `RamblerFlagPolicy.transform` attached in source but feature toggle is **false**.

## Outstanding

- Real APK DEX signature verification, including Gboard variants with overlapping candidate classes.
- Verify LSPosed API 102 Java hook syntax and Android hidden API behavior before building.
- Confirm official selector read/write semantics and integrate selection write only when justified.
- Add automated tests and validate against stock Gboard.
- Rambler actual activation / server eligibility remains unproven.

GitHub workflow has manual-only trigger; this code push does not request a build.
