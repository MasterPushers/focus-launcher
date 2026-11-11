# Tier 3 · Emulator tests and the /test-build command

Asked for by the owner on 2026-11-11: a maintainer-only `/test-build` comment that builds a pull
request and tests it on emulators with screenshots and video; then "real" end-to-end tests with an
architecture that keeps them correct and avoids "closing and opening the app or moving between
screens too much". Contributor-facing rules: `e2e/README.md` (keep the two in step).

## Architecture
- **Tool:** UI Automator 2.4.1 in Kotlin, `app/src/androidTest/java/com/focus/launcher/e2e/`.
  Chosen over Maestro: in the repo, built by Gradle, no outside CLI in CI, and setup can reach
  into the app directly. AndroidX test libs: runner 1.7.0, ext-junit 1.3.0 (minCompileSdk ≤ 34,
  fine with compileSdk 36 / AGP 8.13; checked from the AARs' `aar-metadata.properties`).
- **Layers:** `journeys/` (tests: robots and `Given` only) → `robots/` (one per screen; the only
  code that finds, taps, swipes, waits) → `support/` (`Device`, `Given`, `Environment`,
  `Evidence`, `EmulatorOnly`, `FixtureApp`). `Journey` base class: rule chain
  `EmulatorOnly` → `Evidence`; `@Before` = `Given.cleanStart()` + Home + home page shown.
- **One process, one start per run.** Instrumentation runs inside Focus's process (no
  orchestrator). `Given` resets in place: `Settings()` defaults, `tutorialSeen = true`, all tips
  skipped, passes cleared for the fixtures, accessibility service off (always rewritten, see
  lessons). `Journey`'s `@After` switches the service off. No reinstall or data clear between tests.
- **The runner owns the device setup:** animations off, stay awake, installs, Focus as home
  (only when it is not already: changing it restarts the home screen seconds later), the
  accessibility setting cleared, then one `am instrument`. `Environment` only verifies.
- **Fixtures** (`e2e/fixtures/`, a separate Gradle build so the app's release and F-Droid build
  never see it; `./gradlew -p e2e/fixtures assembleDebug`): `dev.focus.fixture.game` "Dummy Game"
  (`appCategory="game"`) and `dev.focus.fixture.post` "Dummy Mail" (`appCategory="social"` + a
  `SENDTO mailto:` filter; the package name avoids the "mail" hint on purpose, so only the probe
  can classify it). Each shows "<label> is open".
- **Test tags:** `ui/TestTags.kt` (`home_page`, `app_list`, `app_search`, `wall`); `FocusTheme`'s
  root sets `testTagsAsResourceId = true`, so UI Automator finds them as resource ids. Off-screen
  pager pages are not reported (not visible to user), so `home_page` / `app_list` tell which page
  is showing.
- **Evidence:** `Evidence.step(name)` after each confirmed screen →
  `/sdcard/Android/data/com.focus.launcher/files/e2e/<Journey>.<test>/NN-name.png`; on failure
  `FAILED.png`, `FAILED-screen.xml` (window dump), `FAILED.txt`. `adb pull` of that folder works
  on API 36.
- **Guards:** `EmulatorOnly` (outermost rule, `Build.HARDWARE` ranchu/goldfish …) skips every
  test on a phone; `e2e/run-on-emulator.sh` refuses a non-emulator (`ro.kernel.qemu`/`ro.hardware`).
  Never `./gradlew connectedDebugAndroidTest`: it installs on every attached device and
  uninstalls afterwards.
- **Rules check:** `e2e/check-test-rules.sh`, run by the Build workflow on every PR: journeys may
  not use `By.`/`Until.`/`UiDevice`/`Device.`/`click`/`swipe`/`.text =`; no sleeps in journeys or
  robots; no coordinates in robots; no `val x = By.…` anywhere. Proven to fire on a planted file.

## The journeys (7)
| Test | What it proves |
| --- | --- |
| `HomeJourney.homeButtonBringsBackTheHomePageFromTheAppList` | Home from the app list returns to page 0 |
| `HomeJourney.aFastAppOpensFromTheHomePage` | a fast app (Dummy Mail) opens, Home comes back |
| `AppListJourney.searchFindsAnAppAndOpensIt` | "dummy mail" puts Dummy Mail first, not Dummy Game; it opens; back out with **Back** (Home clears the search on its own path and would hide a broken reset), the search is empty |
| `TimerJourney.theWallComesInsteadOfAGameWhoseTimeIsUp` | gate only (service off): nothing interrupts the visit; reopening after the time is used shows the wall naming the game |
| `TimerJourney.theServiceStopsAGameWhenItsTimeRunsOut` | service on: no wall in the first 30 s, the wall over the game within 2 min |
| `ClassificationJourney.aGameGetsTheAutomaticLimit` | Dummy Game under "With a daily limit" |
| `ClassificationJourney.aMailAppThatAndroidCallsSocialIsNeverLimited` | Dummy Mail nowhere on Limited apps (not limited, not "Not sure") |

