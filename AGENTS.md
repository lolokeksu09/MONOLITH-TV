# MONOLITH TV engineering rules

- Repository: `lolokeksu09/MONOLITH-TV`. Production application ID: `dev.monolith.tv`; preview uses `dev.monolith.tv.preview`. Never reuse `dev.monolith.launcher`, the mobile MONOLITH signing certificate, or mobile data keys.
- TV first: remote D-pad, OK, Back, focus visibility, accessibility, 10-foot readability. No touchscreen dependency.
- Installed app icons must be drawn using the Android-provided original Drawable. No transforms, tint, masks, forced backgrounds, saturation changes, generated art or icon replacements. Focus decoration belongs to the surrounding tile, not to the artwork.
- Stock launcher must remain installed and reachable. Never automatically set HOME, disable stock launcher, alter SystemUI, require root, or issue irreversible ADB operations.
- The app must launch without internet, accounts, analytics, ads, VPN services, overlay permission, accessibility service permission, or QUERY_ALL_PACKAGES unless a narrowly reviewed requirement justifies a permission change.
- Never claim that CI, emulator, or screenshot tests validate hardware. Hardware: BOXY HP4421-BOXY Android TV 11, Amlogic S905X4, 2 GB RAM; see docs/DEVICE_ACCEPTANCE.md.
- Prefer one :app module until a measured modularization need arises. Pure domain code is testable without Android.
- Keep English primary documentation and Russian translations for user-visible resources. Preserve versioned settings across upgrades.
- Changes that affect HOME/visibility/app launching/focus require tests and a documented recovery route.
- Do not commit signing keys, credentials, debug binaries, proprietary artwork, logs containing network secrets, or other personal data.
- Android CI must run testDebugUnitTest, lintDebug and assembleDebug. Release builds are blocked until stable signing and device acceptance are set up.

