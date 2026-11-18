#!/usr/bin/env bash
# Runs the emulator tests (app/src/androidTest, see e2e/README.md) on an Android emulator and
# collects everything they leave behind. CI runs it in the "Test build" workflow; it runs the same
# way on a developer's machine.
#
#   e2e/run-on-emulator.sh <folder with the APKs> <results folder>
#
# The APK folder holds app-debug.apk, app-debug-androidTest.apk, game-debug.apk and post-debug.apk:
#   ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
#   ./gradlew -p e2e/fixtures assembleDebug
#
# It refuses to run on anything but an emulator: the tests rewrite Focus's settings and data.
# Set ANDROID_SERIAL (for example emulator-5554) so adb cannot pick a phone that is plugged in.
# E2E_TEST runs one journey or one test instead of all of them, for example
#   E2E_TEST=com.focus.launcher.e2e.journeys.TimerJourney#theServiceStopsAGameWhenItsTimeRunsOut
#
# Results: summary.md, instrumentation.txt (the raw test output), steps/ (a picture at every
# checkpoint of every test, and the screen and its element tree where a test failed), video-N.mp4
# (the whole run, in parts of up to three minutes), logcat.txt, crash.txt.
# Exit status: 0 only if every test ran and passed.
set -uo pipefail

APKS=${1:?folder with the APKs}
OUT=${2:?results folder}
PKG=com.focus.launcher
EVIDENCE=/sdcard/Android/data/$PKG/files/e2e
mkdir -p "$OUT"
: > "$OUT/summary.md"
note() { echo "$*" | tee -a "$OUT/summary.md"; }
die() { echo "::error::$*"; echo "- FAILED: $*" >> "$OUT/summary.md"; exit 1; }

# --- an emulator, and nothing else
adb wait-for-device
qemu=$(adb shell getprop ro.kernel.qemu | tr -d '\r')
hw=$(adb shell getprop ro.hardware | tr -d '\r')
[ "$qemu" = 1 ] || [ "$hw" = ranchu ] || [ "$hw" = goldfish ] \
  || die "this device is not an emulator (ro.hardware=$hw). The emulator tests never run on a phone."
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done

for f in app-debug.apk app-debug-androidTest.apk game-debug.apk post-debug.apk; do
  [ -f "$APKS/$f" ] || die "$APKS/$f is missing. A pull request from before the emulator tests existed has none: merge main into it."
done

release=$(adb shell getprop ro.build.version.release | tr -d '\r')
api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
note "## Android $release (API $api)"

# --- the same conditions every time: no animations, screen stays on, unlocked
for s in window_animation_scale transition_animation_scale animator_duration_scale; do adb shell settings put global $s 0; done
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true

# --- install Focus, its tests, and the two fixture apps
for f in app-debug.apk app-debug-androidTest.apk game-debug.apk post-debug.apk; do
  adb install -r -t -g "$APKS/$f" >/dev/null || die "$f does not install"
done
runner=$(adb shell pm list instrumentation | tr -d '\r' | sed -n "s/^instrumentation:\([^ ]*\) (target=$PKG)\$/\1/p" | head -1)
[ -n "$runner" ] || die "the test APK declares no test runner for $PKG"
# Focus as the home screen, settled before the tests start. Android restarts the home screen a few
# seconds after the choice changes (even to the same app), and a test running at that moment would
# lose its screen: so it is only changed when it has to be, and then given time to settle.
is_home() { adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep -q "$PKG/"; }
in_front() { adb shell dumpsys activity activities | grep -E -m1 'topResumedActivity|mResumedActivity' | grep -q "$PKG/"; }
if ! is_home; then
  adb shell cmd package set-home-activity "$PKG/.MainActivity" >/dev/null 2>&1 \
    || adb shell cmd role add-role-holder android.app.role.HOME "$PKG" >/dev/null 2>&1 || true
  is_home || die "Focus could not be made the home app"
  adb shell input keyevent KEYCODE_HOME
  steady=0
  for _ in $(seq 1 40); do
    if in_front; then steady=$((steady + 1)); else steady=0; adb shell input keyevent KEYCODE_HOME; fi
    [ $steady -ge 8 ] && break
    sleep 1
  done
fi
adb shell rm -rf "$EVIDENCE"
# No accessibility service left on from an earlier run: tests switch it on themselves.
adb shell settings delete secure enabled_accessibility_services >/dev/null
adb logcat -c 2>/dev/null; adb logcat -b crash -c 2>/dev/null

