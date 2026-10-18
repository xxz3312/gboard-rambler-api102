# AICore manifest label diagnostics

0.1.5 observes ity.a(isz, Map, nrj):Object only in AICore 526928, version 0.release.qc8635.prod_aicore_20260917.00_RC01. The hook logs at most six entries for com.google.android.aicore:18103149225492435673, and only the four configuration labels build_id, device_tier, variant and client_group. No other map contents, user input, keys or tokens are logged. Reflection/type errors retain the original request. No label or request values are changed.

Enable Android AICore in LSPosed scope in addition to Private Compute Services and existing Gboard scope. Restart AICore and run the foreground probe. Read logcat -b all -d -s PixelBoardLS for the installed and observed messages. Existing Astrea 116015 single-flag override is retained.

Static findings: gif.c reads AicDataRelease__build_labels with the empty mpj proto default. gif.g reads AicDataRelease__build_id_18103149225492435673 with default 21590. gsj.get builds the label map from these values. ity.a converts label names to request enums and build_id to long. Actual request labels must be observed before selecting any correction. A successful hook installation alone does not prove a request was observed or downloaded.
