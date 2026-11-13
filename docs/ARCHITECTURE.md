# Architecture · ADR 001

**Decision:** Kotlin + Jetpack Compose for TV, one Android :app module, simple state ownership. Do not split modules without measured need.

## Dependency direction

- `MainActivity`: Android lifecycle, activity launch and navigation callbacks.
- `catalog/AppCatalog`: PackageManager discovery, safe explicit launch targets.
- `catalog/AppOrder`: pure favorites filter and deterministic sorting; `LauncherOptions` is a pure state model.
- `storage/FavoritesStore`: local SharedPreferences for favorites. `LauncherOptionsStore`: versioned density/sort booleans in the same file, without rewriting favorites.
- `ui/LauncherScreen`: launcher grid, All/Favorites navigation and local settings pane; tracks D-pad focus and restores focus after favorites reorder, without direct app discovery.
- Android platform is the source for app names and original icons.

## Packages

```
dev.monolith.tv
├── MainActivity.kt
├── catalog/
│   ├── AppCatalog.kt
│   └── AppOrder.kt (includes LauncherOptions)
├── storage/
│   ├── FavoritesStore.kt
│   └── LauncherOptionsStore.kt
└── ui/LauncherScreen.kt
```

## Launch and package visibility

- Manifest advertises HOME + DEFAULT and LEANBACK_LAUNCHER, without silently selecting HOME.
- `<queries>` declares MAIN/LEANBACK_LAUNCHER and MAIN/LAUNCHER for Android 11+ visibility.
- Deduplicate launch activities by package (prefer Leanback candidate); handle removed/disabled target and SecurityException.
- Do not request QUERY_ALL_PACKAGES or use undocumented device-specific shortcuts.
- Async enumeration with stale job cancellation; refresh on resume; user-selected favorite-first or alphabetical sorting.

## Lifecycle and persistence

- MainActivity owns UI-visible state for the pre-alpha MVP.
- Never block the UI thread with PackageManager queries.
- Favorites are a set of package names under a namespaced SharedPreferences file. Compact grid and favorites-first settings are separately stored under `compact_grid.v1` and `favorites_first.v1`.
- App removal must not crash even if an older favorite remains in preferences.
- After first milestone, move state to a ViewModel/StateFlow if complexity warrants it. Do not introduce DI or a database before they solve a real problem.

## Architecture risks

1. Firmware HOME resolver may handle custom launchers differently. Validate on BOXY, keep stock installed.
2. DPAD focus behavior depends on framework and device. Real remote acceptance mandatory.
3. Android package visibility excludes non-launchable packages by design.
4. Preview debug signatures can change between CI runs: no guarantee of in-place upgrades.

### Decision record

Approved initial stack: AGP 8.13.2; Kotlin/Compose compiler 2.3.0; Gradle 8.13; Compose BOM 2025.08.00; Compose TV Material 1.1.0; Android compile SDK 36, target SDK 35, minimum SDK 26. Versions are pinned to avoid Compose 1.12.x requiring AGP 9.1/SDK 37; CI must verify resolved dependency compatibility.

