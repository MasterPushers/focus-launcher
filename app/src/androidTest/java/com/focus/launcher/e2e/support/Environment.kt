package com.focus.launcher.e2e.support

import com.focus.launcher.Graph
import kotlin.time.Duration.Companion.seconds

/**
 * Once per run: Focus is the home screen and may read usage (what a user would have allowed), and
 * the fixture apps are in place. An emulator only; on a phone these switches are the owner's to flip.
 */
object Environment {
    private var ready = false

    fun ensureReady() {
        if (ready) return
        for (app in FixtureApp.entries) {
            check(Device.shell("pm path ${app.packageName}").isNotBlank()) {
                "${app.label} (${app.packageName}) is not installed. Run the tests with e2e/run-on-emulator.sh, which installs it."
            }
        }
        val me = Device.context.packageName
        // Making Focus the home screen is e2e/run-on-emulator.sh's job, before the tests start:
        // Android restarts the home screen a few seconds after that changes, which would wipe
        // whatever a test is doing at that moment.
        val home = Device.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
        check("$me/" in home) { "Focus is not the home app on this emulator. Run the tests with e2e/run-on-emulator.sh, which makes it one." }
        Device.shell("appops set $me GET_USAGE_STATS allow")
        Device.ui.wakeUp()
        Device.shell("wm dismiss-keyguard")
        Device.waitUntil("Focus seeing the fixture apps", 20.seconds) {
            FixtureApp.entries.all { app -> Graph.apps.apps.value.any { it.packageName == app.packageName } }
        }
        ready = true
    }
}
