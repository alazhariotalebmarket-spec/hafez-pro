package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.FinancialTransaction
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
fun FinancialScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    var selectedFilterType by remember { mutableStateOf("সব") } // "সব", "খরচ", "মুহতামিম জমা"
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<FinancialTransaction?>(null) }

    val monthPrefix = selectedDate.take(7)
    val monthlyTransactions = remember(allTransactions, monthPrefix) {
        allTransactions.filter { it.date.startsWith(monthPrefix) }
    }

    val displayTransactions = remember(monthlyTransactions, selectedFilterType) {
        if (selectedFilterType == "সব") monthlyTransactions
        else monthlyTransactions.filter { it.type == selectedFilterType }
    }

    val totalExpense = monthlyTransactions.filter { it.type == "খরচ" }.sumOf { it.amount }
    val totalDeposit = monthlyTransactions.filter { it.type == "মুহতামিম জমা" }.sumOf { it.amount }
    val netBalance = totalDeposit - totalExpense

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "৯. খরচ ও জমা",
                subtitle = "হিসাব মাস: ${BengaliUtils.formatDateToBn(selectedDate).split(",")[0]}",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_add_transaction_topbar")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "হিসাব যোগ", tint = IslamicGreenLight)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                text = { Text("খরচ / জমা এন্ট্রি", color = Color.Black, fontWeight = FontWeight.Bold) },
                containerColor = IslamicGreenPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
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

            // Financial Summary Card
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
                            Text("মুহতামিম জমা", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text(BengaliUtils.formatCurrency(totalDeposit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicGreenLight))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("মোট খরচ", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text(BengaliUtils.formatCurrency(totalExpense), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = IslamicRed))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("অবশিষ্ট তহবিল", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                            Text(
                                text = BengaliUtils.formatCurrency(netBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) IslamicGoldLight else IslamicRed
                                )
                            )
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
                                PdfReportGenerator.generateFinancialPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    transactions = monthlyTransactions,
                                    periodText = "মাসিক আয়-ব্যয় হিসাব ($monthPrefix)"
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_finance_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মাসিক হিসাব পিডিএফ", fontSize = 11.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val yearPrefix = selectedDate.take(4)
                                    val yearlyData = viewModel.getTransactionsForYear(yearPrefix).firstOrNull() ?: emptyList()
                                    PdfReportGenerator.generateFinancialPdf(
                                        context = context,
                                        madrasa = madrasaInfo,
                                        transactions = yearlyData,
                                        periodText = "বাৎসরিক আয়-ব্যয় হিসাব ($yearPrefix শিক্ষাবর্ষ)"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("btn_finance_yearly_pdf"),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Tabs: সব, খরচ, মুহতামিম জমা
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("সব", "খরচ", "মুহতামিম জমা").forEach { type ->
                    val isSelected = type == selectedFilterType
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterType = type },
                        label = { Text(type, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (type == "খরচ") IslamicRedContainer else IslamicGreenContainer,
                            selectedLabelColor = if (type == "খরচ") IslamicRed else IslamicGreenLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (displayTransactions.isEmpty()) {
                EmptyListState(
                    message = "এই মাসে কোনো লেনদেন বা খরচের তথ্য পাওয়া যায়নি।",
                    actionLabel = "খরচ বা জমা এন্ট্রি করুন",
                    onAction = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_transactions"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayTransactions, key = { it.id }) { tx ->
                        FinancialTransactionCard(
                            transaction = tx,
                            onDelete = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFinancialDialog(
            currentDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onSave = { type, category, amount, notes ->
                viewModel.saveTransaction(type, category, amount, notes)
                showAddDialog = false
            }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("লেনদেন মুছে ফেলবেন?", color = IslamicTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে এই লেনদেনের রেকর্ডটি মুছে ফেলতে চান?", color = IslamicTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicRed)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("বাতিল", color = IslamicTextSecondary)
                }
            },
            containerColor = IslamicDarkSurfaceVariant
        )
    }
}

@Composable
fun FinancialTransactionCard(
    transaction: FinancialTransaction,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("transaction_card_${transaction.id}"),
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (transaction.type == "খরচ") IslamicRedContainer else IslamicGreenContainer
                ) {
                    Text(
                        text = transaction.type,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (transaction.type == "খরচ") IslamicRed else IslamicGreenLight,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = IslamicTextPrimary
                        )
                    )
                    Text(
                        text = BengaliUtils.formatDateToBn(transaction.date),
                        style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextMuted)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${if (transaction.type == "খরচ") "-" else "+"}${BengaliUtils.formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == "খরচ") IslamicRed else IslamicGreenLight
                    )
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("btn_delete_tx_${transaction.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন", tint = IslamicTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AddFinancialDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onSave: (type: String, category: String, amount: Double, notes: String) -> Unit
) {
    var type by remember { mutableStateOf("খরচ") } // "খরচ" or "মুহতামিম জমা"
    var category by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val expensePresets = listOf("খাবার ও বাজার খরচ", "বিদ্যুৎ বিল", "শিক্ষক হাদিয়া", "মেরামত ও সংস্কার", "অন্যান্য")
    val depositPresets = listOf("মুহতামিম নিজস্ব জমা", "দান ও অনুদান", "বিশেষ তহবিল", "অন্যান্য জমা")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("আয়-ব্যয় এন্ট্রি", fontWeight = FontWeight.Bold, color = IslamicTextPrimary)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Selector (খরচ বা মুহতামিম জমা)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = "খরচ" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "খরচ") IslamicRed else IslamicDarkSurface
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_select_expense")
                    ) {
                        Text("খরচ", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { type = "মুহতামিম জমা" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "মুহতামিম জমা") IslamicGreenPrimary else IslamicDarkSurface
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_select_deposit")
                    ) {
                        Text("মুহতামিম জমা", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Presets
                val presets = if (type == "খরচ") expensePresets else depositPresets
                Text("দ্রুত নির্বাচন:", style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.take(3).forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IslamicGreenContainer,
                            modifier = Modifier.clickable { category = p }
                        ) {
                            Text(
                                text = p.take(8),
                                fontSize = 10.sp,
                                color = IslamicGreenLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("খাত / বিবরণ *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_tx_category")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_tx_amount")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val a = amountStr.toDoubleOrNull() ?: 0.0
                    if (category.isNotBlank() && a > 0) {
                        onSave(type, category, a, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_tx_dialog")
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
