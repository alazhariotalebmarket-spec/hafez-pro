package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.FeePayment
import com.example.data.Student
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeCollectionScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val allFees by viewModel.allFees.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    var selectedMonth by remember { mutableStateOf(BengaliUtils.getCurrentMonthName()) }
    var selectedYear by remember { mutableIntStateOf(BengaliUtils.getCurrentYear()) }

    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<FeePayment?>(null) }

    val filteredFees = remember(allFees, selectedMonth, selectedYear) {
        allFees.filter { it.monthName == selectedMonth && it.year == selectedYear }
    }

    val studentsMap = remember(students) { students.associateBy { it.id } }

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "২. ছাত্র বেতন আদায়",
                subtitle = "$selectedMonth ${BengaliUtils.toBn(selectedYear)}",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_fee_topbar")
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = "বেতন আদায়", tint = IslamicGreenLight)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                text = { Text("বেতন আদায় এন্ট্রি", color = Color.Black, fontWeight = FontWeight.Bold) },
                containerColor = IslamicGreenPrimary,
                modifier = Modifier.testTag("fab_add_fee")
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

            // Month Selector Scrollable Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BengaliUtils.months) { month ->
                    val isSelected = month == selectedMonth
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) IslamicGreenPrimary else IslamicDarkSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .clickable { selectedMonth = month }
                            .testTag("month_chip_$month")
                    ) {
                        Text(
                            text = month,
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

            // Total Collected & Due Summary Card
            val totalCollected = filteredFees.sumOf { it.paidAmount }
            val totalDue = filteredFees.sumOf { it.dueAmount }

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
                            Text(
                                text = "মোট জমা / আদায়",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = BengaliUtils.formatCurrency(totalCollected),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGreenLight
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "মোট বাকি",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = BengaliUtils.formatCurrency(totalDue),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalDue > 0) IslamicRed else IslamicCheckGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PDF Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                PdfReportGenerator.generateFeePaymentsPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    payments = filteredFees,
                                    studentsMap = studentsMap,
                                    periodText = "$selectedMonth ${BengaliUtils.toBn(selectedYear)} (মাসিক হিসাব)"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_fee_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মাসিক হিসাব পিডিএফ", fontSize = 11.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val yearlyPayments = allFees.filter { it.year == selectedYear }
                                PdfReportGenerator.generateFeePaymentsPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    payments = yearlyPayments,
                                    studentsMap = studentsMap,
                                    periodText = "${BengaliUtils.toBn(selectedYear)} শিক্ষাবর্ষ (বাৎসরিক হিসাব)"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_fee_yearly_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("বাৎসরিক হিসাব পিডিএফ", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredFees.isEmpty()) {
                EmptyListState(
                    message = "$selectedMonth মাসে এখনও কোনো বেতন আদায় করা হয়নি। বেতন এন্ট্রি করতে নিচের বাটনে চাপ দিন।",
                    actionLabel = "বেতন আদায় এন্ট্রি করুন",
                    onAction = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_fees"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredFees, key = { it.id }) { payment ->
                        val student = studentsMap[payment.studentId]
                        FeePaymentCard(
                            payment = payment,
                            student = student,
                            onDelete = { paymentToDelete = payment }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFeePaymentDialog(
            students = students,
            initialMonth = selectedMonth,
            initialYear = selectedYear,
            onDismiss = { showAddDialog = false },
            onSave = { payment ->
                viewModel.saveFeePayment(payment)
                showAddDialog = false
            }
        )
    }

    if (paymentToDelete != null) {
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("বেতন রশিদ মুছবেন?", color = IslamicTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই বেতন জমার হিসাবটি মুছে ফেলতে চান?", color = IslamicTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        paymentToDelete?.let { viewModel.deleteFeePayment(it) }
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicRed)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("বাতিল", color = IslamicTextSecondary)
                }
            },
            containerColor = IslamicDarkSurfaceVariant
        )
    }
}

