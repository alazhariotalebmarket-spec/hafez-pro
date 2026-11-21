package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Student
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.EmptyListState
import com.example.ui.components.IslamicTopBar
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAdmissionScreen(
    viewModel: MadrasaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            IslamicTopBar(
                title = "১. ছাত্র ভর্তি",
                subtitle = "মোট ছাত্র: ${BengaliUtils.toBn(students.size)} জন",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            editingStudent = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("btn_add_student_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "নতুন ছাত্র ভর্তি",
                            tint = IslamicGreenLight
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingStudent = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                text = { Text("নতুন ছাত্র ভর্তি", color = Color.Black, fontWeight = FontWeight.Bold) },
                containerColor = IslamicGreenPrimary,
                modifier = Modifier.testTag("fab_add_student")
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

            // Calculation & PDF Action Card
            val totalMonthly = students.sumOf { it.totalMonthlyFee }
            val totalYearly = totalMonthly * 12

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ফি হিসাব ও বাৎসরিক বিবরণী",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicGold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "মাসিক ধার্য ফি",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = BengaliUtils.formatCurrency(totalMonthly),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGreenLight
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "বাৎসরিক ধার্য ফি",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = BengaliUtils.formatCurrency(totalYearly),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGoldLight
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
                                PdfReportGenerator.generateStudentsPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    students = students,
                                    periodText = "মাসিক ফি বিবরণী (${BengaliUtils.getCurrentMonthName()} ${BengaliUtils.toBn(BengaliUtils.getCurrentYear())})"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_student_monthly_pdf"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = IslamicGreenLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("মাসিক পিডিএফ", fontSize = 12.sp, color = IslamicGreenLight, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                PdfReportGenerator.generateStudentsPdf(
                                    context = context,
                                    madrasa = madrasaInfo,
                                    students = students,
                                    periodText = "বাৎসরিক হিসাব (${BengaliUtils.toBn(BengaliUtils.getCurrentYear())} শিক্ষাবর্ষ)"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_student_yearly_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("বাৎসরিক পিডিএফ", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (students.isEmpty()) {
                EmptyListState(
                    message = "এখনও কোনো ছাত্র ভর্তি করা হয়নি। নতুন ছাত্র যোগ করতে নিচের বাটনে চাপ দিন।",
                    actionLabel = "প্রথম ছাত্র ভর্তি করুন",
                    onAction = {
                        editingStudent = null
                        showAddEditDialog = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("list_students"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentItemCard(
                            student = student,
                            onEdit = {
                                editingStudent = student
                                showAddEditDialog = true
                            },
                            onDelete = { studentToDelete = student }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditStudentDialog(
            student = editingStudent,
            context = context,
            onDismiss = { showAddEditDialog = false },
            onSave = { student ->
                viewModel.saveStudent(student)
                showAddEditDialog = false
            }
        )
    }

    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("ছাত্র মুছে ফেলবেন?", color = IslamicTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিতভাবে ${studentToDelete?.name} এর তথ্য মুছে ফেলতে চান?", color = IslamicTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        studentToDelete?.let { viewModel.deleteStudent(it) }
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicRed)
                ) {
                    Text("মুছে ফেলুন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("বাতিল", color = IslamicTextSecondary)
                }
            },
            containerColor = IslamicDarkSurfaceVariant
        )
    }
}

@Composable
fun StudentItemCard(
    student: Student,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.roll}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture with Rounded Square Shape
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IslamicGreenContainer)
                    .border(1.dp, IslamicBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!student.photoPath.isNullOrEmpty() && File(student.photoPath).exists()) {
                    AsyncImage(
                        model = File(student.photoPath),
                        contentDescription = student.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = student.name.take(2),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = IslamicGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Student Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
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
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = IslamicTextPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "পিতা: ${student.fatherName.ifEmpty { "তথ্য নেই" }}",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary),
                    maxLines = 1
                )

                Text(
                    text = "মোবাইল: ${BengaliUtils.toBn(student.phone.ifEmpty { "নেই" })}",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextMuted),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Fees
                Text(
                    text = "বেতন: ${BengaliUtils.toBn(student.monthlyFee.toLong())} | বিদ্যুৎ: ${BengaliUtils.toBn(student.monthlyElectricityBill.toLong())} | মোট: ${BengaliUtils.formatCurrency(student.totalMonthlyFee)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = IslamicGold,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Actions
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp).testTag("btn_edit_student_${student.roll}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "সম্পাদনা", tint = IslamicGreenLight, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("btn_delete_student_${student.roll}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "মুছে ফেলুন", tint = IslamicRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddEditStudentDialog(
    student: Student?,
    context: Context,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var roll by remember { mutableStateOf(student?.roll?.toString() ?: "") }
    var name by remember { mutableStateOf(student?.name ?: "") }
    var fatherName by remember { mutableStateOf(student?.fatherName ?: "") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var phone by remember { mutableStateOf(student?.phone ?: "") }
    var monthlyFee by remember { mutableStateOf(if (student != null) student.monthlyFee.toLong().toString() else "1500") }
    var monthlyElectricityBill by remember { mutableStateOf(if (student != null) student.monthlyElectricityBill.toLong().toString() else "200") }
    var otherFee by remember { mutableStateOf(if (student != null) student.otherFee.toLong().toString() else "0") }
    var photoPath by remember { mutableStateOf(student?.photoPath) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoPath = saveAndCropImageLocally(context, uri, roll.ifEmpty { "temp" })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (student == null) "নতুন ছাত্র ভর্তি" else "ছাত্রের তথ্য পরিবর্তন",
                fontWeight = FontWeight.Bold,
                color = IslamicTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Picture Picker & Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IslamicDarkSurface)
                            .border(1.dp, IslamicGreenLight, RoundedCornerShape(12.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoPath.isNullOrEmpty() && File(photoPath!!).exists()) {
                            AsyncImage(
                                model = File(photoPath!!),
                                contentDescription = "প্রোফাইল ছবি",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = IslamicGreenLight, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("ছবি দিন", fontSize = 10.sp, color = IslamicTextSecondary)
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "ছাত্রের প্রোফাইল ছবি",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = IslamicTextPrimary)
                        )
                        Text(
                            text = "ছবি যোগ বা পরিবর্তন করতে ট্যাপ করুন",
                            style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                        )
                    }
                }

                OutlinedTextField(
                    value = roll,
                    onValueChange = { roll = it },
                    label = { Text("রোল নাম্বার *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_roll")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ছাত্রের নাম *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_name")
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("পিতার নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_father")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_address")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নাম্বার") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_phone")
                )

                OutlinedTextField(
                    value = monthlyFee,
                    onValueChange = { monthlyFee = it },
                    label = { Text("মাসিক বেতন (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_monthly_fee")
                )

                OutlinedTextField(
                    value = monthlyElectricityBill,
                    onValueChange = { monthlyElectricityBill = it },
                    label = { Text("মাসিক বিদ্যুৎ বিল (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_electricity")
                )

                OutlinedTextField(
                    value = otherFee,
                    onValueChange = { otherFee = it },
                    label = { Text("অন্যান্য ফি (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_student_other_fee")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = roll.toIntOrNull() ?: 1
                    val mFee = monthlyFee.toDoubleOrNull() ?: 0.0
                    val eFee = monthlyElectricityBill.toDoubleOrNull() ?: 0.0
                    val oFee = otherFee.toDoubleOrNull() ?: 0.0

                    val newOrUpdated = student?.copy(
                        roll = r,
                        name = name.ifBlank { "ছাত্র" },
                        fatherName = fatherName,
                        address = address,
                        phone = phone,
                        monthlyFee = mFee,
                        monthlyElectricityBill = eFee,
                        otherFee = oFee,
                        photoPath = photoPath
                    ) ?: Student(
                        roll = r,
                        name = name.ifBlank { "ছাত্র" },
                        fatherName = fatherName,
                        address = address,
                        phone = phone,
                        monthlyFee = mFee,
                        monthlyElectricityBill = eFee,
                        otherFee = oFee,
                        photoPath = photoPath
                    )
                    onSave(newOrUpdated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                modifier = Modifier.testTag("btn_save_student")
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

// Helper to save and crop square image locally
private fun saveAndCropImageLocally(context: Context, uri: Uri, rollOrId: String): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (original != null) {
            // Crop center square
            val dimension = original.width.coerceAtMost(original.height)
            val x = (original.width - dimension) / 2
            val y = (original.height - dimension) / 2
            val cropped = Bitmap.createBitmap(original, x, y, dimension, dimension)

            val dir = File(context.filesDir, "student_photos")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "student_${rollOrId}_${System.currentTimeMillis()}.jpg")
            val out = FileOutputStream(file)
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            file.absolutePath
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
