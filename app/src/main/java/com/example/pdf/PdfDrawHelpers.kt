package com.example.pdf

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.data.ExamResult
import com.example.data.MadrasaInfo
import com.example.data.Student
import com.example.util.BengaliUtils

object PdfConstants {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN = 36f
    const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN // 523f
    const val BOTTOM_SAFE_Y = PAGE_HEIGHT - MARGIN - 30f // 776f

    const val EXAM_FIRST = "প্রথম সাময়িক পরীক্ষা"
    const val EXAM_SECOND = "দ্বিতীয় সাময়িক পরীক্ষা"
    const val EXAM_ANNUAL = "বার্ষিক পরীক্ষা"
}

data class CombinedStudentExamSummary(
    val student: Student,
    val firstTerm: ExamResult?,
    val secondTerm: ExamResult?,
    val annualTerm: ExamResult?,
    val grandTotalObtained: Int,
    val grandTotalFull: Int,
    val enteredExamCount: Int,
    val isOverallPassed: Boolean,
    val meritPosition: Int
) {
    val percentage: Int
        get() = if (grandTotalFull > 0) ((grandTotalObtained * 100.0) / grandTotalFull).toInt() else 0
}

object PdfDrawHelpers {

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(5, 150, 105)
        textSize = 16f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 9.5f
        typeface = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8.8f
        typeface = Typeface.DEFAULT
    }

    val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8.8f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val borderPaint = Paint().apply {
        color = Color.rgb(209, 213, 219)
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
    }

    val headerBgPaint = Paint().apply {
        color = Color.rgb(4, 120, 87)
        style = Paint.Style.FILL
    }

    val headerDividerPaint = Paint().apply {
        color = Color.argb(90, 255, 255, 255)
        strokeWidth = 0.7f
        style = Paint.Style.STROKE
    }

    val zebraPaint = Paint().apply {
        color = Color.rgb(243, 244, 246)
        style = Paint.Style.FILL
    }

    val goldLinePaint = Paint().apply {
        color = Color.rgb(245, 158, 11)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 8f
        textAlign = Paint.Align.CENTER
    }

    fun buildCombinedExamSummaries(
        students: List<Student>,
        allResultsForYear: List<ExamResult>
    ): List<CombinedStudentExamSummary> {
        val groupedByStudent = allResultsForYear.groupBy { it.studentId }
        val raw = students.mapNotNull { student ->
            val list = groupedByStudent[student.id] ?: emptyList()
            if (list.isEmpty()) return@mapNotNull null

            val first = list.find { it.examType == PdfConstants.EXAM_FIRST }
            val second = list.find { it.examType == PdfConstants.EXAM_SECOND }
            val annual = list.find { it.examType == PdfConstants.EXAM_ANNUAL }
            val terms = listOfNotNull(first, second, annual)

            val obtained = terms.sumOf { it.totalMarks }
            val full = terms.sumOf { it.totalFullMarks }
            val passed = terms.isNotEmpty() && terms.all { it.isAllPassed }

            CombinedStudentExamSummary(
                student = student,
                firstTerm = first,
                secondTerm = second,
                annualTerm = annual,
                grandTotalObtained = obtained,
                grandTotalFull = full,
                enteredExamCount = terms.size,
                isOverallPassed = passed,
                meritPosition = 0
            )
        }.sortedWith(
            compareByDescending<CombinedStudentExamSummary> { it.grandTotalObtained }
                .thenBy { it.student.roll }
        )

        return raw.mapIndexed { idx, summary ->
            summary.copy(meritPosition = idx + 1)
        }
    }

    fun drawDocumentHeader(
        canvas: Canvas,
        madrasa: MadrasaInfo?,
        title: String,
        periodText: String
    ): Float {
        var y = PdfConstants.MARGIN + 10f

        subHeaderPaint.textSize = 10.5f
        subHeaderPaint.typeface = Typeface.DEFAULT
        canvas.drawText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", PdfConstants.PAGE_WIDTH / 2f, y, subHeaderPaint)
        y += 17f

        headerPaint.textSize = 15.5f
        drawCenteredFittedText(
            canvas,
            madrasa?.name ?: "দারুল উলুম হিফজুল কুরআন মাদ্রাসা",
            PdfConstants.PAGE_WIDTH / 2f,
            y,
            PdfConstants.CONTENT_WIDTH,
            headerPaint,
            minSize = 11f
        )
        y += 14f

        subHeaderPaint.textSize = 9f
        val addressLine = "${madrasa?.address ?: ""}  |  মোবাইল: ${madrasa?.phone ?: ""}"
        drawCenteredFittedText(
            canvas,
            addressLine,
            PdfConstants.PAGE_WIDTH / 2f,
            y,
            PdfConstants.CONTENT_WIDTH,
            subHeaderPaint,
            minSize = 7.5f
        )
        y += 12f

        canvas.drawLine(PdfConstants.MARGIN, y, PdfConstants.PAGE_WIDTH - PdfConstants.MARGIN, y, goldLinePaint)
        y += 15f

        drawCenteredFittedText(
            canvas,
            title,
            PdfConstants.PAGE_WIDTH / 2f,
            y,
            PdfConstants.CONTENT_WIDTH,
            titlePaint,
            minSize = 10f
        )
        y += 13f

        subHeaderPaint.textSize = 8.5f
        val dateText = "তারিখ: ${BengaliUtils.formatDateToBn(BengaliUtils.getTodayDate())}  |  বিবরণী: $periodText"
        drawCenteredFittedText(
            canvas,
            dateText,
            PdfConstants.PAGE_WIDTH / 2f,
            y,
            PdfConstants.CONTENT_WIDTH,
            subHeaderPaint,
            minSize = 7.2f
        )
        y += 14f

        return y
    }

    fun drawContinuationHeader(
        canvas: Canvas,
        madrasa: MadrasaInfo?,
        title: String,
        periodText: String,
        pageNumber: Int
    ): Float {
        var y = PdfConstants.MARGIN + 8f
        headerPaint.textSize = 12f
        drawCenteredFittedText(
            canvas,
            madrasa?.name ?: "দারুল উলুম হিফজুল কুরআন মাদ্রাসা",
            PdfConstants.PAGE_WIDTH / 2f,
            y,
            PdfConstants.CONTENT_WIDTH,
            headerPaint,
            minSize = 10f
        )
        y += 13f
        subHeaderPaint.textSize = 8.5f
        drawCenteredFittedText(
            canvas,
            "$title ($periodText) - পৃষ্ঠা ${BengaliUtils.toBn(pageNumber)}",
            PdfConstants.PAGE_WIDTH / 2f,
            y,
            PdfConstants.CONTENT_WIDTH,
            subHeaderPaint,
            minSize = 7.5f
        )
        y += 8f
        canvas.drawLine(PdfConstants.MARGIN, y, PdfConstants.PAGE_WIDTH - PdfConstants.MARGIN, y, goldLinePaint)
        return y + 10f
    }

    fun drawCenteredFittedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float,
        basePaint: Paint,
        minSize: Float = 7f
    ) {
        val paint = fitPaintToWidth(basePaint, text, maxWidth, minSize)
        paint.textAlign = Paint.Align.CENTER
        val ellipsized = TextUtils.ellipsize(text, TextPaint(paint), maxWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(ellipsized, centerX, baselineY, paint)
    }

    fun fitPaintToWidth(
        basePaint: Paint,
        text: String,
        maxWidth: Float,
        minSize: Float = 6.6f
    ): Paint {
        val p = Paint(basePaint)
        while (p.measureText(text) > maxWidth && p.textSize > minSize) {
            p.textSize = (p.textSize - 0.4f).coerceAtLeast(minSize)
        }
        return p
    }

    fun drawCellText(
        canvas: Canvas,
        text: String,
        cellX: Float,
        cellTop: Float,
        cellWidth: Float,
        cellHeight: Float,
        basePaint: Paint,
        align: Paint.Align,
        padding: Float = 3.5f
    ) {
        val availableWidth = (cellWidth - 2 * padding).coerceAtLeast(6f)
        val fittedPaint = fitPaintToWidth(basePaint, text, availableWidth, minSize = 6.6f)
        fittedPaint.textAlign = align
        val displayText = TextUtils.ellipsize(
            text,
            TextPaint(fittedPaint),
            availableWidth,
            TextUtils.TruncateAt.END
        ).toString()

        val fm = fittedPaint.fontMetrics
        val baseline = cellTop + cellHeight / 2f - (fm.ascent + fm.descent) / 2f
        val drawX = when (align) {
            Paint.Align.CENTER -> cellX + cellWidth / 2f
            Paint.Align.RIGHT -> cellX + cellWidth - padding
            else -> cellX + padding
        }

        canvas.save()
        canvas.clipRect(cellX + 0.5f, cellTop + 0.5f, cellX + cellWidth - 0.5f, cellTop + cellHeight - 0.5f)
        canvas.drawText(displayText, drawX, baseline, fittedPaint)
        canvas.restore()
    }

    fun drawTableHeader(
        canvas: Canvas,
        y: Float,
        headers: List<String>,
        colWidths: List<Float>
    ): Float {
        val hasMultiLine = headers.any { it.contains("\n") }
        val headerHeight = if (hasMultiLine) 28f else 22f
        val totalWidth = colWidths.sum()

        canvas.drawRect(PdfConstants.MARGIN, y, PdfConstants.MARGIN + totalWidth, y + headerHeight, headerBgPaint)

        var x = PdfConstants.MARGIN
        headers.forEachIndexed { i, rawTitle ->
            val colW = colWidths[i]
            if (i > 0) {
                canvas.drawLine(x, y + 2f, x, y + headerHeight - 2f, headerDividerPaint)
            }
            val lines = rawTitle.split("\n")
            val availW = (colW - 5f).coerceAtLeast(6f)

            canvas.save()
            canvas.clipRect(x + 1f, y + 1f, x + colW - 1f, y + headerHeight - 1f)
            if (lines.size == 1) {
                val fitted = fitPaintToWidth(tableHeaderPaint, lines[0], availW, minSize = 6.5f).apply {
                    textAlign = Paint.Align.CENTER
                }
                val fm = fitted.fontMetrics
                val baseline = y + headerHeight / 2f - (fm.ascent + fm.descent) / 2f
                val ellipsized = TextUtils.ellipsize(lines[0], TextPaint(fitted), availW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(ellipsized, x + colW / 2f, baseline, fitted)
            } else {
                val topLine = lines[0]
                val bottomLine = lines[1]
                val topPaint = fitPaintToWidth(tableHeaderPaint, topLine, availW, minSize = 6.5f).apply {
                    textAlign = Paint.Align.CENTER
                }
                val bottomBase = Paint(tableHeaderPaint).apply { textSize = 7.6f }
                val bottomPaint = fitPaintToWidth(bottomBase, bottomLine, availW, minSize = 6.2f).apply {
                    textAlign = Paint.Align.CENTER
                }
                val ellipsizedTop = TextUtils.ellipsize(topLine, TextPaint(topPaint), availW, TextUtils.TruncateAt.END).toString()
                val ellipsizedBottom = TextUtils.ellipsize(bottomLine, TextPaint(bottomPaint), availW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(ellipsizedTop, x + colW / 2f, y + 11.5f, topPaint)
                canvas.drawText(ellipsizedBottom, x + colW / 2f, y + 22.5f, bottomPaint)
            }
            canvas.restore()
            x += colW
        }
        return y + headerHeight
    }

    fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        canvas.drawText(
            "পৃষ্ঠা ${BengaliUtils.toBn(pageNumber)}  •  Hafez Pro",
            PdfConstants.PAGE_WIDTH / 2f,
            PdfConstants.PAGE_HEIGHT - 14f,
            footerPaint
        )
    }

    fun drawSignatures(canvas: Canvas, y: Float) {
        val signY = (PdfConstants.PAGE_HEIGHT - PdfConstants.MARGIN - 24f).coerceAtLeast(y + 32f)
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }

        val col1 = PdfConstants.MARGIN + 60f
        val col2 = PdfConstants.PAGE_WIDTH / 2f
        val col3 = PdfConstants.PAGE_WIDTH - PdfConstants.MARGIN - 60f

        canvas.drawLine(col1 - 40, signY, col1 + 40, signY, linePaint)
        canvas.drawText("হিসাব রক্ষক", col1, signY + 12f, labelPaint)

        canvas.drawLine(col2 - 40, signY, col2 + 40, signY, linePaint)
        canvas.drawText("নাজেমে তালিমাত", col2, signY + 12f, labelPaint)

        canvas.drawLine(col3 - 50, signY, col3 + 50, signY, linePaint)
        canvas.drawText("মুহতামিম / প্রধান শিক্ষক", col3, signY + 12f, labelPaint)
    }
}

