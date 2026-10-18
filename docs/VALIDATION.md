# Source validation checklist (not yet executed)

Source code follows the Modern libxposed API 102 hook chain convention:
`hook(method).intercept(chain -> { Object result = chain.proceed(); return result; })`.

## Before any APK build

- Verify `XposedModule` constructor expectations and `PackageReadyParam` API using official 102.0.0 AAR.
- Confirm app packaging preserves `META-INF/xposed/java_init.list`, `module.prop`, `scope.list`.
- Run a **manual** debug compilation and unit tests only when explicitly requested.
- Verify actual Gboard obfuscated signatures from the exact target APK, not just upstream source.
- Test `enable_jetson=false` opt-out even while voice settings scope is active.
- Test getter original-value pass-through for unrecognized flags and missing context.
- Do not enable feature toggles or publish a release before Android 17 runtime verification.

## Safety invariants

- Unknown/unsupported Gboard build => no hooks.
- No Context => no flag mutations.
- Explicit user opt-out => no flag mutations.
- Non-allowlisted flag or incompatible boxed return type => preserve original.
- Failures resolving a target => log and skip; do not try guesses or global hook points.
