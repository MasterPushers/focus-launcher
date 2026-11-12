# Emulator tests

The real Focus app, running as the home screen of an Android emulator, driven the way a person
would use it. These tests catch what the unit tests in `app/src/test` cannot: whether Android
actually lets Focus be the home screen, open an app, or put the time's-up wall in front of a game.

They run on GitHub when a maintainer writes `/test-build` on a pull request (workflow
`.github/workflows/test-build.yml`), on Android 8.0 and 16 by default. They never run on a phone:
every test skips itself unless the device is an emulator, and the runner script refuses anything
else.

## How it is built

```
app/src/androidTest/java/com/focus/launcher/e2e/
  journeys/   the tests. One file per feature, each test one short user story
  robots/     one per screen (home, app list, wall, limited apps, another app):
              the only code that finds elements, taps, swipes and waits
  support/    the device, the clean start (Given), screenshots (Evidence), the emulator guard
e2e/fixtures/ two tiny apps installed next to Focus: "Dummy Game" (Android files it as a game)
              and "Dummy Mail" (Android files it as social, but it handles mailto: links)
e2e/run-on-emulator.sh   installs everything, runs the tests once, collects the results
e2e/check-test-rules.sh  checks the rules below that a script can check (runs on every PR)
```

The tests and Focus run in **one process, started once per run**. Before each test, `Given`
resets Focus's settings inside that process and the test presses Home: no reinstall, no clearing
of data, no restart between tests.

A test reads like what a user does:

```kotlin
@Test
fun theServiceStopsAGameWhenItsTimeRunsOut() {
    Given.accessibilityService(on = true)
    Given.minutesLeftToday(GAME, 1)
    home.openAppList().search("dummy g").open(GAME)
    app.isOpen(GAME)
    wall.staysAwayFor(30.seconds)
    wall.appearsWithin(2.minutes).isFor(GAME).close(GAME)
    home.isShown()
}
```

## Rules for writing one

1. **Start on the home page, end on a known screen.** `Journey` resets the state and goes Home
   before every test; a test never relies on what an earlier one left behind.
2. **One user story per test, three or four screen changes at most.** Go straight to the screen a
   test is about (a robot may open a settings page directly) instead of clicking through menus.
3. **`Given` is the only shortcut, and only for setting up.** What the test does and what it
   checks go through the screen.
4. **Journeys use robots and `Given`, nothing else.** No `By.`, `UiDevice`, `click()` or `swipe()`
   in a journey.
5. **No fixed pauses anywhere.** Wait for a condition with a limit (`Device.waitFor`,
   `Device.waitUntil`). A test that passes only sometimes is a bug, never something to retry.
6. **Find elements by their test tag or their text, never by screen position.** Test tags live in
   `app/src/main/java/com/focus/launcher/ui/TestTags.kt`.
7. **Check what the user would see**, not just that nothing crashed: the wall is in front *and*
   names the game.
8. **A new test must be seen failing.** Break the feature it guards (for example, make the wall
   never show), watch the test fail, put the feature back. A test that has never failed proves
   nothing.
9. **Run the whole suite several times in a row on one emulator before merging** (ten is the
   habit). The emulator keeps its usage history between runs, which is what shakes out a test that
   depends on what ran before it.
10. **Never keep a UI Automator selector in a `val`.** `hasDescendant()` changes the selector in
    place, so a stored one silently collects conditions. Build a fresh one for every lookup.

Rules 4, 5, 6 and 10 are checked by `e2e/check-test-rules.sh`.

## Running them yourself

An emulator (any Android 8.0 or newer image), then:

```bash
export ANDROID_SERIAL=emulator-5554   # so adb can never pick a phone that is plugged in
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew -p e2e/fixtures assembleDebug
mkdir -p build/e2e-apks
cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
   e2e/fixtures/game/build/outputs/apk/debug/game-debug.apk e2e/fixtures/post/build/outputs/apk/debug/post-debug.apk build/e2e-apks/
e2e/run-on-emulator.sh build/e2e-apks build/e2e-results
```

Do not use `./gradlew connectedDebugAndroidTest`: it installs on *every* connected device,
a phone included, and uninstalls the app afterwards.

Results: `summary.md`, `steps/<Journey>.<test>/NN-step.png` (and `FAILED.png`,
`FAILED-screen.xml`, `FAILED.txt` where a test failed), `video-N.mp4`, `logcat.txt`.
The two timer tests wait for real time to pass, about two minutes each.
