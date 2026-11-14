# BOXY device acceptance

## Test target

BOXY / HP4421-BOXY, Android TV 11, Amlogic S905X4, 2 GB RAM. Initial ADB address `192.168.0.160:5555` on `TV-HOME` 5 GHz; DHCP can change this address.

## Recorded pre-project stock baseline (not MONOLITH TV)

Google TV launcher `com.google.android.tvlauncher` after gfxinfo reset and manual navigation: 576 frames, 20 janky (3.47%), p90 9 ms, p95 13 ms, p99 28 ms. Earlier unreset sample: 245 frames, 37 janky (15.10%). Treat these as differing sessions, not proof of optimization.

Audio firmware `DOLBY_MS12` uses about 20–24% of one core idle. Do not claim a launcher can fix this vendor audio-driver behavior.

## Manual release checklist

- [ ] Preview APK installed without affecting stock HOME
- [ ] All tiles reachable by physical DPAD, with visible focus
- [ ] Center/Enter launch VK Video, YouTube, Dune HD, Settings
- [ ] Back behavior consistent with HOME routing
- [ ] Favorite toggle persists after process restart and power cycle
- [ ] No incorrect icons; all source icons unchanged
- [ ] No network required to list/launch apps
- [ ] HDMI audio and remote buttons work
- [ ] OEM/Google TV remains available and functional
- [ ] Evaluate gfxinfo after resetting counters, matching same navigation duration
- [ ] Compare startup and available RAM in repeatable sessions
- [ ] App recovers after target is uninstalled/disabled
- [ ] Confirm Android 11 package visibility on real hardware

## Commands (Termux or PC ADB)

```bash
adb -s 192.168.0.160:5555 shell am start -n dev.monolith.tv.preview/dev.monolith.tv.MainActivity
adb -s 192.168.0.160:5555 shell dumpsys gfxinfo dev.monolith.tv.preview reset
# Navigate for one minute, then:
adb -s 192.168.0.160:5555 shell dumpsys gfxinfo dev.monolith.tv.preview
```

Save all long command output under `/storage/emulated/0/Download` on the phone. Never store Wi-Fi passwords or personal tokens in committed logs.


## BOXY visual audit · 13 November 2026

Source: `BOXY_MONOLITH_AUDIT_20261113_085630.tar.gz` from the physical BOXY, Android 11, 1920×1080, density 320.

- First screenshot is **Google Backdrop**, not our launcher. The Dream was still displayed above the started activity; the audit script must wake/dismiss the Dream and verify foreground before capturing.
- Seven later screenshots show MONOLITH TV with visible D-pad focus. Tile height 146dp plus spacing exceeds the 540dp-tall BOXY layout when displaying three rows; top app icons and lower labels become clipped on scrolling.
- Two RIGHT presses at the rightmost Vitrum TV cell yielded pixel-identical screenshots. This is compatible with a right boundary; without focus-tree data it is *not* a confirmed focus bug.
- gfxinfo after navigation sampled just 13 rendered frames, 7 janky (53.85%), p90 85ms. Do not use this to claim steady-state performance or compare with stock Google TV.
- Fix: tile height 118dp, original app icon presentation scaled to 52dp without bitmap processing, 8dp row gap and 6dp bottom padding. Expected to show three full rows on BOXY; **must be verified with a new device capture**.

## 0.1.1-prealpha new device acceptance (not yet completed)

- [ ] On 1080p/320dpi, check that compact and comfortable app rows are never clipped
- [ ] Confirm All / Favorites filter is navigable via D-pad and empty favorites screen has focus
- [ ] MENU on an app adds/removes it without jumping focus to the wrong component
- [ ] With Favorites filter selected, removing the last favorite leaves a usable empty state
- [ ] Settings opens with focus on Done; Back returns to the previously selected app
- [ ] Changing compact grid and favorites-first persists after process restart
- [ ] Android Settings opens from within launcher settings and returning does not reset choices
- [ ] Original app Drawables are still untouched in both grid densities
- [ ] Compare gfxinfo after a meaningful fixed navigation sequence, not a 13-frame sample

