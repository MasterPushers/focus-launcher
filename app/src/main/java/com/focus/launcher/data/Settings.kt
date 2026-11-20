package com.focus.launcher.data

import org.json.JSONArray
import org.json.JSONObject

enum class FontChoice(val label: String) { SANS("Sans"), SERIF("Serif"), MONO("Mono") }

/**
 * SPLIT: the clock on the left, one section on the right, a single vertical line between them and
 * no frame around it. RING: the clock inside a circle whose arc is the battery (or the day).
 */
enum class ClockStyle(val label: String) { SPLIT("Split"), RING("Ring"), PLAIN("Plain") }

/** What the right half of the split clock shows. The section shown there is not repeated below. */
enum class SplitSide(val label: String) { CALENDAR("Calendar"), SCREEN_TIME("Screen time") }

/** What the arc of the clock ring stands for. */
enum class RingMode(val label: String) { BATTERY("Battery level"), DAY("Day passed") }

/** How an app appears when it is opened from the launcher. */
enum class LaunchAnimation(val label: String) { FAST("Fast"), SYSTEM("System default") }

enum class HomeAlign(val label: String) { LEFT("Left"), CENTER("Center"), RIGHT("Right") }

/**
 * Where the fast apps sit on a tall home screen: HIGH just under the clock and the sections,
 * MIDDLE where the original layout puts them, LOW just above the corner shortcuts.
 */
enum class AppBlock(val label: String) { HIGH("High"), MIDDLE("Middle"), LOW("Low") }

/** Order of the app list in the drawer. */
enum class DrawerSort(val label: String) { ALPHA("A–Z"), MOST_USED("Most used"), RECENT("Recent") }

enum class TimeFormat(val label: String) { SYSTEM("Follow system"), H24("24-hour"), H12("12-hour") }

const val MAX_FAVORITES = 5

/** Corner shortcuts that resolve to whatever the phone's default dialer / camera is. */
const val SHORTCUT_PHONE = "auto:phone"
const val SHORTCUT_CAMERA = "auto:camera"

/** What a tap on the clock does. Anything that is not one of these is an [AppEntry.key] to open. */
const val TAP_ALARMS = "tap:alarms"
const val TAP_CALENDAR = "tap:calendar"
const val TAP_SCREEN_TIME = "tap:screentime"
const val TAP_BATTERY = "tap:battery"
const val TAP_NOTHING = "tap:none"

/**
 * What a swipe on the home screen does. A swipe action is one of these strings, or an
 * [AppEntry.key] to open; [ACTION_NOTHING] leaves the swipe unused.
 */
const val ACTION_NOTHING = "act:none"
const val ACTION_DRAWER = "act:drawer"
const val ACTION_SEARCH = "act:search"
const val ACTION_WEB_SEARCH = "act:websearch"
const val ACTION_NOTIFICATIONS = "act:notifications"
const val ACTION_SETTINGS = "act:settings"
const val ACTION_LOCK = "act:lock"

/**
 * Every user-facing preference of the launcher. Immutable; changed through [SettingsStore.update].
 *
 * App references ([favorites], [hidden], [renames], shortcuts) use [AppEntry.key].
 * Timers ([appLimits]) are keyed by package name because usage is tracked per package.
 */
