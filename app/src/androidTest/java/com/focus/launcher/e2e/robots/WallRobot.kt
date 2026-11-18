package com.focus.launcher.e2e.robots

import androidx.test.uiautomator.By
import com.focus.launcher.e2e.support.Device
import com.focus.launcher.e2e.support.Evidence
import com.focus.launcher.e2e.support.FixtureApp
import com.focus.launcher.ui.TestTags
import kotlin.time.Duration

/** The "Time's up" wall. */
class WallRobot(private val evidence: Evidence) {
    // UI Automator's selectors change in place when narrowed (hasDescendant adds to the same
    // object), so every lookup starts from a fresh one.
    private fun wall() = By.res(TestTags.WALL)

    fun appearsWithin(timeout: Duration): WallRobot = apply {
        Device.waitFor(wall(), "The time's-up wall", timeout)
        evidence.step("wall")
    }

    /** Nothing interrupts the app for [duration]. */
    fun staysAwayFor(duration: Duration): WallRobot = apply {
        if (Device.appears(wall(), duration)) {
            evidence.step("wall-too-early")
            throw AssertionError("The wall came up within $duration, and should not have")
        }
    }

    /** The wall is about [app]: its big button closes that app. */
    fun isFor(app: FixtureApp): WallRobot = apply {
        Device.waitFor(wall().hasDescendant(By.text("Close ${app.label}")), "\"Close ${app.label}\" on the wall")
    }

    /** "Close …", the wall's main button. Focus's home page comes back. */
    fun close(app: FixtureApp) {
        Device.waitFor(wall().hasDescendant(By.text("Close ${app.label}")), "\"Close ${app.label}\" on the wall")
            .findObject(By.text("Close ${app.label}")).click()
        Device.waitGone(wall(), "The wall")
    }
}
