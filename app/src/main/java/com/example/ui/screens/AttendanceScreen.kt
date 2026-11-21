package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceRecord
import com.example.data.Student
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.DateSelectorRow
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceForDate.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    val attendanceMap = remember(attendanceRecords) {
        attendanceRecords.associateBy { it.studentId }
    }

    val presentCount = attendanceRecords.count { it.isPresent }
    val absentCount = (students.size - presentCount).coerceAtLeast(0)
    val attendanceRate = if (students.isNotEmpty()) (presentCount * 100 / students.size) else 0

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "১০. ছাত্র হাজিরা",
                subtitle = BengaliUtils.formatDateToBn(selectedDate),
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

            // Date Selector
            DateSelectorRow(
                currentDate = selectedDate,
                onDateSelected = { viewModel.selectedDate.value = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Statistics & PDF Card
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
                            Text("মোট ছাত্র", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text("${BengaliUtils.toBn(students.size)} জন", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicTextPrimary))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("উপস্থিত", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text("${BengaliUtils.toBn(presentCount)} জন", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicGreenLight))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("অনুপস্থিত", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text("${BengaliUtils.toBn(absentCount)} জন", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicRed))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("হার", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text("${BengaliUtils.toBn(attendanceRate)}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicGoldLight))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PDF Download Buttons (মাসিক ও বাৎসরিক হিসাব)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val monthPrefix = selectedDate.take(7)
                                    val monthData = viewModel.getAttendanceForMonth(monthPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateAttendancePdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        attendanceList = monthData,
                                        students = students,
                                        periodText = "মাসিক হাজিরা বিবরণী ($monthPrefix)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_attendance_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মাসিক হাজিরা পিডিএফ", fontSize = 11.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val yearPrefix = selectedDate.take(4)
                                    val yearData = viewModel.getAttendanceForYear(yearPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateAttendancePdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        attendanceList = yearData,
                                        students = students,
                                        periodText = "বাৎসরিক হাজিরা বিবরণী ($yearPrefix শিক্ষাবর্ষ)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_attendance_yearly_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("বাৎসরিক হাজিরা পিডিএফ", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Mark Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.markAllAttendance(true) },
                    modifier = Modifier.weight(1f).testTag("btn_mark_all_present"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = IslamicGreenContainer,
                        contentColor = IslamicGreenLight
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সবাই উপস্থিত", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { viewModel.markAllAttendance(false) },
                    modifier = Modifier.weight(1f).testTag("btn_mark_all_absent"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = IslamicDarkSurfaceVariant,
                        contentColor = IslamicTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সবাই অনুপস্থিত", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (students.isEmpty()) {
                EmptyListState(message = "কোনো ছাত্র পাওয়া যায়নি।")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_attendance"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val record = attendanceMap[student.id]
                        val isPresent = record?.isPresent == true

                        AttendanceStudentCard(
                            student = student,
                            isPresent = isPresent,
                            onToggle = { isPres ->
                                viewModel.markSingleAttendance(student.id, isPres)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceStudentCard(
    student: Student,
    isPresent: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("attendance_card_${student.roll}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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

            // Tick and Cross interactive buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Present Button (উপস্থিত ✓)
                FilledTonalButton(
                    onClick = { onToggle(true) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isPresent) IslamicCheckGreen else IslamicDarkSurface,
                        contentColor = if (isPresent) Color.White else IslamicTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_attend_present_${student.roll}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("উপস্থিত", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Absent Button (অনুপস্থিত ✗)
                FilledTonalButton(
                    onClick = { onToggle(false) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (!isPresent) IslamicRed else IslamicDarkSurface,
                        contentColor = if (!isPresent) Color.White else IslamicTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_attend_absent_${student.roll}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("অনুপস্থিত", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
