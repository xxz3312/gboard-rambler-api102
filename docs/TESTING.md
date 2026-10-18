# On-device testing (source not compiled)

1. Install the APK when a build is explicitly requested.
2. Enable the module in LSPosed and scope only Gboard.
3. Open **Gboard Rambler API102** from the launcher. Enable Diagnostics first.
4. Force stop Gboard, then launch a text field to restart it.
5. Filter Android logcat by `PixelBoardLS`. Module configuration is fetched through a public read-only ContentProvider.
6. Try the Rambler switch only after ensuring your installed Gboard version matches a supported binding; back up settings first. Switch it off and restart Gboard to revert.
7. Writing Tools has **no behavioral hook**. Its switch is displayed for future development only.

The provider exposes only non-sensitive Boolean feature toggles; writes are refused. Hook installation statuses are not shown in the interface yet. There is no APK build or real-device verification in this commit.
