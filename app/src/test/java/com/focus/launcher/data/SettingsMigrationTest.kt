package com.focus.launcher.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Schema 2 made the split clock the default. Settings are stored with every field written out, so
 * a stored "RING" from before is the old default rather than a choice, and moves along once.
 * Everything a user did choose has to survive.
 */
class SettingsMigrationTest {

    private fun stored(version: Int?, style: String?): JSONObject = Settings().toJson().apply {
        if (version == null) remove("v") else put("v", version)
        if (style == null) remove("clockStyle") else put("clockStyle", style)
    }

    @Test fun `a fresh install gets the split clock`() {
        assertEquals(ClockStyle.SPLIT, Settings().clockStyle)
        assertEquals(ClockStyle.SPLIT, Settings.fromJson(JSONObject()).clockStyle)
    }

    @Test fun `a ring stored before schema 2 becomes the split clock`() {
        assertEquals(ClockStyle.SPLIT, Settings.fromJson(stored(version = null, style = "RING")).clockStyle)
        assertEquals(ClockStyle.SPLIT, Settings.fromJson(stored(version = 1, style = "RING")).clockStyle)
    }

    @Test fun `a plain clock chosen before schema 2 is kept`() {
        assertEquals(ClockStyle.PLAIN, Settings.fromJson(stored(version = null, style = "PLAIN")).clockStyle)
    }

    @Test fun `a ring chosen after the change is kept`() {
        val chosen = Settings(clockStyle = ClockStyle.RING)
        assertEquals(ClockStyle.RING, Settings.fromJson(chosen.toJson()).clockStyle)
    }

    @Test fun `settings survive a round trip`() {
        val s = Settings(clockStyle = ClockStyle.SPLIT, splitSide = SplitSide.SCREEN_TIME, showCalendar = true, doubleTapLock = false, musicAutoHide = false)
        assertEquals(s, Settings.fromJson(s.toJson()))
    }

    @Test fun `an install from before the music section could hide gets the hiding`() {
        val old = Settings(showMusic = true).toJson().apply { remove("musicAutoHide") }
        assertEquals(true, Settings.fromJson(old).musicAutoHide)
    }

    @Test fun `an unknown stored value falls back to the default`() {
        assertEquals(SplitSide.CALENDAR, Settings.fromJson(stored(version = 2, style = "SPLIT").put("splitSide", "MAIL")).splitSide)
    }

    /**
     * The four swipes used to be three on/off switches and the app swipe right opened. An install
     * from before the swipe actions must land on exactly what it had.
     */
    @Test fun `an old install keeps what its swipes did`() {
        val allOn = Settings().toJson()
            .apply { remove("swipeLeftAction"); remove("swipeRightAction"); remove("swipeUpAction"); remove("swipeDownAction") }
        val migrated = Settings.fromJson(allOn)
        assertEquals(ACTION_DRAWER, migrated.swipeLeftAction)
        assertEquals(ACTION_WEB_SEARCH, migrated.swipeRightAction)
        assertEquals(ACTION_SEARCH, migrated.swipeUpAction)
        assertEquals(ACTION_NOTIFICATIONS, migrated.swipeDownAction)

        val off = Settings().toJson().apply {
            remove("swipeLeftAction"); remove("swipeRightAction"); remove("swipeUpAction"); remove("swipeDownAction")
            put("swipeUpSearch", false); put("swipeDownNotifications", false); put("swipeRightSearch", false)
        }
        assertEquals(ACTION_NOTHING, Settings.fromJson(off).swipeUpAction)
        assertEquals(ACTION_NOTHING, Settings.fromJson(off).swipeDownAction)
        assertEquals(ACTION_NOTHING, Settings.fromJson(off).swipeRightAction)
    }

    @Test fun `the app swipe right opened is carried over`() {
        val old = Settings().toJson().apply {
            remove("swipeLeftAction"); remove("swipeRightAction"); remove("swipeUpAction"); remove("swipeDownAction")
            put("swipeRightApp", "com.example.mail")
        }
        assertEquals("com.example.mail", Settings.fromJson(old).swipeRightAction)
    }

    @Test fun `the swipe actions and the home layout survive a round trip`() {
        val s = Settings(
            swipeLeftAction = ACTION_NOTHING,
            swipeRightAction = "com.example.app",
            swipeUpAction = ACTION_DRAWER,
            swipeDownAction = ACTION_SETTINGS,
            appBlock = AppBlock.LOW,
            fastAppSpacing = 12,
            showWallpaper = true,
            wallpaperDim = 0.75f,
        )
        assertEquals(s, Settings.fromJson(s.toJson()))
    }

    @Test fun `new actions take precedence over legacy toggles`() {
        val old = JSONObject()
            .put("swipeRightSearch", false)
            .put("swipeRightAction", ACTION_DRAWER)
            .put("swipeUpSearch", true)
            .put("swipeUpAction", ACTION_NOTHING)
        val settings = Settings.fromJson(old)
        assertEquals(ACTION_DRAWER, settings.swipeRightAction)
        assertEquals(ACTION_NOTHING, settings.swipeUpAction)
    }

    @Test fun `out of range layout and wallpaper values are bounded`() {
        val settings = Settings.fromJson(JSONObject()
            .put("fastAppSpacing", 1000)
            .put("appBlock", "INVALID")
            .put("wallpaperDim", 12.0))
        assertEquals(24, settings.fastAppSpacing)
        assertEquals(AppBlock.MIDDLE, settings.appBlock)
        assertEquals(0.9f, settings.wallpaperDim)
        assertEquals(0, Settings.fromJson(JSONObject().put("fastAppSpacing", -1)).fastAppSpacing)
    }
}
