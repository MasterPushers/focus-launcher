package com.focus.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Time in a browser's Custom Tab goes to the app that opened it (issue 51). Times in ms. */
class CustomTabCreditTest {
    private val home = "launcher"
    private val tab = "org.chromium.chrome.browser.customtabs.CustomTabActivity"
    private val firefoxTab = "org.mozilla.fenix.customtabs.ExternalAppBrowserActivity"
    private val firefoxHandOff = "org.mozilla.fenix.IntentReceiverActivity"
    private fun credit() = CustomTabCredit { it == home }

    @Test fun `the class names of Chromium and Firefox Custom Tabs are recognised`() {
        assertTrue(CustomTabCredit.isCustomTab(tab))
        assertTrue(CustomTabCredit.isCustomTab(firefoxTab))
        assertFalse(CustomTabCredit.isCustomTab("com.brave.browser.ChromeTabbedActivity"))
        assertFalse(CustomTabCredit.isCustomTab("org.mozilla.fenix.HomeActivity"))
        assertFalse(CustomTabCredit.isCustomTab(firefoxHandOff))
        assertFalse(CustomTabCredit.isCustomTab(null))
    }

    @Test fun `a Custom Tab counts for the app that opened it, until it is closed`() {
        val c = credit()
        assertEquals("news", c.resumed("news", "news.Main", 0))
        assertEquals("news", c.paused("news", "news.Main"))
        assertEquals("news", c.resumed("brave", tab, 100))
        assertEquals("news", c.paused("brave", tab))
    }

    @Test fun `the browser's own hand-off activity in between does not take the tab`() {
        // The order Firefox produced on a test device: reader paused, IntentReceiverActivity
        // resumed and paused in the same second, then the Custom Tab.
        val c = credit()
        c.resumed("reader", "reader.Main", 0)
        c.paused("reader", "reader.Main")
        assertEquals("fennec", c.resumed("fennec", firefoxHandOff, 1_000))
        c.paused("fennec", firefoxHandOff)
        assertEquals("reader", c.resumed("fennec", firefoxTab, 1_050))
        assertEquals("reader", c.paused("fennec", firefoxTab))
    }

    @Test fun `the browser itself still counts for the browser`() {
        val c = credit()
        c.resumed("news", "news.Main", 0)
        assertEquals("brave", c.resumed("brave", "com.brave.browser.ChromeTabbedActivity", 100))
    }

    @Test fun `switching from the browser to a tab long open keeps the tab's owner`() {
        val c = credit()
        c.resumed("news", "news.Main", 0)
        c.resumed("brave", tab, 100)
        c.resumed("mail", "mail.Inbox", 10_000)
        c.resumed("brave", "com.brave.browser.ChromeTabbedActivity", 20_000)
        assertEquals("news", c.resumed("brave", tab, 60_000))
    }

    @Test fun `coming back to a Custom Tab from the home screen keeps its owner`() {
        val c = credit()
        c.resumed("news", "news.Main", 0)
        c.resumed("brave", tab, 100)
        c.paused("brave", tab)
        c.resumed(home, "launcher.Home", 5_000)
        assertEquals("news", c.resumed("brave", tab, 9_000))
    }

    @Test fun `the next app to open a Custom Tab owns it`() {
        val c = credit()
        c.resumed("news", "news.Main", 0)
        c.resumed("brave", tab, 100)
        c.paused("brave", tab)
        c.resumed("mail", "mail.Inbox", 5_000)
        assertEquals("mail", c.resumed("brave", tab, 6_000))
        assertEquals("mail", c.paused("brave", tab))
    }

    @Test fun `a Custom Tab with no app before it stays the browser's`() {
        val c = credit()
        c.resumed(home, "launcher.Home", 0)
        assertEquals("brave", c.resumed("brave", tab, 100))
    }

    @Test fun `with the tracker, the time lands on the opener`() {
        val out = ArrayList<Triple<String, Long, Long>>()
        val tracker = ForegroundTracker { pkg, start, end -> out += Triple(pkg, start, end) }
        val c = credit()
        tracker.resumed(c.resumed("news", "news.Main", 0), 0)
        tracker.paused(c.paused("news", "news.Main"), 10_000)
        tracker.resumed(c.resumed("brave", tab, 10_050), 10_050)
        tracker.paused(c.paused("brave", tab), 70_000)
        tracker.resumed(c.resumed(home, "launcher.Home", 70_050), 70_050)
        tracker.finish(80_000)
        assertEquals(listOf(Triple("news", 0L, 10_000L), Triple("news", 10_050L, 70_000L), Triple(home, 70_050L, 80_000L)), out)
    }
}