data class Settings(
    // Appearance
    val dark: Boolean = true,
    val font: FontChoice = FontChoice.SANS,
    val textScale: Float = 1f,
    val hideStatusBar: Boolean = false,
    val launchAnimation: LaunchAnimation = LaunchAnimation.FAST,
    /** Draw the phone's wallpaper behind the home screen, still in black and white text. */
    val showWallpaper: Boolean = false,
    /** Veil over the wallpaper (the theme's background colour), so the text stays readable. */
    val wallpaperDim: Float = 0.55f,

    // Home
    val clockStyle: ClockStyle = ClockStyle.SPLIT,
    /** Right half of the split clock. CALENDAR needs [showCalendar]; without it screen time is shown. */
    val splitSide: SplitSide = SplitSide.CALENDAR,
    val ringMode: RingMode = RingMode.BATTERY,
    val clockTap: String = TAP_ALARMS,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM,
    val showDate: Boolean = true,
    val showCalendar: Boolean = false,
    /** [CalendarInfo.key] of the single calendar shown on the home screen, or [CALENDAR_AUTO] / [CALENDAR_ALL]. */
    val calendarKey: String = CALENDAR_AUTO,
    /** The Mon-Sun strip with today marked. Off: the ring already carries the date. */
    val showWeekStrip: Boolean = false,
    /** Previous, play or pause, next, and what is playing, as a section of the home screen. */
    val showMusic: Boolean = false,
    /**
     * The music section leaves the home screen while nothing is playing and comes back with the
     * music (the owner's wish). Off = it is always there, as a way into the music app.
     */
    val musicAutoHide: Boolean = true,
    /** A few lines of the user's own as a section of the home screen; [note] is the text. */
    val showNote: Boolean = false,
    val note: String = "",
    /** [AppEntry.key] opened by a tap on the music section while nothing is playing. "" = not chosen yet: the first tap asks. */
    val musicApp: String = "",
    /** [AppEntry.key] of the notes app offered next to the note's title; "" = none. */
    val noteApp: String = "",
    /** With a notes app chosen: a link to one page in it (what its "copy link" gives). Blank = the app's own start screen. */
    val noteLink: String = "",
    val homeAlign: HomeAlign = HomeAlign.CENTER,
    /** Where the fast apps sit in the empty height of a tall screen. */
    val appBlock: AppBlock = AppBlock.MIDDLE,
    /** Extra space between two fast apps, in dp: 0 keeps the list tight. */
    val fastAppSpacing: Int = 0,
    val favorites: List<String> = emptyList(),
    val showShortcuts: Boolean = true,
    val leftShortcut: String = SHORTCUT_PHONE,
    val rightShortcut: String = SHORTCUT_CAMERA,

    // Drawer
    val autoKeyboard: Boolean = false,
    val autoLaunch: Boolean = false,
    val showRecentInstalls: Boolean = true,
    val showUsageInDrawer: Boolean = true,
    val drawerSort: DrawerSort = DrawerSort.ALPHA,
    val hidden: Set<String> = emptySet(),
    val renames: Map<String, String> = emptyMap(),

    // App timers
    val timersEnabled: Boolean = true,
    /** Daily minutes applied to every social app without its own limit. 0 = off. */
    val socialDefaultMin: Int = 30,
    /** Daily minutes applied to every game without its own limit. 0 = off. */
    val gameDefaultMin: Int = 30,
    /** Daily minutes applied to every video / streaming app without its own limit. 0 = off. */
    val videoDefaultMin: Int = 0,
    /** package -> daily minutes. 0 means "explicitly unlimited", overriding the category default. */
    val appLimits: Map<String, Int> = emptyMap(),
    val allowContinue: Boolean = true,
    val continueOptions: List<Int> = listOf(1, 5, 15),
    val allowBypass: Boolean = true,
    /** After "ignore the limit for today", still ask before every single open of that app. */
    val askAfterBypass: Boolean = true,
    /** Seconds the "continue" choices stay locked after the wall appears. */
    val frictionSeconds: Int = 5,
    /** Heads-up this many minutes before a limit runs out. 0 = off. */
    val warnMinutes: Int = 1,

    // Weekly review
    val weeklyEnabled: Boolean = true,
    val weekStartsMonday: Boolean = true,
    val reviewHour: Int = 20,

    // Gestures
    /** What a swipe towards the page left of home (finger moves left) does. */
    val swipeLeftAction: String = ACTION_DRAWER,
    /** What a swipe towards the page right of home (finger moves right) does. */
    val swipeRightAction: String = ACTION_WEB_SEARCH,
    /** What a swipe up on empty space does. */
    val swipeUpAction: String = ACTION_SEARCH,
    /** What a swipe down on empty space does. */
    val swipeDownAction: String = ACTION_NOTIFICATIONS,
    val doubleTapLock: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("v", SCHEMA)
        put("dark", dark)
        put("font", font.name)
        put("textScale", textScale.toDouble())
        put("hideStatusBar", hideStatusBar)
        put("launchAnimation", launchAnimation.name)
        put("showWallpaper", showWallpaper)
        put("wallpaperDim", wallpaperDim.toDouble())

        put("clockStyle", clockStyle.name)
        put("splitSide", splitSide.name)
        put("ringMode", ringMode.name)
        put("clockTap", clockTap)
        put("timeFormat", timeFormat.name)
        put("showDate", showDate)
        put("showCalendar", showCalendar)
        put("calendarKey", calendarKey)
        put("showWeekStrip", showWeekStrip)
        put("showMusic", showMusic)
        put("musicAutoHide", musicAutoHide)
        put("showNote", showNote)
        put("note", note)
        put("musicApp", musicApp)
        put("noteApp", noteApp)
        put("noteLink", noteLink)
        put("homeAlign", homeAlign.name)
        put("appBlock", appBlock.name)
        put("fastAppSpacing", fastAppSpacing)
        put("favorites", JSONArray(favorites))
        put("showShortcuts", showShortcuts)
        put("leftShortcut", leftShortcut)
        put("rightShortcut", rightShortcut)

        put("autoKeyboard", autoKeyboard)
        put("autoLaunch", autoLaunch)
        put("showRecentInstalls", showRecentInstalls)
        put("showUsageInDrawer", showUsageInDrawer)
        put("drawerSort", drawerSort.name)
        put("hidden", JSONArray(hidden.toList()))
        put("renames", JSONObject(renames))

        put("timersEnabled", timersEnabled)
        put("socialDefaultMin", socialDefaultMin)
        put("gameDefaultMin", gameDefaultMin)
        put("videoDefaultMin", videoDefaultMin)
        put("appLimits", JSONObject(appLimits))
        put("allowContinue", allowContinue)
        put("continueOptions", JSONArray(continueOptions))
        put("allowBypass", allowBypass)
        put("askAfterBypass", askAfterBypass)
        put("frictionSeconds", frictionSeconds)
        put("warnMinutes", warnMinutes)

        put("weeklyEnabled", weeklyEnabled)
        put("weekStartsMonday", weekStartsMonday)
        put("reviewHour", reviewHour)

        put("swipeLeftAction", swipeLeftAction)
        put("swipeRightAction", swipeRightAction)
        put("swipeUpAction", swipeUpAction)
        put("swipeDownAction", swipeDownAction)
        put("doubleTapLock", doubleTapLock)
    }

    companion object {
        /** Tolerant of missing keys, so older saved settings survive app updates. */
        /** Version of the stored settings. Bump it only when an old stored value has to be reinterpreted. */
        const val SCHEMA = 2

        fun fromJson(o: JSONObject): Settings {
            val d = Settings()
            return Settings(
                dark = o.optBoolean("dark", d.dark),
                font = enumOr(o.optString("font"), d.font),
                textScale = o.optDouble("textScale", d.textScale.toDouble()).toFloat().coerceIn(0.8f, 1.4f),
                hideStatusBar = o.optBoolean("hideStatusBar", d.hideStatusBar),
                launchAnimation = enumOr(o.optString("launchAnimation"), d.launchAnimation),
                showWallpaper = o.optBoolean("showWallpaper", d.showWallpaper),
                wallpaperDim = o.optDouble("wallpaperDim", d.wallpaperDim.toDouble()).toFloat()
                    .takeIf { it.isFinite() }?.coerceIn(0f, 0.9f) ?: d.wallpaperDim,

                // Schema 2 made the split clock the default, at the owner's request. A ring stored by an
                // older version was the old default, not a choice, so it moves along once; the ring stays
                // one tap away in Settings. A stored "Plain" was a choice and is kept.
                clockStyle = enumOr(o.optString("clockStyle"), d.clockStyle)
                    .let { if (o.optInt("v", 1) < 2 && it == ClockStyle.RING) ClockStyle.SPLIT else it },
                splitSide = enumOr(o.optString("splitSide"), d.splitSide),
                ringMode = enumOr(o.optString("ringMode"), d.ringMode),
                clockTap = o.optString("clockTap", d.clockTap).ifEmpty { d.clockTap },
                timeFormat = enumOr(o.optString("timeFormat"), d.timeFormat),
                showDate = o.optBoolean("showDate", d.showDate),
                showCalendar = o.optBoolean("showCalendar", d.showCalendar),
                calendarKey = o.optString("calendarKey").ifEmpty {
                    // Written for a few hours by an earlier build as a bare personal-calendar id.
                    when (val legacy = o.optLong("calendarId", -1L)) {
                        -1L -> CALENDAR_AUTO
                        -2L -> CALENDAR_ALL
                        else -> "p:$legacy"
                    }
                },
                showWeekStrip = o.optBoolean("showWeekStrip", d.showWeekStrip),
                showMusic = o.optBoolean("showMusic", d.showMusic),
                musicAutoHide = o.optBoolean("musicAutoHide", d.musicAutoHide),
                showNote = o.optBoolean("showNote", d.showNote),
                note = o.optString("note", d.note),
                musicApp = o.optString("musicApp", d.musicApp),
                noteApp = o.optString("noteApp", d.noteApp),
                noteLink = o.optString("noteLink", d.noteLink),
                homeAlign = enumOr(o.optString("homeAlign"), d.homeAlign),
                appBlock = enumOr(o.optString("appBlock"), d.appBlock),
                fastAppSpacing = o.optInt("fastAppSpacing", d.fastAppSpacing).coerceIn(0, 24),
                favorites = o.optJSONArray("favorites").strings().take(MAX_FAVORITES),
                showShortcuts = o.optBoolean("showShortcuts", d.showShortcuts),
                leftShortcut = o.optString("leftShortcut", d.leftShortcut),
                rightShortcut = o.optString("rightShortcut", d.rightShortcut),

                autoKeyboard = o.optBoolean("autoKeyboard", d.autoKeyboard),
                autoLaunch = o.optBoolean("autoLaunch", d.autoLaunch),
                showRecentInstalls = o.optBoolean("showRecentInstalls", d.showRecentInstalls),
                showUsageInDrawer = o.optBoolean("showUsageInDrawer", d.showUsageInDrawer),
                drawerSort = enumOr(o.optString("drawerSort"), d.drawerSort),
                hidden = o.optJSONArray("hidden").strings().toSet(),
                renames = o.optJSONObject("renames").stringMap(),

                timersEnabled = o.optBoolean("timersEnabled", d.timersEnabled),
                socialDefaultMin = o.optInt("socialDefaultMin", d.socialDefaultMin),
                gameDefaultMin = o.optInt("gameDefaultMin", d.gameDefaultMin),
                videoDefaultMin = o.optInt("videoDefaultMin", d.videoDefaultMin),
                appLimits = o.optJSONObject("appLimits").intMap(),
                allowContinue = o.optBoolean("allowContinue", d.allowContinue),
                continueOptions = o.optJSONArray("continueOptions").ints().ifEmpty { d.continueOptions },
                allowBypass = o.optBoolean("allowBypass", d.allowBypass),
                askAfterBypass = o.optBoolean("askAfterBypass", d.askAfterBypass),
                frictionSeconds = o.optInt("frictionSeconds", d.frictionSeconds),
                warnMinutes = o.optInt("warnMinutes", d.warnMinutes),

                weeklyEnabled = o.optBoolean("weeklyEnabled", d.weeklyEnabled),
                weekStartsMonday = o.optBoolean("weekStartsMonday", d.weekStartsMonday),
                reviewHour = o.optInt("reviewHour", d.reviewHour).coerceIn(0, 23),

                // Gestures: an install from before the swipe actions has only the old toggles and
                // the one app swipe right opened; its choices are carried over once.
                swipeLeftAction = o.optString("swipeLeftAction").ifEmpty { ACTION_DRAWER },
                swipeRightAction = o.optString("swipeRightAction").ifEmpty {
                    when {
                        !o.optBoolean("swipeRightSearch", true) -> ACTION_NOTHING
                        o.optString("swipeRightApp").isNotEmpty() -> o.optString("swipeRightApp")
                        else -> ACTION_WEB_SEARCH
                    }
                },
                swipeUpAction = o.optString("swipeUpAction").ifEmpty {
                    if (o.optBoolean("swipeUpSearch", true)) ACTION_SEARCH else ACTION_NOTHING
                },
                swipeDownAction = o.optString("swipeDownAction").ifEmpty {
                    if (o.optBoolean("swipeDownNotifications", true)) ACTION_NOTIFICATIONS else ACTION_NOTHING
                },
                doubleTapLock = o.optBoolean("doubleTapLock", d.doubleTapLock),
            )
        }

        private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: fallback

        private fun JSONArray?.strings(): List<String> =
            if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }

        private fun JSONArray?.ints(): List<Int> =
            if (this == null) emptyList() else (0 until length()).map { optInt(it) }.filter { it > 0 }

        private fun JSONObject?.stringMap(): Map<String, String> {
            if (this == null) return emptyMap()
            val out = LinkedHashMap<String, String>()
            for (k in keys()) optString(k).takeIf { it.isNotEmpty() }?.let { out[k] = it }
            return out
        }

        private fun JSONObject?.intMap(): Map<String, Int> {
            if (this == null) return emptyMap()
            val out = LinkedHashMap<String, Int>()
            for (k in keys()) out[k] = optInt(k, 0)
            return out
        }
    }
}
