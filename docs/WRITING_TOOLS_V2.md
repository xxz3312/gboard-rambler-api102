# Writing Tools V2 backend experiment — 0.1.8

Branch: writing-tools-v2-experiment. Based on the tested 0.1.7 cloud entry build.
The user's screenshot confirms the basic entry and English proofreading result.
Text replacement, other styles and V2 backends have not yet been confirmed.

## Implemented

- Independent V2 opt-in, off by default. The main Writing Tools switch is required.
- AICore (backend 2): hybrid=true. From 0.1.11, on-device proofread defaults false
  and is an independent opt-in; earlier builds enabled it together with backend 2.
- Astrea (backend 3): hybrid=true, on-device proofread=false.
- The V2 presentation flags follow the selected experimental mode.
- Disabling V2 restores the exact 0.1.7 basic policy at the next Gboard start:
  backend 1, V2=false, hybrid=false, on-device proofread=false.
- Both settings are synced through LSPosed API 102 remote preferences.
- Exact Gboard version and method descriptors guard V2 activation. Unknown or
  mismatched profiles retain the basic cloud policy.
- Unknown backend configuration cannot activate V2.
- Consent, account/age checks, model provisioning/readiness, endpoints, model
  IDs, feature IDs, build labels and device identity are never fabricated.
- Rambler implementation is unchanged.

## Verified against the supplied APKs

Gboard: 18.4.1.985164140-release-arm64-v8a / 176027078.

- oli.j is the Long writing_tools_v2_backend_type flag.
- oli.b():omd selects the backend. omd values 1/2/3 are GBOARD_SERVER,
  PRIVATE_INFERENCE_AICORE and PRIVATE_INFERENCE_ASTREA.
- ojq.n requests Feature 703 for private inference stylization; ojq.l(String,int)
  returns the session preparation future. This is different from the public
  ML Kit rewriting/proofreading features 607 and 614.
- oju.h(Context,pxf) creates the Astrea client bound to
  com.google.android.as.oss/com.google.android.apps.miphone.astrea.grpc.AstreaGrpcService.
- xin.b(Runnable,Executor) registers completion listeners. Diagnostic listeners
  only inspect a completed future with zero timeout, and never close/cancel it.
- omo.a():omq creates a response. omq.h (omr) and omq.k (omn) hold only status
  and inference-source enums. No response text is read by diagnostics.
- qyn.a():qym reports network state, not backend identity. Its old log label
  was wrong and is now network=. The real selector is logged separately.
- Remove the remaining stale diagnostics claim that pmb.n reads plx.n.

AICore: 0.release.qc8635.prod_aicore_20260917.00_RC01 / 526928.
A version-guarded, read-only observer logs four manifest labels for the previously
identified client. It does not apply the old 22347 build-ID experiment.

Astrea: 1.0.release.825068975 / 116015.
When V2 is explicitly enabled, the inspected efh.g(String):String getter returns
"true" only for ProtectedDownload__enabled, allowing the stock manifest path
to run. The exported AstreaGrpcService has no manifest binding permission in
the supplied APK; successful declaration lookup is not RPC authentication.

The upstream PixelBoard runtime (GPL-3.0) supplies backend policy values and
V2 flag names. Its Pixel-only project whitelist is not copied as a claim of
Xiaomi support. This opt-in experiment instead requires the inspected binary
profile and retains actual backend eligibility and readiness checks.

## Unresolved

Earlier device logs show GetManifestConfig returning INVALID_ARGUMENT.
This build does not claim to fix the remote request validation. It makes the
real V2 path testable and distinguishes its result from unrelated public APIs.
Neither a V2 menu nor a successful API query proves that inference works.

Previously Gboard's BIND_SERVICE permission check was false even though the
supplied Gboard APK declares that normal permission. The status page reports
the current grant. If binding still fails, permission reconciliation must be
diagnosed from the real V2 attempt; no system-wide permission bypass is added.

## Device test

1. Install this Debug update; keep the original Gboard installed.
2. Scope the module to Gboard, AICore and Private Compute Services.
3. Enable the main Writing Tools switch, then V2. Select one backend.
4. Restart all three processes (or reboot after updating LSPosed scope).
5. In Notes with English (US), try a freeform edit instruction or V2 style.
   Complete Gboard's own setup/consent if presented.
6. Verify an actual returned edit and applying it. Compare AICore and Astrea
   separately, restarting all three after changing settings.
7. If blocked, disable V2 and restart all three to restore the basic policy.

Relevant logs (bounded; no keyboard text logged by this module):
```sh
su -c 'logcat -b all -d -v threadtime -s PixelBoardLS' | tail -n 120
```

The optional public API probe is labelled 607/614 and does not switch backends,
start inference or prove that Gboard Feature 703 works.

## 0.1.9 binding guard

The user tested 0.1.8 with AICore selected and reported that Writing Tools became
unavailable. The supplied report still shows Gboard missing the service's
BIND_SERVICE permission. Public API features 607/614 remain unavailable; no
real V2 session log has yet been supplied.

0.1.9 checks the selected service declaration, enabled/exported state and
Gboard's actual grant before applying any V2/backend flags. If these prerequisites
fail or cannot be verified, the Gboard process keeps the 0.1.7 basic cloud
policy for its entire lifetime. The app shows the reason, and logs distinguish
requestedBackend from effectiveBackend. Restart all affected processes after
installing or changing settings; already running hooks cannot be removed by a
preference update alone.

