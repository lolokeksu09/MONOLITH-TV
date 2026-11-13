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
