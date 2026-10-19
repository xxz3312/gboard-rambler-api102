# AICore ordinary system-app experiment for SM8850

Install the `AICore-SM8850-SystemApp-0.1.0-INSTALL-DIRECTLY` artifact ZIP directly
in KernelSU, then reboot. Keep the separate feature-declaration module enabled.
Use the root manager's existing systemless mounting provider. No AICore APK is
redistributed in this ZIP: installation snapshots the already installed app.

The input must match the analyzed Google-signed 513248 APK set, SHA-256 pinned
per file. Every installed split is validated and copied alongside base.apk;
base and arm64 are mandatory. An unknown, changed, missing, duplicate or mixed
split aborts. Check at least 500 MB of free space before installing. This APK
sets extractNativeLibs=true and has compressed native libraries: the installer
also extracts the verified arm64 split into the system-app `lib/arm64` layout,
including HTP V81. The original data APKs and libraries are not modified.

The snapshot is staged only inside this module under
`system/product/app/PixelBoardAiCore`. It is an ordinary system app, NOT a
privileged app. No privileged-permission allowlist, SELinux rules, pm grant,
DeviceConfig overrides, property spoofing, keystore or attestation edits are
included. Android 17 declares READ_DEVICE_CONFIG as signature|preinstalled;
this experiment lets the real package permission policy evaluate system-app
status on reboot. Other preinstalled-only permissions may also be granted by
Android; this is not a narrowly scoped single-permission runtime grant.

The original data APKs and app data are not removed. Android may continue to
show a `/data/app` codePath for an updated system app; use `pm list packages -s`
and the actual granted permission, rather than codePath alone, as acceptance.
If mounting/package classification/grant does not take effect, stop here;
do not claim configuration access was fixed. This does not provide a full OEM
AICore platform integration or guarantee protected downloads/model inference.

After reboot, use the module Action button. Expected: the package is listed as
a system package and READ_DEVICE_CONFIG appears with granted=true. Then query
the PixelBoard 0.1.20 local Prompt screen. The stored DeviceConfig labels can
still be absent or unsuitable even when they are readable; no arbitrary build
or variant is substituted.

To undo: disable/remove ONLY this system-app module and reboot. This removes
the systemless APK snapshot; the pre-existing data installation is retained.
Permission status is reevaluated by Android. Any later app updates/data changes
are managed separately. For a later AICore version, disable this snapshot and
reboot before replacing the data APK; this installer intentionally rejects
unreviewed versions.

Host tests cover installer guards, split completeness, hashes and staged copy.
On-device mounting, system-app classification, permission grant and inference
have not yet been verified on the user's device.

Reference:
https://android.googlesource.com/platform/frameworks/base/+/android17-release/core/res/AndroidManifest.xml