The preview APK certificate changes between fresh GitHub Actions runners; do not assume
`adb install -r` is a supported upgrade path. Uninstalling the old preview deletes its
local favorite and layout preferences. Never uninstall the stock Google TV launcher.


## 0.1.1 live BOXY observation · 13 November 2026, 20:33–20:34 MSK

These observations concern the physically installed `dev.monolith.tv.preview` version
`0.1.1-prealpha-preview` (versionCode 2), not merely the CI artifact.

- **PASS (observed):** launched the preview through the local Termux/ADB bridge.
  A real 1920×1080 screenshot shows 11 app tiles on three complete visible rows,
  without the earlier cropped first-row icons or last-row labels.
- **PASS (observed):** the white D-pad focus outline was clearly visible on a
  grid tile; captures showed focus on BrowseHere and later DUNE.
- **NOT VERIFIED:** whether one RIGHT reliably advances exactly one cell.
  The private bridge produced duplicate result comments for a single command,
  indicating concurrent worker execution or an equivalent race. Screenshots
  therefore cannot currently be attributed reliably to one key event.
- **Bridge fix prepared:** a single-agent kernel lock and Android 11 focus-status
  fallback were merged in `vitrum-tv-bridge` PR #85. The running Termux worker
  must be stopped, updated and restarted before trustworthy repeat testing.
- **NOT RUN on this controlled revision:** complete Settings / Favorites / Back
  interaction paths, process-restart persistence, repeatable gfxinfo, power-cycle
  behavior, same-certificate update, and stock-HOME rollback.

The existing `0.1.1-prealpha` acceptance checklist above remains **open**;
a successful screenshot and CI do not establish release acceptance. Do not
replace the stock launcher yet.


## 0.1.1 controlled BOXY remote-key replay · 13 November 2026, 20:53–21:01 MSK

Target: installed `dev.monolith.tv.preview` `0.1.1-prealpha-preview`
(versionCode 2), Android TV 11 on the same physical BOXY. The private
`vitrum-tv-bridge` agent was restarted after merging its single-worker
fix (commit `87de6b8`), and keys were injected one at a time by ADB.
This is **real-device UI evidence**, but **not a physical-remote test**.

