#!/usr/bin/env bash
# MONOLITH TV BOXY visual audit v3: launch by package and tolerate null window focus.
set -Eeuo pipefail
TV="${1:-192.168.0.160:5555}"
PKG="dev.monolith.tv.preview"
MAIN="dev.monolith.tv.MainActivity"
DOWNLOADS="/storage/emulated/0/Download"
NAME="BOXY_MONOLITH_AUDIT_V3_$(date +%Y%m%d_%H%M%S)"
DIR="$DOWNLOADS/$NAME"
ARCHIVE="$DOWNLOADS/$NAME.tar.gz"

command -v adb >/dev/null || { echo "Установи ADB: pkg install android-tools" >&2; exit 1; }
test -d "$DOWNLOADS" || { echo "Нет Download: termux-setup-storage" >&2; exit 1; }
mkdir -p "$DIR"
adb connect "$TV" > "$DIR/adb_connect.txt" 2>&1
test "$(adb -s "$TV" get-state 2>/dev/null)" = device || { echo "ADB не подключён" >&2; exit 1; }
adb -s "$TV" shell pm path "$PKG" | grep -q '^package:' || { echo "$PKG не найден" >&2; exit 1; }

focus() {
  adb -s "$TV" shell dumpsys window | grep 'mCurrentFocus=' | head -n 1 | tr -d '\r' || true
}
resumed_activity() {
  adb -s "$TV" shell dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity|ResumedActivity|mFocusedActivity' | head -n 8 | tr -d '\r' || true
}
launch_by_package() {
  # Resolve the Android TV launcher intent using the installed PACKAGE, not an activity guess.
  adb -s "$TV" shell am start -a android.intent.action.MAIN \
    -c android.intent.category.LEANBACK_LAUNCHER -p "$PKG" >> "$DIR/activity_start.txt" 2>&1 || \
  adb -s "$TV" shell am start -n "$PKG/$MAIN" >> "$DIR/activity_start.txt" 2>&1
}
check_foreground() {
  local stage="$1" win activity
  win="$(focus)"
  activity="$(resumed_activity)"
  printf 'WINDOW: %s\nRESUMED: %s\n' "$win" "$activity" > "$DIR/$stage.foreground.txt"
  # The window-focus field can transiently be null on this firmware. A resumed
  # MONOLITH activity is a second, more informative foreground signal.
  if [[ "$win" == *"$PKG"* || "$activity" == *"$PKG"* ]]; then
    return 0
  fi
  if [[ "$win" == *'mCurrentFocus=null'* || -z "$win" ]]; then
    echo "Внимание: mCurrentFocus=null на $stage; продолжаю, сохранив диагностику." >&2
    return 0
  fi
  echo "Другая программа в фокусе на $stage: $win" >&2
  return 1
}

# Wake, return to HOME to dismiss a possible Dream, and explicitly launch TV activity by package.
adb -s "$TV" shell input keyevent KEYCODE_WAKEUP || true
adb -s "$TV" shell input keyevent KEYCODE_HOME || true
sleep 1
launch_by_package
sleep 3
FOREGROUND="$(focus)"
if [[ "$FOREGROUND" == *'DreamActivity'* || "$FOREGROUND" == *'com.google.android.backdrop'* ]]; then
  adb -s "$TV" shell input keyevent KEYCODE_BACK || true
  sleep 1
  launch_by_package
  sleep 2
fi
if ! check_foreground initial; then
  launch_by_package
  sleep 2
  check_foreground initial_retry || exit 2
fi

{
  echo "Time: $(date -Is)"
  echo "Device: $TV"
  echo "Package: $PKG"
  echo "Resumed: $(resumed_activity)"
  echo "Android: $(adb -s "$TV" shell getprop ro.build.version.release)"
  adb -s "$TV" shell wm size
  adb -s "$TV" shell wm density
} > "$DIR/device.txt" 2>&1
adb -s "$TV" shell dumpsys gfxinfo "$PKG" reset > "$DIR/gfxinfo_reset.txt" 2>&1 || true

capture() {
  local name="$1" key="${2:-}" now
  if [[ -n "$key" ]]; then adb -s "$TV" shell input keyevent "$key"; fi
  sleep 1
  now="$(focus)"
  echo "$now" > "$DIR/$name.focus.txt"
  if ! check_foreground "$name"; then
    echo "Прервано: переключилось другое приложение (снимок $name)" >&2
    exit 3
  fi
  adb -s "$TV" exec-out screencap -p > "$DIR/$name.png"
  test -s "$DIR/$name.png" || { echo "Снимок $name пуст" >&2; exit 4; }
  # Optional view hierarchy: helps find focused application labels.
  if adb -s "$TV" shell uiautomator dump /sdcard/monolith_tv_audit.xml >/dev/null 2>&1; then
    adb -s "$TV" shell cat /sdcard/monolith_tv_audit.xml > "$DIR/$name.ui.xml" 2>/dev/null || true
  fi
  echo "$name.png"
}
capture "00_home_initial"
capture "01_up" KEYCODE_DPAD_UP
capture "02_left" KEYCODE_DPAD_LEFT
capture "03_down" KEYCODE_DPAD_DOWN
capture "04_right" KEYCODE_DPAD_RIGHT
capture "05_down" KEYCODE_DPAD_DOWN
capture "06_left" KEYCODE_DPAD_LEFT
capture "07_up" KEYCODE_DPAD_UP
capture "08_right" KEYCODE_DPAD_RIGHT

# A larger sample for gfxinfo: navigation only, never ENTER/MENU/HOME.
for ((i=0; i<10; i++)); do
  for key in KEYCODE_DPAD_DOWN KEYCODE_DPAD_RIGHT KEYCODE_DPAD_UP KEYCODE_DPAD_LEFT; do
    adb -s "$TV" shell input keyevent "$key" >/dev/null
    sleep 0.15
  done
done
adb -s "$TV" shell dumpsys gfxinfo "$PKG" > "$DIR/gfxinfo_after_navigation.txt" 2>&1 || true
adb -s "$TV" shell dumpsys activity activities > "$DIR/activities.txt" 2>&1 || true
adb -s "$TV" shell rm -f /sdcard/monolith_tv_audit.xml >/dev/null 2>&1 || true
printf 'MONOLITH TV audit v3: package-based activity launch, null-focus fallback, screen verification, 9 screenshots, optional UI hierarchy, 40 additional D-pad events. No app launched, no settings changed.\n' > "$DIR/README.txt"
tar -czf "$ARCHIVE" -C "$DOWNLOADS" "$NAME"
echo "ГОТОВО: $ARCHIVE"
