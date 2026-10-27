package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun package_name_isConsistent() {
        val expectedNamespace = "com.example"
        assertEquals("com.example", expectedNamespace)
        assertTrue(expectedNamespace.isNotEmpty())
    }
}
