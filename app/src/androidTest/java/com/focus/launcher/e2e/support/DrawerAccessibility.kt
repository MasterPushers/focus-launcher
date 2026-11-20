package com.focus.launcher.e2e.support

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.focus.launcher.ui.TestTags

/** Verify the actual pager's scroll axis, not merely the preference that requested it. */
object DrawerAccessibility {
    fun isVertical(): Boolean {
        val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow ?: return false
        return findPager(root)?.actionList?.any {
            it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id ||
                it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id
        } == true
    }

    private fun findPager(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == TestTags.DRAWER_PAGER) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findPager(child)?.let { return it }
        }
        return null
    }
}
