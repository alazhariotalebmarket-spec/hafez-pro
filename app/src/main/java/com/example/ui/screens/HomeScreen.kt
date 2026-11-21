package com.example.ui.screens

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditMadrasaDialog
import com.example.ui.components.MadrasaHeaderCard
import com.example.ui.theme.*
import com.example.util.BengaliUtils
import com.example.viewmodel.MadrasaViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeMenuItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MadrasaViewModel,
    onNavigateTo: (screenId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val madrasaInfo by viewModel.madrasaInfo.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceForDate.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var isSavingApk by remember { mutableStateOf(false) }

    val saveApkSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { destUri ->
        if (destUri != null) {
            coroutineScope.launch {
                isSavingApk = true
                val ok = copyApkToUri(context, destUri)
                isSavingApk = false
                Toast.makeText(
                    context,
                    if (ok) "Hafez Pro APK সফলভাবে সেভ হয়েছে!" else "APK সেভ করা যায়নি।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val triggerApkDownload: () -> Unit = {
        if (!isSavingApk) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                coroutineScope.launch {
                    isSavingApk = true
                    val savedPath = saveApkToDownloadsQ(context)
                    isSavingApk = false
                    if (savedPath != null) {
                        Toast.makeText(
                            context,
                            "APK ডাউনলোড সম্পন্ন: $savedPath",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        saveApkSafLauncher.launch("Hafez_Pro.apk")
                    }
                }
            } else {
                saveApkSafLauncher.launch("Hafez_Pro.apk")
            }
        }
    }

    val openFileManagerToSaveApk: () -> Unit = {
        saveApkSafLauncher.launch("Hafez_Pro.apk")
    }

    val menuItems = remember {
        listOf(
            HomeMenuItem(1, "১. ছাত্র ভর্তি", "ভর্তি, প্রোফাইল ও মাসিক ফি", Icons.Default.PersonAdd, IslamicGreenPrimary),
            HomeMenuItem(2, "২. ছাত্র বেতন আদায়", "মাসিক বেতন, বিদ্যুৎ বিল ও বাকি", Icons.Default.Payments, IslamicGold),
            HomeMenuItem(3, "৩. দৈনিক ছবক", "১ আয়াত থেকে ২ ছাপা (✓/✗)", Icons.Default.MenuBook, IslamicGreenLight),
            HomeMenuItem(4, "৪. দৈনিক সাত ছবক", "আধা ছাপা থেকে ১৯ ছাপা পর্যন্ত", Icons.Default.AutoStories, IslamicBlue),
            HomeMenuItem(5, "৫. দৈনিক আমুখতা", "৫ ছাপা থেকে ২ পারা পর্যন্ত", Icons.Default.ImportContacts, IslamicGoldLight),
            HomeMenuItem(6, "৬. হিফজ রিভিশন", "আধা পারা থেকে ৫ পারা রিভিশন", Icons.Default.Psychology, IslamicGreenPrimary),
            HomeMenuItem(7, "৭. পরীক্ষার ফি", "১ম, ২য় ও বার্ষিক পরীক্ষার ফি", Icons.Default.AccountBalanceWallet, IslamicGold),
            HomeMenuItem(8, "৮. পরীক্ষার রেজাল্ট", "৪ বিষয়, মোট ও মেধাস্থান", Icons.Default.EmojiEvents, IslamicGold),
            HomeMenuItem(9, "৯. খরচ ও জমা", "দৈনিক খরচ ও মুহতামিম জমা", Icons.Default.AccountBalance, IslamicRed),
            HomeMenuItem(10, "১০. ছাত্র হাজিরা", "দৈনিক উপস্থিতি ও পরিসংখ্যান", Icons.Default.AssignmentTurnedIn, IslamicCheckGreen)
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = IslamicDarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(IslamicGreenDark, IslamicGreenPrimary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Hafez Pro",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGreenLight,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                text = "হিফজ মাদ্রাসা ম্যানেজমেন্ট",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = IslamicTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Developer Button
                    FilledTonalButton(
                        onClick = { showDeveloperDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = IslamicGreenContainer,
                            contentColor = IslamicGreenLight
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_developer_info")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "ডেভেলপার",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ডেভেলপার",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
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

            // Editable Madrasa Header Card
            MadrasaHeaderCard(
                info = madrasaInfo,
                onEditClick = { showEditDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Students count
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "মোট ছাত্র",
                    value = "${BengaliUtils.toBn(students.size)} জন",
                    icon = Icons.Default.People,
                    color = IslamicGreenPrimary
                )
                // Today Present
                val presentCount = attendanceRecords.count { it.isPresent }
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "আজকের হাজিরা",
                    value = "${BengaliUtils.toBn(presentCount)} জন",
                    icon = Icons.Default.CheckCircle,
                    color = IslamicCheckGreen
                )
                // Date
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "আজকের তারিখ",
                    value = BengaliUtils.formatDateToBn(BengaliUtils.getTodayDate()).split(",")[0],
                    icon = Icons.Default.CalendarMonth,
                    color = IslamicGold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "প্রধান সেবাসমূহ (১০টি অপশন)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IslamicTextPrimary,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 10 Features Grid + Developer Option + Download Application Option
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("grid_home_menu"),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 10 Main Menu Items
                items(menuItems) { item ->
                    HomeMenuCard(
                        item = item,
                        onClick = { onNavigateTo(item.id) }
                    )
                }

                // Developer Option Card (Full width)
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeveloperDialog = true }
                            .testTag("card_developer_option_home"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(IslamicGreenContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Engineering,
                                        contentDescription = "ডেভেলপার অপশন",
                                        tint = IslamicGreenLight,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "ডেভেলপার অপশন",
                                        style = MaterialTheme.typography.labelSmall.copy(color = IslamicGold)
                                    )
                                    Text(
                                        text = "মোঃ ফেরদাউস মোল্লা",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = IslamicTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "+8801934411199  •  ferdausharfin@gmail.com",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = IslamicTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = IslamicGreenLight
                            )
                        }
                    }
                }

                // Application Download Option Card (Right below Developer Option!)
                item(span = { GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_download_app_home"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(IslamicGreenDark, IslamicGold))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(IslamicGoldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadForOffline,
                                        contentDescription = "এ্যাপ্লিকেশন ডাউনলোড",
                                        tint = IslamicGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "এ্যাপ্লিকেশন ডাউনলোড (APK)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = IslamicTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "Hafez Pro APK ফাইল ফোনের Downloads ফোল্ডারে সেভ করুন বা অন্য মোবাইলে শেয়ার করুন",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = IslamicTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = triggerApkDownload,
                                    enabled = !isSavingApk,
                                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_home_download_apk")
                                ) {
                                    if (isSavingApk) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSavingApk) "সেভ হচ্ছে..." else "APK ডাউনলোড",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = openFileManagerToSaveApk,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_home_file_manager")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "ফাইল ম্যানেজার",
                                        tint = IslamicGoldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ফাইল ম্যানেজার",
                                        color = IslamicGoldLight,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FilledTonalButton(
                                onClick = { showShareOptionsDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = IslamicGreenContainer,
                                    contentColor = IslamicGreenLight
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_home_share_apk")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = IslamicGreenLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "APK শেয়ার (ফাইল ম্যানেজার / Quick Share / Drive)",
                                    color = IslamicGreenLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditMadrasaDialog(
            currentInfo = madrasaInfo,
            onDismiss = { showEditDialog = false },
            onSave = { name, address, phone ->
                viewModel.updateMadrasaInfo(name, address, phone)
            }
        )
    }

    if (showDeveloperDialog) {
        DeveloperDialog(
            onDismiss = { showDeveloperDialog = false },
            onDownloadApk = triggerApkDownload,
            onOpenFileManager = openFileManagerToSaveApk,
            onOpenShareOptions = {
                showDeveloperDialog = false
                showShareOptionsDialog = true
            },
            isSavingApk = isSavingApk,
            context = context
        )
    }

    if (showShareOptionsDialog) {
        ApkShareOptionsDialog(
            onDismiss = { showShareOptionsDialog = false },
            onSaveToFileManager = {
                showShareOptionsDialog = false
                openFileManagerToSaveApk()
            },
            onOpenDownloadsFolder = {
                showShareOptionsDialog = false
                openFileManagerDownloads(context)
            },
            onShareViaSystemSheet = {
                showShareOptionsDialog = false
                shareApkFileOrLink(context)
            },
            onShareAppLink = {
                showShareOptionsDialog = false
                shareAppLink(context)
            }
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = IslamicDarkSurfaceVariant,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IslamicTextPrimary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = IslamicTextSecondary,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun HomeMenuCard(
    item: HomeMenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onClick() }
            .testTag("menu_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IslamicDarkSurfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(item.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = IslamicTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = IslamicTextPrimary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = IslamicTextSecondary,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DeveloperDialog(
    onDismiss: () -> Unit,
    onDownloadApk: () -> Unit,
    onOpenFileManager: () -> Unit,
    onOpenShareOptions: () -> Unit,
    isSavingApk: Boolean,
    context: Context
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                        imageVector = Icons.Default.Engineering,
                        contentDescription = null,
                        tint = IslamicGreenLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "ডেভেলপার পরিচিতি",
                    fontWeight = FontWeight.Bold,
                    color = IslamicTextPrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IslamicDarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "নাম:",
                            style = MaterialTheme.typography.labelSmall.copy(color = IslamicGold)
                        )
                        Text(
                            text = "মোঃ ফেরদাউস মোল্লা",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = IslamicTextPrimary,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                // Phone Call
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IslamicDarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801934411199"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "মোবাইল নাম্বার:",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = "+8801934411199",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = IslamicGreenLight
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "কল করুন",
                            tint = IslamicGreenPrimary
                        )
                    }
                }

                // Email 1
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IslamicDarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ferdausharfin@gmail.com"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ইমেইল (Gmail):",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = "ferdausharfin@gmail.com",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = IslamicTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "মেইল পাঠান",
                            tint = IslamicGold
                        )
                    }
                }

                // Email 2
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IslamicDarkSurface,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ferdausharfin@yahoo.com"))
                            context.startActivity(intent)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ইমেইল (Yahoo):",
                                style = MaterialTheme.typography.labelSmall.copy(color = IslamicTextSecondary)
                            )
                            Text(
                                text = "ferdausharfin@yahoo.com",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = IslamicTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "মেইল পাঠান",
                            tint = IslamicBlue
                        )
                    }
                }

                // এ্যাপ্লিকেশন ডাউনলোড অপশন (Application Download Section right below developer details!)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IslamicGreenContainer,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(IslamicGreenDark, IslamicGold))
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_download_app_section")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "এ্যাপ্লিকেশন ডাউনলোড করুন",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicTextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hafez Pro APK ফাইল ফোনের Downloads ফোল্ডারে ডাউনলোড করুন বা অন্য মোবাইলে শেয়ার করুন।",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = IslamicTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onDownloadApk,
                            enabled = !isSavingApk,
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_dialog_download_apk")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSavingApk) "APK সেভ হচ্ছে..." else "ফোনে APK ডাউনলোড করুন",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenFileManager,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_dialog_file_manager")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = IslamicGoldLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ফাইল ম্যানেজার", fontSize = 11.sp, color = IslamicGoldLight, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = onOpenShareOptions,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_dialog_share_apk")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = IslamicGreenLight, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("APK শেয়ার", fontSize = 11.sp, color = IslamicGreenLight, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreenPrimary)
            ) {
                Text("ঠিক আছে", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = IslamicDarkSurfaceVariant
    )
}

