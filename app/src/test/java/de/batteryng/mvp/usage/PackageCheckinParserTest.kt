package de.batteryng.mvp.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class PackageCheckinParserTest {
    @Test
    fun mapsOnlyProfilesWherePackageIsInstalledAndKeepsSharedUids() {
        val output = """
            vers,1
            pkg,com.microsoft.office.outlook,10305,72642120,1793257006151,installer,0
            pkg-splt,base,0
            pkg-usr,0,IbsualesLiavpiqha,0,com.android.vending,1779011757391,
            pkg-usr,10,IbsualesLiavpiqha,0,com.android.vending,1738942756821,
            pkg,com.microsoft.teams,10306,2026193045,1793177490098,installer,0
            pkg-usr,0,ibsualeSliavpiqha,0,?,0,
            pkg-usr,10,IbsualesLiavpiqha,0,com.android.vending,1738942768092,
            pkg,com.google.android.gms,10150,264231035,1793256874874,installer,0
            pkg-usr,0,IbsualesLiavpiqha,0,?,0,
            pkg-usr,10,IbsualesLiavpiqha,0,?,0,
            pkg,com.google.android.gsf,10150,1,0,?,0
            pkg-usr,0,IbsualesLiavpiqha,0,?,0,
            pkg-usr,10,IbsualesLiavpiqha,0,?,0,
        """.trimIndent()

        val result = PackageCheckinParser.parse(StringReader(output))

        assertFalse(result.permissionDenied)
        assertEquals(listOf("com.microsoft.office.outlook"), result.packagesByUid[10_305])
        assertEquals(listOf("com.microsoft.office.outlook"), result.packagesByUid[1_010_305])
        assertFalse(result.packagesByUid.containsKey(10_306))
        assertEquals(listOf("com.microsoft.teams"), result.packagesByUid[1_010_306])
        assertEquals(
            listOf("com.google.android.gms", "com.google.android.gsf"),
            result.packagesByUid[1_010_150],
        )
    }

    @Test
    fun reportsPermissionDenial() {
        val result = PackageCheckinParser.parse(StringReader("Permission Denial: missing DUMP"))
        assertTrue(result.permissionDenied)
        assertTrue(result.packagesByUid.isEmpty())
    }
}
