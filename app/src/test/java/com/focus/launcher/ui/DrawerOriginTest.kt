package com.focus.launcher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerOriginTest {
    @Test fun `drawer from below uses the forward vertical axis`() {
        assertTrue(DrawerOrigin.BOTTOM.vertical)
        assertFalse(DrawerOrigin.BOTTOM.reversed)
    }

    @Test fun `drawer from above reverses the vertical axis`() {
        assertTrue(DrawerOrigin.TOP.vertical)
        assertTrue(DrawerOrigin.TOP.reversed)
    }

    @Test fun `horizontal origins use opposing directions`() {
        assertFalse(DrawerOrigin.RIGHT.vertical)
        assertFalse(DrawerOrigin.RIGHT.reversed)
        assertFalse(DrawerOrigin.LEFT.vertical)
        assertTrue(DrawerOrigin.LEFT.reversed)
    }
}
