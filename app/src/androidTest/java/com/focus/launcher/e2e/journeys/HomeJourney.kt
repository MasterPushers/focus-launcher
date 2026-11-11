package com.focus.launcher.e2e.journeys

import com.focus.launcher.e2e.support.FixtureApp.MAIL
import com.focus.launcher.e2e.support.Given
import org.junit.Test

class HomeJourney : Journey() {
    @Test
    fun homeButtonBringsBackTheHomePageFromTheAppList() {
        home.openAppList()
        home.pressHome().isShown()
    }

    /**
     * Dummy Mail, because Focus never limits it (ClassificationJourney holds it to that): it opens
     * however much any app has been used on this emulator today. Dummy Game may already be over
     * its automatic limit.
     */
    @Test
    fun aFastAppOpensFromTheHomePage() {
        Given.fastApps(MAIL)
        home.openFastApp(MAIL)
        app.isOpen(MAIL)
        home.pressHome().isShown()
    }
}
