# Upstream sync

Reviewed PixelBoard 1.3.2 at
[`65c21b6`](https://github.com/Akshayykadam/PixelBoard/commit/65c21b61c4b41542417522530152895a828ab1c6).

- Added the upstream 18.5.3 Rambler selector descriptors and `oki.g` flag family.
  Exact signature checks and the semantic resolver remain mandatory; the new
  profile has not yet been confirmed on a physical device with Gboard 18.5.3.
- Followed the current Rambler policy: stop overriding four obsolete toolbar
  animation flags; set the deactivation delay to zero only while Rambler is enabled
  and the stock selection is not explicitly disabled.
- Adapted `GboardRamblerDictationRuntime`: restore verbatim speech only for
  COMPOSE intent 4. Preserve edit, history, rewrite and ramble intents. Install
  this optional hook only for the two complete upstream handler descriptors;
  unknown schemas are left unchanged. No dictated text is logged.
- Match upstream's stable Writing Tools defaults for cooperative mode and the
  selected-text helper: both are disabled. Keep the existing cloud backend and
  on-device/V2 policy. Account eligibility and consent are not overridden.
- Native Dictionary/Physical Keyboard preservation fixes do not need porting:
  this module does not remove or replace Gboard's native settings entries.
- Clipboard/emoji/resource-injection features of the modified upstream APK are
  not part of this Rambler/Writing Tools module update.

The module update checker is independent of PixelBoard's APK updater. It checks
only this repository's latest stable release after a user tap, compares numeric
versions, and opens the matching module APK or release page in the browser.
It does not check on launch, download in the background, or install packages.

Regression coverage includes COMPOSE vs editing intent preservation, typed delay
policy, disabled Writing Tools experimental gates, update metadata/version/URL
validation, and settings startup in light/dark themes.
