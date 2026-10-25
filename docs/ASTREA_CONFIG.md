# Astrea protected download configuration test

PCS 1.0.release.825068975 (116015), inspected from the supplied APKPure XAPK:
- fin initializes ProtectedDownload__enabled to false.
- fin.a reads it through the DeviceFlagManager efh.g(String).
- The namespace is device_personalization_services.
- Without READ_DEVICE_CONFIG, efh.g returns null, selecting the false default.
- fju.g rejects manifest/download RPCs with FAILED_PRECONDITION: feature disabled.

0.1.4 adds com.google.android.as.oss to the suggested LSPosed scope. Enable that scope and restart Astrea and AICore. Only the inspected PCS version is accepted; the exact String getter signature must match. Only ProtectedDownload__enabled returns true. Other keys call the original getter. Gboard configuration, attestation, VM settings and permission enforcement are unchanged. Remove Astrea from scope and restart it to revert.

Success is demonstrated by the flag-hook applied log and a changed manifest initialization result, not merely by an installed-hook message. Download, device eligibility, model compatibility and inference remain unverified.
