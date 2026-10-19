#!/system/bin/sh
[ "$BOOTMODE" = true ] || abort "Install from the root manager while Android is running."
[ "$(getprop ro.soc.model)" = SM8850 ] || abort "Only SM8850 is supported."
sdk_level=$(getprop ro.build.version.sdk)
case "$sdk_level" in ''|*[!0-9]*) abort "Cannot read API level." ;; esac
[ "$sdk_level" -ge 34 ] || abort "API 34 or newer is required."

ui_print "- AICore 513248 ordinary system-app snapshot"
ui_print "- Copies your installed base APK and all installed splits"
ui_print "- SHA-256 must match the analyzed 513248 APK set"
ui_print "- Destination is product/app, not priv-app"
ui_print "- Requires systemless mounting and reboot"

paths_file="$TMPDIR/pixelboard_aicore_paths.txt"
. "$MODPATH/resolve-apks.sh"
resolve_aicore_apks
[ -s "$paths_file" ] || abort "No AICore APK paths resolved."
target_dir="$MODPATH/system/product/app/PixelBoardAiCore"
mkdir -p "$target_dir" || abort "Cannot create staged module directory."
base_seen=false
arm_seen=false
count=0
while IFS= read -r entry; do
  case "$entry" in package:/*) src=${entry#package:} ;; *) abort "Unexpected package path response." ;; esac
  name=${src##*/}
  expected=$(awk -v file="$name" '$2 == file {print $1}' "$MODPATH/apk-sha256.txt")
  [ -n "$expected" ] || abort "Unrecognized AICore split: $name. Refusing mixed APK versions."
  [ -f "$src" ] || abort "Cannot read APK: $name"
  actual=$(sha256sum "$src" | awk '{print $1}')
  [ "$actual" = "$expected" ] || abort "AICore APK differs from the verified 513248 set: $name"
  [ ! -e "$target_dir/$name" ] || abort "Duplicate APK name: $name"
  cp "$src" "$target_dir/$name" || abort "Cannot copy APK; check free space."
  copied=$(sha256sum "$target_dir/$name" | awk '{print $1}')
  [ "$copied" = "$expected" ] || abort "Copied APK verification failed: $name"
  case "$name" in base.apk) base_seen=true ;; split_config.arm64_v8a.apk) arm_seen=true ;; esac
  count=$((count + 1))
done < "$paths_file"
[ "$base_seen" = true ] || abort "Base APK missing."
[ "$arm_seen" = true ] || abort "Required arm64 split missing."
# This APK has extractNativeLibs=true. System apps need the on-disk ISA layout
# as well as APKs; merely copying a compressed native split is insufficient.
native_tmp="$MODPATH/.native-stage"
mkdir -p "$native_tmp" || abort "Cannot stage native libraries."
unzip -o "$target_dir/split_config.arm64_v8a.apk" 'lib/arm64-v8a/*.so' -d "$native_tmp" >/dev/null \
  || abort "Cannot extract verified native libraries; check free space."
[ -f "$native_tmp/lib/arm64-v8a/libQnnHtpV81Skel.so" ] || abort "Verified split lacks V81 runtime."
[ -f "$native_tmp/lib/arm64-v8a/libgoogle3.so" ] || abort "Verified split lacks core native library."
mkdir -p "$target_dir/lib" || abort "Cannot create native library destination."
mv "$native_tmp/lib/arm64-v8a" "$target_dir/lib/arm64" || abort "Cannot stage native ISA layout."
rm -rf "$native_tmp"
rm -f "$paths_file"
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/action.sh" 0 0 0755
ui_print "- Verified and staged $count APKs. Reboot to activate."
ui_print "- Keep the existing SM8850 feature-declaration module enabled."
ui_print "- Model delivery and inference remain unverified."
