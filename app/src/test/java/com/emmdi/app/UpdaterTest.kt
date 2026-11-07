package com.emmdi.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun newerVersions() {
        assertTrue(Updater.isNewer("1.1.0", "1.0.1"))
        assertTrue(Updater.isNewer("1.0.10", "1.0.9"))
        assertFalse(Updater.isNewer("1.0.1", "1.0.1"))
        assertFalse(Updater.isNewer("1.0.0", "1.0.1"))
    }
}
