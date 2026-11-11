package com.focus.launcher.e2e.support

import android.app.UiAutomation
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The emulator, as UI Automator sees it. Robots and support code use this; journeys never do.
 * Every wait here waits for a condition, with a limit: there is no fixed pause anywhere.
 */
object Device {
    val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    val ui: UiDevice by lazy {
        // UI Automator switches every other accessibility service off while it runs, unless told
        // not to. Focus's own service is one of them, and the mid-session timer test needs it.
        Configurator.getInstance().uiAutomationFlags = UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    /** These tests rewrite Focus's settings and data, so they only ever run on the emulator. */
    val isEmulator: Boolean
        get() = Build.HARDWARE == "ranchu" || Build.HARDWARE == "goldfish" ||
            Build.PRODUCT.startsWith("sdk") || Build.FINGERPRINT.startsWith("generic")

    fun shell(command: String): String = ui.executeShellCommand(command)

    /** [selector] on screen within [timeout], or a failure that says what was in front instead. */
    fun waitFor(selector: BySelector, what: String, timeout: Duration = 20.seconds): UiObject2 =
        ui.wait(Until.findObject(selector), timeout.inWholeMilliseconds)
            ?: throw AssertionError("$what did not appear within $timeout (in front: ${ui.currentPackageName})")

    /** Whether [selector] shows up within [timeout]; for checking that something does NOT happen. */
    fun appears(selector: BySelector, timeout: Duration): Boolean =
        ui.wait(Until.hasObject(selector), timeout.inWholeMilliseconds) == true

    fun waitGone(selector: BySelector, what: String, timeout: Duration = 20.seconds) {
        if (ui.wait(Until.gone(selector), timeout.inWholeMilliseconds) != true) {
            throw AssertionError("$what was still on screen after $timeout")
        }
    }

    /** Polls [condition] until it holds, for state that is not on screen (a service starting, usage counting up). */
    fun waitUntil(what: String, timeout: Duration, every: Duration = 1.seconds, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeout.inWholeMilliseconds
        while (!condition()) {
            if (SystemClock.uptimeMillis() > end) throw AssertionError("$what: not within $timeout")
            SystemClock.sleep(every.inWholeMilliseconds)
        }
    }
}
