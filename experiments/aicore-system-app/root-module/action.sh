#!/system/bin/sh
echo "AICore system-app and permission check (read only)"
/system/bin/cmd package list packages -s --user 0 com.google.android.aicore
/system/bin/dumpsys package com.google.android.aicore | grep -E 'codePath=|pkgFlags=|READ_DEVICE_CONFIG'
echo "Expected: system package listed; READ_DEVICE_CONFIG granted=true."
echo "These checks do not establish model delivery or inference."
