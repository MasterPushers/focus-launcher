package com.focus.launcher.e2e.robots

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import com.focus.launcher.e2e.support.Device
import com.focus.launcher.e2e.support.Evidence
import com.focus.launcher.e2e.support.FixtureApp
import com.focus.launcher.ui.TestTags

/** Focus's home page. */
class HomeRobot(private val evidence: Evidence) {
    // UI Automator's selectors change in place when narrowed (hasDescendant adds to the same
    // object), so every lookup starts from a fresh one.
    private fun page() = By.res(TestTags.HOME_PAGE)

    /** The Home button, from wherever the test is. */
    fun pressHome(): HomeRobot = apply { Device.ui.pressHome() }

    fun isShown(): HomeRobot = apply {
        Device.waitFor(page(), "Focus's home page")
        evidence.step("home")
    }

    /** One swipe to the left, as a thumb would do it. */
    fun openAppList(): AppListRobot {
        val home = Device.waitFor(page(), "Focus's home page")
        home.swipe(Direction.LEFT, 0.8f)
        return AppListRobot(evidence).isShown()
    }

    fun openFastApp(app: FixtureApp) {
        Device.waitFor(page().hasDescendant(By.text(app.label)), "${app.label} among the fast apps")
            .findObject(By.text(app.label)).click()
    }
}
