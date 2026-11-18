package com.focus.launcher.e2e.support

import java.io.File
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A picture at every checkpoint a robot confirms and, when a test fails, the screen, everything
 * UI Automator saw on it, the error, and Android's accessibility state. Written to Focus's own external folder, from where
 * e2e/run-on-emulator.sh pulls it:  …/files/e2e/<Journey>.<test>/NN-step.png
 */
class Evidence : TestWatcher() {
    private var dir: File? = null
    private var steps = 0

    override fun starting(description: Description) {
        steps = 0
        dir = File(Device.context.getExternalFilesDir("e2e"), "${description.testClass.simpleName}.${description.methodName}")
            .apply { deleteRecursively(); mkdirs() }
    }

    /** Called by robots once the screen they expected is confirmed. */
    fun step(name: String) {
        val folder = dir ?: return
        steps++
        Device.ui.waitForIdle()
        Device.ui.takeScreenshot(File(folder, "${steps.toString().padStart(2, '0')}-$name.png"))
    }

    override fun failed(e: Throwable, description: Description) {
        val folder = dir ?: return
        runCatching { Device.ui.takeScreenshot(File(folder, "FAILED.png")) }
        runCatching { File(folder, "FAILED-screen.xml").outputStream().use { Device.ui.dumpWindowHierarchy(it) } }
        runCatching { File(folder, "FAILED.txt").writeText(e.stackTraceToString()) }
        // Which accessibility services Android had bound at that moment (the timer service among them).
        runCatching { File(folder, "FAILED-accessibility.txt").writeText(Device.shell("dumpsys accessibility")) }
    }
}
