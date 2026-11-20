package com.focus.launcher.ui

/** Edge the drawer enters from, opposite the direction the finger travels. */
enum class DrawerOrigin(val vertical: Boolean, val reversed: Boolean) {
    RIGHT(false, false),
    LEFT(false, true),
    BOTTOM(true, false),
    TOP(true, true),
}
