package com.focus.launcher.e2e.support

import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.focus.launcher.MainActivity

/** Inspect the real home window on the main thread; no production test hooks. */
object HomeWindow {
    private fun visit(block: (MainActivity) -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().firstOrNull()?.let(block)
        }
    }

    fun wallpaperSurfaceReady(): Boolean {
        var ready = false
        visit { home ->
            val attrs = home.window.attributes
            ready = attrs.format == PixelFormat.TRANSLUCENT &&
                attrs.flags and WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER != 0
        }
        return ready
    }

    /** Simulate the opaque surface state that must be corrected when returning from another app. */
    fun makeOpaque() = visit { it.window.setFormat(PixelFormat.OPAQUE) }
}
