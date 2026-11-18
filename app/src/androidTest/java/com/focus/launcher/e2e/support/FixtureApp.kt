package com.focus.launcher.e2e.support

/**
 * The two apps from e2e/fixtures, installed next to Focus by e2e/run-on-emulator.sh. They make the
 * tests independent of whatever apps an emulator image happens to have.
 */
enum class FixtureApp(val packageName: String, val label: String) {
    /** Android files it as a game: Focus gives it the automatic daily limit. */
    GAME("dev.focus.fixture.game", "Dummy Game"),

    /** Android files it as "social", but it handles mailto: links. Focus must never limit it on its own. */
    MAIL("dev.focus.fixture.post", "Dummy Mail");

    /** What the fixture shows while it is in front. */
    val openText: String get() = "$label is open"
}
