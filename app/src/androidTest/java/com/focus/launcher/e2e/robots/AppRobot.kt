package com.focus.launcher.e2e.robots

import androidx.test.uiautomator.By
import com.focus.launcher.e2e.support.Device
import com.focus.launcher.e2e.support.Evidence
import com.focus.launcher.e2e.support.FixtureApp

/** Another app than Focus: one of the fixtures. */
class AppRobot(private val evidence: Evidence) {
    fun isOpen(app: FixtureApp): AppRobot = apply {
        Device.waitFor(By.pkg(app.packageName).text(app.openText), "${app.label} in front")
        evidence.step(app.name.lowercase() + "-open")
    }

    /** The Back button: the fixture closes and Android returns to whatever was under it. */
    fun goBack(): AppRobot = apply { Device.ui.pressBack() }
}
