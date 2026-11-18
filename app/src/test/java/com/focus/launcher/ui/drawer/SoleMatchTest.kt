package com.focus.launcher.ui.drawer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which app "Open when one app is left" opens (issue 53). Names are given already normalized. */
class SoleMatchTest {
    private val apps = listOf("calculator", "calendar", "camera", "call recorder", "clock", "contacts", "gmail", "google maps", "maps")

    private fun pick(query: String) = soleMatch(rankMatches(apps, query) { it })

    @Test fun `one app by name opens although loose matches widen the list`() {
        // "calc" is also a loose in-order match for "call recorder" (c-a-l … c).
        assertEquals(listOf("calculator", "call recorder"), rankMatches(apps, "calc") { it }.map { it.second })
        assertEquals("calculator", pick("calc"))
    }

    @Test fun `two apps by name open nothing`() {
        assertNull(pick("cal"))
        assertNull(pick("ma")) // "maps" and "google maps" (and "gmail" contains "ma")
    }

    @Test fun `a word inside the name counts as a match by name`() {
        assertEquals("call recorder", pick("recor"))
    }

    @Test fun `a name match wins over initials`() {
        // Only gmail has "gm" in its name; google maps matches by its initials only.
        assertEquals("gmail", pick("gm"))
    }

    @Test fun `a single loose match opens when nothing matches by name`() {
        assertEquals("contacts", pick("cntc"))
    }

    @Test fun `several loose matches open nothing`() {
        assertNull(pick("cla")) // calculator and calendar, both only letters in order
    }

    @Test fun `nothing matches, nothing opens`() {
        assertNull(pick("xyz"))
    }
}
