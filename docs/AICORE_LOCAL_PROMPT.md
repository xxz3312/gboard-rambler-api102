# AICore local Prompt path (0.1.20)

This is a real foreground ML Kit Prompt API call from the module UI, not a
replacement for Gboard's private feature 703. It does not change Gboard's
backend selection, the original Rambler hooks, device identity, feature
declarations, model labels, permissions, or attestation. There is no Astrea or
other cloud inference fallback in this path.

The generic foundation model may have different availability from the public
rewriting/proofreading adapters (607/614) and Gboard's private feature (703).
Therefore an unavailable adapter alone is not proof that generic generation is
unavailable. Conversely, this screen working does not establish Gboard V2 works.

## Implementation

- `genai-prompt:1.0.0-beta4`, using the documented Java Futures wrapper.
- Explicit status/name query; model-name failure is diagnostic and does not
  overwrite the feature's original status. Status timeout 30 seconds, name 15.
- An explicit download button invokes AICore download only for DOWNLOADABLE or
  DOWNLOADING, shows progress, waits at most 15 minutes, and rechecks status.
- Proofreading and formal rewrite build a short English instruction and invoke
  `generateContent` only after AVAILABLE. Input is capped at 1000 characters,
  output at 256 tokens, generation wait at 120 seconds.
- AVAILABLE is not inference success. Only a completed call with a non-empty
  candidate produces INFERENCE_RETURNED_TEXT. Finish reason is shown without
  asserting output quality or completeness.
- Futures are observed without a blocking main-thread get. Operation identity
  and pending-future identity reject late completions. Cancel/timeout/stop
  invalidate the operation. On stop or failure the model is closed; stop also
  cancels the current future. Cancelling a wait does not guarantee cancellation
  of AICore's system-managed download.
- Input/output are not persisted, broadcast, logged, or included in the
  diagnostic copy. The activity is not exported and does not accept keyboard
  data from another process. SDK error messages remain visible for diagnosis.

## Device acceptance

1. Install the module and open **AICore 端侧执行 · 基础模型**. No module root
   grant or Gboard restart is needed for this independent foreground activity.
   Disable the earlier Pixel identity experiment for the separate Gboard tests.
2. Query. Record AICore version, actual model name, original feature status and
   hardware declarations. Missing SM8850 declaration is reported, not forged.
3. If DOWNLOADABLE, explicitly request download and keep this page foreground.
   New installations may need time online for AICore configuration first.
4. Execute proofreading and formal rewrite with short English text. Check the
   returned text itself. A deterministic expected answer is not substituted.
5. After download/preparation, disconnect Wi-Fi and mobile data and repeat to
   validate offline execution. This build has not been tested on the user's
   hardware; SDK success and offline acceptance still require this step.

If generic generation remains unavailable, inspect configuration/download and
runtime compatibility before attempting a Gboard bridge. Google explicitly does
not support unlocked bootloaders; this implementation does not change that.
Do not equate installing another APK with successfully obtaining model assets.

References:
- https://developers.google.com/ml-kit/genai/prompt/android/get-started
- https://developers.google.com/android/reference/com/google/mlkit/genai/prompt/java/GenerativeModelFutures
- https://developers.google.com/ml-kit/genai
