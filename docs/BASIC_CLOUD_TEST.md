# Basic cloud Writing Tools test
This staging build 0.1.1-cloud-test follows PixelBoard's basic cloud policy:
legacy tools on, V2 off, backend Long(1), hybrid and on-device proofread off,
language allowlists "*", empty modeless smart-edit regex version "v3".
Derived from Akshayykadam/PixelBoard GboardAiWritingToolsRuntime.java (GPL-3.0).

Rambler policies/bindings/selection are unchanged. Consent, official preferences,
age restrictions, service endpoints and model readiness are not overridden.
This is a policy implementation, not a verified complete Writing Tools port.
Instruction-level voice-route patches and settings-controller adaptation remain
unimplemented. Actual entry visibility and server responses require device testing.

Install over the previous Debug module. Keep its Gboard scope enabled. Open
module settings and enable AI writing tools; keep Rambler in its existing state.
Restart Gboard (force stop and reopen, or reboot). In Gboard settings use the
official writing-tools controls if available. Check both the suggestion strip
and tools drawer after typing a sentence. Confirm Rambler still works.
Logs identify policy=basic-cloud-v1. Each targeted flag is logged once per process,
so repeated V2 reads no longer exhaust the budget before other flags appear.
Do not interpret compile/test success as proof of device availability.
