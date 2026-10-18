# Gboard Rambler — LSPosed API 102

Experimental runtime port based on [PixelBoard](https://github.com/Akshayykadam/PixelBoard),
GPL-3.0. Rambler works on the user's tested setup. Writing Tools remains experimental.

The `writing-tools-upstream-port` branch builds Debug APKs through GitHub Actions.
Current version: **0.1.7-cloud-entry**. It targets the supplied Gboard
18.4.1.985164140-release-arm64-v8a for the new entry hook, and scopes only Gboard.
Earlier AICore/Astrea provisioning experiments are inactive in this build.

Enable features individually in the module, then force-stop and reopen Gboard.
[Cloud entry test and known limits](docs/BASIC_CLOUD_TEST.md).
[Upstream adaptation notes](docs/WRITING_TOOLS_UPSTREAM_PORT.md).
Logcat tag: `PixelBoardLS`.

Build validation checks compilation and policy unit tests. It does not establish
that Google's cloud feature is available or that the Writing Tools UI works.
