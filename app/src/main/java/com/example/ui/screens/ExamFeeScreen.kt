package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.data.ExamFee
import com.example.data.Student
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamFeeScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val selectedExamType by viewModel.selectedExamType.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val examFees by viewModel.examFeesForSelection.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    val examTypes = remember {
        listOf("প্রথম সাময়িক পরীক্ষা", "দ্বিতীয় সাময়িক পরীক্ষা", "বার্ষিক পরীক্ষা")
    }

    val examFeesMap = remember(examFees) { examFees.associateBy { it.studentId } }
    val studentsMap = remember(students) { students.associateBy { it.id } }

    var showEditFeeDialog by remember { mutableStateOf(false) }
    var selectedStudentForFee by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "৭. পরীক্ষার ফি",
                subtitle = "$selectedExamType (${BengaliUtils.toBn(selectedYear)})",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            selectedStudentForFee = students.firstOrNull()
                            showEditFeeDialog = true
                        },
                        modifier = Modifier.testTag("btn_add_exam_fee_topbar")
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = "ফি এন্ট্রি", tint = IslamicGreenLight)
                    }
                }
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

            // Exam Type Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(examTypes) { exam ->
                    val isSelected = exam == selectedExamType
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) IslamicGreenPrimary else IslamicDarkSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .clickable { viewModel.selectedExamType.value = exam }
                            .testTag("tab_exam_$exam")
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Summary & PDF Button Card
            val totalFeeSum = examFees.sumOf { it.totalFee }
            val totalPaidSum = examFees.sumOf { it.paidAmount }
            val totalDueSum = examFees.sumOf { it.dueAmount }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট ধার্য ফি", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text(BengaliUtils.formatCurrency(totalFeeSum), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicTextPrimary))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("মোট আদায়", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text(BengaliUtils.formatCurrency(totalPaidSum), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicGreenLight))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("মোট বাকি", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text(BengaliUtils.formatCurrency(totalDueSum), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (totalDueSum > 0) IslamicRed else IslamicCheckGreen))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            PdfReportGenerator.generateExamFeesPdf(
                                context = context,
                                madrasa = madrasaInfo,
                                feesList = examFees,
                                studentsMap = studentsMap,
                                examType = selectedExamType,
                                year = selectedYear
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_exam_fee_pdf"),
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("$selectedExamType ফি পিডিএফ ডাউনলোড", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (students.isEmpty()) {
                EmptyListState(message = "কোনো ছাত্র পাওয়া যায়নি।")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_exam_fees"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val currentFee = examFeesMap[student.id]

                        ExamFeeStudentCard(
                            student = student,
                            examFee = currentFee,
                            onEdit = {
                                selectedStudentForFee = student
                                showEditFeeDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditFeeDialog && selectedStudentForFee != null) {
        val student = selectedStudentForFee!!
        val currentFee = examFeesMap[student.id]

        EditExamFeeDialog(
            student = student,
            examType = selectedExamType,
            currentFee = currentFee,
            onDismiss = { showEditFeeDialog = false },
            onSave = { totalFee, paidAmount ->
                viewModel.saveExamFee(student.id, totalFee, paidAmount)
                showEditFeeDialog = false
            }
        )
    }
}

@Composable
fun ExamFeeStudentCard(
    student: Student,
    examFee: ExamFee?,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("exam_fee_card_${student.roll}"),
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

                FilledTonalIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("btn_edit_exam_fee_${student.roll}"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = IslamicGreenContainer,
                        contentColor = IslamicGreenLight
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "ফি পরিবর্তন", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val total = examFee?.totalFee ?: 500.0
            val paid = examFee?.paidAmount ?: 0.0
            val due = examFee?.dueAmount ?: (total - paid)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "টাকা: ${BengaliUtils.formatCurrency(total)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IslamicCheckContainer
                ) {
                    Text(
                        text = "জমা: ${BengaliUtils.formatCurrency(paid)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = IslamicGreenLight,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (due > 0) IslamicRedContainer else IslamicCheckContainer
                ) {
                    Text(
                        text = "বাকি: ${BengaliUtils.formatCurrency(due)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (due > 0) IslamicRed else IslamicCheckGreen,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditExamFeeDialog(
    student: Student,
    examType: String,
    currentFee: ExamFee?,
    onDismiss: () -> Unit,
    onSave: (totalFee: Double, paidAmount: Double) -> Unit
) {
    var totalFeeStr by remember {
        mutableStateOf(currentFee?.totalFee?.toLong()?.toString() ?: "500")
    }
    var paidAmountStr by remember {
        mutableStateOf(currentFee?.paidAmount?.toLong()?.toString() ?: "500")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${student.name} - $examType ফি",
                fontWeight = FontWeight.Bold,
                color = IslamicTextPrimary,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = totalFeeStr,
                    onValueChange = { totalFeeStr = it },
                    label = { Text("পরীক্ষার ফি (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_exam_fee_total")
                )

                OutlinedTextField(
                    value = paidAmountStr,
                    onValueChange = { paidAmountStr = it },
                    label = { Text("জমা প্রদান (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_exam_fee_paid")
                )

                val t = totalFeeStr.toDoubleOrNull() ?: 0.0
                val p = paidAmountStr.toDoubleOrNull() ?: 0.0
                val due = (t - p).coerceAtLeast(0.0)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (due > 0) IslamicRedContainer else IslamicCheckContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট ধার্য: ${BengaliUtils.formatCurrency(t)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextPrimary))
                        Text("বাকি থাকবে: ${BengaliUtils.formatCurrency(due)}", style = MaterialTheme.typography.bodySmall.copy(color = if (due > 0) IslamicRed else IslamicCheckGreen, fontWeight = FontWeight.Bold))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val t = totalFeeStr.toDoubleOrNull() ?: 0.0
                    val p = paidAmountStr.toDoubleOrNull() ?: 0.0
                    onSave(t, p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_exam_fee_dialog")
            ) {
                Text("সংরক্ষণ", color = Color.Black, fontWeight = FontWeight.Bold)
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
