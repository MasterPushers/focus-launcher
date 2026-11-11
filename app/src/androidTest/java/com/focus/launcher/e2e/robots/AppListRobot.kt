package com.focus.launcher.e2e.robots

import androidx.test.uiautomator.By
import com.focus.launcher.e2e.support.Device
import com.focus.launcher.e2e.support.Evidence
import com.focus.launcher.e2e.support.FixtureApp
import com.focus.launcher.ui.TestTags
import java.util.regex.Pattern

/** The app list, one swipe left of home. */
class AppListRobot(private val evidence: Evidence) {
    // UI Automator's selectors change in place when narrowed (hasDescendant adds to the same
    // object), so every lookup starts from a fresh one.
    private fun list() = By.res(TestTags.APP_LIST)
    private fun search() = By.res(TestTags.APP_SEARCH)

    fun isShown(): AppListRobot = apply {
        Device.waitFor(search(), "The app list's search field")
        evidence.step("app-list")
    }

    fun search(text: String): AppListRobot = apply {
        Device.waitFor(search(), "The app list's search field").text = text
        Device.waitFor(list().hasDescendant(By.text("Clear")), "The search taking \"$text\"")
        evidence.step("search-" + text.replace(' ', '-'))
    }

    fun shows(app: FixtureApp): AppListRobot = apply {
        Device.waitFor(list().hasDescendant(By.text(app.label)), "${app.label} in the app list")
    }

    /** [app] is the top result: the one the keyboard's Go key opens. */
    fun firstResultIs(app: FixtureApp): AppListRobot = apply {
        val field = Device.waitFor(search(), "The app list's search field").visibleBounds
        val row = Device.waitFor(list().hasDescendant(By.text(app.label)), "${app.label} in the app list")
            .findObject(By.text(app.label)).visibleBounds
        val above = Device.ui.findObjects(By.pkg(Device.context.packageName).text(Pattern.compile(".+")))
            .filter { it.visibleBounds.top > field.bottom && it.visibleBounds.top < row.top }
        if (above.isNotEmpty()) throw AssertionError("${app.label} is not the first result: \"${above.first().text}\" is above it")
    }

    fun doesNotShow(app: FixtureApp): AppListRobot = apply {
        val found = Device.ui.findObject(list().hasDescendant(By.text(app.label)))
        if (found != null) throw AssertionError("${app.label} is in the app list, and should not be")
    }

    /** The search field is empty: no "Clear" next to it. */
    fun searchIsEmpty(): AppListRobot = apply {
        Device.waitFor(search(), "The app list's search field")
        if (Device.ui.findObject(list().hasDescendant(By.text("Clear"))) != null) {
            throw AssertionError("The search still holds text from before")
        }
    }

    /** Taps the app's row. Whether the app or the wall comes up is for the next robot to say. */
    fun open(app: FixtureApp) {
        Device.waitFor(list().hasDescendant(By.text(app.label)), "${app.label} in the app list")
            .findObject(By.text(app.label)).click()
    }
}
