package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ExamResult
import com.example.data.Student
import com.example.pdf.CombinedStudentExamSummary
import com.example.pdf.PdfDrawHelpers
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val selectedExamType by viewModel.selectedExamType.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val examResults by viewModel.examResultsForSelection.collectAsStateWithLifecycle()
    val allYearResults by viewModel.allExamResultsForYear.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    val examTypes = remember {
        listOf("প্রথম সাময়িক পরীক্ষা", "দ্বিতীয় সাময়িক পরীক্ষা", "বার্ষিক পরীক্ষা")
    }
    var isCombinedTabSelected by remember { mutableStateOf(false) }

    val studentsMap = remember(students) { students.associateBy { it.id } }
    val resultsMap = remember(examResults) { examResults.associateBy { it.studentId } }

    // Calculate merit positions by total marks descending
    val sortedResults = remember(examResults) {
        examResults.sortedByDescending { it.totalMarks }
    }
    val meritMap = remember(sortedResults) {
        sortedResults.mapIndexed { index, res -> res.studentId to (index + 1) }.toMap()
    }

    val combinedSummaries = remember(students, allYearResults) {
        PdfDrawHelpers.buildCombinedExamSummaries(students, allYearResults)
    }
    val combinedMap = remember(combinedSummaries) {
        combinedSummaries.associateBy { it.student.id }
    }

    var editingStudentForResult by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "৮. পরীক্ষার রেজাল্ট",
                subtitle = if (isCombinedTabSelected) {
                    "৩ পরীক্ষার সমন্বিত ফলাফল (${BengaliUtils.toBn(selectedYear)})"
                } else {
                    "$selectedExamType (${BengaliUtils.toBn(selectedYear)})"
                },
                onBack = onBack
            )
        },
        containerColor = IslamicBlack
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Exam Type Tabs + Combined 3-Exam Tab
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(examTypes) { exam ->
                    val isSelected = !isCombinedTabSelected && exam == selectedExamType
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) IslamicGreenPrimary else IslamicDarkSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .clickable {
                                isCombinedTabSelected = false
                                viewModel.selectedExamType.value = exam
                            }
                            .testTag("tab_result_exam_$exam")
                    ) {
                        Text(
                            text = exam,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else IslamicTextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isCombinedTabSelected) IslamicGold else IslamicDarkSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .clickable { isCombinedTabSelected = true }
                            .testTag("tab_result_combined_3_exams")
                    ) {
                        Text(
                            text = "৩ পরীক্ষার সমন্বিত",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCombinedTabSelected) Color.Black else IslamicGoldLight
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PDF Download Options Card (পরীক্ষা ভিত্তিক ও ৩ পরীক্ষার বাৎসরিক সমন্বিত পিডিএফ)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ফলাফল ও মার্কশিট পিডিএফ ডাউনলোড",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = IslamicGold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isCombinedTabSelected) {
                                    coroutineScope.launch {
                                        val yearResults = viewModel.getAllExamResultsForYear(selectedYear).firstOrNull() ?: allYearResults
                                        PdfReportGenerator.generateCombinedAnnualExamResultPdf(
                                            context = context,
                                            madrasa = madrasaInfo,
                                            students = students,
                                            allResultsForYear = yearResults,
                                            year = selectedYear
                                        )
                                    }
                                } else {
                                    PdfReportGenerator.generateExamResultPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        resultsList = examResults,
                                        studentsMap = studentsMap,
                                        examType = selectedExamType,
                                        year = selectedYear
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_exam_result_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCombinedTabSelected) "সমন্বিত রেজাল্ট শিট" else "রেজাল্ট শিট পিডিএফ",
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val yearResults = viewModel.getAllExamResultsForYear(selectedYear).firstOrNull() ?: allYearResults
                                    PdfReportGenerator.generateCombinedAnnualExamResultPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        students = students,
                                        allResultsForYear = yearResults,
                                        year = selectedYear
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_exam_annual_result_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGoldLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("বাৎসরিক (৩ পরীক্ষা) PDF", fontSize = 11.sp, color = IslamicGoldLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (students.isEmpty()) {
                EmptyListState(message = "কোনো ছাত্র পাওয়া যায়নি।")
            } else if (isCombinedTabSelected) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_combined_exam_results"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val summary = combinedMap[student.id]
                        CombinedExamResultStudentCard(
                            student = student,
                            summary = summary,
                            onDownloadCombinedMarksheet = {
                                if (summary != null) {
                                    PdfReportGenerator.generateCombinedIndividualStudentResultPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        summary = summary,
                                        year = selectedYear
                                    )
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_exam_results"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val result = resultsMap[student.id]
                        val meritPos = meritMap[student.id]

                        ExamResultStudentCard(
                            student = student,
                            result = result,
                            meritPosition = meritPos,
                            onEditMarks = { editingStudentForResult = student },
                            onDownloadIndividualCard = {
                                if (result != null && meritPos != null) {
                                    PdfReportGenerator.generateIndividualStudentResultPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        student = student,
                                        result = result,
                                        meritPosition = meritPos,
                                        examType = selectedExamType,
                                        year = selectedYear
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (editingStudentForResult != null) {
        val student = editingStudentForResult!!
        val currentResult = resultsMap[student.id]

        EditExamResultDialog(
            student = student,
            examType = selectedExamType,
            currentResult = currentResult,
            onDismiss = { editingStudentForResult = null },
            onSave = { qPass, qObt, qFull, tPass, tObt, tFull, kPass, kObt, kFull, dPass, dObt, dFull ->
                viewModel.saveExamResult(
                    studentId = student.id,
                    quranPass = qPass, quranObtained = qObt, quranFull = qFull,
                    tajweedPass = tPass, tajweedObtained = tObt, tajweedFull = tFull,
                    kalimaPass = kPass, kalimaObtained = kObt, kalimaFull = kFull,
                    duaHadithPass = dPass, duaHadithObtained = dObt, duaHadithFull = dFull
                )
                editingStudentForResult = null
            }
        )
    }
}

@Composable
fun ExamResultStudentCard(
    student: Student,
    result: ExamResult?,
    meritPosition: Int?,
    onEditMarks: () -> Unit,
    onDownloadIndividualCard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("result_card_${student.roll}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IslamicGreenContainer
                    ) {
                        Text(
                            text = "রোল: ${BengaliUtils.toBn(student.roll)}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IslamicGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicTextPrimary
                        )
                    )
                }

                if (meritPosition != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IslamicGoldContainer
                    ) {
                        Text(
                            text = "মেধাস্থান: ${BengaliUtils.toBn(meritPosition)}ম",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IslamicGoldLight,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (result != null) {
                // Marks breakdown grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("কোরআন: ${BengaliUtils.toBn(result.quranObtained)}/${BengaliUtils.toBn(result.quranFull)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                    Text("তাজবীদ: ${BengaliUtils.toBn(result.tajweedObtained)}/${BengaliUtils.toBn(result.tajweedFull)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("কালিমা: ${BengaliUtils.toBn(result.kalimaObtained)}/${BengaliUtils.toBn(result.kalimaFull)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                    Text("দোয়া: ${BengaliUtils.toBn(result.duaHadithObtained)}/${BengaliUtils.toBn(result.duaHadithFull)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সর্বমোট নম্বর: ${BengaliUtils.toBn(result.totalMarks)} / ${BengaliUtils.toBn(result.totalFullMarks)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicGreenLight
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (result.isAllPassed) IslamicCheckContainer else IslamicRedContainer
                    ) {
                        Text(
                            text = if (result.isAllPassed) "উত্তীর্ণ (পাস)" else "অনুত্তীর্ণ (ফেল)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (result.isAllPassed) IslamicGreenLight else IslamicRed,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "নম্বর এখনও এন্ট্রি করা হয়নি",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextMuted)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons: Edit Marks & Individual Marksheet PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEditMarks,
                    modifier = Modifier.weight(1f).testTag("btn_edit_marks_${student.roll}"),
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenContainer),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = IslamicGreenLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (result == null) "নম্বর দিন" else "নম্বর সম্পাদনা", fontSize = 11.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                }

                if (result != null) {
                    OutlinedButton(
                        onClick = onDownloadIndividualCard,
                        modifier = Modifier.weight(1f).testTag("btn_single_marksheet_${student.roll}"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মার্কশিট কার্ড", fontSize = 11.sp, color = IslamicGold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedExamResultStudentCard(
    student: Student,
    summary: CombinedStudentExamSummary?,
    onDownloadCombinedMarksheet: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("combined_result_card_${student.roll}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IslamicGreenContainer
                    ) {
                        Text(
                            text = "রোল: ${BengaliUtils.toBn(student.roll)}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IslamicGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicTextPrimary
                        )
                    )
                }

                if (summary != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IslamicGoldContainer
                    ) {
                        Text(
                            text = "মেধাস্থান: ${BengaliUtils.toBn(summary.meritPosition)}ম",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IslamicGoldLight,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (summary != null) {
                fun fmt(res: ExamResult?): String =
                    if (res == null) "নেই" else "${BengaliUtils.toBn(res.totalMarks)}/${BengaliUtils.toBn(res.totalFullMarks)}"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("১ম সাময়িক: ${fmt(summary.firstTerm)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                    Text("২য় সাময়িক: ${fmt(summary.secondTerm)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("বার্ষিক: ${fmt(summary.annualTerm)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary))
                    Text("গড়: ${BengaliUtils.toBn(summary.percentage)}%", style = MaterialTheme.typography.bodySmall.copy(color = IslamicGoldLight, fontWeight = FontWeight.SemiBold))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "৩ পরীক্ষার সর্বমোট: ${BengaliUtils.toBn(summary.grandTotalObtained)} / ${BengaliUtils.toBn(summary.grandTotalFull)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicGreenLight
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (summary.isOverallPassed) IslamicCheckContainer else IslamicRedContainer
                    ) {
                        Text(
                            text = if (summary.isOverallPassed) "উত্তীর্ণ" else "অনুত্তীর্ণ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (summary.isOverallPassed) IslamicGreenLight else IslamicRed,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDownloadCombinedMarksheet,
                    modifier = Modifier.fillMaxWidth().testTag("btn_combined_marksheet_${student.roll}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("৩ পরীক্ষার সমন্বিত মার্কশিট PDF", fontSize = 11.sp, color = IslamicGold, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    text = "তিন পরীক্ষার কোনো নম্বর এখনও এন্ট্রি করা হয়নি",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextMuted)
                )
            }
        }
    }
}

@Composable
fun EditExamResultDialog(
    student: Student,
    examType: String,
    currentResult: ExamResult?,
    onDismiss: () -> Unit,
    onSave: (
        qPass: Int, qObt: Int, qFull: Int,
        tPass: Int, tObt: Int, tFull: Int,
        kPass: Int, kObt: Int, kFull: Int,
        dPass: Int, dObt: Int, dFull: Int
    ) -> Unit
) {
    // 1. Quran
    var qPass by remember { mutableStateOf((currentResult?.quranPass ?: 33).toString()) }
    var qObt by remember { mutableStateOf((currentResult?.quranObtained ?: 85).toString()) }
    var qFull by remember { mutableStateOf((currentResult?.quranFull ?: 100).toString()) }

    // 2. Tajweed
    var tPass by remember { mutableStateOf((currentResult?.tajweedPass ?: 33).toString()) }
    var tObt by remember { mutableStateOf((currentResult?.tajweedObtained ?: 80).toString()) }
    var tFull by remember { mutableStateOf((currentResult?.tajweedFull ?: 100).toString()) }

    // 3. Kalima & Masala
    var kPass by remember { mutableStateOf((currentResult?.kalimaPass ?: 33).toString()) }
    var kObt by remember { mutableStateOf((currentResult?.kalimaObtained ?: 75).toString()) }
    var kFull by remember { mutableStateOf((currentResult?.kalimaFull ?: 100).toString()) }

    // 4. Dua & Hadith
    var dPass by remember { mutableStateOf((currentResult?.duaHadithPass ?: 33).toString()) }
    var dObt by remember { mutableStateOf((currentResult?.duaHadithObtained ?: 90).toString()) }
    var dFull by remember { mutableStateOf((currentResult?.duaHadithFull ?: 100).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${student.name} - $examType ফলাফল",
                fontWeight = FontWeight.Bold,
                color = IslamicTextPrimary,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subject 1: কোরআন মাজীদ (মৌখিক)
                SubjectMarksInputSection(
                    subjectTitle = "১. কোরআন মাজীদ (মৌখিক)",
                    passValue = qPass, onPassChange = { qPass = it },
                    obtValue = qObt, onObtChange = { qObt = it },
                    fullValue = qFull, onFullChange = { qFull = it }
                )

                // Subject 2: তাজবীদ (মৌখিক)
                SubjectMarksInputSection(
                    subjectTitle = "২. তাজবীদ (মৌখিক)",
                    passValue = tPass, onPassChange = { tPass = it },
                    obtValue = tObt, onObtChange = { tObt = it },
                    fullValue = tFull, onFullChange = { tFull = it }
                )

                // Subject 3: কালিমা ও মাসায়ালা (মৌখিক)
                SubjectMarksInputSection(
                    subjectTitle = "৩. কালিমা ও মাসায়ালা (মৌখিক)",
                    passValue = kPass, onPassChange = { kPass = it },
                    obtValue = kObt, onObtChange = { kObt = it },
                    fullValue = kFull, onFullChange = { kFull = it }
                )

                // Subject 4: দোয়া ও হাদিস (মৌখিক)
                SubjectMarksInputSection(
                    subjectTitle = "৪. দোয়া ও হাদিস (মৌখিক)",
                    passValue = dPass, onPassChange = { dPass = it },
                    obtValue = dObt, onObtChange = { dObt = it },
                    fullValue = dFull, onFullChange = { dFull = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        qPass.toIntOrNull() ?: 33, qObt.toIntOrNull() ?: 0, qFull.toIntOrNull() ?: 100,
                        tPass.toIntOrNull() ?: 33, tObt.toIntOrNull() ?: 0, tFull.toIntOrNull() ?: 100,
                        kPass.toIntOrNull() ?: 33, kObt.toIntOrNull() ?: 0, kFull.toIntOrNull() ?: 100,
                        dPass.toIntOrNull() ?: 33, dObt.toIntOrNull() ?: 0, dFull.toIntOrNull() ?: 100
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_marks_dialog")
            ) {
                Text("ফলাফল সংরক্ষণ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = IslamicTextSecondary)
            }
        },
        containerColor = IslamicDarkSurfaceVariant
    )
}

@Composable
fun SubjectMarksInputSection(
    subjectTitle: String,
    passValue: String, onPassChange: (String) -> Unit,
    obtValue: String, onObtChange: (String) -> Unit,
    fullValue: String, onFullChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = IslamicDarkSurface,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = subjectTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IslamicGold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = obtValue,
                    onValueChange = onObtChange,
                    label = { Text("প্রাপ্ত", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = passValue,
                    onValueChange = onPassChange,
                    label = { Text("পাস", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = fullValue,
                    onValueChange = onFullChange,
                    label = { Text("পূর্ণমান", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
