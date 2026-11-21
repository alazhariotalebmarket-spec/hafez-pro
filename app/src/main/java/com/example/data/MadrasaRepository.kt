package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MadrasaRepository(private val dao: MadrasaDao) {

    // --- Madrasa Info ---
    val madrasaInfo: Flow<MadrasaInfo?> = dao.getMadrasaInfo()

    suspend fun saveMadrasaInfo(info: MadrasaInfo) {
        dao.saveMadrasaInfo(info)
    }

    // --- Students ---
    val allStudents: Flow<List<Student>> = dao.getAllStudents()

    suspend fun getStudentById(id: Long): Student? = dao.getStudentById(id)

    fun observeStudentById(id: Long): Flow<Student?> = dao.observeStudentById(id)

    suspend fun insertStudent(student: Student): Long = dao.insertStudent(student)

    suspend fun updateStudent(student: Student) = dao.updateStudent(student)

    suspend fun deleteStudent(student: Student) = dao.deleteStudent(student)

    // --- Fees ---
    val allFeePayments: Flow<List<FeePayment>> = dao.getAllFeePayments()

    fun getFeePaymentsByMonth(monthName: String, year: Int): Flow<List<FeePayment>> =
        dao.getFeePaymentsByMonth(monthName, year)

    fun getFeePaymentsByYear(year: Int): Flow<List<FeePayment>> =
        dao.getFeePaymentsByYear(year)

    fun getFeePaymentsForStudent(studentId: Long): Flow<List<FeePayment>> =
        dao.getFeePaymentsForStudent(studentId)

    suspend fun insertFeePayment(payment: FeePayment): Long = dao.insertFeePayment(payment)

    suspend fun deleteFeePayment(payment: FeePayment) = dao.deleteFeePayment(payment)

    // --- Daily Sabaq ---
    fun getDailySabaqByDate(date: String): Flow<List<DailySabaq>> = dao.getDailySabaqByDate(date)
    fun getDailySabaqByMonth(monthPrefix: String): Flow<List<DailySabaq>> = dao.getDailySabaqByMonth(monthPrefix)
    fun getDailySabaqByYear(yearPrefix: String): Flow<List<DailySabaq>> = dao.getDailySabaqByYear(yearPrefix)
    suspend fun saveDailySabaq(sabaq: DailySabaq): Long = dao.insertOrUpdateDailySabaq(sabaq)
    suspend fun deleteDailySabaq(sabaq: DailySabaq) = dao.deleteDailySabaq(sabaq)

    // --- Daily Sat Sabaq ---
    fun getDailySatSabaqByDate(date: String): Flow<List<DailySatSabaq>> = dao.getDailySatSabaqByDate(date)
    fun getDailySatSabaqByMonth(monthPrefix: String): Flow<List<DailySatSabaq>> = dao.getDailySatSabaqByMonth(monthPrefix)
    fun getDailySatSabaqByYear(yearPrefix: String): Flow<List<DailySatSabaq>> = dao.getDailySatSabaqByYear(yearPrefix)
    suspend fun saveDailySatSabaq(satSabaq: DailySatSabaq): Long = dao.insertOrUpdateDailySatSabaq(satSabaq)
    suspend fun deleteDailySatSabaq(satSabaq: DailySatSabaq) = dao.deleteDailySatSabaq(satSabaq)

    // --- Daily Amukhta ---
    fun getDailyAmukhtaByDate(date: String): Flow<List<DailyAmukhta>> = dao.getDailyAmukhtaByDate(date)
    fun getDailyAmukhtaByMonth(monthPrefix: String): Flow<List<DailyAmukhta>> = dao.getDailyAmukhtaByMonth(monthPrefix)
    fun getDailyAmukhtaByYear(yearPrefix: String): Flow<List<DailyAmukhta>> = dao.getDailyAmukhtaByYear(yearPrefix)
    suspend fun saveDailyAmukhta(amukhta: DailyAmukhta): Long = dao.insertOrUpdateDailyAmukhta(amukhta)
    suspend fun deleteDailyAmukhta(amukhta: DailyAmukhta) = dao.deleteDailyAmukhta(amukhta)

    // --- Hifz Revision ---
    fun getHifzRevisionByDate(date: String): Flow<List<HifzRevision>> = dao.getHifzRevisionByDate(date)
    fun getHifzRevisionByMonth(monthPrefix: String): Flow<List<HifzRevision>> = dao.getHifzRevisionByMonth(monthPrefix)
    fun getHifzRevisionByYear(yearPrefix: String): Flow<List<HifzRevision>> = dao.getHifzRevisionByYear(yearPrefix)
    suspend fun saveHifzRevision(revision: HifzRevision): Long = dao.insertOrUpdateHifzRevision(revision)
    suspend fun deleteHifzRevision(revision: HifzRevision) = dao.deleteHifzRevision(revision)

    // --- Exam Fees ---
    fun getExamFees(examType: String, year: Int): Flow<List<ExamFee>> = dao.getExamFees(examType, year)
    fun getExamFeesByYear(year: Int): Flow<List<ExamFee>> = dao.getExamFeesByYear(year)
    fun getExamFeesForStudent(studentId: Long): Flow<List<ExamFee>> = dao.getExamFeesForStudent(studentId)
    suspend fun saveExamFee(fee: ExamFee): Long = dao.insertOrUpdateExamFee(fee)
    suspend fun deleteExamFee(fee: ExamFee) = dao.deleteExamFee(fee)

    // --- Exam Results ---
    fun getExamResults(examType: String, year: Int): Flow<List<ExamResult>> = dao.getExamResults(examType, year)
    fun getExamResultsByYear(year: Int): Flow<List<ExamResult>> = dao.getExamResultsByYear(year)
    fun getExamResultsForStudent(studentId: Long): Flow<List<ExamResult>> = dao.getExamResultsForStudent(studentId)
    suspend fun saveExamResult(result: ExamResult): Long = dao.insertOrUpdateExamResult(result)
    suspend fun deleteExamResult(result: ExamResult) = dao.deleteExamResult(result)

    // --- Transactions (খরচ ও জমা) ---
    val allTransactions: Flow<List<FinancialTransaction>> = dao.getAllTransactions()
    fun getTransactionsByMonth(monthPrefix: String): Flow<List<FinancialTransaction>> = dao.getTransactionsByMonth(monthPrefix)
    fun getTransactionsByYear(yearPrefix: String): Flow<List<FinancialTransaction>> = dao.getTransactionsByYear(yearPrefix)
    suspend fun insertTransaction(transaction: FinancialTransaction): Long = dao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: FinancialTransaction) = dao.deleteTransaction(transaction)

    // --- Attendance ---
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>> = dao.getAttendanceByDate(date)
    fun getAttendanceByMonth(monthPrefix: String): Flow<List<AttendanceRecord>> = dao.getAttendanceByMonth(monthPrefix)
    fun getAttendanceByYear(yearPrefix: String): Flow<List<AttendanceRecord>> = dao.getAttendanceByYear(yearPrefix)
    suspend fun saveAttendanceRecord(record: AttendanceRecord): Long = dao.insertAttendanceRecord(record)
    suspend fun saveAllAttendance(records: List<AttendanceRecord>) = dao.insertAllAttendance(records)
    suspend fun deleteAttendanceByDate(date: String) = dao.deleteAttendanceByDate(date)

    // Initialize default madrasa settings if not present
    suspend fun initializeDefaultsIfNeeded() {
        val existing = dao.getMadrasaInfo().firstOrNull()
        if (existing == null) {
            dao.saveMadrasaInfo(
                MadrasaInfo(
                    id = 1,
                    name = "দারুল উলুম হিফজুল কুরআন মাদ্রাসা",
                    address = "মিরপুর-১০, ঢাকা, বাংলাদেশ",
                    phone = "+৮৮০১৯৩৪৪১১১৯৯"
                )
            )
        }
    }
}
