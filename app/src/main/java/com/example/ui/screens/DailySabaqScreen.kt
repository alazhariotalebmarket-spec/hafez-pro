package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.ParaSelectorBadge
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailySabaq
import com.example.data.Student
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.DateSelectorRow
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.components.StatusCheckCrossButtons
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySabaqScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val sabaqList by viewModel.sabaqForDate.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    val lessonTypes = remember {
        listOf("এক আয়াত", "৫ আয়াত", "আধা ছাপা", "এক ছাপা", "দুই ছাপা")
    }

    val sabaqMap = remember(sabaqList) { sabaqList.associateBy { it.studentId } }
    val studentsMap = remember(students) { students.associateBy { it.id } }

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "৩. দৈনিক ছবক",
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

            // PDF Download Options Card (দৈনিক, মাসিক ও বাৎসরিক পিডিএফ)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ছবক রিপোর্ট ডাউনলোড",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IslamicGold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Daily PDF
                        Button(
                            onClick = {
                                PdfReportGenerator.generateDailySabaqPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    sabaqList = sabaqList,
                                    studentsMap = studentsMap,
                                    periodText = "দৈনিক ছবক (${BengaliUtils.formatDateToBn(selectedDate)})"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_sabaq_daily_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("দৈনিক", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Monthly PDF
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val monthPrefix = selectedDate.take(7) // YYYY-MM
                                    val monthData = viewModel.getSabaqForMonth(monthPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateDailySabaqPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        sabaqList = monthData,
                                        studentsMap = studentsMap,
                                        periodText = "মাসিক ছবক রেজিস্টার ($monthPrefix)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_sabaq_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("মাসিক", fontSize = 11.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                        }

                        // Yearly PDF
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val yearPrefix = selectedDate.take(4) // YYYY
                                    val yearData = viewModel.getSabaqForYear(yearPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateDailySabaqPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        sabaqList = yearData,
                                        studentsMap = studentsMap,
                                        periodText = "বাৎসরিক ছবক রেজিস্টার ($yearPrefix শিক্ষাবর্ষ)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_sabaq_yearly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("বাৎসরিক", fontSize = 11.sp, color = IslamicGoldLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (students.isEmpty()) {
                EmptyListState(
                    message = "কোনো ছাত্র পাওয়া যায়নি। প্রথমে ছাত্র ভর্তি সেকশনে ছাত্র যোগ করুন।"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_daily_sabaq"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val currentSabaq = sabaqMap[student.id]

                        DailySabaqStudentCard(
                            student = student,
                            sabaq = currentSabaq,
                            lessonTypes = lessonTypes,
                            onSave = { lessonType, isPassed, paraNumber ->
                                viewModel.saveSabaq(student.id, lessonType, isPassed, paraNumber)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySabaqStudentCard(
    student: Student,
    sabaq: DailySabaq?,
    lessonTypes: List<String>,
    onSave: (lessonType: String, isPassed: Boolean, paraNumber: Int) -> Unit
) {
    var selectedLessonType by remember(sabaq) {
        mutableStateOf(sabaq?.lessonType ?: lessonTypes[0])
    }
    var selectedPara by remember(sabaq) {
        mutableIntStateOf(sabaq?.paraNumber ?: 1)
    }
    var isPassedState by remember(sabaq) {
        mutableStateOf(sabaq?.isPassed)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("sabaq_card_${student.roll}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Student Title, Roll & Para Selector (1-30)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    ParaSelectorBadge(
                        selectedPara = selectedPara,
                        studentName = student.name,
                        testTagPrefix = "sabaq_${student.roll}",
                        onParaSelected = { para ->
                            selectedPara = para
                            if (isPassedState != null) {
                                onSave(selectedLessonType, isPassedState!!, para)
                            }
                        }
                    )
                }

                if (isPassedState != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPassedState == true) IslamicCheckContainer else IslamicRedContainer
                    ) {
                        Text(
                            text = if (isPassedState == true) "উত্তীর্ণ (✓)" else "পুনরাবৃত্তি (✗)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isPassedState == true) IslamicGreenLight else IslamicRed,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lesson Type Selector Chips
            Text(
                text = "ছবকের পরিমাণ:",
                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(lessonTypes) { type ->
                    val isSelected = type == selectedLessonType
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedLessonType = type
                            if (isPassedState != null) {
                                onSave(type, isPassedState!!, selectedPara)
                            }
                        },
                        label = { Text(type, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IslamicGreenContainer,
                            selectedLabelColor = IslamicGreenLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Check (✓) and Cross (✗) buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ফলাফল নির্ধারণ:",
                    style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                )

                StatusCheckCrossButtons(
                    isPassed = isPassedState,
                    onPassedChange = { passed ->
                        isPassedState = passed
                        onSave(selectedLessonType, passed, selectedPara)
                    }
                )
            }
        }
    }
}
