package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyAmukhta
import com.example.data.DailySabaq
import com.example.data.DailySatSabaq
import com.example.data.HifzRevision
import com.example.util.BengaliUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hafez Pro", appName)
    }

    @Test
    fun `verify bengali numeral formatting`() {
        assertEquals("১২৩৪", BengaliUtils.toBn(1234))
        assertEquals("৫০০ ৳", BengaliUtils.formatCurrency(500.0))
        assertEquals("৩০", BengaliUtils.toBn(30))
    }

    @Test
    fun `verify para number support on learning entities`() {
        val sabaq = DailySabaq(studentId = 1L, date = "2026-11-21", paraNumber = 15, lessonType = "১ ছাপা", isPassed = true)
        val satSabaq = DailySatSabaq(studentId = 1L, date = "2026-11-21", paraNumber = 14, pagesCount = "৫ ছাপা", isPassed = true)
        val amukhta = DailyAmukhta(studentId = 1L, date = "2026-11-21", paraNumber = 10, amount = "১ পারা", isPassed = true)
        val revision = HifzRevision(studentId = 1L, date = "2026-11-21", paraNumber = 30, amount = "২ পারা", isPassed = false)

        assertEquals(15, sabaq.paraNumber)
        assertEquals(14, satSabaq.paraNumber)
        assertEquals(10, amukhta.paraNumber)
        assertEquals(30, revision.paraNumber)
    }
}
