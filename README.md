# Gboard Rambler — LSPosed API 102

Experimental runtime port based on [PixelBoard](https://github.com/Akshayykadam/PixelBoard), GPL-3.0.

## Test configuration UI (source only)

The new launcher SettingsActivity stores separate experimental toggles for Rambler, inline spellchecker, diagnostic hooks and a future Writing Tools implementation. Gboard reads the toggles from the module's read-only ContentProvider at process startup. Restart the Gboard process after changing toggles.

**Uncompiled, untested.** Rambler cannot be claimed functional; Writing Tools has no hooks. Logcat tag: `PixelBoardLS`. [Testing instructions](docs/TESTING.md).

GitHub Actions builds are manual only; no build or release was triggered.
