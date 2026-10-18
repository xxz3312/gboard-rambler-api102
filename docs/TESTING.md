# Test remote settings after Xiaomi provider rejection

Settings now use LSPosed API 102 remote preferences instead of Gboard querying our module ContentProvider. The settings app registers XposedServiceHelper and mirrors its local feature switches to the framework on connection and after a switch changes. The injected module calls getRemotePreferences("features"); on errors, all toggles stay off and a framework log is emitted.

After installing a newly compiled debug APK, open module settings and verify the service connection and remote sync in PixelBoardLS logs. Enable Diagnostics, force stop Gboard, and reopen it. Inspect LSPosed framework logs for "Remote settings read" and method selection. The Writing Tools switch does not install actual Writing Tools hooks. This source change has not been compiled yet.
