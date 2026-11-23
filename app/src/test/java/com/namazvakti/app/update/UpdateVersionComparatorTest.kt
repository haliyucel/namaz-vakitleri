package com.namazvakti.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionComparatorTest {

    @Test
    fun detectsNewerSemanticVersions() {
        assertTrue(UpdateVersionComparator.isNewer("v1.0.1", "1.0.0"))
        assertTrue(UpdateVersionComparator.isNewer("1.1.0", "1.0.9"))
        assertTrue(UpdateVersionComparator.isNewer("v1.10.0", "1.2.0"))
        assertTrue(UpdateVersionComparator.isNewer("v2.0", "1.9.9"))
        assertTrue(UpdateVersionComparator.isNewer("v1.0.1-beta1", "1.0.0"))
    }

    @Test
    fun ignoresSameOrOlderVersions() {
        assertFalse(UpdateVersionComparator.isNewer("v1.0.0", "1.0.0"))
        assertFalse(UpdateVersionComparator.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateVersionComparator.isNewer("v0.9.9", "1.0.0"))
        assertFalse(UpdateVersionComparator.isNewer("v1.0.0", "1.0.1"))
        assertFalse(UpdateVersionComparator.isNewer("", "1.0.0"))
    }

    @Test
    fun normalizesDisplayTag() {
        assertEquals("v1.2.0", UpdateVersionComparator.normalizeDisplayTag("1.2.0"))
        assertEquals("v1.2.0", UpdateVersionComparator.normalizeDisplayTag("v1.2.0"))
    }
}
