package com.focus.launcher.e2e.support

import android.content.ComponentName
import com.focus.launcher.Graph
import com.focus.launcher.data.Settings
import com.focus.launcher.service.FocusAccessibilityService
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

/**
 * Puts Focus into a known state, directly, inside its own running process: no restart, no clicking
 * through menus. This is the only shortcut the tests take. Everything a journey does and checks
 * after its "given" goes through the screen, the way a person would.
 */
object Given {
    /** Before every test: default settings, no introduction or tips, no passes, service off. */
    fun cleanStart() {
        Environment.ensureReady()
        Graph.settings.update { Settings() }
        Graph.state.tutorialSeen = true
        while (Graph.state.tip.value != null) Graph.state.nextTip()
        for (app in FixtureApp.entries) Graph.limits.clearPasses(app.packageName)
        Graph.limits.sessionConsent = null
        accessibilityService(on = false)
    }

    /** [apps] are the fast apps on the home page. */
    fun fastApps(vararg apps: FixtureApp) {
        val keys = apps.map { app -> Graph.apps.apps.value.first { it.packageName == app.packageName }.key }
        Graph.settings.update { it.copy(favorites = keys) }
    }

    /**
     * [app] gets its own daily limit, set so that at least [minutes] and less than [minutes] + 1
     * of it are left today, whatever earlier tests used. Returns the limit in milliseconds.
     */
    fun minutesLeftToday(app: FixtureApp, minutes: Int): Long {
        val used = Graph.usage.usageTodayMs(app.packageName, maxAgeMs = 0)
        val limit = ceil(used / 60_000.0).toInt() + minutes
        Graph.settings.update { it.copy(appLimits = it.appLimits + (app.packageName to limit)) }
        Graph.limits.clearPasses(app.packageName)
        return limit * 60_000L
    }

    /**
     * Switches Focus's accessibility service on or off and waits until Android has done it. The
     * setting is always written fresh: Android leaves a service unbound after its app was
     * force-stopped (every test run starts that way) even though the setting still names it, and
     * writing the same value again would change nothing.
     */
    fun accessibilityService(on: Boolean) {
        val me = ComponentName(Device.context, FocusAccessibilityService::class.java).flattenToString()
        Device.shell("settings delete secure enabled_accessibility_services")
        Device.waitUntil("Focus's accessibility service stopping", 15.seconds) { !FocusAccessibilityService.isRunning }
        if (!on) return
        Device.shell("settings put secure enabled_accessibility_services $me")
        Device.shell("settings put secure accessibility_enabled 1")
        Device.waitUntil("Focus's accessibility service starting", 15.seconds) { FocusAccessibilityService.isRunning }
    }

    /** Waits, in real time, until Android's usage log says [app] has used [limitMs] today. */
    fun timeRunsOut(app: FixtureApp, limitMs: Long) {
        Device.waitUntil("${app.label} using up its time", 3.minutes, every = 5.seconds) {
            Graph.usage.usageTodayMs(app.packageName, maxAgeMs = 0) >= limitMs
        }
    }
}
