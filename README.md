# MONOLITH TV

**MONOLITH TV** is an independent, offline-first Android TV HOME launcher for remote-controlled devices. Initial validation target: **BOXY / HP4421-BOXY, Android TV 11, 2 GB RAM, Amlogic S905X4**.

> Status: **pre-alpha foundation**. Source, documentation, and CI scaffolding are provided. No claim of device-tested performance or production release.

## Principles

- Real D-pad navigation and visible focus from the TV viewing distance.
- Native installed app icons: **never redraw, tint, crop or replace them**.
- No advertisements, analytics, accounts, network access or unnecessary permissions.
- Preserve the stock launcher; selecting MONOLITH TV as HOME must be reversible.
- Minimal stable Android architecture, no dependencies on the phone MONOLITH app.

## First milestone

The initial source implements a TV app grid, favorite-first ordering, remote OK to launch, and remote MENU to toggle favorites. Features such as app hiding, settings, backup, banner artwork and onboarding are **planned, not shipped**.

## Build

GitHub Actions runs tests, lint and debug APK assembly on every push/PR. For local builds use **JDK 17, Gradle 8.13, Android SDK 36**:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The CI artifact is a **preview/debug APK**, with an ephemeral CI signing identity. Do not confuse it with a permanently signed update channel: different CI builds may require uninstalling the previous debug build. Never uninstall the stock TV launcher.

## Device smoke test

Connect Android TV to Wi-Fi ADB, download the debug APK from Actions, and run:

```bash
adb -s 192.168.0.160:5555 install -r app-debug.apk
adb -s 192.168.0.160:5555 shell am start -n dev.monolith.tv.preview/dev.monolith.tv.MainActivity
```

The address is the initial BOXY test environment, not a hardcoded app setting. **Do not set MONOLITH TV as the default HOME before passing the recovery checklist.**

Documentation: [Product](docs/PRODUCT.md) · [Architecture](docs/ARCHITECTURE.md) · [Design](docs/DESIGN.md) · [Roadmap](docs/ROADMAP.md) · [Device acceptance](docs/DEVICE_ACCEPTANCE.md) · [Recovery](docs/RECOVERY.md) · [Security](docs/SECURITY.md) · [Русский](README.ru.md).

The mobile project [MONOLITH](https://github.com/lolokeksu09/MONOLITH) is separate and unchanged.

