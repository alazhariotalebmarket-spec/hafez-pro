package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.HafezProTheme
import com.example.ui.theme.IslamicBlack
import com.example.viewmodel.MadrasaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MadrasaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HafezProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IslamicBlack
                ) {
                    HafezProApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun HafezProApp(viewModel: MadrasaViewModel) {
    // 0 = Home
    // 1 = Student Admission
    // 2 = Fee Collection
    // 3 = Daily Sabaq
    // 4 = Daily Sat Sabaq
    // 5 = Daily Amukhta
    // 6 = Hifz Revision
    // 7 = Exam Fee
    // 8 = Exam Result
    // 9 = Financial (Expense & Deposit)
    // 10 = Attendance
    var currentScreen by remember { mutableIntStateOf(0) }

    // Hardware and gesture back button handler
    BackHandler(enabled = currentScreen != 0) {
        currentScreen = 0
    }

    when (currentScreen) {
        0 -> HomeScreen(
            viewModel = viewModel,
            onNavigateTo = { screenId -> currentScreen = screenId }
        )
        1 -> StudentAdmissionScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        2 -> FeeCollectionScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        3 -> DailySabaqScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        4 -> DailySatSabaqScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        5 -> DailyAmukhtaScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        6 -> HifzRevisionScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        7 -> ExamFeeScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        8 -> ExamResultScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        9 -> FinancialScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        10 -> AttendanceScreen(
            viewModel = viewModel,
            onBack = { currentScreen = 0 }
        )
        else -> HomeScreen(
            viewModel = viewModel,
            onNavigateTo = { screenId -> currentScreen = screenId }
        )
    }
}