`Given.minutesLeftToday(app, 1)` sets the app's own limit to ceil(used today) + 1 minute, so a
timer test works whatever earlier runs used; `Given.timeRunsOut` polls
`UsageRepository.usageTodayMs` (it counts the app still in front) every 5 s, up to 3 min.
A full run on the local API 36 emulator takes about 260 s; default waits are 20 s (CI emulators
are slower than the Mac).

## Each test seen failing (2026-11-11, local API 36)
A throwaway script broke one feature at a time, rebuilt, ran only that test, and restored the
file. All 7 were caught: Home press ignored on the app list · `start()` never launching · search
not cleared on leaving Focus (missed at first while the test came back with Home; caught after
switching to Back) · gate never blocking · service never starting the wall · games not
recognised · the `mailto:` probe removed (Dummy Mail then sat under "Not sure about these").

## The flaky service test (owner: "we don't want flaky test, keep chasing it")
`theServiceStopsAGameWhenItsTimeRunsOut` failed 2 times in about 17 full runs (wall never came;
the game stayed in front for 2.5 min). Two real setup races were found and fixed on the way (the
unbound service after a force-stop; the home-screen restart; see lessons). The second failure came
after the first fix and is **not explained**. Since then: 0 failures in 11 full runs and 6 solo
runs with a temporary logging build, 7 full runs in a row with the shipping build, and 1 run on
the code pushed to main. Theory not confirmed: right after the service is switched on, Android
re-binds it or drops the app's window event, and the new instance never looks at the app already
in front (the service has no "evaluate what is in front" on connect). If it fails again:
`logcat.txt` has `FocusTimer` lines (`connected`, `<pkg>: wall in N s`, `<pkg>: wall`,
`result dropped`), and the failure folder has `FAILED-accessibility.txt` (`dumpsys accessibility`).
Two `connected` lines in one test would confirm the re-bind theory; then evaluate the app in front
(usage log `lastResumedPackage`) in `onServiceConnected`.

## /test-build (`.github/workflows/test-build.yml`)
- Triggers: `issue_comment` (`/test-build [api …]`, first line) on an open PR, or
  `workflow_dispatch` (`pr`, `apis`). API levels 26–36, at most four, default `26 36`.
- **authorize** (trusted): `collaborators/<user>/permission` must be `admin` or `write`; anyone
  else: exit 0, nothing runs. Reacts 👀 (or "confused" on a bad command). Resolves the PR head SHA.
- **build**: checkout of that SHA (no credentials) → copied to `$RUNNER_TEMP/src`, plus a minimal
  SDK copy (licenses, `platforms/android-<compileSdk>`, `build-tools/3[5-9].*`) → built in
  `eclipse-temurin:21-jdk` with git installed: unit tests, debug APK, test APK, fixtures. APKs
  are taken out only if `realpath` stays inside the copy and they are regular files; `aapt2`
  confirms `com.focus.launcher`. Artifact `apk`.
- **emulator** (matrix): sparse checkout of `e2e/` **from main**, `reactivecircus/
  android-emulator-runner` v2.38.0 (x86_64, google_apis, `pixel_6`, KVM, animations off) runs
  `e2e/run-on-emulator.sh apk results`; artifact `android-api-<N>`.
- **report**: comment on the PR with the verdict per job and the run link.
- **Why the container:** a comment workflow runs in main's context; caches it writes are
  visible to builds on main, including Publish (which signs). PR Gradle files are code, so they
  run only in a container with no token, no cache and no runner access; nothing from the PR runs
  on the host. Residual: the PR head is resolved seconds after the comment (a push in between
  would be tested instead); the result comment names the SHA.
- GitHub runs `issue_comment` and `workflow_dispatch` workflows from the default branch only:
  the workflow does nothing until it is on `main`. Untested on GitHub as of 2026-11-11.

## Commands
```bash
export ANDROID_SERIAL=emulator-5554
./gradlew -D… :app:assembleDebug :app:assembleDebugAndroidTest && ./gradlew -D… -p e2e/fixtures assembleDebug
# copy the four APKs into one folder, then:
e2e/run-on-emulator.sh <apks> <results>                                    # all
E2E_TEST=com.focus.launcher.e2e.journeys.TimerJourney e2e/run-on-emulator.sh <apks> <results>   # one
# local emulator (the shell's ANDROID_HOME points at the Homebrew SDK, which has no system image):
ANDROID_HOME=~/Library/Android/sdk ANDROID_SDK_ROOT=~/Library/Android/sdk ~/Library/Android/sdk/emulator/emulator \
  -avd Pixel_API_36 -no-window -no-snapshot-save -no-boot-anim -noaudio -gpu swiftshader_indirect
```
