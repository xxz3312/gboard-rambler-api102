#!/system/bin/sh
# Read-only check. Do not infer model availability from the feature list.
echo "Declared SoC: $(getprop ro.soc.model)"
features=$(pm list features 2>&1)
for feature in com.google.android.feature.AICORE_QC com.google.android.feature.AICORE_QC_SM8850; do
  if printf '%s\n' "$features" | grep -qx "feature:$feature"; then
    echo "$feature = present"
  else
    echo "$feature = absent (check reboot and systemless mounting)"
  fi
done
echo "Feature declarations do not verify model download or inference."
