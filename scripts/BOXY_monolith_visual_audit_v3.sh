#!/usr/bin/env bash
# MONOLITH TV / BOXY visual audit v4
# Read-only diagnosis; sends WAKEUP + DPAD only. No HOME/BACK/OK/MENU.
set -Eeuo pipefail

TV="${1:-192.168.0.160:5555}"
PKG="dev.monolith.tv.preview"
ACTIVITY="dev.monolith.tv.MainActivity"
COMPONENT="$PKG/$ACTIVITY"
DEST="${MONOLITH_AUDIT_DIR:-/storage/emulated/0/Download}"
STAMP="$(date +%Y%m%d_%H%M%S)"
NAME="BOXY_MONOLITH_AUDIT_V4_${STAMP}"
WORK="$DEST/$NAME"
ARCHIVE="$DEST/$NAME.tar.gz"
STATUS="INCOMPLETE"

command -v adb >/dev/null 2>&1 || { echo 'ADB не найден. Установи: pkg install android-tools' >&2; exit 1; }
command -v tar >/dev/null 2>&1 || { echo 'Не найден tar.' >&2; exit 1; }
if ! mkdir -p "$WORK"; then
  echo 'Не удалось открыть Download. Выполни termux-setup-storage.' >&2
  exit 1
fi

# Even on error, save diagnostics and any captured screenshots.
finish() {
  local code=$?
  trap - EXIT
  if (( code == 0 )); then STATUS="SUCCESS"; else STATUS="FAILED (exit=$code)"; fi
  printf '%s\n' "$STATUS" > "$WORK/status.txt"
  if tar -czf "$ARCHIVE" -C "$DEST" "$NAME"; then
    echo
    echo "АРХИВ: $ARCHIVE"
    echo "РЕЗУЛЬТАТ: $STATUS"
  else
    echo "Архивация не удалась; данные остались в $WORK" >&2
  fi
}
trap finish EXIT

# Capture failures and ADB behavior without dumping unrelated TV data.
log_state() {
  local label="$1"
  {
    echo "=== $label ==="
    echo "$(date -Is)"
    echo '--- Window focus ---'
    adb -s "$TV" shell dumpsys window 2>&1 | grep -E 'mCurrentFocus=|mFocusedApp=' | head -n 8 || true
    echo '--- Activity resumed ---'
    adb -s "$TV" shell dumpsys activity activities 2>&1 | grep -E 'topResumedActivity|mResumedActivity|ResumedActivity|mFocusedActivity' | head -n 12 || true
    echo '--- Process ---'
    adb -s "$TV" shell pidof "$PKG" 2>&1 || true
  } > "$WORK/$label.txt"
}

# An actual foreground app is required before injecting D-pad keys.
# On Android TV some firmware reports mCurrentFocus=null during transitions.
# Accept that case only when activity manager says MONOLITH is resumed.
foreground_ok() {
  local win resumed
  win="$(adb -s "$TV" shell dumpsys window 2>/dev/null | grep -m 1 'mCurrentFocus=' || true)"
  resumed="$(adb -s "$TV" shell dumpsys activity activities 2>/dev/null | grep -E 'topResumedActivity|mResumedActivity|ResumedActivity|mFocusedActivity' | head -n 12 || true)"
  if [[ "$win" == *"$PKG"* ]]; then
    return 0
  fi
  if [[ "$win" == *'mCurrentFocus=null'* || -z "$win" ]]; then
    [[ "$resumed" == *"$PKG"* ]]
    return $?
  fi
  return 1
}

adb connect "$TV" > "$WORK/adb_connect.txt" 2>&1 || true
if [[ "$(adb -s "$TV" get-state 2>/dev/null || true)" != device ]]; then
  echo "ADB недоступен: $TV" >&2
  exit 2
fi
if ! adb -s "$TV" shell pm path "$PKG" > "$WORK/package_path.txt" 2>&1 || \
   ! grep -q '^package:' "$WORK/package_path.txt"; then
  echo "Пакет $PKG не найден на BOXY." >&2
  exit 3
fi

{
  echo "TV=$TV"
  echo "PKG=$PKG"
  echo "COMPONENT=$COMPONENT"
  echo "TIME=$(date -Is)"
  adb -s "$TV" shell getprop ro.build.version.release
  adb -s "$TV" shell wm size
  adb -s "$TV" shell wm density
} > "$WORK/device.txt" 2>&1 || true

log_state before_launch
# Never press HOME here: it would bring Google TV to the foreground.
adb -s "$TV" shell input keyevent KEYCODE_WAKEUP > "$WORK/wakeup.txt" 2>&1 || true

