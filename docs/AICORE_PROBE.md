# AICore probe 0.1.2
User-triggered module-app activity queries ML Kit checkFeatureStatus for:
- English PROFESSIONAL rewriting
- English KEYBOARD proofreading

This uses genai-rewriting and genai-proofreading 1.0.0-beta1.
No downloadFeature or runInference is called. No user text is collected.
Each request has a 20-second timeout. Leaving the activity interrupts detection;
pending futures are cancelled and clients closed. Package visibility is limited
to com.google.android.aicore. This probes module-app public API access, not
Gboard's private APIs or Astrea. AVAILABLE is not proof of successful inference.

Existing cloud and Rambler hooks/policies are unchanged.
Upstream user feedback: basic cloud writing tools appeared and actual usage
succeeded on 2026-10-18; Rambler regression was not separately reconfirmed.

References:
https://developers.google.com/ml-kit/genai/rewriting/android
https://developers.google.com/ml-kit/genai/proofreading/android
Official docs state unlocked bootloaders are unsupported. Root or a hidden
unlock indicator is not evidence of model/feature readiness.
