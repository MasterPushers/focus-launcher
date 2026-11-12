package com.focus.launcher.e2e.journeys

import com.focus.launcher.e2e.robots.LimitedAppsRobot.Companion.WITH_A_LIMIT
import com.focus.launcher.e2e.support.FixtureApp.GAME
import com.focus.launcher.e2e.support.FixtureApp.MAIL
import org.junit.Test

/** Which apps Focus limits on its own (3-details/app-classification.md): it never guesses towards a limit. */
class ClassificationJourney : Journey() {
    @Test
    fun aGameGetsTheAutomaticLimit() {
        limitedApps.open().lists(GAME, under = WITH_A_LIMIT)
    }

    /** The owner's rule: mail, browsers and messengers are never limited automatically. */
    @Test
    fun aMailAppThatAndroidCallsSocialIsNeverLimited() {
        limitedApps.open().lists(GAME, under = WITH_A_LIMIT).doesNotList(MAIL)
    }
}
