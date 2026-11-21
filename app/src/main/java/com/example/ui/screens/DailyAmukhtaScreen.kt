package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailyAmukhta
import com.example.data.Student
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.DateSelectorRow
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.components.ParaSelectorBadge
import com.example.ui.components.StatusCheckCrossButtons
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAmukhtaScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val amukhtaList by viewModel.amukhtaForDate.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    val options = remember {
        listOf("৫ ছাপা", "আধা পারা", "১ পারা", "২ পারা")
    }

    val amukhtaMap = remember(amukhtaList) { amukhtaList.associateBy { it.studentId } }
    val studentsMap = remember(students) { students.associateBy { it.id } }

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "৫. দৈনিক আমুখতা",
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
                        text = "আমুখতা রিপোর্ট ডাউনলোড",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = IslamicGold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                PdfReportGenerator.generateDailyAmukhtaPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    list = amukhtaList,
                                    studentsMap = studentsMap,
                                    periodText = "দৈনিক আমুখতা (${BengaliUtils.formatDateToBn(selectedDate)})"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_amukhta_daily_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("দৈনিক", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val monthPrefix = selectedDate.take(7)
                                    val monthData = viewModel.getAmukhtaForMonth(monthPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateDailyAmukhtaPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        list = monthData,
                                        studentsMap = studentsMap,
                                        periodText = "মাসিক আমুখতা রেজিস্টার ($monthPrefix)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_amukhta_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text("মাসিক", fontSize = 11.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val yearPrefix = selectedDate.take(4)
                                    val yearData = viewModel.getAmukhtaForYear(yearPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateDailyAmukhtaPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        list = yearData,
                                        studentsMap = studentsMap,
                                        periodText = "বাৎসরিক আমুখতা রেজিস্টার ($yearPrefix শিক্ষাবর্ষ)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_amukhta_yearly_pdf"),
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
                EmptyListState(message = "কোনো ছাত্র পাওয়া যায়নি।")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_amukhta"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val current = amukhtaMap[student.id]

                        DailyAmukhtaStudentCard(
                            student = student,
                            record = current,
                            options = options,
                            onSave = { amount, isPassed, paraNumber ->
                                viewModel.saveAmukhta(student.id, amount, isPassed, paraNumber)
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
fun DailyAmukhtaStudentCard(
    student: Student,
    record: DailyAmukhta?,
    options: List<String>,
    onSave: (amount: String, isPassed: Boolean, paraNumber: Int) -> Unit
) {
    var selectedAmount by remember(record) {
        mutableStateOf(record?.amount ?: options[0])
    }
    var selectedPara by remember(record) {
        mutableIntStateOf(record?.paraNumber ?: 1)
    }
    var isPassedState by remember(record) {
        mutableStateOf(record?.isPassed)
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("amukhta_card_${student.roll}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        testTagPrefix = "amukhta_${student.roll}",
                        onParaSelected = { para ->
                            selectedPara = para
                            if (isPassedState != null) {
                                onSave(selectedAmount, isPassedState!!, para)
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

            // Amount Selector (৫ ছাপা, আধা পারা, ১ পারা, ২ পারা)
            Text(
                text = "আমুখতার পরিমাণ:",
                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(options) { opt ->
                    val isSelected = opt == selectedAmount
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedAmount = opt
                            if (isPassedState != null) {
                                onSave(opt, isPassedState!!, selectedPara)
                            }
                        },
                        label = { Text(opt, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IslamicGreenContainer,
                            selectedLabelColor = IslamicGreenLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pass / Fail Toggle
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
                        onSave(selectedAmount, passed, selectedPara)
                    }
                )
            }
        }
    }
}
