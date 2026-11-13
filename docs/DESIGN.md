# MONOLITH TV · AMOLED design system v2 (0.1.3 pre-alpha)

## Verified baseline

BOXY Android TV 11 (1920×1080 at 320dpi; ~960×540dp), private screenshot
`vitrum-tv-bridge/captures/issue-131.png` on 13 Nov 2026:
the 0.1.2 editorial rail occupies the left quarter of the TV and
the library is composed of identical solid gray rectangles.
It is functional but gives every app tile the same emphasis and wastes space.

## 0.1.3 screen architecture

The new screen is a substantial structure change, not just a recolor:

1. **AMOLED-native canvas** (`#000000`) behind the entire experience.
   No wallpaper download, blur, fake content or decorative gradients.
2. **Navigation rail** on the left: MONOLITH TV identity, focusable All,
   Favorites and Settings with real app counts, actual device time and
   concise remote hint. The current section uses a thin cyan indicator.
3. **Selection hero** on the right, above the grid. It displays the actual
   focused app's title and *original Android Drawable*; navigating the grid
   changes the hero without launching an app. Empty Favorites has a
   contextual hero instead of a random previous app.
4. **App gallery**: four columns on the reference BOXY layout, three on
   narrower viewports; small unobtrusive resting cards and prominent
   D-pad focus (140ms scale 1.035, dark-cyan elevation surface,
   2dp light-cyan keyline). No continuous animation, no expensive blur.
5. **Settings panel**: actual persistent Compact Grid/Favorites First
   values in accessible focused rows, a Done action and direct Android
   Settings route. Vertical scrolling remains available.

## Palette / typography

| Token | Hex |
| --- | --- |
| Background | `#000000` |
| Resting tile | `#0B0F13` |
| Selection hero | `#10161B` |
| Focused tile | `#11212A` |
| Primary text | `#F5FAFC` |
| Secondary text | `#B2C0C7` |
| Small metadata | `#84949C` |
| Focus / accent | `#B7F3FF` |

Typography deliberately uses the platform TV sans font and distinct
hierarchy: compact navigation (13–15sp), apps (13–14sp), library heading
(18–20sp), hero title (24–33sp), clock (30–40sp).
Important focus state is indicated by outline and surface change
in addition to cyan, never color alone.

## Geometry

Pure Kotlin `TvLayoutSpec.forViewport(widthDp, heightDp, compactGrid)`
determines columns, rail, margins, hero height and tile sizes.

- Reference 960×540dp: rail 166dp; four columns; hero 122dp;
  96dp compact cards, 10dp gaps. Three rows plus grid padding fit
  the viewport with the 22dp vertical safe area.
- 640×360dp: rail 138dp, three columns, hero 96dp, 88dp compact
  cards; grid scrolls when rows exceed remaining height.
- Comfortable mode increases tile height and scrolls naturally;
  app icon artwork remains unchanged.
- Settings are always vertically scrollable.

## Preserved invariants

- Original application `Drawable` is displayed via Android `ImageView`
  `FIT_CENTER`. No tint, icon packs, forced masks or replacements.
- Stable application component keys and focus restoration after MENU toggle.
- OFFLINE app enumeration, launch, Favorites, compact option and Back.
- No changes to app permissions, production application ID, stock HOME,
  system services, signing identity or installed app metadata.
- English/Russian strings for every new UI element.

## Dependency decision

- Compose BOM **2026.09.00**: latest confirmed stable BOM.
- Activity Compose **1.13.0** and Lifecycle KTX **2.11.0**: stable.
- TV Material **1.1.0** already matches current stable release, so it
  is kept instead of inserting an imaginary newer version.
- Compose animation is explicitly declared for focus interactions.
- Compose 1.12.1 + Lifecycle 2.11 requires API 37 and AGP 9.1+.
  The project is migrated to **AGP 9.3.0**, Gradle **9.5.0** and
  `compileSdk = 37` with built-in Kotlin. Production target/min SDK
  and the application's Android permissions are unchanged.
- The Compose BOM does not manage Kotlin or AGP versions.

## Verification gates

1. JVM tests (`TvLayoutSpecTest` and previous ordering tests), lint, APK.
2. Verify source revision and debug APK signature/version.
3. Install the exact APK on BOXY, retaining stock HOME and app data.
4. Capture 1080p All, Favorites empty, one favorite, Settings; check
   icon originals and visible D-pad focus while moving.
5. Test MENU favorite, Back, compact setting persistence and application
   launch/return; check memory/frame pacing before claiming optimization.
6. 720p must be tested on device or marked NOT RUN.

CI success does not prove TV rendering. Prior 0.1.2 screenshots are only
the baseline for comparison, never 0.1.3 acceptance.
