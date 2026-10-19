# SM8850 AICore feature declaration experiment

Download the `AICore-SM8850-Features-0.1.1-INSTALL-DIRECTLY` Actions artifact.
Install the downloaded ZIP directly in KernelSU's module manager, then reboot.
The artifact contents are the module itself: `module.prop` is at ZIP root.
Do not extract it or select a ZIP containing another module ZIP. Current KernelSU
requires a system-mounting metamodule; older implementations may provide the
mount internally. Do not extract or copy files to the real product partition.
The installer accepts only `ro.soc.model=SM8850`, API 34+, and Android boot mode.

The module contributes one XML file under `/product/etc/sysconfig/`, declaring
only `com.google.android.feature.AICORE_QC` and
`com.google.android.feature.AICORE_QC_SM8850`. These feature names were reported
in the Xiaomi 17 Ultra global firmware on the same SoC; that is a reference,
not proof of full firmware equivalence with Xiaomi 17 Pro Max.

It contains no AICore APK, privileged app conversion, permission grant, model
label overrides, device spoofing, keys, tokens, or inference implementation.
Existing Gboard/Rambler preferences are untouched. It does not grant server
access or bypass model/device checks.

After reboot, use the root manager's Action button for a read-only declaration
check, or `su -c 'pm list features' | grep -i aicore`. In PixelBoard 0.1.20,
open the local Prompt screen and query again. Both feature flags should become
true if the file was mounted and parsed. AICore may still return UNAVAILABLE or
606 until/if a compatible configuration and model are delivered. Do not use
the mere presence of these flags as proof that a model is installed. The SDK
does not officially support unlocked bootloaders.

Disable/remove this module and reboot to undo its declarations. No persistent
property or DeviceConfig value is changed. Any subsequent Play update or
AICore model download is managed separately by those apps.

Source reference:
https://github.com/mweinbach/lineage-os-xiaomi17u/blob/main/docs/aicore-global-20260911.md

Mounting reference:
https://kernelsu.org/guide/module.html

Validation: XML and installer guards are host-tested; mounting, Play selection,
model delivery and offline inference are not yet verified on the user's phone.
