# MONOLITH TV 0.1.4 — Cinema redesign

## Why 0.1.3 was rejected

BOXY hardware captures `captures/issue-175.png`, `issue-177.png`,
`issue-179.png` in the private `vitrum-tv-bridge` show
0.1.3 displaying a permanent left rail plus 11 apps as three identical
rows of charcoal rectangles. A tinted hero over that grid did not
meaningfully change its visual rhythm. The user rejected that result.

## Actual change to the interface

- **Old:** side navigation occupying the left strip, narrow selection
  banner in the top right, 4 × 3 multi-row grid of similarly sized
  opaque rectangles
- **New:** compact *top* navigation; full-viewport-width cinematic
  selection stage with 47sp primary app title and 148dp original app
  icon on the right; single horizontally scrolling dock of six-ish
  floating, icon-led items, expanding focus on the selected icon
- **Navigation:** LEFT/RIGHT scrolls along the dock and updates the stage;
  UP from any dock item goes to its current filter tab; DOWN from a tab
  returns to the selected dock item. MENU still pins/unpins. Favorites
  and Settings remain explicit, no invented content or web wallpaper.
- **Intentional surface:** pure #000000 OLED backdrop; gradient is confined
  to a large feature stage as a visual depth cue, not applied to the entire
  canvas. Focused item has #D5DCFF keyline, purple surface, 1.065 scale,
  165ms feedback. Unfocused dock items are genuinely transparent, not
  11 persistent black rectangles.
- **Strong type:** clear 47sp hero title on reference 1080p TV, 20sp
  section label, 14sp dock titles; 720p short stage uses 28sp hero.
- **Source art:** original installed Android `Drawable` displayed
  with `ImageView FIT_CENTER` without icon packs, recoloring, masks,
  crops, fake reflections or redesigned package icons.

## Device geometry

Reference BOXY screenshot: 1920 × 1080 px, approx. 960 × 540dp.
`TvLayoutSpec` determines safe edges / top bar / hero / dock.
At 960 × 540dp: horizontal inset 28dp, vertical 18dp, top bar 46dp,
feature stage 238dp, dock 150dp, each item 130dp wide in compact mode.
The layout uses 525dp total including its gaps/insets.
At 640 × 360dp: 17/12dp safe edges, 42dp top bar, 126dp
stage and 100dp dock. No vertical grid cropping: the horizontal
dock scrolls and preview metadata remains visible.

## Immutable behavior

- `dev.monolith.tv.preview` 0.1.2 and `dev.monolith.tv.review` 0.1.3
  must remain installed with separate preferences.
- 0.1.4 hardware preview uses a *new* `dev.monolith.tv.cinema` package
  because ephemeral GitHub debug signing certificates may differ.
- Stable keys: original `ComponentName.flattenToString()`; favorites
  continue using `FavoritesStore`, options continue using
  `LauncherOptionsStore`, launch uses original Activity component.
- Stock Google TV HOME not changed. No package uninstall, data wipe,
  shell command execution, new Android permissions or VPN changes.

## Verification gates

- [ ] Exact revision passes Gradle JVM tests, Android Lint and
      `assembleCinema` in GitHub Actions
- [ ] Verify APK SHA-256, successful main-branch workflow, install
      as `dev.monolith.tv.cinema` through allowlisted private bridge
- [ ] Compare 1920×1080 screenshots to 0.1.3: no left rail and no multi-row
      tiles, selected app stage has original icon
- [ ] Single RIGHT and LEFT change the focused item and stage predictably
- [ ] MENU pin/unpin retains focus even when favorites sorting changes
- [ ] Top tab All/Favorites/Settings and Settings BACK round-trip
- [ ] 720p screenshot if BOXY can be configured safely (otherwise NOT RUN)
- [ ] Same-certificate upgrade, process restart and long-run frame time
      are NOT implied by any one preview screenshot or CI success
