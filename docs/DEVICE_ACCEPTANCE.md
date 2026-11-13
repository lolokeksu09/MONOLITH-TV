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
