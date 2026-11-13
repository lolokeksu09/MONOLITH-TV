# MONOLITH TV

**MONOLITH TV** is an independent, offline-first Android TV HOME launcher for remote-controlled devices. Initial validation target: **BOXY / HP4421-BOXY, Android TV 11, 2 GB RAM, Amlogic S905X4**.

> Status: **0.1.3 pre-alpha preview**. The installed 0.1.2 screen was captured on BOXY, but the new 0.1.3 AMOLED layout requires a new APK installation and hardware acceptance.

## Principles

- Real D-pad navigation and visible focus from the TV viewing distance.
- Native installed app icons: **never redraw, tint, crop or replace them**.
- No advertisements, analytics, accounts, network access or unnecessary permissions.
- Preserve the stock launcher; selecting MONOLITH TV as HOME must be reversible.
- Minimal stable Android architecture, no dependencies on the phone MONOLITH app.

## Current preview features

- True-black AMOLED interface with a left D-pad navigation rail, selection hero, animated focus and untouched original Android app icons.
- Four-column 1080p grid, three-column narrow layout, and scrollable Settings on short-height screens.
- **All / Favorites** navigation; use remote **MENU** on a focused app to pin/unpin.
- Launcher settings: compact/comfortable grid, alphabetical or favorites-first ordering.
- Local persistence of both favorites and view settings (survives normal updates using the same signing key).
- Android system Settings available from the in-app settings page; Back returns to apps.
- Catalog refresh on resume, protection from stale loads, bilingual Russian/English text.

Planned, not shipped: app hiding, backup, permanently signed releases and proven remote performance on the new revision.

## Build

GitHub Actions runs tests, lint and debug APK assembly on every push/PR. For local builds use **JDK 17, Gradle 9.5, Android SDK 37, AGP 9.3**:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The CI artifact is a **preview/debug APK**, with an ephemeral CI signing identity. Do not confuse it with a permanently signed update channel: different CI builds may require uninstalling the previous debug build. **Uninstalling deletes local favorites and preferences**. Never uninstall the stock TV launcher.

## Safe parallel device review

GitHub Actions ephemeral debug signatures can prevent `adb install -r` from
updating the existing `dev.monolith.tv.preview` package. The bridge attempted
0.1.3 on BOXY (private bridge issue #132) and ADB returned exit code 1;
the existing 0.1.2 preview remained intact (verified by `app_info` in issue #133).

To validate AMOLED without deleting that installation, CI now also builds a
**parallel** review build type: `dev.monolith.tv.review`. The bridge's pinned
`MONOLITH-TV-preview-debug` artifact contains that review APK under the
historical filename `app-debug.apk`; the normal `dev.monolith.tv.preview`
APK is still available separately as `MONOLITH-TV-standard-debug`.
This is a temporary hardware-acceptance path, not a stable signing strategy.
Never uninstall or clear existing preview data to install the review variant.

## Device smoke test

Connect Android TV to Wi-Fi ADB, download the debug APK from Actions, and run:

```bash
adb -s 192.168.0.160:5555 install -r app-debug.apk # requires a matching APK signature
adb -s 192.168.0.160:5555 shell am start -n dev.monolith.tv.preview/dev.monolith.tv.MainActivity
```

The address is the initial BOXY test environment, not a hardcoded app setting. **Do not set MONOLITH TV as the default HOME before passing the recovery checklist.**

Documentation: [Product](docs/PRODUCT.md) · [Architecture](docs/ARCHITECTURE.md) · [Design](docs/DESIGN.md) · [Roadmap](docs/ROADMAP.md) · [Device acceptance](docs/DEVICE_ACCEPTANCE.md) · [Recovery](docs/RECOVERY.md) · [Security](docs/SECURITY.md) · [Русский](README.ru.md).

The mobile project [MONOLITH](https://github.com/lolokeksu09/MONOLITH) is separate and unchanged.