Backend package hooks also remain inactive when the binding check is blocked.
Astrea is checked against its own exported service and permission declaration;
an absent AICore grant does not by itself block the Astrea choice.

This guard prevents the known configuration failure. It does not grant a
permission, bypass system security, fix missing models or repair the remote
INVALID_ARGUMENT result. A successful preflight is not a successful RPC.

## 0.1.10 in-app runtime report

The user's AICore grant is now true, Gboard is the active IME with a running
process, and the main log buffer is present. Existing logcat output still does
not establish which hooks or V2 requests ran.

This build adds a private, bounded runtime report in the module settings, with
copy and transport self-test buttons. Android 34+ explicit broadcasts use
BroadcastOptions.setShareIdentityEnabled(true). The receiver verifies the
system-provided sender package and UID against the three scoped packages (the
module itself may only send a CHANNEL_TEST). Intent extras cannot provide sender
identity. History is limited to forty records, and the most recent Gboard
initialization record is retained separately. Details contain only fixed event
codes, module version, PID, selected backend and bounded metadata; no keyboard
text, prompts, responses, tokens, raw exceptions or binder objects are sent.

Critical events also go to the LSPosed framework logger, independently of app
logcat. The startup code handles an already-existing Gboard Application with the
same AtomicBoolean once-only guard as the attach hook. This covers late package
initialization without treating that timing issue as the proven current cause.

New event categories distinguish initialization, flag-hook installation,
effective mode, V2 backend selection, session preparation completion/failure,
response source/status, and the prompt panel's status enum. Observers preserve
original return values, future ownership, eligibility and request content.

Open the module once after installation, run the diagnostic channel self-test,
then restart Gboard and reproduce one actual Writing Tools attempt. Return to
the module and copy the runtime status. Self-test delivery alone does not prove
Gboard injection; absence of a Gboard event can mean a process not yet restarted
or delivery failure and is not labelled definitive non-injection.

Compilation and host-side format/policy tests do not verify cross-process
broadcast delivery on the user's ROM. Android versions below 34 retain framework
logging but do not use this status broadcast channel.

## 0.1.11 private availability and optional local proofreading

The supplied 0.1.10 runtime report confirms Gboard hook execution and real backend
selection PRIVATE_INFERENCE_AICORE. It reports proofreadProvisioned/Ready=true,
handlerEverProvisioned=true, but current handler provisioned/ready=false.
This excludes missing injection as the explanation for that attempt.

Decoded 18.4 instructions show qrr.<clinit> initially sets the base proofread
signals true; they do not establish private or on-device model availability.
qrr.b returns the active handler kind (1 base, 2 on-device, 3 hybrid), and qrr.a
maps the current signals to that kind's actual signals.

hfz (HybridGenAi) registers with ojm, checks availability asynchronously and
updates a real oiz status enum. oiz includes UNKNOWN, READY,
AI_CORE_CONNECTION_ERROR, AI_CORE_MODEL_NOT_FOUND, AI_CORE_MODEL_DOWNLOADABLE,
AI_CORE_MODEL_DOWNLOADING and HYBRID_MODEL_TEMPORARILY_UNAVAILABLE.
No status is forced in this module.

Previously backend 2 also enabled inw.a/enable_on_device_proofread, creating an
optional imu local proofreader. hgm.b waits for both the private handler and this
optional local handler before combining availability. A pending local check can
therefore delay the aggregate result even when the private request has finished.
This is a verified dependency, not a claim that it is the only cause of the
user's failure.

0.1.11 separates the local proofreader into an off-by-default switch. Backend 2
still selects the same private AICore path; backend 3 never enables the optional
local proofreader. Base cloud policy, model IDs, consent and readiness remain
unchanged. The new setting is synchronized through the API 102 preference bridge.

New read-only observers report:
- ojq.b(Context):xin completion for the private Feature 703 availability check;
- imu.b(Context):xin completion when optional local proofreading is requested;
- hgm.b(Context):xin aggregate completion;
- active handler kind/class, hfz.g enum, and pending/done metadata for status checks.

The snapshot reads fields and signal-registry state only. It does not invoke
a model-status getter that might schedule work, submit inference, download a
model or alter the existing future. Completion observers use zero-timeout reads
only after completion and never cancel or close the original operation.

The report now acknowledges later Gboard events when its initial broadcast was
not retained, instead of implying that a missing startup record means missing
injection. The new opt-in isolation is covered by host-side policy tests.

Test with the main Writing Tools and V2 switches enabled, backend AICore,
optional local proofreading off. Restart the affected processes, attempt one
Writing Tools operation, then copy the module's runtime report. Private status
failure still requires interpreting the actual enum and backend response;
0.1.11 does not claim to fix a missing Feature 703 or server manifest rejection.

## 0.1.12 live diagnostics

Runtime reports now isolate history by the injected module version. A live query sends
a random challenge to the running Gboard receiver; only an OS-authenticated Gboard
reply matching that challenge is accepted. The reply reports its actual module version
and PID, plus read-only handler metadata. An absent reply does not prove injection failed.

The optional Root reload button runs only on a user tap and stops the three fixed target
packages. Its exit status is displayed; it does not claim that a new module is loaded.
Use the settings test editor to start Gboard, then request a live reply. No input text is
included in diagnostic broadcasts. Model readiness, consent and backend policy are unchanged.
