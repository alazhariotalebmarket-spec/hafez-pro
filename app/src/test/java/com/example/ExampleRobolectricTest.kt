package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyAmukhta
import com.example.data.DailySabaq
import com.example.data.DailySatSabaq
import com.example.data.ExamResult
import com.example.data.HifzRevision
import com.example.data.MadrasaInfo
import com.example.data.Student
import com.example.pdf.ExamPdfGenerator
import com.example.pdf.PdfConstants
import com.example.pdf.PdfDrawHelpers
import com.example.util.BengaliUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `verify combined three exam summary and merit ranking`() {
        val st1 = Student(id = 1L, roll = 1, name = "মোঃ মাহমুদুল হাসান", fatherName = "পিতা", address = "পটুয়াখালী", phone = "017", monthlyFee = 1000.0)
        val st2 = Student(id = 2L, roll = 2, name = "হাফেজ আবদুল্লাহ", fatherName = "পিতা", address = "ঢাকা", phone = "018", monthlyFee = 1000.0)

        val results = listOf(
            ExamResult(studentId = 1L, examType = PdfConstants.EXAM_FIRST, year = 2026, quranObtained = 85, tajweedObtained = 80, kalimaObtained = 75, duaHadithObtained = 90),
            ExamResult(studentId = 1L, examType = PdfConstants.EXAM_SECOND, year = 2026, quranObtained = 90, tajweedObtained = 85, kalimaObtained = 80, duaHadithObtained = 95),
            ExamResult(studentId = 1L, examType = PdfConstants.EXAM_ANNUAL, year = 2026, quranObtained = 95, tajweedObtained = 90, kalimaObtained = 85, duaHadithObtained = 90),
            ExamResult(studentId = 2L, examType = PdfConstants.EXAM_FIRST, year = 2026, quranObtained = 20, tajweedObtained = 70, kalimaObtained = 70, duaHadithObtained = 70)
        )

        val summaries = PdfDrawHelpers.buildCombinedExamSummaries(listOf(st1, st2), results)
        assertEquals(2, summaries.size)
        assertEquals(1L, summaries[0].student.id)
        assertEquals(1, summaries[0].meritPosition)
        assertEquals(1040, summaries[0].grandTotalObtained)
        assertEquals(1200, summaries[0].grandTotalFull)
        assertTrue(summaries[0].isOverallPassed)

        assertEquals(2L, summaries[1].student.id)
        assertEquals(2, summaries[1].meritPosition)
        assertFalse(summaries[1].isOverallPassed)
    }

    @Test
    fun `verify pdf layout constants and pagination thresholds`() {
        assertEquals(523f, PdfConstants.CONTENT_WIDTH)
        assertTrue(PdfConstants.BOTTOM_SAFE_Y < PdfConstants.PAGE_HEIGHT - PdfConstants.MARGIN)

        // With 1 student row (22pt), stays on 1 page
        val singleRowBottom = 160f + 22f + 12f
        assertTrue(singleRowBottom <= PdfConstants.BOTTOM_SAFE_Y)

        // With 40 student rows (40 * 22pt = 880pt), exceeds 1 page and triggers multi-page pagination
        var y = 160f
        var pages = 1
        val rowHeight = 22f
        repeat(40) {
            if (y + rowHeight > PdfConstants.BOTTOM_SAFE_Y) {
                pages++
                y = 100f
            }
            y += rowHeight
        }
        assertTrue(pages >= 2)
    }
}