- **PASS — single worker:** GitHub bridge request #86 produced one result
  comment. Before repair, comparable requests sometimes had two results.
  The repaired focus diagnostics reported Google Backdrop initially
  (correctly identifying the obscuring Dream), and after BACK + relaunch
  reported `dev.monolith.tv.preview/dev.monolith.tv.MainActivity`
  (bridge request #92).
- **PASS — one-cell navigation:** screenshot #93 showed focus on BrowseHere.
  One `RIGHT` request #94 followed by screenshot #95 showed focus on
  `Cx Проводник`, the adjacent tile: no double advancement.
- **PASS — favorite add and remove in All:** while `Cx Проводник` was
  selected, one `MENU` made the favorites counter 1, moved that app
  first and retained focus on it (screenshot #97). A second `MENU`
  restored counter 0 and alphabetical grid ordering without losing focus
  (screenshot #99). This did not test removal of the last favorite **while
  viewing the Favorites filter**.
- **PASS — empty Favorites screen:** `UP` focused All (screenshot #101);
  `RIGHT` + `OK` opened the Favorites filter with 0 items and a visible
  focused Favorites header tile (screenshot #104). It remained navigable.
- **PASS — settings entry and Back:** `RIGHT` + `OK` opened settings
  with a visible focus outline on Done (screenshot #107).
  A subsequent `BACK` returned to the previously selected Favorites
  filter (screenshot #114), rather than exiting to another app.
- **PASS — compact preference toggle and restore:** `DOWN` + `OK`
  changed the focused Compact Grid preference from Enabled to Disabled
  (screenshot #110). Another `OK` changed it back to Enabled
  (screenshot #112). The previous setting was restored before leaving.
  `Favorites First` remained Enabled, unchanged.
- **PASS — return to grid:** `LEFT` + `OK` returned to All with 11
  visible apps and `Cx Проводник` focused (screenshot #117).
- **PASS — basic layout:** in the captured default compact grid all three
  rows of 11 apps had complete tiles, icons and text. This does **not**
  prove unclipped layout under the comfortable setting, other DPI or
  other app counts.

The screenshots are in **private bridge repository history** under
`captures/issue-NNN.png` for the issue numbers above; they are intentionally
not copied into the public MONOLITH-TV repository.

**Still NOT RUN:** physical remote buttons; launching every target app and
returning; process restart, power cycle and persistence of preferences;
Favorites last-item removal while the Favorites filter is active; Android
system Settings round-trip; 1080p comfortable-grid overflow; repeatable
`gfxinfo`/RAM benchmarking; same-certificate APK update; default-HOME
selection and rollback. Do not declare hardware acceptance complete and
do not replace the stock launcher.

## 0.1.2 pre-alpha device status · 13 November 2026

The redesigned source was reviewed without access to an installed 0.1.2
revision. Evidence and limitations are recorded explicitly:

- **CI on source**: check Android TV CI for the exact 0.1.2 head SHA.
  Compilation, lint and JVM tests are *not* evidence that the UI renders
  correctly on BOXY.
- **Connected BOXY diagnostic**: private TV bridge Issues #118–#120 returned
  responses. `app_info` did not find a parseable MONOLITH preview version,
  `pm list packages -3` listed no `dev.monolith.tv.preview`, and
  `focus_status` reported the stock `com.google.android.tvlauncher` Activity.
  This strongly suggests that MONOLITH preview is no longer installed,
  but the package state should be verified again after installing.
- **Installation gate**: the running bridge process supports ADB diagnostics,
  keys and screenshots but does *not* load the newly added
  `install_monolith_ci` operation until it is locally updated and restarted
  with explicit `--allow-installs`. The GitHub repository update alone does not
  modify the running Termux process or grant installation permission.
- **Safety**: no `adb uninstall`, application data clear, stock launcher
  disable, change of default HOME, firmware change or remote arbitrary shell
  was performed.
- **Source correction**: on short-height displays the left rail uses
  compact spacing/icon/typography and Settings becomes scrollable; this
  addresses a layout risk identified from code, not a visually reproduced
  720p defect.

**NOT RUN on version 0.1.2**: installation, launch, new UI screenshots,
D-pad focus traversal, Favorites/Settings flows, 720p / 1080p visual
comparisons, performance, memory, HDMI remote test, cold boot and power-cycle
persistence, and a verified debug-certificate update.

**Release status: NOT DEVICE ACCEPTED.** Keep Google TV as the HOME
until the new APK is physically installed and the device acceptance
checklist is repeated on the exact revision.

## 0.1.3 AMOLED revision acceptance

Source: `feat/amoled-tv-013` — versionCode 4, `0.1.3-prealpha-preview`.
Baseline: actual 0.1.2 BOXY screenshot from private TV bridge issue #131,
captured 13 November 2026. The screenshot shows all 11 installed apps
and confirms the old design's large graphite side panel and 4-column grid.

**New source changes:** true-black AMOLED, separate rail for All/Favorites/
Settings, context-aware selected-app hero with original installed icon,
TV focus scale/elevation/keyline animation, adaptive columns, focus-retaining
favorite toggling, updated settings and refreshed AndroidX build toolchain.

**Hardware acceptance is PENDING on the exact 0.1.3 debug APK.**
Prior screen recordings and screenshots DO NOT validate the new UI.

- [ ] CI testDebugUnitTest, lintDebug and assembleDebug PASS for final revision
- [ ] Install the exact verified APK without uninstalling the old preview
- [ ] Verify installed versionName/versionCode through ADB
- [ ] New 1080p screenshot: All apps, hero, original icons and visible focus
- [ ] Press RIGHT once: focus moves exactly one app and hero updates
- [ ] MENU favorite add/remove: no focus jump, correct count and hero
- [ ] Favorites empty state: section remains reachable from the D-pad
- [ ] Settings: focus, compact mode toggle, restore and BACK
- [ ] Android Settings round-trip; launching a real app and returning
- [ ] Check 720p if a test surface is accessible; otherwise NOT RUN
- [ ] Check cold start, preference persistence, RAM and repeated gfxinfo
- [ ] Confirm stock Google TV remains HOME and is usable

Signing: GitHub-hosted debug APKs may use distinct keys on different runs.
If `adb install -r` fails for certificate mismatch, do not uninstall,
clear data or overwrite the current preview. Record a blocked installation
and use a separately reviewed non-destructive installation strategy.

## 0.1.3 AMOLED review installation

- Initial 0.1.3 main CI APK: `MONOLITH-TV-preview-debug`, run
  `43490839314`, SHA-256 `bbda44d15bef8db878f5d030939516e849a20d371c110ad07cd699645dd44470`.
- Private TV bridge install request #132 returned **ERROR: adb failed (exit 1)**.
  The bridge did not report stderr, so the exact reason is **unconfirmed**;
  a debug signing-certificate mismatch is plausible but not established.
- Private bridge #133 confirmed the original `dev.monolith.tv.preview`
  remains installed with `0.1.2-prealpha-preview`, versionCode 3.
  Bridge #134 confirmed Android 11 / armeabi-v7a device availability.
- To avoid deleting any preview data, CI now generates a separate
  `dev.monolith.tv.review` package. The pinned historical bridge artifact
  `MONOLITH-TV-preview-debug` contains the new *review* APK, while
  `MONOLITH-TV-standard-debug` preserves the regular package separately.
- The review variant is only for TV acceptance and does not change
  production IDs, stock HOME, existing preview data or firmware.

**NOT YET VERIFIED:** sidecar installation, actual version name, screenshot,
D-pad interactions, layout correctness, memory and performance. Update this
section with real-device evidence instead of treating CI as a pass.


## 0.1.3 AMOLED live review — BOXY, 13 November 2026

This section supersedes the earlier **NOT YET VERIFIED** statements only for
the precise checks actually completed below. It is an acceptance log,
not blanket hardware signoff.

- **PASS — build provenance:** `main` commit `ee5c82c2e2603fa34cde582e08cb2a912bb3398f`,
  GitHub Actions successful run `43493086514`, verified review artifact
  `MONOLITH-TV-preview-debug` containing `app-debug.apk`
  SHA-256 `2bdb70945c3554f6f7899a470e6347e019b73335e188d3ac442e77797a67ab91`
  (9,711,041 bytes).
- **PASS — non-destructive install:** private bridge issue #166 returned
  `SUCCESS / install_monolith_ci` after SHA verification. Bridge #167
  confirmed `dev.monolith.tv.review`, `0.1.3-prealpha-review`, versionCode 4.
  #168 independently confirmed the old `dev.monolith.tv.preview`,
  `0.1.2-prealpha-preview`, versionCode 3, was preserved.
- **PASS — launch:** bridge #174 reported foreground Activity
  `dev.monolith.tv.review/dev.monolith.tv.MainActivity` after dismissing
  Google Backdrop and launching the review package.
- **PASS — 1080p initial visual:** private screenshot #175 shows all 11
  installed application tiles in a four-column black AMOLED gallery,
  original Android icons, a visible high-contrast focus outline around
  BrowseHere, and its contextual hero.
- **PASS — D-pad RIGHT once:** bridge #176 and screenshot #177 show focus
  moving one tile from BrowseHere to Cx Проводник. The hero title/icon
  also changed to the corresponding app.
- **PASS — MENU favorite add/remove in All:** bridge #178 and screenshot
  #179 show Cx Проводник moved to the first position with star and
  favorites count 1 while preserving its focus and hero. Bridge #180 and
  screenshot #181 show count returning to 0, alphabetical ordering restored,
  and the same tile still focused.
- **PASS — grid-to-rail LEFT transition:** on relaunch, screenshot #193
  showed BrowseHere focused in the first grid column. One LEFT (#194) made
  the Settings rail item visibly focused (#195). The earlier
  LEFT, LEFT, DOWN, OK sequence (#182-#185) opened BrowseHere (#186)
  because the assumed menu target/order was incorrect; it does **not**
  establish a left-navigation failure. Rail UP/DOWN and Favorites selection
  still require a complete verified sequence.
- **PASS — external app launch:** the same #186 capture shows BrowseHere
  actually opened (although accidentally during the navigation attempt).
  A complete launch/Back return path was *not* established by that capture.

**NOT RUN / NOT PROVEN on this build:** empty Favorites navigation,
Settings and BACK round-trip, physical remote key repeat, 720p visual
layout, compact/comfortable setting change, persistence after restart,
RAM / GPU frame timing, cold boot, and stock HOME rollback.

**Verdict: review APK installed and basic gallery/favorite paths tested;
NOT YET DEVICE ACCEPTED for replacing the stock HOME.** The private
bridge screenshots remain in the private repository at
`captures/issue-175.png`, `issue-177.png`, `issue-179.png`,
`issue-181.png` and `issue-186.png`; they are not copied to this
public project.

## 0.1.4 Cinema live BOXY installation — 13 November 2026

- **PASS — private bridge recovered:** v2 issued #201 (`app_info`) and
  #202 (`device_summary`) were processed by the Termux/ADB worker.
  BOXY is Android 11, 1920×1080, physical density 320.
- **PASS — non-destructive install:** bridge issue #204 reported SUCCESS
  installing SHA-256-verified APK from successful main workflow run
  `43501308271`. `dev.monolith.tv.cinema` installed alongside existing
  `dev.monolith.tv.preview` and `dev.monolith.tv.review` (existing
  variants were present immediately before install in issue #203).
  Bridge #205 confirmed `0.1.4-prealpha-cinema`, versionCode 5.
- **PASS — launch request:** bridge #206 requested launch; #207 returned
  foreground `dev.monolith.tv.cinema/dev.monolith.tv.MainActivity`.
- **BLOCKED — visual capture:** private screenshots #208 and #211 are
  completely black, with identical Git blob SHA
  `906397130a495a3fad788ac0634df012812ab354`.
  HOME key #212 changed foreground to the stock Google TV Launcher (#213),
  yet screenshot #214 is *identically black*. After OK #215, screenshot
  #217 remained identical. This means blank captures are **not specific
  to MONOLITH**, and an HDMI/display sleep or capture failure must be
  excluded before judging whether the app UI renders.
- **NOT VERIFIED:** Cinema typography, dock visuals, D-pad traversal,
  favorites, Settings, 720p, frame pacing, cold boot and persistence.

**Verdict: installation and foreground activity confirmed; visual
acceptance BLOCKED by all-black device screenshots, including stock HOME.**
A screen wake / HDMI-active state is needed before repeating screenshot
and D-pad tests. No default HOME change, uninstall or app data clear.

## 0.1.4 Cinema resumed BOXY visual audit — 14 November 2026

This is a **new physical-device audit**, following the user's restoration
of the TV/HDMI active state. It supersedes the older blanket
"visual capture blocked" finding for the checks performed below.

- **PASS — screen capture restored:** private bridge issue #219 launched
  `dev.monolith.tv.cinema`; #220 confirmed the foreground
  `dev.monolith.tv.cinema/dev.monolith.tv.MainActivity`; screenshot #221
  is a real 1920×1080 render showing the full-width feature stage, top
  navigation and horizontal icon dock. Original Android app icons appear
  at full size. This is visibly different from 0.1.3's multi-row grid.
- **DEFECT — focus on first launch:** screenshot #221 showed no visible dock
  focus. First RIGHT (#222) focused the All top tab (#223) instead of the
  next app. DOWN (#224) explicitly focused BrowseHere in the dock (#225).
- **PASS — dock traversal after focus established:** RIGHT (#226) moved focus
  exactly once from BrowseHere to Cx Проводник, and feature stage title/icon
  updated accordingly (screenshot #227).
- **PASS — MENU favorite toggle:** MENU (#228) pinned Cx Проводник, moved
  it first and retained selected focus (screenshot #229). MENU (#230)
  unpinned it and restored the original sorting (screenshot #231).
- **PASS — Favorites empty state:** UP (#232) focused All (#233);
  RIGHT+OK (#234–#235) opened empty Favorites with a clear focused tab
  and `0` count (screenshot #236). **UX issue:** the empty-state
  guidance is repeated in hero and dock.
- **PASS — Settings view:** RIGHT+OK (#237–#238) opened Settings; the first
  option had visible focus (screenshot #239).
- **PASS — setting toggled:** OK (#240) switched compactGrid from Disabled
  to Enabled (screenshot #241); a subsequent OK (#242) reverted it.
  **UX issue:** UI still calls it "Compact grid", despite Cinema using
  a horizontal app dock. Underlying key is intentionally unchanged.
- **PARTIAL — BACK and connection:** BACK key (#243) was successfully sent,
  but follow-up screenshot #244 failed. Read-only focus_status #245,
  screenshot #246 and device_summary #247 subsequently reported ADB errors.
  Do not treat BACK-to-app screenshot as validated, and do not attribute
  the transport failure to MONOLITH without evidence.
- **NOT RUN:** 720p, extended frame pacing/RAM, HDMI remote repeat,
  power cycle, Android system Settings round-trip, stable signing upgrades.

**Status: Cinema visual layout and core dock/favorite/settings flows
VERIFIED on BOXY, but NOT FULLY DEVICE ACCEPTED.** Track the initial
focus defect and UI-copy issues in GitHub Issue #7. Stock HOME and
previous MONOLITH installations were not changed.


## 0.1.4.1 Cinema focus-fix acceptance — BOXY, 14 November 2026

This record covers the **newly installed** source revision, not the old
0.1.4 Cinema screenshots. Earlier findings above remain historical.

- **PASS — reproducible build:** `main` squash commit
  `b4abf580e136435b65fd14dfcf0b3e42310271d3`,
  GitHub Actions `43560032023` completed SUCCESS. The signed
  `app-debug.apk` from allowlisted `MONOLITH-TV-preview-debug` artifact
  `14099913524` is 9,729,769 bytes with SHA-256
  `345f62d13947317139b039e9d29103478409804a919185635320af0b487c4d35`.
- **PASS — safe installation:** private bridge #254 confirmed the
  SHA-pinned `install_monolith_ci` completed successfully. #255 confirmed
  `dev.monolith.tv.cinemafix`, `0.1.4.1-prealpha-cinemafix`,
  versionCode 6. The first launch encountered system Google Backdrop
  (#257), resolved with BACK (#259) and relaunch (#260); #261 then
  confirmed CinemaFix MainActivity in foreground.
- **PASS — first-launch D-pad focus regression:** private screenshot
  `captures/issue-262.png` showed BrowseHere **already focused in the
  horizontal dock**, before sending any D-pad navigation key. This
  fixes the 0.1.4 first-launch no-focus case (#221–#225).
- **PASS — first RIGHT:** one RIGHT (#263) moved focus exactly once
  to Cx Проводник, updated the large featured title/artwork and kept
  a strong visible keyline (screenshot #264).
- **PASS — MENU favorite toggle/reordering:** MENU (#265) pinned
  Cx Проводник, moved it first and preserved focus and hero (screenshot
  #266). MENU again (#267) removed the favorite (screenshot #268).
- **PASS — Settings wording and return:** UP, RIGHT, RIGHT, OK
  (#269–#272) opened Settings, where screenshot #273 showed
  `Компактная полка` and its horizontal-dock explanation. BACK
  (#274) returned to All while preserving focus on Cx Проводник
  (screenshot #275). No setting data migration was performed.
- **PASS — empty Favorites once:** UP, RIGHT, OK (#276–#278) opened
  Favorites; screenshot #279 showed the empty-state explanation once,
  with no repeated paragraph under the hero. BACK (#280) restored
  All and selected Cx Проводник (screenshot #281).
- **Installed package preservation:** `app_info` #282 confirmed older
  `dev.monolith.tv.cinema` 0.1.4 code 5, and #284 confirmed
  `dev.monolith.tv.review` 0.1.3 code 4. `app_info` #283 returned
  `unknown` for the old `dev.monolith.tv.preview` version; its
  installation status cannot be concluded from this probe. There
  was no uninstall, data clear, or HOME-selection change during this audit.

**Not checked:** physical remote key repeat, 720p layout, cold boot,
RAM/FPS/long-run stability, Android system settings round-trip and stable
APK upgrade signing.

**Verdict: the Cinema first-launch focus defect and the two identified
UX-copy issues are VERIFIED FIXED on BOXY.** Overall release/device
acceptance remains *pre-alpha* because the checks above are still pending.
Screenshots reside in the private `vitrum-tv-bridge` repository; do
not copy them into this public project.
