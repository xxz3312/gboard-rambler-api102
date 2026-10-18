# Cloud entry test: 0.1.7

Target: supplied Gboard 18.4.1.985164140-release-arm64-v8a, versionCode 176027078.
This is an experimental API 102 runtime port, not a verified complete equivalent
of Akshayykadam/PixelBoard (GPL-3.0). The user has confirmed Rambler works;
Writing Tools entry and cloud responses remain unverified.

## Changes

- Enable writing_tools_enable_stable_entrance only on this exact Gboard version,
  behind the existing Writing Tools toggle. The inspected hpz.fM subscribes to
  this flag and activates the stock HandlerEverProvisioned tracking when true.
- Retain basic cloud policy: V2, hybrid and on-device proofread disabled.
- Remove the incorrect pmb.n observer. Actual instruction decoding shows
  pmb.n(nsn):String selects plx.a or plx.b, not plx.n.
- With diagnostic hooks enabled, observe stock hpz.fM and hmp.d completion.
  Log only five provisioning/readiness Booleans and the backend enum name,
  bounded to twelve callbacks per process. Do not log input or returned text.
- Scope only Gboard. Remove active AICore/Astrea hooks and their launcher test
  button. Even if old LSPosed scope selections persist, non-Gboard packages
  return before hook installation. Earlier experiment results remain in history.
- Correct the previous, unsupported claim that cloud tools passed user testing.
- Leave Rambler implementation, consent, account/age checks, model readiness,
  official user preferences and Google endpoints unchanged.

## Verified 18.4 conditions

hcz.getModuleDef registers the Jarvis module with NativeLibHelper.a, mnl.a
(AccessPointsManager_Created), hjp.a (MinorsCheckerPassedTag), and qrr.h
(HandlerEverProvisionedTag), plus writing-helper/proofread flags and language.
Never force these signals based solely on an obfuscated field name.

qrr.c/d are ProofreadModelProvisioned/Ready; qrr.g/h/i are
CurrentHandlerModelProvisioned/HandlerEverProvisioned/CurrentHandlerModelReady.
hmp.d reads provisioning, gjl.ac and gjl.ab before constructing the menu.
The new switch does not establish that any provisioning signal is true.

hpg.d, proofread_supported_apps, contains a default exclusion entry for
com.openai.chatgpt as well as Gmail, Docs and Chrome. Leave that policy intact;
use an ordinary Notes editor for the first test to avoid this known confounder.
Upstream 18.0.3 resource IDs and signal mappings must not be copied to 18.4.

## Test once

1. Install this Debug update. In LSPosed, keep Gboard enabled for the module.
2. Enable AI Writing Tools and Diagnostic hooks in the module. Preserve the
   existing Rambler setting. Force-stop Gboard and open it again.
3. Select English (US). In Notes type: "I has a meeting tomorrow."
   Inspect the toolbar/tools drawer and select the sentence to check actions.
   If Gboard shows its official Writing Tools setup, finish it.
4. Verify whether the entry appears and whether proofreading/rewriting returns
   an actual result. Check Rambler still works.

If the entry is absent, collect only the short relevant log:
```sh
su -c 'logcat -b all -d -v threadtime -s PixelBoardLS' | tail -n 80
```
A successful build or flag override is not proof of functional cloud Writing Tools.