# Explicit component launch, with no implicit intent resolution.
# HOT is expected if an instance already exists; it is not proof of visibility.
launched=0
for attempt in 1 2 3; do
  if adb -s "$TV" shell am start --user 0 -W -n "$COMPONENT" \
       > "$WORK/launch_attempt_${attempt}.txt" 2>&1; then
    launched=1
  fi
  sleep 2
  log_state "after_launch_${attempt}"
  if foreground_ok; then
    launched=2
    break
  fi
  echo "Попытка $attempt: MONOLITH TV пока не на переднем плане." >&2
done

if [[ "$launched" -ne 2 ]]; then
  echo 'MONOLITH TV не стал активным экраном. Сохраняю диагностику и снимок без отправки кнопок.' >&2
  adb -s "$TV" exec-out screencap -p > "$WORK/NOT_MONOLITH_screen.png" 2>/dev/null || true
  adb -s "$TV" shell dumpsys activity activities > "$WORK/activity_diagnostics.txt" 2>&1 || true
  adb -s "$TV" shell logcat -d -t 250 -v brief 2>/dev/null | \
    grep -E 'AndroidRuntime|FATAL EXCEPTION|dev\.monolith\.tv|ActivityTaskManager' \
    > "$WORK/recent_launch_logs.txt" || true
  exit 4
fi

echo 'MONOLITH TV подтверждён в foreground; снимаю экраны.'
adb -s "$TV" shell dumpsys gfxinfo "$PKG" reset > "$WORK/gfxinfo_reset.txt" 2>&1 || true

capture() {
  local label="$1" key="${2:-}"
  if [[ -n "$key" ]]; then
    if ! foreground_ok; then
      log_state "left_launcher_before_${label}"
      echo "Прервано: MONOLITH TV не активен перед $label." >&2
      exit 5
    fi
    adb -s "$TV" shell input keyevent "$key" > "$WORK/${label}_key.txt" 2>&1 || true
  fi
  sleep 0.7
  log_state "focus_${label}"
  if ! foreground_ok; then
    echo "Прервано: переключился экран после $label." >&2
    adb -s "$TV" exec-out screencap -p > "$WORK/INTERRUPTED_${label}.png" 2>/dev/null || true
    exit 6
  fi
  if ! adb -s "$TV" exec-out screencap -p > "$WORK/${label}.png" 2> "$WORK/${label}.error.txt" || \
      [[ ! -s "$WORK/${label}.png" ]]; then
    echo "Не удалось создать снимок ${label}.png" >&2
    exit 7
  fi
  echo "Снимок: ${label}.png"
}

capture '00_initial'
capture '01_right' KEYCODE_DPAD_RIGHT
capture '02_right' KEYCODE_DPAD_RIGHT
capture '03_down' KEYCODE_DPAD_DOWN
capture '04_left' KEYCODE_DPAD_LEFT
capture '05_down' KEYCODE_DPAD_DOWN
capture '06_left' KEYCODE_DPAD_LEFT
capture '07_up' KEYCODE_DPAD_UP
capture '08_up' KEYCODE_DPAD_UP

# More real interactions for a meaningful gfxinfo sample; no OK or MENU.
for ((i=0; i<12; i++)); do
  for key in KEYCODE_DPAD_RIGHT KEYCODE_DPAD_DOWN KEYCODE_DPAD_LEFT KEYCODE_DPAD_UP; do
    if ! foreground_ok; then
      log_state 'left_launcher_during_benchmark'
      echo 'Вышли из MONOLITH TV во время замера.' >&2
      exit 8
    fi
    adb -s "$TV" shell input keyevent "$key" >/dev/null 2>&1 || true
    sleep 0.12
  done
done

adb -s "$TV" shell dumpsys gfxinfo "$PKG" > "$WORK/gfxinfo_after_navigation.txt" 2>&1 || true
# UI hierarchy is optional; firmware may refuse it. Keep screenshots regardless.
if adb -s "$TV" shell uiautomator dump /sdcard/monolith_tv_audit.xml \
     > "$WORK/uiautomator.txt" 2>&1; then
  adb -s "$TV" shell cat /sdcard/monolith_tv_audit.xml > "$WORK/ui_hierarchy.xml" 2>/dev/null || true
  adb -s "$TV" shell rm -f /sdcard/monolith_tv_audit.xml >/dev/null 2>&1 || true
fi
cat > "$WORK/README.txt" <<'END'
MONOLITH TV/BOXY visual audit v4. Screens 00..08 are from the actual device.
The script verifies the foreground app before sending D-pad keys. It never
sends HOME, BACK, OK or MENU, changes launcher defaults, or uninstalls apps.
An archive is produced on errors too; inspect status.txt and launch attempts.
END
