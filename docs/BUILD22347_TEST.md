# Controlled build ID experiment

0.1.5 runtime observation: client com.google.android.aicore:18103149225492435673 submits build_id=21590, empty device_tier, VARIANT_UNSPECIFIED, client_group=all. Root DeviceConfig query returns build ID 22347 and empty build_labels. These observations do not identify which argument the remote service rejects.

0.1.6 changes only gif.g():Integer to 22347, after checking AICore 526928/full version and device model 2509FPN0BC/device popsicle. This value comes from the user's DeviceConfig query, not a guessed release. Device tier, variant, attestation and other build roles stay unchanged. The existing Astrea fix remains. Diagnostics report the original and effective build ID, then the request labels.

This is a device-specific experiment, not a general model enablement fix. Another INVALID_ARGUMENT result does not establish that the labels or the build ID are correct. Return to 0.1.5 or disable the AICore LSPosed scope and restart AICore to revert the experiment. No DeviceConfig writes or package/data changes are made.
