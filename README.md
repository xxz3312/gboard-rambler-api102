# PixelBoard LSPosed — API 102

Experimental Modern LSPosed port for official Gboard, adapted from [Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard) (GPL-3.0).

## State (source-only)

- API 102 entry and static scope for `com.google.android.inputmethod.latin`.
- Version-specific candidate mappings for Gboard 18.0.3, 18.3.1, 18.4.1, fail-closed descriptor detection.
- Lifecycle scope wrappers and selection observers.
- `RamblerSelection`: read-only stock `enable_jetson` preference inspection, preserves explicit opt-out.
- `RamblerFlagPolicy`: strongly typed, bounded allowlist for Rambler feature flags, ready for a later verified flag reader integration.
- Flag policy **not hooked to Gboard yet**. Rambler, Writing Tools, static dex patches and resources are **not enabled**.
- All experimental hooks disabled by default.

See [Rambler porting status](docs/RAMBLER_PORT.md) and [overall plan](docs/PORTING.md).

## Build

Requires JDK 17, Android SDK, Gradle. Build with `gradle :app:assembleDebug` once ready. GitHub Actions is **manual-only** (`workflow_dispatch`); commits do not build.

## License

GPL-3.0. Preserve Akshay Kadam / PixelBoard attribution in derived adaptations.
