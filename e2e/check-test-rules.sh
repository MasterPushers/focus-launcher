#!/usr/bin/env bash
# The mechanical half of e2e/README.md's rules for emulator tests. CI runs it on every pull
# request (Build workflow); it reads files only and needs nothing installed.
#   journeys: only robots and Given; no waiting, finding, tapping or swiping of their own
#   robots:   no fixed pauses, no screen coordinates
#   all:      no UI Automator selector kept in a val (they change in place)
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
E2E=app/src/androidTest/java/com/focus/launcher/e2e
[ -d "$E2E" ] || { echo "no emulator tests in this tree"; exit 0; }
status=0
check() { # <folder> <what is wrong> <regex>
  local hits
  hits=$(grep -rnE "$3" "$E2E/$1" --include='*.kt' || true)
  if [ -n "$hits" ]; then
    while IFS= read -r line; do echo "::error::$2: $line"; done <<<"$hits"
    status=1
  fi
}
check journeys "journeys go through robots, not UI Automator" '\bBy\.|\bUntil\.|UiDevice|UiObject|Device\.|\.click\(|\.swipe\(|\.text = |InstrumentationRegistry'
check journeys "no fixed pauses: wait for a condition (Device.waitFor / Device.waitUntil)" 'Thread\.sleep|SystemClock\.sleep|\bdelay\('
check robots "no fixed pauses: wait for a condition (Device.waitFor / Device.waitUntil)" 'Thread\.sleep|SystemClock\.sleep|\bdelay\('
check robots "no screen coordinates: find the element, then act on it" 'swipe\([0-9]|click\([0-9]'
check . "a selector kept in a val is changed by every hasDescendant() on it: build a fresh one per lookup" 'val [A-Za-z_]+( *: *BySelector)? *= *By\.'
[ $status = 0 ] && echo "the emulator tests follow the rules"
exit $status
