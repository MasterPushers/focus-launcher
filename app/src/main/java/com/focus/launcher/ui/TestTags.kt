package com.focus.launcher.ui

/**
 * Names the emulator tests find screens by (e2e/README.md). [FocusTheme] exposes every Compose
 * test tag to UI Automator as a resource id, so these work across apps and in every language,
 * where the visible text would not. They change nothing a user can see.
 */
object TestTags {
    const val HOME_PAGE = "home_page"
    const val APP_LIST = "app_list"
    const val APP_SEARCH = "app_search"
    const val WALL = "wall"
}
