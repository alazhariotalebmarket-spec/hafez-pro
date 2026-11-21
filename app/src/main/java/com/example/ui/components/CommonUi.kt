package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MadrasaInfo
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = IslamicDarkSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicTextPrimary,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IslamicGreenLight,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_topbar_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = IslamicGreenLight
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = IslamicTextPrimary
            )
        )
    }
}

@Composable
fun MadrasaHeaderCard(
    info: MadrasaInfo?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_madrasa_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(IslamicBorder, IslamicGreenDark)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IslamicGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "মাদ্রাসা",
                            tint = IslamicGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "হিফজুল কুরআন মাদ্রাসা",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = IslamicGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                FilledTonalIconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_edit_madrasa_info"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = IslamicGreenContainer,
                        contentColor = IslamicGreenLight
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "মাদ্রাসার তথ্য পরিবর্তন",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = info?.name ?: "দারুল উলুম হিফজুল কুরআন মাদ্রাসা",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = IslamicTextPrimary,
                    fontSize = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "ঠিকানা",
                    tint = IslamicTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = info?.address ?: "মিরপুর-১০, ঢাকা, বাংলাদেশ",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = IslamicTextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "মোবাইল",
                    tint = IslamicGreenLight,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = info?.phone ?: "+৮৮০১৯৩৪৪১১১৯৯",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = IslamicGreenLight,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
fun EditMadrasaDialog(
    currentInfo: MadrasaInfo?,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(currentInfo?.name ?: "") }
    var address by remember { mutableStateOf(currentInfo?.address ?: "") }
    var phone by remember { mutableStateOf(currentInfo?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "মাদ্রাসার তথ্য পরিবর্তন",
                fontWeight = FontWeight.Bold,
                color = IslamicTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("মাদ্রাসার নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_madrasa_name")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_madrasa_address")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মাদ্রাসার মোবাইল নাম্বার") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_madrasa_phone")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, address, phone)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_madrasa_dialog")
            ) {
                Text("সংরক্ষণ করুন", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun DateSelectorRow(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = IslamicDarkSurfaceVariant,
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "তারিখ",
                    tint = IslamicGreenLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "তারিখ: ${BengaliUtils.formatDateToBn(currentDate)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = IslamicTextPrimary
                    )
                )
            }

            OutlinedButton(
                onClick = {
                    showDatePicker(context, currentDate) { selected ->
                        onDateSelected(selected)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_change_date")
            ) {
                Text("তারিখ বদলান", style = MaterialTheme.typography.bodySmall.copy(color = IslamicGreenLight))
            }
        }
    }
}

fun showDatePicker(context: Context, currentDate: String, onDateSet: (String) -> Unit) {
    val cal = Calendar.getInstance()
    try {
        val parts = currentDate.split("-")
        if (parts.size == 3) {
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
    } catch (_: Exception) {}

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSet(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun StatusCheckCrossButtons(
    isPassed: Boolean?,
    onPassedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Check ✓ (উত্তীর্ণ / পাস)
        FilledTonalButton(
            onClick = { onPassedChange(true) },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isPassed == true) IslamicCheckGreen else IslamicDarkSurface,
                contentColor = if (isPassed == true) Color.White else IslamicTextSecondary
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_status_pass")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "উত্তীর্ণ",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "উত্তীর্ণ (✓)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Cross ✗ (পুনরাবৃত্তি / ফেল)
        FilledTonalButton(
            onClick = { onPassedChange(false) },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isPassed == false) IslamicRed else IslamicDarkSurface,
                contentColor = if (isPassed == false) Color.White else IslamicTextSecondary
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_status_fail")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "পুনরাবৃত্তি",
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "পুনরাবৃত্তি (✗)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun PdfExportFloatingButton(
    onClick: () -> Unit,
    label: String = "পিডিএফ ডাউনলোড",
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = label,
                tint = Color.Black
            )
        },
        text = {
            Text(
                text = label,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = IslamicGreenPrimary,
        contentColor = Color.Black,
        modifier = modifier.testTag("btn_pdf_download_fab")
    )
}

@Composable
fun EmptyListState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(IslamicGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = IslamicGreenLight,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = IslamicTextSecondary,
                textAlign = TextAlign.Center
            )
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_empty_action")
            ) {
                Text(actionLabel, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ParaSelectorBadge(
    selectedPara: Int,
    studentName: String,
    testTagPrefix: String,
    onParaSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showParaDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = IslamicGoldContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showParaDialog = true }
            .testTag("${testTagPrefix}_para_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = "পারা নির্বাচন",
                tint = IslamicGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "পারা ${BengaliUtils.toBn(selectedPara)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = IslamicGoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = IslamicGoldLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    if (showParaDialog) {
        ParaPickerDialog(
            studentName = studentName,
            currentPara = selectedPara,
            onDismiss = { showParaDialog = false },
            onSelect = { para ->
                onParaSelected(para)
                showParaDialog = false
            }
        )
    }
}

@Composable
fun ParaPickerDialog(
    studentName: String,
    currentPara: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val paras = remember { (1..30).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "কোরআন শরীফের পারা নির্বাচন",
                    fontWeight = FontWeight.Bold,
                    color = IslamicTextPrimary,
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$studentName (১ থেকে ৩০ নং পারা)",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicGreenLight)
                )
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .testTag("grid_para_picker"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(paras) { para ->
                    val isSelected = para == currentPara
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) IslamicGreenPrimary else IslamicDarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) IslamicGreenLight else IslamicBorder
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(para) }
                            .testTag("para_option_$para")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = BengaliUtils.toBn(para),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else IslamicTextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "পারা",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else IslamicTextSecondary,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = IslamicTextSecondary)
            }
        },
        containerColor = IslamicDarkSurfaceVariant
    )
}