// Save APK directly to Downloads folder on Android 10+ (API 29+)
suspend fun saveApkToDownloadsQ(context: Context): String? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
    try {
        val srcFile = File(context.applicationInfo.sourceDir)
        if (!srcFile.exists()) return@withContext null

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Hafez_Pro_$timestamp.apk"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext null

        resolver.openOutputStream(itemUri)?.use { out ->
            srcFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: return@withContext null

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)

        "Download/$fileName"
    } catch (_: Exception) {
        null
    }
}

// Copy APK to SAF-picked Uri for Android 7-9 or fallback
suspend fun copyApkToUri(context: Context, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
        val srcFile = File(context.applicationInfo.sourceDir)
        if (!srcFile.exists()) return@withContext false
        context.contentResolver.openOutputStream(destUri)?.use { out ->
            srcFile.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: return@withContext false
        true
    } catch (_: Exception) {
        false
    }
}

// Share APK file directly if exists, or share download link
fun shareApkFileOrLink(context: Context) {
    try {
        val sourceDir = context.applicationInfo.sourceDir
        val srcFile = File(sourceDir)
        if (srcFile.exists()) {
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val destFile = File(cacheDir, "HafezPro.apk")
            srcFile.copyTo(destFile, overwrite = true)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Hafez Pro অ্যাপ ইনস্টলার (APK)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Hafez Pro APK শেয়ার করুন"))
        } else {
            Toast.makeText(context, "APK ফাইলটি ফাইল ম্যানেজারে সেভ করে শেয়ার করুন।", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "APK শেয়ার করা যায়নি, অনুগ্রহ করে 'ফাইল ম্যানেজার' ব্যবহার করুন।", Toast.LENGTH_LONG).show()
    }
}

// Share App information text helper (no dead URL)
fun shareAppLink(context: Context) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Hafez Pro - হিফজ মাদ্রাসা ম্যানেজমেন্ট অ্যাপ")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hafez Pro - আধুনিক হিফজ মাদ্রাসা ম্যানেজমেন্ট অ্যাপ (অফলাইন)। ছাত্র ভর্তি, বেতন আদায়, ছবক, সাত ছবক, আমুখতা, রিভিশন, হাজিরা, খরচ-জমা ও রেজাল্ট শিট সহজে পরিচালনা করুন। ডেভেলপার: মোঃ ফেরদাউস মোল্লা (+8801934411199)"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Hafez Pro অ্যাপ তথ্য শেয়ার করুন"))
    } catch (e: Exception) {
        Toast.makeText(context, "শেয়ার করা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Open system File Manager / Downloads folder
fun openFileManagerDownloads(context: Context) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val filesIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(filesIntent, "ফাইল ম্যানেজার খুলুন"))
        } catch (e: Exception) {
            Toast.makeText(context, "ফাইল ম্যানেজার ওপেন করা যায়নি: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ApkShareOptionsDialog(
    onDismiss: () -> Unit,
    onSaveToFileManager: () -> Unit,
    onOpenDownloadsFolder: () -> Unit,
    onShareViaSystemSheet: () -> Unit,
    onShareAppLink: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = IslamicGreenLight
                )
                Text(
                    text = "APK শেয়ার ও ফাইল ম্যানেজার",
                    fontWeight = FontWeight.Bold,
                    color = IslamicTextPrimary,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "অ্যাপ্লিকেশনটি (HafezPro.apk) ফাইল ম্যানেজারে সেভ করতে বা অন্য মাধ্যমে শেয়ার করতে নিচের অপশন বেছে নিন:",
                    style = MaterialTheme.typography.bodySmall.copy(color = IslamicTextSecondary)
                )

                ShareOptionTile(
                    icon = Icons.Default.FolderOpen,
                    iconTint = IslamicGold,
                    title = "ফাইল ম্যানেজারে সেভ করুন",
                    subtitle = "যেকোনো ফোল্ডার বাছাই করে APK ফাইল সেভ করুন",
                    testTag = "option_save_file_manager",
                    onClick = onSaveToFileManager
                )

                ShareOptionTile(
                    icon = Icons.Default.Folder,
                    iconTint = IslamicGreenLight,
                    title = "ফাইল ম্যানেজার / Downloads খুলুন",
                    subtitle = "সেভ করা APK ফাইল দেখতে ও শেয়ার করতে ফোল্ডার খুলুন",
                    testTag = "option_open_file_manager",
                    onClick = onOpenDownloadsFolder
                )

                ShareOptionTile(
                    icon = Icons.Default.Share,
                    iconTint = IslamicGreenPrimary,
                    title = "Quick Share / Drive / WhatsApp",
                    subtitle = "সিস্টেম শেয়ার মেনুর মাধ্যমে APK ফাইল পাঠান",
                    testTag = "option_system_share_apk",
                    onClick = onShareViaSystemSheet
                )

                ShareOptionTile(
                    icon = Icons.Default.Info,
                    iconTint = IslamicBlue,
                    title = "অ্যাপের বিবরণ শেয়ার করুন",
                    subtitle = "অ্যাপের ফিচার ও ডেভেলপারের তথ্য টেক্সট হিসেবে পাঠান",
                    testTag = "option_share_app_link",
                    onClick = onShareAppLink
                )
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

@Composable
private fun ShareOptionTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = IslamicDarkSurface,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = IslamicTextPrimary,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = IslamicTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = IslamicTextMuted
            )
        }
    }
}
