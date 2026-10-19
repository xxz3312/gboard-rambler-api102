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
