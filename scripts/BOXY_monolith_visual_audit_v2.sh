#!/usr/bin/env bash
# MONOLITH TV BOXY visual audit v2: screens and focus, no HOME changes.
set -Eeuo pipefail
TV="${1:-192.168.0.160:5555}"
PKG="dev.monolith.tv.preview"
MAIN="dev.monolith.tv.MainActivity"
DOWNLOADS="/storage/emulated/0/Download"
NAME="BOXY_MONOLITH_AUDIT_V2_$(date +%Y%m%d_%H%M%S)"
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
# Wake first. If a Dream remains on top, dismiss only the Dream, never BACK from our app.
adb -s "$TV" shell input keyevent KEYCODE_WAKEUP || true
adb -s "$TV" shell am start -n "$PKG/$MAIN" > "$DIR/activity_start.txt" 2>&1 || true
sleep 2
FOREGROUND="$(focus)"
if [[ "$FOREGROUND" == *"DreamActivity"* || "$FOREGROUND" == *"com.google.android.backdrop"* ]]; then
  echo "$FOREGROUND" > "$DIR/dream_before.txt"
  adb -s "$TV" shell input keyevent KEYCODE_BACK || true
  sleep 1
  adb -s "$TV" shell am start -n "$PKG/$MAIN" >> "$DIR/activity_start.txt" 2>&1 || true
  sleep 2
  FOREGROUND="$(focus)"
fi
echo "$FOREGROUND" > "$DIR/foreground_before.txt"
[[ "$FOREGROUND" == *"$PKG"* ]] || { echo "MONOLITH TV не на экране: $FOREGROUND" >&2; exit 2; }

{
  echo "Time: $(date -Is)"
  echo "Device: $TV"
  echo "Package: $PKG"
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
  [[ "$now" == *"$PKG"* ]] || { echo "Вышли из лаунчера на $name" >&2; exit 3; }
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
printf 'MONOLITH TV audit v2: screen verification, 9 screenshots, optional UI hierarchy, 40 additional D-pad events. No app launched, no settings changed.\n' > "$DIR/README.txt"
tar -czf "$ARCHIVE" -C "$DOWNLOADS" "$NAME"
echo "ГОТОВО: $ARCHIVE"
