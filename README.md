# PixelBoard LSPosed — API 102

Experimental Modern LSPosed port for official Google Gboard, based on design and target mappings documented by [Akshayykadam/PixelBoard](https://github.com/Akshayykadam/PixelBoard) (GPL-3.0).

## Current status

- API 102 module scaffold, static Gboard scope and Gradle source configuration.
- Exact-reflection target signatures and fail-closed 18.0.3 / 18.3.1 / 18.4.1 Rambler mapping detection.
- Reentrant, exception-safe voice-settings / default-selection scopes and non-mutating selection observers.
- Experimental spellchecker boolean gate; **disabled**.
- No genuine Rambler activation, AI Writing Tools, resource/manifest rewriting, or runtime DEX replacement yet.
- All hooks **disabled by default**, including diagnostics; installing this project alone does not enable AI features.

## No-build development mode

GitHub Actions triggers **only** via the manual `workflow_dispatch` action. Code commits do not build an APK. Do not enable switches until validating exact Gboard dex signatures and runtime behavior.

## Attribution and license

PixelBoard target mapping concept and mappings originate from Akshay Kadam's GPL-3.0 project. The runtime scaffolding here is a new implementation. Distribute adapted code under GPL-3.0 with upstream attribution.