@Composable
fun FeePaymentCard(
    payment: FeePayment,
    student: Student?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("fee_card_${payment.id}"),
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
                            text = "রোল: ${BengaliUtils.toBn(student?.roll ?: 0)}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IslamicGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = student?.name ?: "ছাত্র-${payment.studentId}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicTextPrimary
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("btn_delete_fee_${payment.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন", tint = IslamicRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "বেতন: ${BengaliUtils.formatCurrency(payment.tuitionFee)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary)
                )
                Text(
                    text = "বিদ্যুৎ: ${BengaliUtils.formatCurrency(payment.electricityBill)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary)
                )
                if (payment.otherFee > 0) {
                    Text(
                        text = "অন্যান্য: ${BengaliUtils.formatCurrency(payment.otherFee)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Paid & Due
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IslamicCheckContainer
                ) {
                    Text(
                        text = "জমা: ${BengaliUtils.formatCurrency(payment.paidAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = IslamicGreenLight,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (payment.dueAmount > 0) IslamicRedContainer else IslamicDarkSurface
                ) {
                    Text(
                        text = "বাকি: ${BengaliUtils.formatCurrency(payment.dueAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (payment.dueAmount > 0) IslamicRed else IslamicCheckGreen,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeePaymentDialog(
    students: List<Student>,
    initialMonth: String,
    initialYear: Int,
    onDismiss: () -> Unit,
    onSave: (FeePayment) -> Unit
) {
    var selectedStudent by remember { mutableStateOf<Student?>(students.firstOrNull()) }
    var monthName by remember { mutableStateOf(initialMonth) }
    var year by remember { mutableIntStateOf(initialYear) }

    var tuitionFee by remember {
        mutableStateOf(selectedStudent?.monthlyFee?.toLong()?.toString() ?: "1500")
    }
    var electricityBill by remember {
        mutableStateOf(selectedStudent?.monthlyElectricityBill?.toLong()?.toString() ?: "200")
    }
    var otherFee by remember {
        mutableStateOf(selectedStudent?.otherFee?.toLong()?.toString() ?: "0")
    }
    var paidAmount by remember { mutableStateOf(tuitionFee) }

    var studentDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ছাত্র বেতন আদায় এন্ট্রি", fontWeight = FontWeight.Bold, color = IslamicTextPrimary)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Student selector dropdown
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = !studentDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedStudent?.name ?: "ছাত্র নির্বাচন করুন"} (রোল: ${BengaliUtils.toBn(selectedStudent?.roll ?: 0)})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ছাত্র নির্বাচন *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("dropdown_select_student")
                    )

                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (রোল: ${BengaliUtils.toBn(s.roll)})") },
                                onClick = {
                                    selectedStudent = s
                                    tuitionFee = s.monthlyFee.toLong().toString()
                                    electricityBill = s.monthlyElectricityBill.toLong().toString()
                                    otherFee = s.otherFee.toLong().toString()
                                    paidAmount = s.totalMonthlyFee.toLong().toString()
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Month Name
                OutlinedTextField(
                    value = monthName,
                    onValueChange = { monthName = it },
                    label = { Text("মাসের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_fee_month")
                )

                // Tuition Fee
                OutlinedTextField(
                    value = tuitionFee,
                    onValueChange = { tuitionFee = it },
                    label = { Text("বেতন (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_fee_tuition")
                )

                // Electricity
                OutlinedTextField(
                    value = electricityBill,
                    onValueChange = { electricityBill = it },
                    label = { Text("বিদ্যুৎ বিল (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_fee_electricity")
                )

                // Other
                OutlinedTextField(
                    value = otherFee,
                    onValueChange = { otherFee = it },
                    label = { Text("অন্যান্য ফি (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_fee_other")
                )

                // Paid amount (জমা)
                OutlinedTextField(
                    value = paidAmount,
                    onValueChange = { paidAmount = it },
                    label = { Text("জমা প্রদান (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_fee_paid")
                )

                // Auto calculate due
                val t = tuitionFee.toDoubleOrNull() ?: 0.0
                val e = electricityBill.toDoubleOrNull() ?: 0.0
                val o = otherFee.toDoubleOrNull() ?: 0.0
                val p = paidAmount.toDoubleOrNull() ?: 0.0
                val totalReq = t + e + o
                val due = (totalReq - p).coerceAtLeast(0.0)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (due > 0) IslamicRedContainer else IslamicCheckContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট ধার্য: ${BengaliUtils.formatCurrency(totalReq)}", style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextPrimary))
                        Text("বাকি থাকবে: ${BengaliUtils.formatCurrency(due)}", style = MaterialTheme.typography.bodySmall.copy(color = if (due > 0) IslamicRed else IslamicCheckGreen, fontWeight = FontWeight.Bold))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStudent != null) {
                        val t = tuitionFee.toDoubleOrNull() ?: 0.0
                        val e = electricityBill.toDoubleOrNull() ?: 0.0
                        val o = otherFee.toDoubleOrNull() ?: 0.0
                        val p = paidAmount.toDoubleOrNull() ?: 0.0
                        val due = ((t + e + o) - p).coerceAtLeast(0.0)

                        val payment = FeePayment(
                            studentId = selectedStudent!!.id,
                            monthName = monthName,
                            year = year,
                            tuitionFee = t,
                            electricityBill = e,
                            otherFee = o,
                            paidAmount = p,
                            dueAmount = due
                        )
                        onSave(payment)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_fee_payment")
            ) {
                Text("জমা সংরক্ষণ", color = Color.Black, fontWeight = FontWeight.Bold)
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
