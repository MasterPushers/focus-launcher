package com.focus.launcher.e2e.robots

import android.content.Intent
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import com.focus.launcher.SettingsActivity
import com.focus.launcher.e2e.support.Device
import com.focus.launcher.e2e.support.Evidence
import com.focus.launcher.e2e.support.FixtureApp
import com.focus.launcher.ui.settings.Routes

/** Settings → App timers → Limited apps: which apps a limit covers, and in which section. */
class LimitedAppsRobot(private val evidence: Evidence) {
    /** Straight to the page, the way Focus's own links open a settings page. */
    fun open(): LimitedAppsRobot = apply {
        Device.context.startActivity(
            SettingsActivity.intent(Device.context, Routes.TIMER_APPS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        Device.waitFor(By.text("Add a timer"), "The Limited apps page")
        evidence.step("limited-apps")
    }

    fun lists(app: FixtureApp, under: String): LimitedAppsRobot = apply {
        val found = sections()
        if (app.label !in found) throw AssertionError("${app.label} is not on the Limited apps page; expected it under \"$under\"")
        if (found[app.label] != under) throw AssertionError("${app.label} is listed under \"${found[app.label]}\", expected \"$under\"")
    }

    fun doesNotList(app: FixtureApp): LimitedAppsRobot = apply {
        val found = sections()
        if (app.label in found) throw AssertionError("${app.label} is listed under \"${found[app.label]}\", and should not be on this page")
    }

    /**
     * Reads the whole page, top to bottom: every app label on it, with the section heading above
     * it. Scrolls if the page is longer than the screen.
     */
    private fun sections(): Map<String, String?> {
        val out = LinkedHashMap<String, String?>()
        var current: String? = null
        val scroller = Device.ui.findObject(By.scrollable(true))
        repeat(20) {
            val labels = Device.ui.findObjects(By.text(java.util.regex.Pattern.compile(".+")))
                .filter { it.applicationPackage == Device.context.packageName }
                .sortedBy { it.visibleBounds.top }
            for (label in labels) {
                val text = label.text ?: continue
                // Section headings are drawn in capitals; compare without case.
                val heading = HEADINGS.firstOrNull { it.equals(text, ignoreCase = true) }
                if (heading != null) current = heading
                else if (FixtureApp.entries.any { it.label == text }) out[text] = current
            }
            if (scroller == null || !scroller.scroll(Direction.DOWN, 0.7f)) return out
        }
        return out
    }

    companion object {
        const val WITH_A_LIMIT = "With a daily limit"
        const val EXCLUDED = "Excluded from the automatic limits"
        const val NOT_SURE = "Not sure about these"
        private val HEADINGS = setOf(WITH_A_LIMIT, EXCLUDED, NOT_SURE)
    }
}
