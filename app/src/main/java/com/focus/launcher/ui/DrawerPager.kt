package com.focus.launcher.ui

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Use a real vertical pager for vertical gestures, not a sideways page with a vertical trigger. */
@Composable
fun DrawerPager(
    origin: DrawerOrigin,
    state: PagerState,
    modifier: Modifier,
    userScrollEnabled: Boolean,
    content: @Composable PagerScope.(Int) -> Unit,
) {
    if (origin.vertical) {
        VerticalPager(
            state = state,
            modifier = modifier.testTag(TestTags.DRAWER_PAGER),
            reverseLayout = origin.reversed,
            beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled,
            key = { it },
            pageContent = content,
        )
    } else {
        HorizontalPager(
            state = state,
            modifier = modifier.testTag(TestTags.DRAWER_PAGER),
            reverseLayout = origin.reversed,
            beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled,
            key = { it },
            pageContent = content,
        )
    }
}
