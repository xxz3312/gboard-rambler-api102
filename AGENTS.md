# Project instructions

## Release policy

- Publish a Release only when the user explicitly requests publication.
- Use plain numeric app versions (for example, 0.1.29).
- Release downloads must contain only APK files and the corresponding source code. GitHub's automatic source archives satisfy the source-download requirement.
- Do not attach R8 mapping files, checksum text files, dependency reports, notices ZIPs, or other diagnostic files to Releases. Keep build diagnostics in CI artifacts when needed.
- Display the released APK SHA-256 in README instead of attaching a checksum file.
- Keep R8 code shrinking and resource shrinking enabled for Release builds.
- Preserve license notices in the APK and source repository.
- Name every published APK `<application-name>-<architecture>-<version>.apk`, for example `Gboard-Rambler-API102-arm64-0.1.29.apk`.
- The architecture label must match the actual APK contents: use `arm64` for arm64-v8a-only APKs, `arm` for armeabi-v7a-only APKs, and `universal` for APKs containing multiple ABIs. Do not label a universal APK as arm64.
