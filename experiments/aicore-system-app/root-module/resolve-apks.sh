#!/system/bin/sh
# Root-manager environments can break the Java pm wrapper. Prefer native cmd.
# This helper only resolves paths. The caller must pin-check EVERY APK.
resolve_aicore_apks() {
  query_error="$TMPDIR/pixelboard_aicore_query_error.txt"
  if /system/bin/cmd package path --user 0 com.google.android.aicore > "$paths_file" 2> "$query_error" \
      && [ -s "$paths_file" ]; then
    ui_print "- AICore paths resolved through native cmd package"
    rm -f "$query_error"
    return 0
  fi
  ui_print "- Package query unavailable; checking exact AICore data-app directory"
  head -n 6 "$query_error" | while IFS= read -r line; do ui_print "  $line"; done
  rm -f "$query_error"
  found_dir=""
  directory_count=0
  for base_file in /data/app/*/com.google.android.aicore-*/base.apk /data/app/com.google.android.aicore-*/base.apk; do
    [ -f "$base_file" ] || continue
    directory_count=$((directory_count + 1))
    found_dir=${base_file%/*}
  done
  [ "$directory_count" -eq 1 ] || abort "Expected one AICore APK directory; found $directory_count. Refusing ambiguous paths."
  : > "$paths_file"
  for apk_file in "$found_dir"/*.apk; do
    [ -f "$apk_file" ] || continue
    printf 'package:%s\n' "$apk_file" >> "$paths_file"
  done
  ui_print "- One directory found; all APK hashes will be verified before staging"
}
