package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.*
import com.example.util.BengaliUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width (pt)
    private const val PAGE_HEIGHT = 842 // A4 standard height (pt)
    private const val MARGIN = 36f

    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(5, 150, 105) // Emerald green
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 10f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.DEFAULT
    }

    private val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val borderPaint = Paint().apply {
        color = Color.rgb(209, 213, 219)
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
    }

    private val headerBgPaint = Paint().apply {
        color = Color.rgb(4, 120, 87) // Deep emerald
        style = Paint.Style.FILL
    }

    private val zebraPaint = Paint().apply {
        color = Color.rgb(243, 244, 246)
        style = Paint.Style.FILL
    }

    private val goldLinePaint = Paint().apply {
        color = Color.rgb(245, 158, 11)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private fun drawDocumentHeader(canvas: Canvas, madrasa: MadrasaInfo?, title: String, periodText: String): Float {
        var y = MARGIN + 12f

        // Bismillah
        subHeaderPaint.textSize = 11f
        subHeaderPaint.typeface = Typeface.DEFAULT
        canvas.drawText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", PAGE_WIDTH / 2f, y, subHeaderPaint)
        y += 18f

        // Madrasa Name
        headerPaint.textSize = 16f
        canvas.drawText(madrasa?.name ?: "দারুল উলুম হিফজুল কুরআন মাদ্রাসা", PAGE_WIDTH / 2f, y, headerPaint)
        y += 15f

        // Address & Phone
        subHeaderPaint.textSize = 9.5f
        val addressLine = "${madrasa?.address ?: ""}  |  মোবাইল: ${madrasa?.phone ?: ""}"
        canvas.drawText(addressLine, PAGE_WIDTH / 2f, y, subHeaderPaint)
        y += 14f

        // Golden divider
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, goldLinePaint)
        y += 16f

        // Report Title
        canvas.drawText(title, PAGE_WIDTH / 2f, y, titlePaint)
        y += 13f

        // Period & Date
        subHeaderPaint.textSize = 8.5f
        val dateText = "তারিখ: ${BengaliUtils.formatDateToBn(BengaliUtils.getTodayDate())}  |  বিবরণী: $periodText"
        canvas.drawText(dateText, PAGE_WIDTH / 2f, y, subHeaderPaint)
        y += 18f

        return y
    }

    private fun drawSignatures(canvas: Canvas, y: Float) {
        val signY = (PAGE_HEIGHT - MARGIN - 20f).coerceAtLeast(y + 35f)
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }

        // 3 signatures
        val col1 = MARGIN + 60f
        val col2 = PAGE_WIDTH / 2f
        val col3 = PAGE_WIDTH - MARGIN - 60f

        canvas.drawLine(col1 - 40, signY, col1 + 40, signY, linePaint)
        canvas.drawText("হিসাব রক্ষক", col1, signY + 12f, labelPaint)

        canvas.drawLine(col2 - 40, signY, col2 + 40, signY, linePaint)
        canvas.drawText("নাজেমে তালিমাত", col2, signY + 12f, labelPaint)

        canvas.drawLine(col3 - 50, signY, col3 + 50, signY, linePaint)
        canvas.drawText("মুহতামিম / প্রধান শিক্ষক", col3, signY + 12f, labelPaint)
    }

    // 1. Student Admission & Fee List PDF
    fun generateStudentsPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        students: List<Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "ছাত্র ভর্তি ও মাসিক ফি তালিকা", periodText)

        val headers = listOf("রোল", "নাম", "পিতার নাম", "মোবাইল", "মাসিক বেতন", "বিদ্যুৎ বিল", "অন্যান্য", "মোট")
        val colWidths = listOf(35f, 95f, 95f, 75f, 55f, 55f, 55f, 58f)
        val rowHeight = 20f

        // Header
        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        // Rows
        var totalMonthly = 0.0
        var totalElectricity = 0.0
        var totalOther = 0.0
        var grandTotal = 0.0

        students.forEachIndexed { idx, student ->
            if (y > PAGE_HEIGHT - 100f) return@forEachIndexed // Fits on page

            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            }
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            x = MARGIN
            val rowValues = listOf(
                BengaliUtils.toBn(student.roll),
                student.name,
                student.fatherName,
                BengaliUtils.toBn(student.phone),
                BengaliUtils.formatCurrency(student.monthlyFee),
                BengaliUtils.formatCurrency(student.monthlyElectricityBill),
                BengaliUtils.formatCurrency(student.otherFee),
                BengaliUtils.formatCurrency(student.totalMonthlyFee)
            )

            totalMonthly += student.monthlyFee
            totalElectricity += student.monthlyElectricityBill
            totalOther += student.otherFee
            grandTotal += student.totalMonthlyFee

            rowValues.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 0) Paint.Align.CENTER else if (i >= 4) Paint.Align.RIGHT else Paint.Align.LEFT
                val textX = if (i == 0) x + colWidths[i] / 2f else if (i >= 4) x + colWidths[i] - 4f else x + 4f
                canvas.drawText(text, textX, y + 14f, cellPaint)
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Summary Row
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        x = MARGIN
        cellBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("মোট হিসাব:", x + (colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3]) / 2f, y + 14f, cellBoldPaint)
        x += colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3]

        cellBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(BengaliUtils.formatCurrency(totalMonthly), x + colWidths[4] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[4]
        canvas.drawText(BengaliUtils.formatCurrency(totalElectricity), x + colWidths[5] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[5]
        canvas.drawText(BengaliUtils.formatCurrency(totalOther), x + colWidths[6] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[6]
        canvas.drawText(BengaliUtils.formatCurrency(grandTotal), x + colWidths[7] - 4f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Student_Admission_Report_${System.currentTimeMillis()}.pdf")
    }

    // 2. Student Fee Collection PDF
    fun generateFeePaymentsPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        payments: List<FeePayment>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "ছাত্র বেতন আদায় বিবরণী", periodText)

        val headers = listOf("রোল", "ছাত্রের নাম", "মাস ও বছর", "বেতন", "বিদ্যুৎ", "অন্যান্য", "জমা", "বাকি")
        val colWidths = listOf(35f, 100f, 85f, 55f, 55f, 55f, 68f, 70f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var totalPaid = 0.0
        var totalDue = 0.0

        payments.forEachIndexed { idx, p ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed

            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            }
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[p.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${p.studentId}"
            val monthYear = "${p.monthName} ${BengaliUtils.toBn(p.year)}"

            totalPaid += p.paidAmount
            totalDue += p.dueAmount

            val values = listOf(
                roll,
                name,
                monthYear,
                BengaliUtils.formatCurrency(p.tuitionFee),
                BengaliUtils.formatCurrency(p.electricityBill),
                BengaliUtils.formatCurrency(p.otherFee),
                BengaliUtils.formatCurrency(p.paidAmount),
                BengaliUtils.formatCurrency(p.dueAmount)
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 0 || i == 2) Paint.Align.CENTER else if (i >= 3) Paint.Align.RIGHT else Paint.Align.LEFT
                val textX = if (i == 0 || i == 2) x + colWidths[i] / 2f else if (i >= 3) x + colWidths[i] - 4f else x + 4f
                canvas.drawText(text, textX, y + 14f, cellPaint)
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Summary
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        x = MARGIN
        cellBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("সর্বমোট আদায় ও বাকি:", x + (colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4] + colWidths[5]) / 2f, y + 14f, cellBoldPaint)
        x += colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] + colWidths[4] + colWidths[5]

        cellBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(BengaliUtils.formatCurrency(totalPaid), x + colWidths[6] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[6]
        canvas.drawText(BengaliUtils.formatCurrency(totalDue), x + colWidths[7] - 4f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Fee_Collection_Report_${System.currentTimeMillis()}.pdf")
    }

    // 3. Daily Sabaq PDF
    fun generateDailySabaqPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        sabaqList: List<DailySabaq>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "দৈনিক ছবক রেজিস্টার রিপোর্ট", periodText)

        val headers = listOf("তারিখ", "রোল", "ছাত্রের নাম", "পারা", "ছবকের পরিমাণ", "ফলাফল", "মন্তব্য")
        val colWidths = listOf(72f, 34f, 118f, 45f, 96f, 78f, 80f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var passedCount = 0

        sabaqList.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed

            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            }
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[item.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${item.studentId}"
            val paraText = "পারা ${BengaliUtils.toBn(item.paraNumber)}"
            val status = if (item.isPassed) { passedCount++; "✓ উত্তীর্ণ" } else "✗ পুনরাবৃত্তি"

            val values = listOf(
                BengaliUtils.formatDateToBn(item.date),
                roll,
                name,
                paraText,
                item.lessonType,
                status,
                if (item.remarks.isEmpty()) "-" else item.remarks
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 2 || i == 6) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 2 || i == 6) x + 4f else x + colWidths[i] / 2f

                if (i == 5) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (item.isPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 14f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 14f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Summary
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        cellBoldPaint.textAlign = Paint.Align.CENTER
        val summaryStr = "মোট এন্ট্রি: ${BengaliUtils.toBn(sabaqList.size)} টি  |  উত্তীর্ণ: ${BengaliUtils.toBn(passedCount)} জন  |  পুনরাবৃত্তি: ${BengaliUtils.toBn(sabaqList.size - passedCount)} জন"
        canvas.drawText(summaryStr, PAGE_WIDTH / 2f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Daily_Sabaq_Report_${System.currentTimeMillis()}.pdf")
    }

    // 4. Daily Sat Sabaq PDF
    fun generateDailySatSabaqPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        list: List<DailySatSabaq>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "দৈনিক সাত ছবক রেজিস্টার রিপোর্ট", periodText)

        val headers = listOf("তারিখ", "রোল", "ছাত্রের নাম", "পারা", "পৃষ্ঠা / ছাপা", "ফলাফল", "মন্তব্য")
        val colWidths = listOf(72f, 34f, 118f, 45f, 96f, 78f, 80f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var passedCount = 0

        list.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[item.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${item.studentId}"
            val paraText = "পারা ${BengaliUtils.toBn(item.paraNumber)}"
            val status = if (item.isPassed) { passedCount++; "✓ উত্তীর্ণ" } else "✗ পুনরাবৃত্তি"

            val values = listOf(
                BengaliUtils.formatDateToBn(item.date),
                roll,
                name,
                paraText,
                item.pagesCount,
                status,
                if (item.remarks.isEmpty()) "-" else item.remarks
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 2 || i == 6) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 2 || i == 6) x + 4f else x + colWidths[i] / 2f

                if (i == 5) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (item.isPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 14f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 14f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        cellBoldPaint.textAlign = Paint.Align.CENTER
        val summaryStr = "মোট এন্ট্রি: ${BengaliUtils.toBn(list.size)} টি  |  উত্তীর্ণ: ${BengaliUtils.toBn(passedCount)} জন  |  পুনরাবৃত্তি: ${BengaliUtils.toBn(list.size - passedCount)} জন"
        canvas.drawText(summaryStr, PAGE_WIDTH / 2f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Daily_Sat_Sabaq_Report_${System.currentTimeMillis()}.pdf")
    }

    // 5. Daily Amukhta PDF
    fun generateDailyAmukhtaPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        list: List<DailyAmukhta>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "দৈনিক আমুখতা রেজিস্টার রিপোর্ট", periodText)

        val headers = listOf("তারিখ", "রোল", "ছাত্রের নাম", "পারা", "আমুখতা", "ফলাফল", "মন্তব্য")
        val colWidths = listOf(72f, 34f, 118f, 45f, 96f, 78f, 80f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var passedCount = 0

        list.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[item.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${item.studentId}"
            val paraText = "পারা ${BengaliUtils.toBn(item.paraNumber)}"
            val status = if (item.isPassed) { passedCount++; "✓ উত্তীর্ণ" } else "✗ পুনরাবৃত্তি"

            val values = listOf(
                BengaliUtils.formatDateToBn(item.date),
                roll,
                name,
                paraText,
                item.amount,
                status,
                if (item.remarks.isEmpty()) "-" else item.remarks
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 2 || i == 6) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 2 || i == 6) x + 4f else x + colWidths[i] / 2f

                if (i == 5) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (item.isPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 14f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 14f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        cellBoldPaint.textAlign = Paint.Align.CENTER
        val summaryStr = "মোট এন্ট্রি: ${BengaliUtils.toBn(list.size)} টি  |  উত্তীর্ণ: ${BengaliUtils.toBn(passedCount)} জন  |  পুনরাবৃত্তি: ${BengaliUtils.toBn(list.size - passedCount)} জন"
        canvas.drawText(summaryStr, PAGE_WIDTH / 2f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Daily_Amukhta_Report_${System.currentTimeMillis()}.pdf")
    }

    // 6. Hifz Revision PDF
    fun generateHifzRevisionPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        list: List<HifzRevision>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "হিফজ রিভিশন রেজিস্টার রিপোর্ট", periodText)

        val headers = listOf("তারিখ", "রোল", "ছাত্রের নাম", "পারা", "রিভিশন", "ফলাফল", "মন্তব্য")
        val colWidths = listOf(72f, 34f, 118f, 45f, 96f, 78f, 80f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var passedCount = 0

        list.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[item.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${item.studentId}"
            val paraText = "পারা ${BengaliUtils.toBn(item.paraNumber)}"
            val status = if (item.isPassed) { passedCount++; "✓ উত্তীর্ণ" } else "✗ পুনরাবৃত্তি"

            val values = listOf(
                BengaliUtils.formatDateToBn(item.date),
                roll,
                name,
                paraText,
                item.amount,
                status,
                if (item.remarks.isEmpty()) "-" else item.remarks
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 2 || i == 6) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 2 || i == 6) x + 4f else x + colWidths[i] / 2f

                if (i == 5) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (item.isPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 14f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 14f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        cellBoldPaint.textAlign = Paint.Align.CENTER
        val summaryStr = "মোট এন্ট্রি: ${BengaliUtils.toBn(list.size)} টি  |  উত্তীর্ণ: ${BengaliUtils.toBn(passedCount)} জন  |  পুনরাবৃত্তি: ${BengaliUtils.toBn(list.size - passedCount)} জন"
        canvas.drawText(summaryStr, PAGE_WIDTH / 2f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Hifz_Revision_Report_${System.currentTimeMillis()}.pdf")
    }

    // 7. Exam Fee PDF
    fun generateExamFeesPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        feesList: List<ExamFee>,
        studentsMap: Map<Long, Student>,
        examType: String,
        year: Int
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "পরীক্ষার ফি আদায় রিপোর্ট", "$examType (${BengaliUtils.toBn(year)})")

        val headers = listOf("রোল", "ছাত্রের নাম", "পরীক্ষা", "ধার্য ফি", "জমা", "বাকি", "অবস্থা")
        val colWidths = listOf(40f, 120f, 110f, 65f, 65f, 65f, 58f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var totalTotal = 0.0
        var totalPaid = 0.0
        var totalDue = 0.0

        feesList.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[item.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${item.studentId}"
            val status = if (item.dueAmount <= 0) "পরিশোধিত" else "বাকি আছে"

            totalTotal += item.totalFee
            totalPaid += item.paidAmount
            totalDue += item.dueAmount

            val values = listOf(
                roll,
                name,
                item.examType,
                BengaliUtils.formatCurrency(item.totalFee),
                BengaliUtils.formatCurrency(item.paidAmount),
                BengaliUtils.formatCurrency(item.dueAmount),
                status
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 0 || i == 6) Paint.Align.CENTER else if (i in 3..5) Paint.Align.RIGHT else Paint.Align.LEFT
                val textX = if (i == 0 || i == 6) x + colWidths[i] / 2f else if (i in 3..5) x + colWidths[i] - 4f else x + 4f

                if (i == 6) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (item.dueAmount <= 0) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 14f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 14f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Summary
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        x = MARGIN
        cellBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("মোট হিসাব:", x + (colWidths[0] + colWidths[1] + colWidths[2]) / 2f, y + 14f, cellBoldPaint)
        x += colWidths[0] + colWidths[1] + colWidths[2]

        cellBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(BengaliUtils.formatCurrency(totalTotal), x + colWidths[3] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[3]
        canvas.drawText(BengaliUtils.formatCurrency(totalPaid), x + colWidths[4] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[4]
        canvas.drawText(BengaliUtils.formatCurrency(totalDue), x + colWidths[5] - 4f, y + 14f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Exam_Fees_Report_${System.currentTimeMillis()}.pdf")
    }

    // 8. Exam Result Marksheet & Merit List PDF (With Madrasa, Student Name, Roll, Marks, Total, Merit Position)
    fun generateExamResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        resultsList: List<ExamResult>,
        studentsMap: Map<Long, Student>,
        examType: String,
        year: Int
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "পরীক্ষার ফলাফল ও মেধা তালিকা (রেজাল্ট শিট)", "$examType (${BengaliUtils.toBn(year)})")

        // Sort by total marks descending to calculate merit position
        val sortedResults = resultsList.sortedByDescending { it.totalMarks }

        val headers = listOf("মেধা", "রোল", "ছাত্রের নাম", "কোরআন\n(১০০)", "তাজবীদ\n(১০০)", "কালিমা\n(১০০)", "দোয়া\n(১০০)", "মোট", "অবস্থা")
        val colWidths = listOf(35f, 35f, 105f, 50f, 50f, 50f, 50f, 50f, 53f)
        val rowHeight = 22f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            val singleLineTitle = title.replace("\n", " ")
            canvas.drawText(singleLineTitle, x + colWidths[i] / 2f, y + 15f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        sortedResults.forEachIndexed { rankIdx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (rankIdx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val student = studentsMap[item.studentId]
            val roll = student?.roll?.let { BengaliUtils.toBn(it) } ?: "-"
            val name = student?.name ?: "ছাত্র-${item.studentId}"
            val meritPosition = "${BengaliUtils.toBn(rankIdx + 1)}ম"
            val status = if (item.isAllPassed) "উত্তীর্ণ" else "অনুত্তীর্ণ"

            val values = listOf(
                meritPosition,
                roll,
                name,
                BengaliUtils.toBn(item.quranObtained),
                BengaliUtils.toBn(item.tajweedObtained),
                BengaliUtils.toBn(item.kalimaObtained),
                BengaliUtils.toBn(item.duaHadithObtained),
                BengaliUtils.toBn(item.totalMarks),
                status
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 2) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 2) x + 4f else x + colWidths[i] / 2f

                if (i == 0 || i == 7) {
                    val highlightPaint = Paint(cellBoldPaint).apply {
                        color = Color.rgb(4, 120, 87)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 15f, highlightPaint)
                } else if (i == 8) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (item.isAllPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 15f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 15f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Summary
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        val passedStudents = sortedResults.count { it.isAllPassed }
        val summaryStr = "মোট পরীক্ষার্থী: ${BengaliUtils.toBn(sortedResults.size)} জন  |  পাস: ${BengaliUtils.toBn(passedStudents)} জন  |  ফেল: ${BengaliUtils.toBn(sortedResults.size - passedStudents)} জন"
        cellBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(summaryStr, PAGE_WIDTH / 2f, y + 15f, cellBoldPaint)
        y += rowHeight + 15f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Exam_Result_Sheet_${System.currentTimeMillis()}.pdf")
    }

    // 8B. Individual Student Grade Card PDF (with Student Photo if available, Roll, Name, Individual Marks, Total, Merit Position)
    fun generateIndividualStudentResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        student: Student,
        result: ExamResult,
        meritPosition: Int,
        examType: String,
        year: Int
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "ছাত্র পরীক্ষার মার্কশিট ও ফলাফল কার্ড", "$examType (${BengaliUtils.toBn(year)})")

        // Draw Student Card Box
        val boxTop = y
        val boxHeight = 85f
        val cardPaint = Paint().apply {
            color = Color.rgb(249, 250, 251)
            style = Paint.Style.FILL
        }
        canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxTop + boxHeight, cardPaint)
        canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxTop + boxHeight, borderPaint)

        // Try load student photo if available
        var photoDrawn = false
        if (!student.photoPath.isNullOrEmpty()) {
            val file = File(student.photoPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val photoRect = RectF(MARGIN + 10f, boxTop + 10f, MARGIN + 70f, boxTop + 75f)
                        canvas.drawBitmap(bitmap, null, photoRect, null)
                        photoDrawn = true
                    }
                } catch (e: Exception) {
                    // Ignore photo decode error
                }
            }
        }

        if (!photoDrawn) {
            // Placeholder box
            val photoBox = Paint().apply {
                color = Color.rgb(229, 231, 235)
                style = Paint.Style.FILL
            }
            canvas.drawRect(MARGIN + 10f, boxTop + 10f, MARGIN + 70f, boxTop + 75f, photoBox)
            cellPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("ছবি", MARGIN + 40f, boxTop + 45f, cellPaint)
        }

        // Student Info Text
        val infoLeft = MARGIN + 85f
        cellBoldPaint.textAlign = Paint.Align.LEFT
        cellBoldPaint.textSize = 11f
        canvas.drawText("ছাত্রের নাম: ${student.name}", infoLeft, boxTop + 24f, cellBoldPaint)

        cellPaint.textAlign = Paint.Align.LEFT
        cellPaint.textSize = 9.5f
        canvas.drawText("রোল: ${BengaliUtils.toBn(student.roll)}    |    পিতার নাম: ${student.fatherName}", infoLeft, boxTop + 42f, cellPaint)
        canvas.drawText("ঠিকানা: ${student.address}    |    মোবাইল: ${BengaliUtils.toBn(student.phone)}", infoLeft, boxTop + 60f, cellPaint)

        // Merit Badge on the right
        val meritX = PAGE_WIDTH - MARGIN - 60f
        val meritBadgePaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(meritX - 45f, boxTop + 15f, meritX + 45f, boxTop + 70f, 8f, 8f, meritBadgePaint)
        val meritTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("মেধাস্থান", meritX, boxTop + 33f, meritTextPaint)
        val meritPosPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 224, 71) // Gold
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${BengaliUtils.toBn(meritPosition)}ম", meritX, boxTop + 56f, meritPosPaint)

        y = boxTop + boxHeight + 20f

        // Marks Table
        val headers = listOf("বিষয়সমূহ (মৌখিক)", "পূর্ণমান", "পাস মার্ক", "প্রাপ্ত নম্বর", "ফলাফল")
        val colWidths = listOf(190f, 80f, 80f, 90f, 83f)
        val rowHeight = 24f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 16f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        val subjects = listOf(
            Triple("১. কোরআন মাজীদ (মৌখিক)", result.quranFull to result.quranPass, result.quranObtained),
            Triple("২. তাজবীদ (মৌখিক)", result.tajweedFull to result.tajweedPass, result.tajweedObtained),
            Triple("৩. কালিমা ও মাসায়ালা (মৌখিক)", result.kalimaFull to result.kalimaPass, result.kalimaObtained),
            Triple("৪. দোয়া ও হাদিস (মৌখিক)", result.duaHadithFull to result.duaHadithPass, result.duaHadithObtained)
        )

        subjects.forEachIndexed { idx, sub ->
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val full = sub.second.first
            val pass = sub.second.second
            val obtained = sub.third
            val isPass = obtained >= pass

            val values = listOf(
                sub.first,
                BengaliUtils.toBn(full),
                BengaliUtils.toBn(pass),
                BengaliUtils.toBn(obtained),
                if (isPass) "উত্তীর্ণ" else "অনুত্তীর্ণ"
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 0) x + 8f else x + colWidths[i] / 2f

                if (i == 4) {
                    val statusPaint = Paint(cellBoldPaint).apply {
                        color = if (isPass) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 16f, statusPaint)
                } else {
                    canvas.drawText(text, textX, y + 16f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Total Row
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        x = MARGIN
        cellBoldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("সর্বমোট নম্বর:", x + 8f, y + 16f, cellBoldPaint)
        x += colWidths[0]

        cellBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(BengaliUtils.toBn(result.totalFullMarks), x + colWidths[1] / 2f, y + 16f, cellBoldPaint)
        x += colWidths[1]
        canvas.drawText("-", x + colWidths[2] / 2f, y + 16f, cellBoldPaint)
        x += colWidths[2]
        canvas.drawText(BengaliUtils.toBn(result.totalMarks), x + colWidths[3] / 2f, y + 16f, cellBoldPaint)
        x += colWidths[3]

        val finalStatus = if (result.isAllPassed) "উত্তীর্ণ" else "অনুত্তীর্ণ"
        val statusPaint = Paint(cellBoldPaint).apply {
            color = if (result.isAllPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(finalStatus, x + colWidths[4] / 2f, y + 16f, statusPaint)
        y += rowHeight + 25f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Marksheet_${student.name}_Roll_${student.roll}.pdf")
    }

    // 9. Financial Transactions (খরচ ও জমা) PDF
    fun generateFinancialPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        transactions: List<FinancialTransaction>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "মাদ্রাসার খরচ ও মুহতামিম জমা হিসাব বিবরণী", periodText)

        val headers = listOf("তারিখ", "ধরনের নাম", "বিবরণ / খাত", "খরচ (টাকা)", "মুহতামিম জমা (টাকা)")
        val colWidths = listOf(85f, 100f, 140f, 100f, 98f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        var totalExpense = 0.0
        var totalDeposit = 0.0

        transactions.forEachIndexed { idx, item ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val expenseStr = if (item.type == "খরচ") {
                totalExpense += item.amount
                BengaliUtils.formatCurrency(item.amount)
            } else "-"

            val depositStr = if (item.type == "মুহতামিম জমা") {
                totalDeposit += item.amount
                BengaliUtils.formatCurrency(item.amount)
            } else "-"

            val values = listOf(
                BengaliUtils.formatDateToBn(item.date),
                item.type,
                item.category,
                expenseStr,
                depositStr
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 0 || i == 1) Paint.Align.CENTER else if (i >= 3) Paint.Align.RIGHT else Paint.Align.LEFT
                val textX = if (i == 0 || i == 1) x + colWidths[i] / 2f else if (i >= 3) x + colWidths[i] - 4f else x + 4f
                canvas.drawText(text, textX, y + 14f, cellPaint)
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        // Summary Row
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)
        x = MARGIN
        cellBoldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("মোট হিসাব:", x + (colWidths[0] + colWidths[1] + colWidths[2]) / 2f, y + 14f, cellBoldPaint)
        x += colWidths[0] + colWidths[1] + colWidths[2]

        cellBoldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(BengaliUtils.formatCurrency(totalExpense), x + colWidths[3] - 4f, y + 14f, cellBoldPaint)
        x += colWidths[3]
        canvas.drawText(BengaliUtils.formatCurrency(totalDeposit), x + colWidths[4] - 4f, y + 14f, cellBoldPaint)
        y += rowHeight + 8f

        // Balance Box
        val netBalance = totalDeposit - totalExpense
        val balanceBoxPaint = Paint().apply {
            color = if (netBalance >= 0) Color.rgb(236, 253, 245) else Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f, 6f, 6f, balanceBoxPaint)
        canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f, 6f, 6f, borderPaint)

        val balancePaint = Paint(cellBoldPaint).apply {
            color = if (netBalance >= 0) Color.rgb(4, 120, 87) else Color.rgb(185, 28, 28)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val balanceText = "বর্তমান অবশিষ্ট তহবিল (মুহতামিম জমা - খরচ) = ${BengaliUtils.formatCurrency(netBalance)}"
        canvas.drawText(balanceText, PAGE_WIDTH / 2f, y + 17f, balancePaint)
        y += 40f

        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Financial_Report_${System.currentTimeMillis()}.pdf")
    }

    // 10. Student Attendance PDF
    fun generateAttendancePdf(
        context: Context,
        madrasa: MadrasaInfo?,
        attendanceList: List<AttendanceRecord>,
        students: List<Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, madrasa, "ছাত্র হাজিরা বিবরণী ও পরিসংখ্যান", periodText)

        val headers = listOf("রোল", "ছাত্রের নাম", "উপস্থিত দিন", "অনুপস্থিত দিন", "মোট দিন", "উপস্থিতির হার")
        val colWidths = listOf(45f, 160f, 80f, 80f, 80f, 78f)
        val rowHeight = 20f

        var x = MARGIN
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, headerBgPaint)
        headers.forEachIndexed { i, title ->
            canvas.drawText(title, x + colWidths[i] / 2f, y + 14f, tableHeaderPaint)
            x += colWidths[i]
        }
        y += rowHeight

        // Group attendance by studentId
        val groupedByStudent = attendanceList.groupBy { it.studentId }

        students.forEachIndexed { idx, student ->
            if (y > PAGE_HEIGHT - 90f) return@forEachIndexed
            if (idx % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, zebraPaint)
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, borderPaint)

            val records = groupedByStudent[student.id] ?: emptyList()
            val presentCount = records.count { it.isPresent }
            val absentCount = records.count { !it.isPresent }
            val totalDays = records.size
            val percentage = if (totalDays > 0) (presentCount * 100 / totalDays) else 0

            val values = listOf(
                BengaliUtils.toBn(student.roll),
                student.name,
                BengaliUtils.toBn(presentCount),
                BengaliUtils.toBn(absentCount),
                BengaliUtils.toBn(totalDays),
                "${BengaliUtils.toBn(percentage)}%"
            )

            x = MARGIN
            values.forEachIndexed { i, text ->
                canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                cellPaint.textAlign = if (i == 1) Paint.Align.LEFT else Paint.Align.CENTER
                val textX = if (i == 1) x + 6f else x + colWidths[i] / 2f

                if (i == 5) {
                    val ratePaint = Paint(cellBoldPaint).apply {
                        color = if (percentage >= 80) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(text, textX, y + 14f, ratePaint)
                } else {
                    canvas.drawText(text, textX, y + 14f, cellPaint)
                }
                x += colWidths[i]
            }
            canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
            y += rowHeight
        }

        y += 15f
        drawSignatures(canvas, y)
        document.finishPage(page)
        saveAndSharePdf(context, document, "Attendance_Report_${System.currentTimeMillis()}.pdf")
    }

    private fun saveAndSharePdf(context: Context, document: PdfDocument, fileName: String) {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Hafez Pro রিপোর্ট")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "পিডিএফ প্রিন্ট বা শেয়ার করুন"))
            Toast.makeText(context, "পিডিএফ সফলভাবে তৈরি হয়েছে!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "পিডিএফ তৈরিতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
