package com.focus.launcher.e2e.journeys

import com.focus.launcher.e2e.support.FixtureApp.GAME
import com.focus.launcher.e2e.support.FixtureApp.MAIL
import org.junit.Test

class AppListJourney : Journey() {
    /**
     * Dummy Mail opens whatever was used today (see HomeJourney). The search forgives typos with a
     * loose in-order match, so the query is the full name: "dummy m" would also list Dummy Game.
     *
     * The way back is the Back button on purpose: the Home button clears the search on its own
     * path, so coming back with Home would not notice if leaving Focus stopped clearing it.
     */
    @Test
    fun searchFindsAnAppAndOpensIt() {
        home.openAppList().search("dummy mail").firstResultIs(MAIL).doesNotShow(GAME).open(MAIL)
        app.isOpen(MAIL).goBack()
        home.isShown().openAppList().searchIsEmpty()
    }
}
