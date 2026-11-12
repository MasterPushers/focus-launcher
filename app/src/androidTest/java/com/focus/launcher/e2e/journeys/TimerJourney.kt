package com.focus.launcher.e2e.journeys

import com.focus.launcher.e2e.support.FixtureApp.GAME
import com.focus.launcher.e2e.support.Given
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

/**
 * The two layers of a daily limit, each on its own (2-overview/app.md, "Timers work in two
 * layers"). These wait for real time to pass: about two minutes each.
 */
class TimerJourney : Journey() {
    /** Usage access alone: a game whose time is up does not open from Focus; the wall comes instead. */
    @Test
    fun theWallComesInsteadOfAGameWhoseTimeIsUp() {
        val limit = Given.minutesLeftToday(GAME, 1)
        home.openAppList().search("dummy g").open(GAME)
        app.isOpen(GAME)
        Given.timeRunsOut(GAME, limit)
        // Without the accessibility service nothing interrupts the visit itself.
        wall.staysAwayFor(5.seconds)
        home.pressHome().isShown().openAppList().search("dummy g").open(GAME)
        wall.appearsWithin(10.seconds).isFor(GAME).close(GAME)
        home.isShown()
    }

    /** With the accessibility service, the wall comes up over the game while it is being played. */
    @Test
    fun theServiceStopsAGameWhenItsTimeRunsOut() {
        Given.accessibilityService(on = true)
        Given.minutesLeftToday(GAME, 1)
        home.openAppList().search("dummy g").open(GAME)
        app.isOpen(GAME)
        // At least a minute is left: the wall must not come early...
        wall.staysAwayFor(30.seconds)
        // ...and must come once the time is used.
        wall.appearsWithin(2.minutes).isFor(GAME).close(GAME)
        home.isShown()
    }
}
