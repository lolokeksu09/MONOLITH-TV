# MONOLITH TV · Visual system v1

## Direction: editorial gallery, not a tile dashboard

The 0.1.1 preview had an oversized top toolbar, equal emphasis on every outline,
and a large unused region below the app grid on 1920×1080 BOXY screenshots.
The redesigned screen uses a deliberate two-part composition:

- **Left information rail:** MONOLITH TV identity, actual device time, real
  app/favorite counts, and the currently focused application's own icon/title.
  This is a functional selection preview, not an advertising banner.
- **Right app gallery:** quiet four-column app grid on the reference BOXY
  viewport, larger app tiles, clean text alignment and no permanent outlines.
  On narrower layouts it switches to three columns and a narrower rail.
- **Top-level navigation:** All, Favorites and Settings remain explicit
  remote-focusable controls. Focus has a bright neutral keyline plus a surface
  change; active-tab state is deliberately more subtle.
- **Settings:** identical visual language and selection rail, three legible
  focusable preference rows with their real persistent values, and a single
  Done action.

## Tokens

| Role | Value |
| --- | --- |
| Canvas | `#090B0E` |
| Left rail | `#14171C` |
| App / preference surface | `#1A1E24` |
| Focused surface | `#303741` |
| Primary text | `#F4F5F7` |
| Secondary text | `#B0B9C4` |
| Focus keyline / subtle accent | `#E1EAF4` |

Typography uses the platform sans serif, with one clear hierarchy: large
selected app name, medium grid labels, small helpful metadata. Avoid
gratuitous tracking, all-caps eyebrow labels, texture, glow and gradients.

## TV layout targets

- Reference BOXY: 1920×1080, Android 11, density 320 (logical 960×540dp).
- 28dp horizontal and 24dp vertical outer margin on the reference class.
- Rail width 214dp; separation 24dp; right content uses four columns,
  11dp gaps and 116dp compact / 128dp comfortable card heights.
- Width under 800dp: 176dp rail, 20dp outer margin, 16dp separation and
  three columns; height under 510dp uses shorter app cards, a compact rail
  (reduced time typography/preview icon/metadata) and 18dp margins.
- Settings content scrolls vertically so all three preferences and Android
  Settings remain reachable by D-pad on short-height devices.
- The app grid scrolls independently. No new manual scrolling gesture or
  touch requirement is introduced.
- Preference rows and navigation controls remain D-pad focusable and readable
  from a television viewing distance.

## Immutable product rules

1. Installed application artwork comes directly from the actual Android
   `Drawable`. No redraw, icon pack, backdrop tile, tint, clip or generated
   logo is permitted. The rail uses the same source icon as the selected tile.
2. Keep All / Favorites, MENU toggling, OK launching, Settings and Back,
   persistence, catalog loading/error handling and package visibility.
3. Do not change package IDs, signing, HOME selection, permissions, Android
   services, stock launcher settings, or unrelated mobile MONOLITH code.
4. No ads, invented recommendations, accounts, network calls or metrics.
5. Remote-focused elements require visible focus beyond a color change.

## Acceptance gates

- **CI:** JVM unit tests, Android lint and debug APK assembly pass on the
  exact redesigned revision.
- **Hardware (pending until new APK is installed):** repeat 1080p screenshots
  with All, Favorites-empty, one favorite, Settings and both grid densities;
  verify each D-pad direction, MENU focus retention, selected-app preview,
  Back restoration and no clipped text or icons.
- **Limit:** a successful CI build or prior 0.1.1 hardware test does not
  constitute a pass on the new visual revision. The GitHub Actions debug APK
  has an ephemeral certificate and is not a supported in-place update channel.

## 0.1.2 layout review

Source inspection identified a short-height case that CI compilation could not
catch visually: the original left rail stacked large time, selected-app icon,
metadata and hints with `SpaceBetween`, which could exceed a 720p screen's
logical height. Additionally, Settings used a non-scrollable Column with three
98dp rows plus a heading. Version 0.1.2 compacts the rail for screens below
510dp high and gives Settings a vertical scroll container plus 82dp rows.

**Evidence:** source-level change and Gradle CI only. 720p screenshot/D-pad
validation and 1080p comparison remain required on a running APK.
