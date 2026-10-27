package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BatterySnapshot
import com.example.data.model.DiagnosticItem
import com.example.data.model.TestCategory
import com.example.data.model.TestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Phone Diagnostics", appName)
    }

    @Test
    fun `battery snapshot default state is valid`() {
        val battery = BatterySnapshot()
        assertEquals(100, battery.levelPercent)
        assertEquals("Good", battery.health)
        assertTrue(battery.voltageMv > 0)
    }

    @Test
    fun `diagnostic item handles status update`() {
        val item = DiagnosticItem(
            id = "test_screen",
            title = "Screen Dead Pixels",
            description = "Dead pixel inspection",
            category = TestCategory.SCREEN,
            status = TestStatus.PASSED,
            detailMessage = "All 8 color fields clean"
        )
        assertEquals(TestStatus.PASSED, item.status)
        assertEquals("All 8 color fields clean", item.detailMessage)
    }
}
