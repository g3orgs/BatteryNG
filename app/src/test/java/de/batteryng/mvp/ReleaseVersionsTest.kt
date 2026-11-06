package de.batteryng.mvp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseVersionsTest {
    @Test
    fun comparesNumericComponentsRatherThanText() {
        assertEquals(1, ReleaseVersions.compare("0.12.10", "0.12.9"))
        assertEquals(-1, ReleaseVersions.compare("0.12.2", "0.12.3"))
        assertEquals(0, ReleaseVersions.compare("v0.12.3", "0.12.3.0"))
    }

    @Test
    fun avoidsClaimingAnUpdateForAnUnrecognizedTag() {
        assertNull(ReleaseVersions.compare("v0.13.0-beta", "0.12.3"))
        assertNull(ReleaseVersions.compare("", "0.12.3"))
    }
}
