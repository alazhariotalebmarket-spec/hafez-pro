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

object PdfReportGenerator {

    private fun drawTableRow(
        writer: PaginatedPdfWriter,
        rowIdx: Int,
        rowHeight: Float,
        colWidths: List<Float>,
        values: List<String>,
        aligns: List<Paint.Align>,
        paints: List<Paint>? = null
    ) {
        writer.ensureSpaceForRow(rowHeight)
        val canvas = writer.canvas
        val y = writer.y

        if (rowIdx % 2 == 1) {
            canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        }
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        var x = PdfConstants.MARGIN
        values.forEachIndexed { i, text ->
            val w = colWidths[i]
            if (i > 0) canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
            PdfDrawHelpers.drawCellText(
                canvas, text, x, y, w, rowHeight,
                paints?.getOrNull(i) ?: PdfDrawHelpers.cellPaint,
                aligns[i]
            )
            x += w
        }
        writer.y += rowHeight
    }

    // 1. Student Admission & Fee List PDF
    fun generateStudentsPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        students: List<Student>,
        periodText: String
    ) {
        val document = PdfDocument()
        val headers = listOf("রোল", "নাম", "পিতার নাম", "মোবাইল", "মাসিক বেতন", "বিদ্যুৎ বিল", "অন্যান্য", "মোট")
        val colWidths = listOf(35f, 95f, 95f, 75f, 55f, 55f, 55f, 58f) // Sum = 523f
        val rowHeight = 20f

        val writer = PaginatedPdfWriter(document, madrasa, "ছাত্র ভর্তি ও মাসিক ফি তালিকা", periodText, headers, colWidths)

        var totalMonthly = 0.0
        var totalElectricity = 0.0
        var totalOther = 0.0
        var grandTotal = 0.0

        val aligns = listOf(
            Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.LEFT,
            Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT
        )

        students.forEachIndexed { idx, student ->
            totalMonthly += student.monthlyFee
            totalElectricity += student.monthlyElectricityBill
            totalOther += student.otherFee
            grandTotal += student.totalMonthlyFee

            drawTableRow(
                writer, idx, rowHeight, colWidths,
                listOf(
                    BengaliUtils.toBn(student.roll),
                    student.name,
                    student.fatherName,
                    BengaliUtils.toBn(student.phone),
                    BengaliUtils.formatCurrency(student.monthlyFee),
                    BengaliUtils.formatCurrency(student.monthlyElectricityBill),
                    BengaliUtils.formatCurrency(student.otherFee),
                    BengaliUtils.formatCurrency(student.totalMonthlyFee)
                ),
                aligns
            )
        }

        // Summary Row
        writer.ensureSpaceForRow(rowHeight + 12f)
        val y = writer.y
        val canvas = writer.canvas
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        var x = PdfConstants.MARGIN
        val labelWidth = colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3]
        PdfDrawHelpers.drawCellText(canvas, "মোট হিসাব:", x, y, labelWidth, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += labelWidth
        listOf(totalMonthly, totalElectricity, totalOther, grandTotal).forEachIndexed { i, amt ->
            val w = colWidths[4 + i]
            canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
            PdfDrawHelpers.drawCellText(canvas, BengaliUtils.formatCurrency(amt), x, y, w, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.RIGHT)
            x += w
        }
        writer.y += rowHeight + 12f

        writer.finishDocumentWithSignatures()
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
        val headers = listOf("রোল", "ছাত্রের নাম", "মাস ও বছর", "বেতন", "বিদ্যুৎ", "অন্যান্য", "জমা", "বাকি")
        val colWidths = listOf(35f, 100f, 85f, 55f, 55f, 55f, 68f, 70f) // Sum = 523f
        val rowHeight = 20f

        val writer = PaginatedPdfWriter(document, madrasa, "ছাত্র বেতন আদায় বিবরণী", periodText, headers, colWidths)

        var totalPaid = 0.0
        var totalDue = 0.0
        val aligns = listOf(
            Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER,
            Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT
        )

        payments.forEachIndexed { idx, p ->
            val student = studentsMap[p.studentId]
            totalPaid += p.paidAmount
            totalDue += p.dueAmount

            drawTableRow(
                writer, idx, rowHeight, colWidths,
                listOf(
                    student?.roll?.let { BengaliUtils.toBn(it) } ?: "-",
                    student?.name ?: "ছাত্র-${p.studentId}",
                    "${p.monthName} ${BengaliUtils.toBn(p.year)}",
                    BengaliUtils.formatCurrency(p.tuitionFee),
                    BengaliUtils.formatCurrency(p.electricityBill),
                    BengaliUtils.formatCurrency(p.otherFee),
                    BengaliUtils.formatCurrency(p.paidAmount),
                    BengaliUtils.formatCurrency(p.dueAmount)
                ),
                aligns
            )
        }

        writer.ensureSpaceForRow(rowHeight + 12f)
        val y = writer.y
        val canvas = writer.canvas
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        var x = PdfConstants.MARGIN
        val labelWidth = colWidths.take(6).sum()
        PdfDrawHelpers.drawCellText(canvas, "সর্বমোট আদায় ও বাকি:", x, y, labelWidth, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += labelWidth
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, BengaliUtils.formatCurrency(totalPaid), x, y, colWidths[6], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.RIGHT)
        x += colWidths[6]
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, BengaliUtils.formatCurrency(totalDue), x, y, colWidths[7], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.RIGHT)
        writer.y += rowHeight + 12f

        writer.finishDocumentWithSignatures()
        saveAndSharePdf(context, document, "Fee_Collection_Report_${System.currentTimeMillis()}.pdf")
    }

    // Shared helper for Sabaq, Sat Sabaq, Amukhta, Hifz Revision PDFs
    private fun generateLessonRegisterPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        title: String,
        periodText: String,
        amountHeader: String,
        filePrefix: String,
        rows: List<LessonPdfRow>
    ) {
        val document = PdfDocument()
        val headers = listOf("তারিখ", "রোল", "ছাত্রের নাম", "পারা", amountHeader, "ফলাফল", "মন্তব্য")
        val colWidths = listOf(72f, 34f, 118f, 45f, 96f, 78f, 80f) // Sum = 523f
        val rowHeight = 20f

        val writer = PaginatedPdfWriter(document, madrasa, title, periodText, headers, colWidths)

        val aligns = listOf(
            Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.LEFT,
            Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.LEFT
        )
        var passedCount = 0

        rows.forEachIndexed { idx, row ->
            if (row.isPassed) passedCount++
            val statusPaint = Paint(PdfDrawHelpers.cellBoldPaint).apply {
                color = if (row.isPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            }
            val paints = listOf(
                PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint,
                PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, statusPaint, PdfDrawHelpers.cellPaint
            )
            drawTableRow(
                writer, idx, rowHeight, colWidths,
                listOf(
                    BengaliUtils.formatDateToBn(row.date),
                    row.rollText,
                    row.studentName,
                    "পারা ${BengaliUtils.toBn(row.paraNumber)}",
                    row.amountText,
                    if (row.isPassed) "✓ উত্তীর্ণ" else "✗ পুনরাবৃত্তি",
                    row.remarks.ifEmpty { "-" }
                ),
                aligns, paints
            )
        }

        writer.ensureSpaceForRow(rowHeight + 12f)
        val y = writer.y
        val canvas = writer.canvas
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)
        val summaryStr = "মোট এন্ট্রি: ${BengaliUtils.toBn(rows.size)} টি  |  উত্তীর্ণ: ${BengaliUtils.toBn(passedCount)} জন  |  পুনরাবৃত্তি: ${BengaliUtils.toBn(rows.size - passedCount)} জন"
        PdfDrawHelpers.drawCellText(canvas, summaryStr, PdfConstants.MARGIN, y, PdfConstants.CONTENT_WIDTH, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        writer.y += rowHeight + 12f

        writer.finishDocumentWithSignatures()
        saveAndSharePdf(context, document, "${filePrefix}_${System.currentTimeMillis()}.pdf")
    }

    private data class LessonPdfRow(
        val date: String,
        val rollText: String,
        val studentName: String,
        val paraNumber: Int,
        val amountText: String,
        val isPassed: Boolean,
        val remarks: String
    )

    // 3. Daily Sabaq PDF
    fun generateDailySabaqPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        sabaqList: List<DailySabaq>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val rows = sabaqList.map { item ->
            val st = studentsMap[item.studentId]
            LessonPdfRow(
                date = item.date,
                rollText = st?.roll?.let { BengaliUtils.toBn(it) } ?: "-",
                studentName = st?.name ?: "ছাত্র-${item.studentId}",
                paraNumber = item.paraNumber,
                amountText = item.lessonType,
                isPassed = item.isPassed,
                remarks = item.remarks
            )
        }
        generateLessonRegisterPdf(context, madrasa, "দৈনিক ছবক রেজিস্টার রিপোর্ট", periodText, "ছবকের পরিমাণ", "Daily_Sabaq_Report", rows)
    }

    // 4. Daily Sat Sabaq PDF
    fun generateDailySatSabaqPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        list: List<DailySatSabaq>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val rows = list.map { item ->
            val st = studentsMap[item.studentId]
            LessonPdfRow(
                date = item.date,
                rollText = st?.roll?.let { BengaliUtils.toBn(it) } ?: "-",
                studentName = st?.name ?: "ছাত্র-${item.studentId}",
                paraNumber = item.paraNumber,
                amountText = item.pagesCount,
                isPassed = item.isPassed,
                remarks = item.remarks
            )
        }
        generateLessonRegisterPdf(context, madrasa, "দৈনিক সাত ছবক রেজিস্টার রিপোর্ট", periodText, "পৃষ্ঠা / ছাপা", "Daily_Sat_Sabaq_Report", rows)
    }

    // 5. Daily Amukhta PDF
    fun generateDailyAmukhtaPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        list: List<DailyAmukhta>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val rows = list.map { item ->
            val st = studentsMap[item.studentId]
            LessonPdfRow(
                date = item.date,
                rollText = st?.roll?.let { BengaliUtils.toBn(it) } ?: "-",
                studentName = st?.name ?: "ছাত্র-${item.studentId}",
                paraNumber = item.paraNumber,
                amountText = item.amount,
                isPassed = item.isPassed,
                remarks = item.remarks
            )
        }
        generateLessonRegisterPdf(context, madrasa, "দৈনিক আমুখতা রেজিস্টার রিপোর্ট", periodText, "আমুখতা", "Daily_Amukhta_Report", rows)
    }

    // 6. Hifz Revision PDF
    fun generateHifzRevisionPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        list: List<HifzRevision>,
        studentsMap: Map<Long, Student>,
        periodText: String
    ) {
        val rows = list.map { item ->
            val st = studentsMap[item.studentId]
            LessonPdfRow(
                date = item.date,
                rollText = st?.roll?.let { BengaliUtils.toBn(it) } ?: "-",
                studentName = st?.name ?: "ছাত্র-${item.studentId}",
                paraNumber = item.paraNumber,
                amountText = item.amount,
                isPassed = item.isPassed,
                remarks = item.remarks
            )
        }
        generateLessonRegisterPdf(context, madrasa, "হিফজ রিভিশন রেজিস্টার রিপোর্ট", periodText, "রিভিশন", "Hifz_Revision_Report", rows)
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
        val headers = listOf("রোল", "ছাত্রের নাম", "পরীক্ষা", "ধার্য ফি", "জমা", "বাকি", "অবস্থা")
        val colWidths = listOf(40f, 120f, 110f, 65f, 65f, 65f, 58f) // Sum = 523f
        val rowHeight = 20f

        val writer = PaginatedPdfWriter(document, madrasa, "পরীক্ষার ফি আদায় রিপোর্ট", "$examType (${BengaliUtils.toBn(year)})", headers, colWidths)

        var totalTotal = 0.0
        var totalPaid = 0.0
        var totalDue = 0.0
        val aligns = listOf(
            Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.LEFT,
            Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.CENTER
        )

        feesList.forEachIndexed { idx, item ->
            val student = studentsMap[item.studentId]
            totalTotal += item.totalFee
            totalPaid += item.paidAmount
            totalDue += item.dueAmount

            val statusPaint = Paint(PdfDrawHelpers.cellBoldPaint).apply {
                color = if (item.dueAmount <= 0) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            }
            val paints = listOf(
                PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint,
                PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, statusPaint
            )

            drawTableRow(
                writer, idx, rowHeight, colWidths,
                listOf(
                    student?.roll?.let { BengaliUtils.toBn(it) } ?: "-",
                    student?.name ?: "ছাত্র-${item.studentId}",
                    item.examType,
                    BengaliUtils.formatCurrency(item.totalFee),
                    BengaliUtils.formatCurrency(item.paidAmount),
                    BengaliUtils.formatCurrency(item.dueAmount),
                    if (item.dueAmount <= 0) "পরিশোধিত" else "বাকি আছে"
                ),
                aligns, paints
            )
        }

        writer.ensureSpaceForRow(rowHeight + 12f)
        val y = writer.y
        val canvas = writer.canvas
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        var x = PdfConstants.MARGIN
        val labelWidth = colWidths[0] + colWidths[1] + colWidths[2]
        PdfDrawHelpers.drawCellText(canvas, "মোট হিসাব:", x, y, labelWidth, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += labelWidth
        listOf(totalTotal, totalPaid, totalDue).forEachIndexed { i, amt ->
            val w = colWidths[3 + i]
            canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
            PdfDrawHelpers.drawCellText(canvas, BengaliUtils.formatCurrency(amt), x, y, w, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.RIGHT)
            x += w
        }
        writer.y += rowHeight + 12f

        writer.finishDocumentWithSignatures()
        saveAndSharePdf(context, document, "Exam_Fees_Report_${System.currentTimeMillis()}.pdf")
    }

    // 8A. Single Exam Result Sheet PDF
    fun generateExamResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        resultsList: List<ExamResult>,
        studentsMap: Map<Long, Student>,
        examType: String,
        year: Int
    ) {
        ExamPdfGenerator.generateExamResultPdf(context, madrasa, resultsList, studentsMap, examType, year) { doc, fileName ->
            saveAndSharePdf(context, doc, fileName)
        }
    }

    // 8B. Combined Annual 3-Exam Result Sheet PDF
    fun generateCombinedAnnualExamResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        students: List<Student>,
        allResultsForYear: List<ExamResult>,
        year: Int
    ) {
        ExamPdfGenerator.generateCombinedAnnualExamResultPdf(context, madrasa, students, allResultsForYear, year) { doc, fileName ->
            saveAndSharePdf(context, doc, fileName)
        }
    }

    // 8C. Individual Student Single-Exam Marksheet PDF
    fun generateIndividualStudentResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        student: Student,
        result: ExamResult,
        meritPosition: Int,
        examType: String,
        year: Int
    ) {
        ExamPdfGenerator.generateIndividualStudentResultPdf(context, madrasa, student, result, meritPosition, examType, year) { doc, fileName ->
            saveAndSharePdf(context, doc, fileName)
        }
    }

    // 8D. Individual Student Combined 3-Exam Marksheet PDF
    fun generateCombinedIndividualStudentResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        summary: CombinedStudentExamSummary,
        year: Int
    ) {
        ExamPdfGenerator.generateCombinedIndividualStudentResultPdf(context, madrasa, summary, year) { doc, fileName ->
            saveAndSharePdf(context, doc, fileName)
        }
    }

    // 9. Financial Transactions (খরচ ও জমা) PDF
    fun generateFinancialPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        transactions: List<FinancialTransaction>,
        periodText: String
    ) {
        val document = PdfDocument()
        val headers = listOf("তারিখ", "ধরনের নাম", "বিবরণ / খাত", "খরচ (টাকা)", "মুহতামিম জমা")
        val colWidths = listOf(85f, 100f, 140f, 100f, 98f) // Sum = 523f
        val rowHeight = 20f

        val writer = PaginatedPdfWriter(document, madrasa, "মাদ্রাসার খরচ ও মুহতামিম জমা হিসাব বিবরণী", periodText, headers, colWidths)

        var totalExpense = 0.0
        var totalDeposit = 0.0
        val aligns = listOf(Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT)

        transactions.forEachIndexed { idx, item ->
            val expenseStr = if (item.type == "খরচ") {
                totalExpense += item.amount
                BengaliUtils.formatCurrency(item.amount)
            } else "-"

            val depositStr = if (item.type == "মুহতামিম জমা") {
                totalDeposit += item.amount
                BengaliUtils.formatCurrency(item.amount)
            } else "-"

            drawTableRow(
                writer, idx, rowHeight, colWidths,
                listOf(BengaliUtils.formatDateToBn(item.date), item.type, item.category, expenseStr, depositStr),
                aligns
            )
        }

        writer.ensureSpaceForRow(rowHeight + 46f)
        val y = writer.y
        val canvas = writer.canvas
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        var x = PdfConstants.MARGIN
        val labelWidth = colWidths[0] + colWidths[1] + colWidths[2]
        PdfDrawHelpers.drawCellText(canvas, "মোট হিসাব:", x, y, labelWidth, rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += labelWidth
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, BengaliUtils.formatCurrency(totalExpense), x, y, colWidths[3], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.RIGHT)
        x += colWidths[3]
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, BengaliUtils.formatCurrency(totalDeposit), x, y, colWidths[4], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.RIGHT)

        val boxY = y + rowHeight + 8f
        val netBalance = totalDeposit - totalExpense
        val balanceBoxPaint = Paint().apply {
            color = if (netBalance >= 0) Color.rgb(236, 253, 245) else Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(PdfConstants.MARGIN, boxY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, boxY + 26f, 6f, 6f, balanceBoxPaint)
        canvas.drawRoundRect(PdfConstants.MARGIN, boxY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, boxY + 26f, 6f, 6f, PdfDrawHelpers.borderPaint)

        val balancePaint = Paint(PdfDrawHelpers.cellBoldPaint).apply {
            color = if (netBalance >= 0) Color.rgb(4, 120, 87) else Color.rgb(185, 28, 28)
            textSize = 9.5f
        }
        PdfDrawHelpers.drawCellText(
            canvas,
            "বর্তমান অবশিষ্ট তহবিল (মুহতামিম জমা - খরচ) = ${BengaliUtils.formatCurrency(netBalance)}",
            PdfConstants.MARGIN, boxY, PdfConstants.CONTENT_WIDTH, 26f,
            balancePaint, Paint.Align.CENTER
        )
        writer.y = boxY + 34f

        writer.finishDocumentWithSignatures()
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
        val headers = listOf("রোল", "ছাত্রের নাম", "উপস্থিত দিন", "অনুপস্থিত দিন", "মোট দিন", "উপস্থিতির হার")
        val colWidths = listOf(45f, 160f, 80f, 80f, 80f, 78f) // Sum = 523f
        val rowHeight = 20f

        val writer = PaginatedPdfWriter(document, madrasa, "ছাত্র হাজিরা বিবরণী ও পরিসংখ্যান", periodText, headers, colWidths)

        val groupedByStudent = attendanceList.groupBy { it.studentId }
        val aligns = listOf(
            Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER,
            Paint.Align.CENTER, Paint.Align.CENTER, Paint.Align.CENTER
        )

        students.forEachIndexed { idx, student ->
            val records = groupedByStudent[student.id] ?: emptyList()
            val presentCount = records.count { it.isPresent }
            val absentCount = records.count { !it.isPresent }
            val totalDays = records.size
            val percentage = if (totalDays > 0) (presentCount * 100 / totalDays) else 0

            val ratePaint = Paint(PdfDrawHelpers.cellBoldPaint).apply {
                color = if (percentage >= 80) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            }
            val paints = listOf(
                PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint,
                PdfDrawHelpers.cellPaint, PdfDrawHelpers.cellPaint, ratePaint
            )

            drawTableRow(
                writer, idx, rowHeight, colWidths,
                listOf(
                    BengaliUtils.toBn(student.roll),
                    student.name,
                    BengaliUtils.toBn(presentCount),
                    BengaliUtils.toBn(absentCount),
                    BengaliUtils.toBn(totalDays),
                    "${BengaliUtils.toBn(percentage)}%"
                ),
                aligns, paints
            )
        }

        writer.y += 12f
        writer.finishDocumentWithSignatures()
        saveAndSharePdf(context, document, "Attendance_Report_${System.currentTimeMillis()}.pdf")
    }

    private fun saveAndSharePdf(context: Context, document: PdfDocument, fileName: String) {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
                outputStream.flush()
            }
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
