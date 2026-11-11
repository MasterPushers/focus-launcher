package com.focus.launcher.e2e.support

import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Outermost rule: on a real phone every test is skipped before it can touch anything. */
class EmulatorOnly : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            assumeTrue("The emulator tests change Focus's settings and data; they never run on a phone.", Device.isEmulator)
            base.evaluate()
        }
    }
}
