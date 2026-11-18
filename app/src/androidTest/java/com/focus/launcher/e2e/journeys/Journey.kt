package com.focus.launcher.e2e.journeys

import com.focus.launcher.e2e.robots.AppListRobot
import com.focus.launcher.e2e.robots.AppRobot
import com.focus.launcher.e2e.robots.HomeRobot
import com.focus.launcher.e2e.robots.LimitedAppsRobot
import com.focus.launcher.e2e.robots.WallRobot
import com.focus.launcher.e2e.support.EmulatorOnly
import com.focus.launcher.e2e.support.Evidence
import com.focus.launcher.e2e.support.Given
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain

/**
 * Every journey starts the same way: on Focus's home page, with default settings, whatever the test
 * before it did. Focus is not restarted for that; its state is reset in place (see [Given]).
 *
 * The rules for writing one are in e2e/README.md; e2e/check-test-rules.sh enforces the mechanical ones.
 */
abstract class Journey {
    protected val evidence = Evidence()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(EmulatorOnly()).around(evidence)

    protected val home = HomeRobot(evidence)
    protected val appList = AppListRobot(evidence)
    protected val app = AppRobot(evidence)
    protected val wall = WallRobot(evidence)
    protected val limitedApps = LimitedAppsRobot(evidence)

    @Before
    fun startOnTheHomePage() {
        Given.cleanStart()
        home.pressHome().isShown()
    }

    /** Leaves the emulator as it found it: the next test, or the next run, starts clean. */
    @After
    fun switchTheServiceOff() {
        Given.accessibilityService(on = false)
    }
}
