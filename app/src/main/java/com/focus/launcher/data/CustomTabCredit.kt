package com.focus.launcher.data

/**
 * Credits the time spent in a browser's Custom Tab (the in-app browser that news readers, mail and
 * chat apps open links in) to the app that opened it, as Android's own Digital Wellbeing does
 * (issue 51). The system reports a Custom Tab as an activity of the browser, so without this the
 * time went to the browser: wrong in the review, and an app's timer never saw its links.
 *
 * Android knows which app owns the task, but only system apps may ask. What any app can see is the
 * activity's class name, and every Custom Tab has "customtab" in it (Chromium-based browsers:
 * `org.chromium.chrome.browser.customtabs.CustomTabActivity`; Firefox and its forks:
 * `org.mozilla.fenix.customtabs.ExternalAppBrowserActivity`). The app in front just before a Custom
 * Tab opens is the one that asked for it. Browsers first pass the request through an invisible
 * activity of their own (Firefox: `IntentReceiverActivity`, seen on a test device), so when the
 * browser itself came to the front a moment earlier, the app before it is the one. Coming back to
 * the tab later (from Recents, the home screen) keeps that owner; a Custom Tab nobody can be found
 * for stays the browser's.
 *
 * A pure state machine, like [ForegroundTracker], which it sits in front of: feed it every
 * RESUMED in order (and every PAUSED) and pass on the package it returns. [isHome] = home screens
 * and the system UI, which are never the owner.
 */
class CustomTabCredit(private val isHome: (String) -> Boolean) {
    /** The last package that came to the front with an ordinary activity, home screens included; when; and the one before it. */
    private var front: String? = null
    private var frontSince = 0L
    private var beforeFront: String? = null
    /** "browser/class" → the package its time is credited to. */
    private val owners = HashMap<String, String>()

    fun resumed(pkg: String, cls: String?, ts: Long): String {
        if (!isCustomTab(cls)) {
            if (pkg != front) {
                beforeFront = front
                front = pkg
                frontSince = ts
            }
            return pkg
        }
        val key = "$pkg/$cls"
        val opener = when {
            front != pkg -> front
            ts - frontSince <= HAND_OFF_MS -> beforeFront // through the browser's own hand-off activity
            else -> null // from the browser itself to a tab already open: keep its owner
        }?.takeIf { it != pkg && !isHome(it) }
        val owner = opener ?: owners[key] ?: pkg
        owners[key] = owner
        return owner
    }

    fun paused(pkg: String, cls: String?): String =
        if (isCustomTab(cls)) owners["$pkg/$cls"] ?: pkg else pkg

    companion object {
        /** How long after the browser came to the front its Custom Tab still counts as handed on from the app before. */
        const val HAND_OFF_MS = 3_000L

        fun isCustomTab(cls: String?): Boolean = cls != null && cls.contains("customtab", ignoreCase = true)
    }
}
