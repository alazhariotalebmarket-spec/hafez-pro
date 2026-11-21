package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.BengaliUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MadrasaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MadrasaRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MadrasaRepository(db.madrasaDao())

        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            seedSampleStudentsIfEmpty()
        }
    }

    // Madrasa Info
    val madrasaInfo: StateFlow<MadrasaInfo?> = repository.madrasaInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Students
    val students: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active filters
    val selectedDate = MutableStateFlow(BengaliUtils.getTodayDate())
    val selectedMonth = MutableStateFlow(BengaliUtils.getCurrentMonthName())
    val selectedYear = MutableStateFlow(BengaliUtils.getCurrentYear())

    // Month prefix for SQL LIKE queries (e.g., "2026-11")
    val selectedMonthPrefix = MutableStateFlow(BengaliUtils.getCurrentMonthPrefix())

    // Fees
    val allFees: StateFlow<List<FeePayment>> = repository.allFeePayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Sabaq for selected date
    val sabaqForDate: StateFlow<List<DailySabaq>> = selectedDate
        .flatMapLatest { repository.getDailySabaqByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Sat Sabaq for selected date
    val satSabaqForDate: StateFlow<List<DailySatSabaq>> = selectedDate
        .flatMapLatest { repository.getDailySatSabaqByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Amukhta for selected date
    val amukhtaForDate: StateFlow<List<DailyAmukhta>> = selectedDate
        .flatMapLatest { repository.getDailyAmukhtaByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hifz Revision for selected date
    val hifzRevisionForDate: StateFlow<List<HifzRevision>> = selectedDate
        .flatMapLatest { repository.getHifzRevisionByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exam Type Selection
    val selectedExamType = MutableStateFlow("প্রথম সাময়িক পরীক্ষা")

    // Exam Fees for selected exam & year
    val examFeesForSelection: StateFlow<List<ExamFee>> = combine(selectedExamType, selectedYear) { exam, year ->
        exam to year
    }.flatMapLatest { (exam, year) ->
        repository.getExamFees(exam, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exam Results for selected exam & year
    val examResultsForSelection: StateFlow<List<ExamResult>> = combine(selectedExamType, selectedYear) { exam, year ->
        exam to year
    }.flatMapLatest { (exam, year) ->
        repository.getExamResults(exam, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions
    val allTransactions: StateFlow<List<FinancialTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance for selected date
    val attendanceForDate: StateFlow<List<AttendanceRecord>> = selectedDate
        .flatMapLatest { repository.getAttendanceByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun updateMadrasaInfo(name: String, address: String, phone: String) {
        viewModelScope.launch {
            repository.saveMadrasaInfo(
                MadrasaInfo(
                    id = 1,
                    name = name.ifBlank { "দারুল উলুম হিফজুল কুরআন মাদ্রাসা" },
                    address = address.ifBlank { "মিরপুর-১০, ঢাকা, বাংলাদেশ" },
                    phone = phone.ifBlank { "+৮৮০১৯৩৪৪১১১৯৯" }
                )
            )
        }
    }

    fun saveStudent(student: Student) {
        viewModelScope.launch {
            if (student.id == 0L) {
                repository.insertStudent(student)
            } else {
                repository.updateStudent(student)
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    fun saveFeePayment(payment: FeePayment) {
        viewModelScope.launch {
            repository.insertFeePayment(payment)
        }
    }

    fun deleteFeePayment(payment: FeePayment) {
        viewModelScope.launch {
            repository.deleteFeePayment(payment)
        }
    }

    // Sabaq save/toggle
    fun saveSabaq(studentId: Long, lessonType: String, isPassed: Boolean, paraNumber: Int = 1, remarks: String = "") {
        viewModelScope.launch {
            val date = selectedDate.value
            val existing = sabaqForDate.value.find { it.studentId == studentId }
            val item = DailySabaq(
                id = existing?.id ?: 0L,
                studentId = studentId,
                date = date,
                paraNumber = paraNumber,
                lessonType = lessonType,
                isPassed = isPassed,
                remarks = remarks
            )
            repository.saveDailySabaq(item)
        }
    }

    fun deleteSabaq(sabaq: DailySabaq) {
        viewModelScope.launch {
            repository.deleteDailySabaq(sabaq)
        }
    }

    // Sat Sabaq save
    fun saveSatSabaq(studentId: Long, pagesCount: String, isPassed: Boolean, paraNumber: Int = 1, remarks: String = "") {
        viewModelScope.launch {
            val date = selectedDate.value
            val existing = satSabaqForDate.value.find { it.studentId == studentId }
            val item = DailySatSabaq(
                id = existing?.id ?: 0L,
                studentId = studentId,
                date = date,
                paraNumber = paraNumber,
                pagesCount = pagesCount,
                isPassed = isPassed,
                remarks = remarks
            )
            repository.saveDailySatSabaq(item)
        }
    }

    fun deleteSatSabaq(item: DailySatSabaq) {
        viewModelScope.launch {
            repository.deleteDailySatSabaq(item)
        }
    }

    // Amukhta save
    fun saveAmukhta(studentId: Long, amount: String, isPassed: Boolean, paraNumber: Int = 1, remarks: String = "") {
        viewModelScope.launch {
            val date = selectedDate.value
            val existing = amukhtaForDate.value.find { it.studentId == studentId }
            val item = DailyAmukhta(
                id = existing?.id ?: 0L,
                studentId = studentId,
                date = date,
                paraNumber = paraNumber,
                amount = amount,
                isPassed = isPassed,
                remarks = remarks
            )
            repository.saveDailyAmukhta(item)
        }
    }

    fun deleteAmukhta(item: DailyAmukhta) {
        viewModelScope.launch {
            repository.deleteDailyAmukhta(item)
        }
    }

    // Hifz Revision save
    fun saveHifzRevision(studentId: Long, amount: String, isPassed: Boolean, paraNumber: Int = 1, remarks: String = "") {
        viewModelScope.launch {
            val date = selectedDate.value
            val existing = hifzRevisionForDate.value.find { it.studentId == studentId }
            val item = HifzRevision(
                id = existing?.id ?: 0L,
                studentId = studentId,
                date = date,
                paraNumber = paraNumber,
                amount = amount,
                isPassed = isPassed,
                remarks = remarks
            )
            repository.saveHifzRevision(item)
        }
    }

    fun deleteHifzRevision(item: HifzRevision) {
        viewModelScope.launch {
            repository.deleteHifzRevision(item)
        }
    }

    // Exam Fee save
    fun saveExamFee(studentId: Long, totalFee: Double, paidAmount: Double) {
        viewModelScope.launch {
            val exam = selectedExamType.value
            val year = selectedYear.value
            val existing = examFeesForSelection.value.find { it.studentId == studentId }
            val due = (totalFee - paidAmount).coerceAtLeast(0.0)
            val fee = ExamFee(
                id = existing?.id ?: 0L,
                studentId = studentId,
                examType = exam,
                year = year,
                totalFee = totalFee,
                paidAmount = paidAmount,
                dueAmount = due
            )
            repository.saveExamFee(fee)
        }
    }

    fun deleteExamFee(fee: ExamFee) {
        viewModelScope.launch {
            repository.deleteExamFee(fee)
        }
    }

    // Exam Result save
    fun saveExamResult(
        studentId: Long,
        quranPass: Int, quranObtained: Int, quranFull: Int,
        tajweedPass: Int, tajweedObtained: Int, tajweedFull: Int,
        kalimaPass: Int, kalimaObtained: Int, kalimaFull: Int,
        duaHadithPass: Int, duaHadithObtained: Int, duaHadithFull: Int
    ) {
        viewModelScope.launch {
            val exam = selectedExamType.value
            val year = selectedYear.value
            val existing = examResultsForSelection.value.find { it.studentId == studentId }
            val result = ExamResult(
                id = existing?.id ?: 0L,
                studentId = studentId,
                examType = exam,
                year = year,
                quranPass = quranPass,
                quranObtained = quranObtained,
                quranFull = quranFull,
                tajweedPass = tajweedPass,
                tajweedObtained = tajweedObtained,
                tajweedFull = tajweedFull,
                kalimaPass = kalimaPass,
                kalimaObtained = kalimaObtained,
                kalimaFull = kalimaFull,
                duaHadithPass = duaHadithPass,
                duaHadithObtained = duaHadithObtained,
                duaHadithFull = duaHadithFull
            )
            repository.saveExamResult(result)
        }
    }

    // Financial Transaction save
    fun saveTransaction(type: String, category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val date = selectedDate.value
            val transaction = FinancialTransaction(
                type = type,
                category = category,
                amount = amount,
                date = date,
                notes = notes
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun markSingleAttendance(studentId: Long, isPresent: Boolean) {
        viewModelScope.launch {
            val date = selectedDate.value
            val existing = attendanceForDate.value.find { it.studentId == studentId }
            val record = AttendanceRecord(
                id = existing?.id ?: 0L,
                studentId = studentId,
                date = date,
                isPresent = isPresent
            )
            repository.saveAttendanceRecord(record)
        }
    }

    // Attendance
    fun toggleAttendance(studentId: Long) {
        viewModelScope.launch {
            val date = selectedDate.value
            val existing = attendanceForDate.value.find { it.studentId == studentId }
            val isNowPresent = if (existing == null) true else !existing.isPresent
            val record = AttendanceRecord(
                id = existing?.id ?: 0L,
                studentId = studentId,
                date = date,
                isPresent = isNowPresent
            )
            repository.saveAttendanceRecord(record)
        }
    }

    fun markAllAttendance(isPresent: Boolean) {
        viewModelScope.launch {
            val date = selectedDate.value
            val allList = students.value.map { student ->
                val existing = attendanceForDate.value.find { it.studentId == student.id }
                AttendanceRecord(
                    id = existing?.id ?: 0L,
                    studentId = student.id,
                    date = date,
                    isPresent = isPresent
                )
            }
            repository.saveAllAttendance(allList)
        }
    }

    // Monthly / Yearly Queries helpers for reports
    fun getSabaqForMonth(monthPrefix: String): Flow<List<DailySabaq>> = repository.getDailySabaqByMonth(monthPrefix)
    fun getSabaqForYear(yearPrefix: String): Flow<List<DailySabaq>> = repository.getDailySabaqByYear(yearPrefix)

    fun getSatSabaqForMonth(monthPrefix: String): Flow<List<DailySatSabaq>> = repository.getDailySatSabaqByMonth(monthPrefix)
    fun getSatSabaqForYear(yearPrefix: String): Flow<List<DailySatSabaq>> = repository.getDailySatSabaqByYear(yearPrefix)

    fun getAmukhtaForMonth(monthPrefix: String): Flow<List<DailyAmukhta>> = repository.getDailyAmukhtaByMonth(monthPrefix)
    fun getAmukhtaForYear(yearPrefix: String): Flow<List<DailyAmukhta>> = repository.getDailyAmukhtaByYear(yearPrefix)

    fun getHifzRevisionForMonth(monthPrefix: String): Flow<List<HifzRevision>> = repository.getHifzRevisionByMonth(monthPrefix)
    fun getHifzRevisionForYear(yearPrefix: String): Flow<List<HifzRevision>> = repository.getHifzRevisionByYear(yearPrefix)

    fun getAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceRecord>> = repository.getAttendanceByMonth(monthPrefix)
    fun getAttendanceForYear(yearPrefix: String): Flow<List<AttendanceRecord>> = repository.getAttendanceByYear(yearPrefix)

    fun getTransactionsForMonth(monthPrefix: String): Flow<List<FinancialTransaction>> = repository.getTransactionsByMonth(monthPrefix)
    fun getTransactionsForYear(yearPrefix: String): Flow<List<FinancialTransaction>> = repository.getTransactionsByYear(yearPrefix)

    private suspend fun seedSampleStudentsIfEmpty() {
        val current = repository.allStudents.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val samples = listOf(
                Student(
                    roll = 1,
                    name = "হাফেজ আবদুল্লাহ",
                    fatherName = "মুহাম্মদ রফিকুল ইসলাম",
                    address = "মিরপুর-১০, ঢাকা",
                    phone = "01711223344",
                    monthlyFee = 1500.0,
                    monthlyElectricityBill = 200.0,
                    otherFee = 100.0
                ),
                Student(
                    roll = 2,
                    name = "মুহাম্মদ উমর ফারুক",
                    fatherName = "মাওলানা আবদুল কুদ্দুস",
                    address = "মোহাম্মদপুর, ঢাকা",
                    phone = "01822334455",
                    monthlyFee = 1500.0,
                    monthlyElectricityBill = 200.0,
                    otherFee = 50.0
                ),
                Student(
                    roll = 3,
                    name = "তালহা জুবায়ের",
                    fatherName = "মোস্তাফিজুর রহমান",
                    address = "উত্তরা, ঢাকা",
                    phone = "01933445566",
                    monthlyFee = 1200.0,
                    monthlyElectricityBill = 200.0,
                    otherFee = 0.0
                ),
                Student(
                    roll = 4,
                    name = "মুহাম্মদ সালমান",
                    fatherName = "জাকির হোসেন",
                    address = "পল্লবী, ঢাকা",
                    phone = "01644556677",
                    monthlyFee = 1500.0,
                    monthlyElectricityBill = 200.0,
                    otherFee = 100.0
                )
            )
            samples.forEach { repository.insertStudent(it) }
        }
    }
}
