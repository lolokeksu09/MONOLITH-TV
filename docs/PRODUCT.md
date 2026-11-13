# Product specification · MONOLITH TV

## Purpose and scope

Replace the cluttered Android TV home experience with a fast, predictable, offline app launcher optimized for a physical D-pad. MONOLITH TV is a separate product, not a mobile MONOLITH port. The launch target is a BOXY HP4421-BOXY (Android 11, 2 GB RAM).

## Users and jobs

1. Launch VK Video, YouTube, Dune HD or another installed TV app in one or two decisions.
2. See where focus is without touching the screen.
3. Pin/unpin favorites with a remote; changes persist.
4. Access Android Settings and return to the stock HOME when necessary.
5. Use the device without creating an account or contacting external services.

## Baseline requirements

| ID | Priority | Requirement | Acceptance |
| --- | --- | --- | --- |
| TV-01 | P0 | D-pad navigation | All app tiles reachable via UP/DOWN/LEFT/RIGHT |
| TV-02 | P0 | Confirm to launch | DPAD_CENTER/ENTER opens the selected installed activity |
| TV-03 | P0 | Original icons | Render source Drawable with no artistic transforms |
| TV-04 | P0 | Stock HOME recovery | Can return to stock launcher without root or wiping device |
| TV-05 | P0 | Offline | Launch and list apps with networking unavailable |
| TV-06 | P0 | Package visibility | Enumerate visible TV launchable activities via explicit queries |
| TV-07 | P1 | Favorites | Toggle/persist shortcuts; original apps remain in All Apps |
| TV-08 | P1 | App package changes | Installed/removed apps refresh on returning HOME |
| TV-09 | P1 | Empty/error states | No blank screen on empty app list or failed launch |
| TV-10 | P1 | Bilingual UI | All user-facing text in English and Russian |
| TV-11 | P1 | Performance | BOXY metrics from repeatable warm-navigation script |
| TV-12 | P2 | Configurable wallpaper, accent, density | User opted-in; no heavy effects |
| TV-13 | P2 | Reorder, hide, folders | Separate reviewed scope and migration tests |
| TV-14 | P2 | Backup/restore preferences | Explicit export/import with schema version |

## Non-goals for initial releases

No streaming aggregation, recommendations, login, app store, custom VPN, device control service, icon packs, widget hosting, ad blocking or system modification.

## Quality and privacy

No Internet permission, telemetry, advertising or tracking. Preferences are stored locally. A production release requires a permanent signing key, documented upgrade path and successful BOXY acceptance.

## Definition of done

Implementation + tests + Lint + actual Android TV device verification. CI success is necessary but not sufficient.

