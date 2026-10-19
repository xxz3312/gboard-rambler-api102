#!/system/bin/sh
# Sourced by KernelSU/Magisk module installers; never write to the real partition.
[ "$BOOTMODE" = true ] || abort "Install from the root manager while Android is running."
soc_model=$(getprop ro.soc.model)
sdk_level=$(getprop ro.build.version.sdk)
[ "$soc_model" = SM8850 ] || abort "Unsupported SoC: $soc_model. This experiment is restricted to SM8850."
case "$sdk_level" in
  ''|*[!0-9]*) abort "Cannot read Android API level." ;;
esac
[ "$sdk_level" -ge 34 ] || abort "Android API 34 or newer is required."
ui_print "- SM8850 feature declaration experiment"
ui_print "- Adds only AICORE_QC and AICORE_QC_SM8850"
ui_print "- No model, APK, identity, permission or attestation changes"
ui_print "- Reboot required after installation or removal"
if [ "$KSU" = true ]; then
  ui_print "- Your KernelSU must support mounting the system directory."
  ui_print "- Current KernelSU requires a mounting metamodule."
fi
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/action.sh" 0 0 0755