class PaginatedPdfWriter(
    val document: PdfDocument,
    private val madrasa: MadrasaInfo?,
    private val reportTitle: String,
    private val periodText: String,
    private val headers: List<String>,
    private val colWidths: List<Float>
) {
    var pageNumber: Int = 1
        private set
    private var currentPage: PdfDocument.Page
    var canvas: Canvas
        private set
    var y: Float

    init {
        val pageInfo = PdfDocument.PageInfo.Builder(
            PdfConstants.PAGE_WIDTH,
            PdfConstants.PAGE_HEIGHT,
            pageNumber
        ).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage.canvas
        val headerBottom = PdfDrawHelpers.drawDocumentHeader(canvas, madrasa, reportTitle, periodText)
        y = if (headers.isNotEmpty()) {
            PdfDrawHelpers.drawTableHeader(canvas, headerBottom, headers, colWidths)
        } else {
            headerBottom
        }
    }

    fun ensureSpaceForRow(rowHeight: Float) {
        if (y + rowHeight > PdfConstants.BOTTOM_SAFE_Y) {
            startNextPage(repeatTableHeader = headers.isNotEmpty())
        }
    }

    fun ensureSpaceForBlock(neededHeight: Float) {
        if (y + neededHeight > PdfConstants.PAGE_HEIGHT - PdfConstants.MARGIN - 20f) {
            startNextPage(repeatTableHeader = false)
        }
    }

    private fun startNextPage(repeatTableHeader: Boolean) {
        PdfDrawHelpers.drawPageFooter(canvas, pageNumber)
        document.finishPage(currentPage)
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(
            PdfConstants.PAGE_WIDTH,
            PdfConstants.PAGE_HEIGHT,
            pageNumber
        ).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage.canvas
        val contBottom = PdfDrawHelpers.drawContinuationHeader(canvas, madrasa, reportTitle, periodText, pageNumber)
        y = if (repeatTableHeader) {
            PdfDrawHelpers.drawTableHeader(canvas, contBottom, headers, colWidths)
        } else {
            contBottom
        }
    }

    fun finishDocumentWithSignatures() {
        ensureSpaceForBlock(65f)
        PdfDrawHelpers.drawSignatures(canvas, y)
        PdfDrawHelpers.drawPageFooter(canvas, pageNumber)
        document.finishPage(currentPage)
    }
}
