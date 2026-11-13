# Release and verification protocol

1. PR includes user-visible changes, risk, regression plan, and screenshots/video for UI changes.
2. CI must run `testDebugUnitTest`, `lintDebug` and `assembleDebug`.
3. Preview artifact is for testing, not stable distribution; signature is runner-generated.
4. On BOXY verify D-pad, focus, HOME fallback, installed-app discovery, localized labels, and repeatable gfxinfo measurements.
5. Before first public signed build: provision dedicated TV keystore outside the repo; pin public SHA-256 signing certificate; verify a second in-place upgrade using the same key; only then publish a release APK.
6. Document change log by version with Added/Improved/Fixed, accurate device-acceptance status, and recovery steps.
7. No automatic production release on a push; keep release a deliberate reviewed action.

