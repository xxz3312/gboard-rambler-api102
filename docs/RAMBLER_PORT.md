# Rambler runtime migration status

Based on GPL-3.0 PixelBoard upstream sources:
- `GboardRambler1803OfficialSelectionRuntime.java`
- `GboardRambler1803StockPolicy.java`
- `GboardRambler1803OfficialSelectorPatch.kt`
- `GboardFlagFamilyComposer.kt`

## Implemented source (unbuilt)

- Complete-descriptor Rambler selector mapping for three published version families.
- Reentrant voice-settings and default-selection scope wrappers.
- Selection read hook observes a returned Boolean, using the same host Context argument; selection write hook currently pass-through.
- Typed flag getter candidate mapping: `odx.g()/a` (18.4.1), `obw.g()/a` (18.3.1 release), `acps.g()/a` (18.3.1), `nxw.g()/a` (18.0.3).
- Getter hook is **pass-through only**, until Gboard Context acquisition and owner/flag semantics are verified on-device.
- All behavior-changing feature toggles disabled, builds manual only.

## Pending

1. Verify flag getter owner/field with actual target APK and distinguish all patch variants.
2. Wire explicit Gboard app Context acquisition rather than relying on null or global mutable context.
3. Verify official selector write argument semantics; do not assume its Boolean is the selected mode.
4. Wire `RamblerFlagPolicy.transform` only when Context and flag receiver are verified.
5. Unit-test selection / scope cases and ART hook installation, then consider enabling flags.

No claim of functional Rambler activation yet.
