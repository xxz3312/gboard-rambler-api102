# Writing Tools V2 backend experiment — 0.1.8

Branch: writing-tools-v2-experiment. Based on the tested 0.1.7 cloud entry build.
The user's screenshot confirms the basic entry and English proofreading result.
Text replacement, other styles and V2 backends have not yet been confirmed.

## Implemented

- Independent V2 opt-in, off by default. The main Writing Tools switch is required.
- AICore (backend 2): hybrid=true, on-device proofread=true.
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
