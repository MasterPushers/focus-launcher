package com.focus.launcher.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.focus.launcher.data.ACTION_DRAWER
import com.focus.launcher.data.ACTION_LOCK
import com.focus.launcher.data.ACTION_NOTHING
import com.focus.launcher.data.ACTION_NOTIFICATIONS
import com.focus.launcher.data.ACTION_SEARCH
import com.focus.launcher.data.ACTION_SETTINGS
import com.focus.launcher.data.ACTION_WEB_SEARCH
import com.focus.launcher.data.AppEntry
import com.focus.launcher.service.FocusAccessibilityService
import com.focus.launcher.ui.openWebSearch

/**
 * The four swipes on the home screen each run one action. An action is one of the `ACTION_*`
 * strings in [com.focus.launcher.data.Settings], or an [AppEntry.key] to open that app; anything
 * else is left alone, so a saved choice for an app that has since been uninstalled does nothing.
 */
fun homeActionLabel(spec: String, apps: List<AppEntry>): String = when (spec) {
    ACTION_NOTHING -> "Nothing"
    ACTION_DRAWER -> "App drawer"
    ACTION_SEARCH -> "App search"
    ACTION_WEB_SEARCH -> "Web search"
    ACTION_NOTIFICATIONS -> "Notifications"
    ACTION_SETTINGS -> "Focus settings"
    ACTION_LOCK -> "Lock the screen"
    else -> apps.firstOrNull { it.key == spec }?.label ?: "Not set"
}

/** Runs one swipe action. */
fun runHomeAction(
    context: Context,
    spec: String,
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: (route: String?) -> Unit,
) {
    when (spec) {
        ACTION_NOTHING -> Unit
        ACTION_DRAWER -> onOpenDrawer(false)
        // The drawer with the keyboard up: the same place "swipe up" has always gone to.
        ACTION_SEARCH -> onOpenDrawer(true)
        ACTION_WEB_SEARCH -> openWebSearch(context)
        ACTION_NOTIFICATIONS -> expandNotifications(context)
        ACTION_SETTINGS -> onOpenSettings(null)
        ACTION_LOCK -> if (!FocusAccessibilityService.lockScreen()) {
            Toast.makeText(context, "Turn on the Focus timer service to lock with a gesture", Toast.LENGTH_SHORT).show()
        }
        else -> apps.firstOrNull { it.key == spec }?.let(onLaunch)
    }
}

/** Pulls the notification shade down: via the timer service when it is on, else the status-bar service. */
@SuppressLint("WrongConstant", "PrivateApi")
internal fun expandNotifications(context: Context) {
    if (FocusAccessibilityService.openNotifications()) return
    try {
        val statusBar = context.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(statusBar)
    } catch (_: Exception) {
    }
}
