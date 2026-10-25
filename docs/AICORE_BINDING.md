# AICore binding failure follow-up
User installed AICore 0.release.qc8635.prod_aicore_20260917.00_RC01 (526928).
Both probes failed with 601 BINDING_FAILURE, rooted in SecurityException:
Not allowed to bind to AiCoreMultiUserService. Model availability was not reached.

0.1.3 explicitly requests com.google.android.apps.aicore.service.BIND_SERVICE.
The same permission is used by Google's Private Compute Services manifest:
https://github.com/google/private-compute-services/blob/master/src/com/google/android/as/oss/AndroidManifest.xml

Do not assume the installed AICore permission protection level or that declaring
a permission guarantees a grant. The foreground probe now reports the installed
service descriptors (including enabled/exported/permission), the permission
definition owner/protectionLevel, and grants for this module and Gboard.
Only diagnostics and the module's permission declaration changed.
No permission enforcement hooks, model downloads, inference requests, AICore
repackaging, or cloud/Rambler policy changes are introduced.
