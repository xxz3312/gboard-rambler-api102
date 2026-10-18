# PixelBoard -> runtime LSPosed port notes

| Upstream patch | Runtime strategy | Status |
| --- | --- | --- |
| Rambler selector lifecycle instrumentation | Match complete versioned method descriptors; intercept and scope with finally | Implemented, disabled |
| Rambler selection read/write observer | Call original via chain.proceed(); diagnostics only | Implemented, disabled |
| Rambler capability flag gating | Scope-limited predicate adaptation | Not implemented |
| Default selection suppression | Scope-only observation; no behavioral override | Not implemented |
| Writing Tools flags and route injection | Requires data-flow understanding; no naive global true hooks | Not implemented |
| Spell checker compatibility gate | Exact return/argument method matching | Prototype, disabled |
| New manifest permissions/components | Cannot transparently add using normal Java hook | Not implemented |
| Resource / dex patching | Extra classloader does not rewrite original dex | Not implemented |

Exact obfuscated method mappings are specific to the source's Gboard variants. Always verify on an actual official Gboard dex; matching reflection signatures alone does not establish semantic equivalence.