# --- film the whole run; screenrecord stops after three minutes, so it is restarted in parts
rm -f "$OUT"/video-*.mp4
touch "$OUT/.recording"
(
  i=0
  while [ -f "$OUT/.recording" ]; do
    adb shell screenrecord --time-limit 180 --bit-rate 4000000 "/sdcard/e2e-video-$i.mp4" >/dev/null 2>&1 || break
    i=$((i + 1))
  done
) &
recorder=$!

# --- the tests: one run of the instrumentation, Focus started once for all of them
only=()
if [ -n "${E2E_TEST:-}" ]; then
  [[ "$E2E_TEST" =~ ^[A-Za-z0-9_.#]+$ ]] || die "E2E_TEST='$E2E_TEST' is not a class or class#test name"
  only=(-e class "$E2E_TEST")
  note "- only: $E2E_TEST"
fi
echo "running the emulator tests ($runner)"
adb shell am instrument -w -r ${only[@]+"${only[@]}"} "$runner" | tr -d '\r' > "$OUT/instrumentation.txt"

rm -f "$OUT/.recording"
adb shell pkill -INT screenrecord >/dev/null 2>&1 || true
for _ in $(seq 1 15); do kill -0 "$recorder" 2>/dev/null || break; sleep 1; done
kill "$recorder" 2>/dev/null || true
for v in $(adb shell 'ls /sdcard/e2e-video-*.mp4 2>/dev/null' | tr -d '\r'); do
  adb pull "$v" "$OUT/video-$(basename "$v" .mp4 | sed 's/e2e-video-//').mp4" >/dev/null 2>&1
  adb shell rm -f "$v"
done

# --- what happened, test by test
adb pull "$EVIDENCE" "$OUT/steps" >/dev/null 2>&1 || mkdir -p "$OUT/steps"
adb logcat -d -v time > "$OUT/logcat.txt" 2>/dev/null
adb logcat -d -b crash > "$OUT/crash.txt" 2>/dev/null

results=$(awk '
  /^INSTRUMENTATION_STATUS: class=/ { cls = substr($0, index($0, "=") + 1); sub(/.*\./, "", cls) }
  /^INSTRUMENTATION_STATUS: test=/  { test = substr($0, index($0, "=") + 1) }
  /^INSTRUMENTATION_STATUS_CODE: /  {
    code = $2
    if (code == 0)  print "passed  " cls "." test
    if (code == -2) print "FAILED  " cls "." test
    if (code == -1) print "ERROR   " cls "." test
    if (code == -3 || code == -4) print "skipped " cls "." test
  }' "$OUT/instrumentation.txt")
passed=$(grep -c '^passed' <<<"$results" || true)
failed=$(grep -cE '^(FAILED|ERROR)' <<<"$results" || true)
skipped=$(grep -c '^skipped' <<<"$results" || true)
{
  echo ""
  echo '```'
  echo "${results:-no test reported a result}"
  echo '```'
  echo "- $passed passed, $failed failed, $skipped skipped"
  echo "- $(find "$OUT/steps" -name '*.png' | wc -l | tr -d ' ') pictures in steps/, $(ls "$OUT"/video-*.mp4 2>/dev/null | wc -l | tr -d ' ') video part(s)"
} >> "$OUT/summary.md"

status=0
if grep -q 'INSTRUMENTATION_RESULT: shortMsg=' "$OUT/instrumentation.txt"; then
  echo "::error::the test run was cut short: $(grep -m1 'shortMsg=' "$OUT/instrumentation.txt")"; status=1
fi
if [ "$failed" != 0 ]; then echo "::error::$failed emulator test(s) failed (see summary.md and steps/)"; status=1; fi
if [ "$passed" = 0 ]; then echo "::error::no emulator test passed"; status=1; fi
if ! grep -q '^OK (' "$OUT/instrumentation.txt"; then status=1; fi
crashes=$(grep -c "Process: $PKG" "$OUT/crash.txt" || true)
if [ "$crashes" != 0 ]; then echo "::error::Focus crashed $crashes time(s) during the run (crash.txt)"; status=1; fi

[ -n "${GITHUB_STEP_SUMMARY:-}" ] && cat "$OUT/summary.md" >> "$GITHUB_STEP_SUMMARY"
cat "$OUT/summary.md"
exit $status
