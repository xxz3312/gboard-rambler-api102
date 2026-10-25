# Third-party notices

- PixelBoard, Akshay Kadam / Akshayykadam and contributors: GPL-3.0; see LICENSE and NOTICE.
- libxposed API 102.0.0 (compile-only) and libxposed service/interface 102.0.0:
  Apache License 2.0. Source: https://github.com/libxposed/api and https://github.com/libxposed/service.
  Dependency POMs used for this release are included in the release notices archive.
- JUnit 4.13.2: test-only; not bundled in the module APK.
- Android platform APIs and the separately installed LSPosed framework/Gboard are
  external runtime requirements, not redistributed by this module.

The cloud-stable build removes the unused ML Kit AICore probe and its dependencies.
It includes no Google Gboard/AICore/PCS APK, model, account credential or service token.
Experimental branch dependencies have their own bundled notices and are not
included in the cloud-stable release.

- Material Components for Android 1.13.0 and AndroidX dependencies: Apache-2.0.
  https://github.com/material-components/material-components-android
  https://android.googlesource.com/platform/frameworks/support/
  Resolved dependency POMs accompany the release notices archive.

- DexKit 2.3.0: Android layer Apache-2.0, native core LGPL-3.0.
  https://github.com/LuckyPray/DexKit/tree/2.3.0
  Full license texts and upstream notice: assets/licenses/dexkit-*.txt.
  Native core is bundled without project modifications. To replace/relink it,
  build the matching upstream core or supply a compatible replacement shared
  library, replace lib/<ABI>/libdexkit.so, rebuild and sign the module APK.
  Module build scripts and source are provided with the release tag.
