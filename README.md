# PixelBoard LSPosed (API 102)

Experimental Modern LSPosed module for official Gboard (`com.google.android.inputmethod.latin`), inspired by [PixelBoard](https://github.com/Akshayykadam/PixelBoard).

**Status:** initial scaffold, not a feature-complete port. The spellchecker hook is opt-in and untested. Rambler, Writing Tools, static DEX patching, manifest and resource patches are not implemented.

Build with JDK 17, Android SDK 35, Gradle 8.10.2: `gradle :app:assembleDebug`.

Uses `io.github.libxposed:api:102.0.0`. Does not modify Gboard APK or guarantee server-side feature eligibility.

Licensed under GPL-3.0; retain upstream attribution for any subsequently ported code.
