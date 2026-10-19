#!/system/bin/sh
echo "AICore system-app and permission check (read only)"
pm list packages -s --user 0 com.google.android.aicore
dumpsys package com.google.android.aicore | grep -E 'codePath=|pkgFlags=|READ_DEVICE_CONFIG'
echo "Expected: system package listed; READ_DEVICE_CONFIG granted=true."
echo "These checks do not establish model delivery or inference."
