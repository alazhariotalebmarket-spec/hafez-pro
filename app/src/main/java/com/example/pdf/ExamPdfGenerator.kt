package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.ExamResult
import com.example.data.MadrasaInfo
import com.example.data.Student
import com.example.util.BengaliUtils
import java.io.File

object ExamPdfGenerator {

    // 8A. Single Exam Result Sheet PDF (Multi-page, Zero Overlap)
    fun generateExamResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        resultsList: List<ExamResult>,
        studentsMap: Map<Long, Student>,
        examType: String,
        year: Int,
        onFinishPdf: (PdfDocument, String) -> Unit
    ) {
        val document = PdfDocument()
        val headers = listOf(
            "মেধা",
            "রোল",
            "ছাত্রের নাম",
            "কোরআন\n(১০০)",
            "তাজবীদ\n(১০০)",
            "কালিমা\n(১০০)",
            "দোয়া\n(১০০)",
            "মোট",
            "অবস্থা"
        )
        val colWidths = listOf(36f, 34f, 128f, 53f, 53f, 53f, 53f, 55f, 58f) // Sum = 523f
        val rowHeight = 22f

        val writer = PaginatedPdfWriter(
            document = document,
            madrasa = madrasa,
            reportTitle = "পরীক্ষার ফলাফল ও মেধা তালিকা (রেজাল্ট শিট)",
            periodText = "$examType (${BengaliUtils.toBn(year)})",
            headers = headers,
            colWidths = colWidths
        )

        val sortedResults = resultsList.sortedByDescending { it.totalMarks }

        if (sortedResults.isEmpty()) {
            writer.ensureSpaceForRow(rowHeight)
            val canvas = writer.canvas
            val y = writer.y
            canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)
            PdfDrawHelpers.drawCellText(
                canvas,
                "এই পরীক্ষার জন্য কোনো নম্বর এখনও এন্ট্রি করা হয়নি",
                PdfConstants.MARGIN, y, PdfConstants.CONTENT_WIDTH, rowHeight,
                PdfDrawHelpers.cellPaint, Paint.Align.CENTER
            )
            writer.y += rowHeight
        } else {
            sortedResults.forEachIndexed { rankIdx, item ->
                writer.ensureSpaceForRow(rowHeight)
                val canvas = writer.canvas
                val y = writer.y

                if (rankIdx % 2 == 1) {
                    canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
                }
                canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

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

                var x = PdfConstants.MARGIN
                values.forEachIndexed { i, text ->
                    val w = colWidths[i]
                    if (i > 0) canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
                    val align = if (i == 2) Paint.Align.LEFT else Paint.Align.CENTER
                    val paint = when (i) {
                        0, 7 -> Paint(PdfDrawHelpers.cellBoldPaint).apply { color = Color.rgb(4, 120, 87) }
                        8 -> Paint(PdfDrawHelpers.cellBoldPaint).apply {
                            color = if (item.isAllPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        }
                        else -> PdfDrawHelpers.cellPaint
                    }
                    PdfDrawHelpers.drawCellText(canvas, text, x, y, w, rowHeight, paint, align)
                    x += w
                }
                writer.y += rowHeight
            }
        }

        // Summary Row
        writer.ensureSpaceForRow(rowHeight + 10f)
        val summaryY = writer.y
        val summaryCanvas = writer.canvas
        summaryCanvas.drawRect(PdfConstants.MARGIN, summaryY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, summaryY + rowHeight, PdfDrawHelpers.zebraPaint)
        summaryCanvas.drawRect(PdfConstants.MARGIN, summaryY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, summaryY + rowHeight, PdfDrawHelpers.borderPaint)
        val passedStudents = sortedResults.count { it.isAllPassed }
        val summaryStr = "মোট পরীক্ষার্থী: ${BengaliUtils.toBn(sortedResults.size)} জন  |  পাস: ${BengaliUtils.toBn(passedStudents)} জন  |  ফেল: ${BengaliUtils.toBn(sortedResults.size - passedStudents)} জন"
        PdfDrawHelpers.drawCellText(
            summaryCanvas, summaryStr,
            PdfConstants.MARGIN, summaryY, PdfConstants.CONTENT_WIDTH, rowHeight,
            PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER
        )
        writer.y += rowHeight + 12f

        writer.finishDocumentWithSignatures()
        onFinishPdf(document, "Exam_Result_Sheet_${System.currentTimeMillis()}.pdf")
    }

    // 8B. Combined Annual 3-Exam Result Sheet PDF (১ম সাময়িক + ২য় সাময়িক + বার্ষিক একত্রে)
    fun generateCombinedAnnualExamResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        students: List<Student>,
        allResultsForYear: List<ExamResult>,
        year: Int,
        onFinishPdf: (PdfDocument, String) -> Unit
    ) {
        val document = PdfDocument()
        val summaries = PdfDrawHelpers.buildCombinedExamSummaries(students, allResultsForYear)

        val headers = listOf(
            "মেধা",
            "রোল",
            "ছাত্রের নাম",
            "১ম সাময়িক\n(প্রাপ্ত/পূর্ণ)",
            "২য় সাময়িক\n(প্রাপ্ত/পূর্ণ)",
            "বার্ষিক\n(প্রাপ্ত/পূর্ণ)",
            "সর্বমোট\n(৩ পরীক্ষা)",
            "অবস্থা"
        )
        val colWidths = listOf(36f, 32f, 125f, 70f, 70f, 70f, 62f, 58f) // Sum = 523f
        val rowHeight = 22f

        val writer = PaginatedPdfWriter(
            document = document,
            madrasa = madrasa,
            reportTitle = "বাৎসরিক সমন্বিত পরীক্ষার ফলাফল (৩ পরীক্ষা একত্রে)",
            periodText = "১ম সাময়িক + ২য় সাময়িক + বার্ষিক (${BengaliUtils.toBn(year)})",
            headers = headers,
            colWidths = colWidths
        )

        if (summaries.isEmpty()) {
            writer.ensureSpaceForRow(rowHeight)
            val canvas = writer.canvas
            val y = writer.y
            canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)
            PdfDrawHelpers.drawCellText(
                canvas,
                "এই শিক্ষাবর্ষের কোনো পরীক্ষার ফলাফল এখনও এন্ট্রি করা হয়নি",
                PdfConstants.MARGIN, y, PdfConstants.CONTENT_WIDTH, rowHeight,
                PdfDrawHelpers.cellPaint, Paint.Align.CENTER
            )
            writer.y += rowHeight
        } else {
            summaries.forEachIndexed { idx, item ->
                writer.ensureSpaceForRow(rowHeight)
                val canvas = writer.canvas
                val y = writer.y

                if (idx % 2 == 1) {
                    canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
                }
                canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

                fun formatTermScore(res: ExamResult?): String {
                    return if (res == null) "-" else "${BengaliUtils.toBn(res.totalMarks)}/${BengaliUtils.toBn(res.totalFullMarks)}"
                }

                val meritText = "${BengaliUtils.toBn(item.meritPosition)}ম"
                val rollText = BengaliUtils.toBn(item.student.roll)
                val grandText = "${BengaliUtils.toBn(item.grandTotalObtained)}/${BengaliUtils.toBn(item.grandTotalFull)}"
                val statusText = when {
                    !item.isOverallPassed -> "অনুত্তীর্ণ"
                    item.enteredExamCount < 3 -> "উত্তীর্ণ*"
                    else -> "উত্তীর্ণ"
                }

                val values = listOf(
                    meritText,
                    rollText,
                    item.student.name,
                    formatTermScore(item.firstTerm),
                    formatTermScore(item.secondTerm),
                    formatTermScore(item.annualTerm),
                    grandText,
                    statusText
                )

                var x = PdfConstants.MARGIN
                values.forEachIndexed { i, text ->
                    val w = colWidths[i]
                    if (i > 0) canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
                    val align = if (i == 2) Paint.Align.LEFT else Paint.Align.CENTER
                    val paint = when (i) {
                        0, 6 -> Paint(PdfDrawHelpers.cellBoldPaint).apply { color = Color.rgb(4, 120, 87) }
                        7 -> Paint(PdfDrawHelpers.cellBoldPaint).apply {
                            color = if (item.isOverallPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                        }
                        else -> PdfDrawHelpers.cellPaint
                    }
                    PdfDrawHelpers.drawCellText(canvas, text, x, y, w, rowHeight, paint, align)
                    x += w
                }
                writer.y += rowHeight
            }
        }

        // Summary Row
        writer.ensureSpaceForRow(rowHeight + 22f)
        val summaryY = writer.y
        val summaryCanvas = writer.canvas
        summaryCanvas.drawRect(PdfConstants.MARGIN, summaryY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, summaryY + rowHeight, PdfDrawHelpers.zebraPaint)
        summaryCanvas.drawRect(PdfConstants.MARGIN, summaryY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, summaryY + rowHeight, PdfDrawHelpers.borderPaint)

        val passedCount = summaries.count { it.isOverallPassed }
        val summaryStr = "মোট পরীক্ষার্থী: ${BengaliUtils.toBn(summaries.size)} জন  |  উত্তীর্ণ: ${BengaliUtils.toBn(passedCount)} জন  |  অনুত্তীর্ণ: ${BengaliUtils.toBn(summaries.size - passedCount)} জন"
        PdfDrawHelpers.drawCellText(
            summaryCanvas, summaryStr,
            PdfConstants.MARGIN, summaryY, PdfConstants.CONTENT_WIDTH, rowHeight,
            PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER
        )
        writer.y += rowHeight + 6f

        if (summaries.any { it.enteredExamCount < 3 }) {
            val notePaint = Paint(PdfDrawHelpers.cellPaint).apply {
                textSize = 7.8f
                color = Color.DKGRAY
            }
            PdfDrawHelpers.drawCellText(
                writer.canvas,
                "* দ্রষ্টব্য: যেসব ছাত্রের নামের পাশে (*) চিহ্ন আছে, তাদের ১ বা ২টি পরীক্ষার নম্বর এন্ট্রি হয়েছে।",
                PdfConstants.MARGIN, writer.y, PdfConstants.CONTENT_WIDTH, 14f,
                notePaint, Paint.Align.LEFT
            )
            writer.y += 14f
        }

        writer.finishDocumentWithSignatures()
        onFinishPdf(document, "Combined_Annual_Exam_Result_${year}_${System.currentTimeMillis()}.pdf")
    }

    // 8C. Individual Student Single-Exam Marksheet PDF
    fun generateIndividualStudentResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        student: Student,
        result: ExamResult,
        meritPosition: Int,
        examType: String,
        year: Int,
        onFinishPdf: (PdfDocument, String) -> Unit
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PdfConstants.PAGE_WIDTH, PdfConstants.PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = PdfDrawHelpers.drawDocumentHeader(
            canvas,
            madrasa,
            "ছাত্র পরীক্ষার মার্কশিট ও ফলাফল কার্ড",
            "$examType (${BengaliUtils.toBn(year)})"
        )

        y = drawStudentProfileCard(canvas, y, student, meritPosition, "মেধাস্থান")
        y += 16f

        val headers = listOf("বিষয়সমূহ (মৌখিক)", "পূর্ণমান", "পাস মার্ক", "প্রাপ্ত নম্বর", "ফলাফল")
        val colWidths = listOf(190f, 80f, 80f, 90f, 83f) // Sum = 523f
        val rowHeight = 24f

        y = PdfDrawHelpers.drawTableHeader(canvas, y, headers, colWidths)

        val subjects = listOf(
            Triple("১. কোরআন মাজীদ (মৌখিক)", result.quranFull to result.quranPass, result.quranObtained),
            Triple("২. তাজবীদ (মৌখিক)", result.tajweedFull to result.tajweedPass, result.tajweedObtained),
            Triple("৩. কালিমা ও মাসায়ালা (মৌখিক)", result.kalimaFull to result.kalimaPass, result.kalimaObtained),
            Triple("৪. দোয়া ও হাদিস (মৌখিক)", result.duaHadithFull to result.duaHadithPass, result.duaHadithObtained)
        )

        subjects.forEachIndexed { idx, sub ->
            if (idx % 2 == 1) canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
            canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

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

            var x = PdfConstants.MARGIN
            values.forEachIndexed { i, text ->
                val w = colWidths[i]
                if (i > 0) canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
                val align = if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
                val paint = if (i == 4) {
                    Paint(PdfDrawHelpers.cellBoldPaint).apply {
                        color = if (isPass) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                    }
                } else {
                    PdfDrawHelpers.cellPaint
                }
                PdfDrawHelpers.drawCellText(canvas, text, x, y, w, rowHeight, paint, align, padding = 6f)
                x += w
            }
            y += rowHeight
        }

        // Total Row
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        var x = PdfConstants.MARGIN
        PdfDrawHelpers.drawCellText(canvas, "সর্বমোট নম্বর:", x, y, colWidths[0], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.LEFT, padding = 6f)
        x += colWidths[0]
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, BengaliUtils.toBn(result.totalFullMarks), x, y, colWidths[1], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += colWidths[1]
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, "-", x, y, colWidths[2], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += colWidths[2]
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
        PdfDrawHelpers.drawCellText(canvas, BengaliUtils.toBn(result.totalMarks), x, y, colWidths[3], rowHeight, PdfDrawHelpers.cellBoldPaint, Paint.Align.CENTER)
        x += colWidths[3]
        canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)

        val statusPaint = Paint(PdfDrawHelpers.cellBoldPaint).apply {
            color = if (result.isAllPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
        }
        PdfDrawHelpers.drawCellText(
            canvas,
            if (result.isAllPassed) "উত্তীর্ণ" else "অনুত্তীর্ণ",
            x, y, colWidths[4], rowHeight, statusPaint, Paint.Align.CENTER
        )
        y += rowHeight + 20f

        PdfDrawHelpers.drawSignatures(canvas, y)
        PdfDrawHelpers.drawPageFooter(canvas, 1)
        document.finishPage(page)
        onFinishPdf(document, "Marksheet_${student.name}_Roll_${student.roll}.pdf")
    }

    // 8D. Combined 3-Exam Individual Student Marksheet PDF (Single A4 page, Zero Overlap)
    fun generateCombinedIndividualStudentResultPdf(
        context: Context,
        madrasa: MadrasaInfo?,
        summary: CombinedStudentExamSummary,
        year: Int,
        onFinishPdf: (PdfDocument, String) -> Unit
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PdfConstants.PAGE_WIDTH, PdfConstants.PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val student = summary.student
        var y = PdfDrawHelpers.drawDocumentHeader(
            canvas,
            madrasa,
            "বার্ষিক সমন্বিত পরীক্ষার মার্কশিট (৩ পরীক্ষা একত্রে)",
            "শিক্ষাবর্ষ: ${BengaliUtils.toBn(year)}  |  ১ম সাময়িক + ২য় সাময়িক + বার্ষিক"
        )

        y = drawStudentProfileCard(canvas, y, student, summary.meritPosition, "সমন্বিত মেধাস্থান")
        y += 14f

        val headers = listOf(
            "বিষয়সমূহ (মৌখিক)",
            "১ম সাময়িক\n(প্রাপ্ত/পূর্ণ)",
            "২য় সাময়িক\n(প্রাপ্ত/পূর্ণ)",
            "বার্ষিক\n(প্রাপ্ত/পূর্ণ)",
            "বিষয় মোট\n(প্রাপ্ত/পূর্ণ)",
            "ফলাফল"
        )
        val colWidths = listOf(155f, 78f, 78f, 78f, 74f, 60f) // Sum = 523f
        val rowHeight = 24f

        y = PdfDrawHelpers.drawTableHeader(canvas, y, headers, colWidths)

        data class SubjectRow(
            val title: String,
            val t1Obt: Int?, val t1Full: Int?, val t1Pass: Int?,
            val t2Obt: Int?, val t2Full: Int?, val t2Pass: Int?,
            val t3Obt: Int?, val t3Full: Int?, val t3Pass: Int?
        )

        val f1 = summary.firstTerm
        val f2 = summary.secondTerm
        val f3 = summary.annualTerm

        val rows = listOf(
            SubjectRow(
                "১. কোরআন মাজীদ",
                f1?.quranObtained, f1?.quranFull, f1?.quranPass,
                f2?.quranObtained, f2?.quranFull, f2?.quranPass,
                f3?.quranObtained, f3?.quranFull, f3?.quranPass
            ),
            SubjectRow(
                "২. তাজবীদ",
                f1?.tajweedObtained, f1?.tajweedFull, f1?.tajweedPass,
                f2?.tajweedObtained, f2?.tajweedFull, f2?.tajweedPass,
                f3?.tajweedObtained, f3?.tajweedFull, f3?.tajweedPass
            ),
            SubjectRow(
                "৩. কালিমা ও মাসায়ালা",
                f1?.kalimaObtained, f1?.kalimaFull, f1?.kalimaPass,
                f2?.kalimaObtained, f2?.kalimaFull, f2?.kalimaPass,
                f3?.kalimaObtained, f3?.kalimaFull, f3?.kalimaPass
            ),
            SubjectRow(
                "৪. দোয়া ও হাদিস",
                f1?.duaHadithObtained, f1?.duaHadithFull, f1?.duaHadithPass,
                f2?.duaHadithObtained, f2?.duaHadithFull, f2?.duaHadithPass,
                f3?.duaHadithObtained, f3?.duaHadithFull, f3?.duaHadithPass
            )
        )

        fun fmtMark(obt: Int?, full: Int?): String {
            return if (obt == null || full == null) "-" else "${BengaliUtils.toBn(obt)}/${BengaliUtils.toBn(full)}"
        }

        rows.forEachIndexed { idx, row ->
            if (idx % 2 == 1) canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
            canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

            val subObt = (row.t1Obt ?: 0) + (row.t2Obt ?: 0) + (row.t3Obt ?: 0)
            val subFull = (row.t1Full ?: 0) + (row.t2Full ?: 0) + (row.t3Full ?: 0)
            val subPassOk = listOf(
                row.t1Obt?.let { it >= (row.t1Pass ?: 0) },
                row.t2Obt?.let { it >= (row.t2Pass ?: 0) },
                row.t3Obt?.let { it >= (row.t3Pass ?: 0) }
            ).filterNotNull().let { it.isNotEmpty() && it.all { ok -> ok } }

            val values = listOf(
                row.title,
                fmtMark(row.t1Obt, row.t1Full),
                fmtMark(row.t2Obt, row.t2Full),
                fmtMark(row.t3Obt, row.t3Full),
                "${BengaliUtils.toBn(subObt)}/${BengaliUtils.toBn(subFull)}",
                if (subPassOk) "উত্তীর্ণ" else "অনুত্তীর্ণ"
            )

            var x = PdfConstants.MARGIN
            values.forEachIndexed { i, text ->
                val w = colWidths[i]
                if (i > 0) canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
                val align = if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
                val paint = when (i) {
                    4 -> PdfDrawHelpers.cellBoldPaint
                    5 -> Paint(PdfDrawHelpers.cellBoldPaint).apply {
                        color = if (subPassOk) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                    }
                    else -> PdfDrawHelpers.cellPaint
                }
                PdfDrawHelpers.drawCellText(canvas, text, x, y, w, rowHeight, paint, align, padding = 5f)
                x += w
            }
            y += rowHeight
        }

        // Grand Total Row
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.zebraPaint)
        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + rowHeight, PdfDrawHelpers.borderPaint)

        val totalValues = listOf(
            "সর্বমোট নম্বর (৩ পরীক্ষা)",
            fmtMark(f1?.totalMarks, f1?.totalFullMarks),
            fmtMark(f2?.totalMarks, f2?.totalFullMarks),
            fmtMark(f3?.totalMarks, f3?.totalFullMarks),
            "${BengaliUtils.toBn(summary.grandTotalObtained)}/${BengaliUtils.toBn(summary.grandTotalFull)}",
            if (summary.isOverallPassed) "উত্তীর্ণ" else "অনুত্তীর্ণ"
        )
        var x = PdfConstants.MARGIN
        totalValues.forEachIndexed { i, text ->
            val w = colWidths[i]
            if (i > 0) canvas.drawLine(x, y, x, y + rowHeight, PdfDrawHelpers.borderPaint)
            val align = if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
            val paint = if (i == 5) {
                Paint(PdfDrawHelpers.cellBoldPaint).apply {
                    color = if (summary.isOverallPassed) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
                }
            } else {
                PdfDrawHelpers.cellBoldPaint
            }
            PdfDrawHelpers.drawCellText(canvas, text, x, y, w, rowHeight, paint, align, padding = 5f)
            x += w
        }
        y += rowHeight + 14f

        // Overall Summary Highlight Box
        val boxBg = Paint().apply {
            color = if (summary.isOverallPassed) Color.rgb(236, 253, 245) else Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + 34f, 6f, 6f, boxBg)
        canvas.drawRoundRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, y + 34f, 6f, 6f, PdfDrawHelpers.borderPaint)

        val summaryPaint = Paint(PdfDrawHelpers.cellBoldPaint).apply {
            color = if (summary.isOverallPassed) Color.rgb(4, 120, 87) else Color.rgb(185, 28, 28)
            textSize = 9.5f
        }
        val boxText = "তিন পরীক্ষার সর্বমোট: ${BengaliUtils.toBn(summary.grandTotalObtained)} / ${BengaliUtils.toBn(summary.grandTotalFull)}  |  গড় নম্বর: ${BengaliUtils.toBn(summary.percentage)}%  |  সম্মিলিত মেধাস্থান: ${BengaliUtils.toBn(summary.meritPosition)}ম"
        PdfDrawHelpers.drawCellText(
            canvas, boxText,
            PdfConstants.MARGIN, y, PdfConstants.CONTENT_WIDTH, 34f,
            summaryPaint, Paint.Align.CENTER
        )
        y += 50f

        PdfDrawHelpers.drawSignatures(canvas, y)
        PdfDrawHelpers.drawPageFooter(canvas, 1)
        document.finishPage(page)
        onFinishPdf(document, "Combined_Marksheet_${student.name}_Roll_${student.roll}.pdf")
    }

    private fun drawStudentProfileCard(
        canvas: Canvas,
        topY: Float,
        student: Student,
        meritPosition: Int,
        meritLabel: String
    ): Float {
        val boxHeight = 82f
        val cardPaint = Paint().apply {
            color = Color.rgb(249, 250, 251)
            style = Paint.Style.FILL
        }
        canvas.drawRect(PdfConstants.MARGIN, topY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, topY + boxHeight, cardPaint)
        canvas.drawRect(PdfConstants.MARGIN, topY, PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH, topY + boxHeight, PdfDrawHelpers.borderPaint)

        var photoDrawn = false
        if (!student.photoPath.isNullOrEmpty()) {
            val file = File(student.photoPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val photoRect = RectF(PdfConstants.MARGIN + 10f, topY + 9f, PdfConstants.MARGIN + 70f, topY + 73f)
                        canvas.drawBitmap(bitmap, null, photoRect, null)
                        photoDrawn = true
                    }
                } catch (_: Exception) {
                }
            }
        }
        if (!photoDrawn) {
            val photoBox = Paint().apply {
                color = Color.rgb(229, 231, 235)
                style = Paint.Style.FILL
            }
            canvas.drawRect(PdfConstants.MARGIN + 10f, topY + 9f, PdfConstants.MARGIN + 70f, topY + 73f, photoBox)
            PdfDrawHelpers.drawCellText(
                canvas, "ছবি",
                PdfConstants.MARGIN + 10f, topY + 9f, 60f, 64f,
                PdfDrawHelpers.cellPaint, Paint.Align.CENTER
            )
        }

        val infoLeft = PdfConstants.MARGIN + 80f
        val badgeLeft = PdfConstants.MARGIN + PdfConstants.CONTENT_WIDTH - 102f
        val infoWidth = (badgeLeft - infoLeft - 6f).coerceAtLeast(150f)

        val namePaint = Paint(PdfDrawHelpers.cellBoldPaint).apply { textSize = 11f }
        PdfDrawHelpers.drawCellText(
            canvas, "ছাত্রের নাম: ${student.name}",
            infoLeft, topY + 10f, infoWidth, 20f,
            namePaint, Paint.Align.LEFT, padding = 2f
        )

        val infoPaint = Paint(PdfDrawHelpers.cellPaint).apply { textSize = 9.2f }
        PdfDrawHelpers.drawCellText(
            canvas, "রোল: ${BengaliUtils.toBn(student.roll)}  |  পিতার নাম: ${student.fatherName}",
            infoLeft, topY + 32f, infoWidth, 18f,
            infoPaint, Paint.Align.LEFT, padding = 2f
        )

        PdfDrawHelpers.drawCellText(
            canvas, "ঠিকানা: ${student.address}  |  মোবাইল: ${BengaliUtils.toBn(student.phone)}",
            infoLeft, topY + 52f, infoWidth, 18f,
            infoPaint, Paint.Align.LEFT, padding = 2f
        )

        val badgeWidth = 92f
        val meritBadgePaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeLeft, topY + 13f, badgeLeft + badgeWidth, topY + 69f, 8f, 8f, meritBadgePaint)

        val meritLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
        }
        PdfDrawHelpers.drawCellText(
            canvas, meritLabel,
            badgeLeft, topY + 16f, badgeWidth, 18f,
            meritLabelPaint, Paint.Align.CENTER, padding = 3f
        )

        val meritPosPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 224, 71)
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        PdfDrawHelpers.drawCellText(
            canvas, "${BengaliUtils.toBn(meritPosition)}ম",
            badgeLeft, topY + 36f, badgeWidth, 26f,
            meritPosPaint, Paint.Align.CENTER, padding = 3f
        )

        return topY + boxHeight
    }
}
