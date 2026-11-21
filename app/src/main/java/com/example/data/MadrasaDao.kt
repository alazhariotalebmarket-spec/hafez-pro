package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MadrasaDao {

    // --- Madrasa Info ---
    @Query("SELECT * FROM madrasa_info WHERE id = 1 LIMIT 1")
    fun getMadrasaInfo(): Flow<MadrasaInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMadrasaInfo(info: MadrasaInfo)

    // --- Students ---
    @Query("SELECT * FROM students ORDER BY roll ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): Student?

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun observeStudentById(id: Long): Flow<Student?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    // --- Fees ---
    @Query("SELECT * FROM fee_payments ORDER BY paymentDate DESC")
    fun getAllFeePayments(): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE monthName = :monthName AND year = :year")
    fun getFeePaymentsByMonth(monthName: String, year: Int): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE year = :year")
    fun getFeePaymentsByYear(year: Int): Flow<List<FeePayment>>

    @Query("SELECT * FROM fee_payments WHERE studentId = :studentId ORDER BY year DESC, id DESC")
    fun getFeePaymentsForStudent(studentId: Long): Flow<List<FeePayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeePayment(payment: FeePayment): Long

    @Delete
    suspend fun deleteFeePayment(payment: FeePayment)

    // --- Daily Sabaq ---
    @Query("SELECT * FROM daily_sabaq WHERE date = :date")
    fun getDailySabaqByDate(date: String): Flow<List<DailySabaq>>

    @Query("SELECT * FROM daily_sabaq WHERE date LIKE :monthPrefix || '%'")
    fun getDailySabaqByMonth(monthPrefix: String): Flow<List<DailySabaq>> // e.g. "2026-11"

    @Query("SELECT * FROM daily_sabaq WHERE date LIKE :yearPrefix || '%'")
    fun getDailySabaqByYear(yearPrefix: String): Flow<List<DailySabaq>> // e.g. "2026"

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySabaq(sabaq: DailySabaq): Long

    @Delete
    suspend fun deleteDailySabaq(sabaq: DailySabaq)

    // --- Daily Sat Sabaq ---
    @Query("SELECT * FROM daily_sat_sabaq WHERE date = :date")
    fun getDailySatSabaqByDate(date: String): Flow<List<DailySatSabaq>>

    @Query("SELECT * FROM daily_sat_sabaq WHERE date LIKE :monthPrefix || '%'")
    fun getDailySatSabaqByMonth(monthPrefix: String): Flow<List<DailySatSabaq>>

    @Query("SELECT * FROM daily_sat_sabaq WHERE date LIKE :yearPrefix || '%'")
    fun getDailySatSabaqByYear(yearPrefix: String): Flow<List<DailySatSabaq>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySatSabaq(satSabaq: DailySatSabaq): Long

    @Delete
    suspend fun deleteDailySatSabaq(satSabaq: DailySatSabaq)

    // --- Daily Amukhta ---
    @Query("SELECT * FROM daily_amukhta WHERE date = :date")
    fun getDailyAmukhtaByDate(date: String): Flow<List<DailyAmukhta>>

    @Query("SELECT * FROM daily_amukhta WHERE date LIKE :monthPrefix || '%'")
    fun getDailyAmukhtaByMonth(monthPrefix: String): Flow<List<DailyAmukhta>>

    @Query("SELECT * FROM daily_amukhta WHERE date LIKE :yearPrefix || '%'")
    fun getDailyAmukhtaByYear(yearPrefix: String): Flow<List<DailyAmukhta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyAmukhta(amukhta: DailyAmukhta): Long

    @Delete
    suspend fun deleteDailyAmukhta(amukhta: DailyAmukhta)

    // --- Hifz Revision ---
    @Query("SELECT * FROM hifz_revision WHERE date = :date")
    fun getHifzRevisionByDate(date: String): Flow<List<HifzRevision>>

    @Query("SELECT * FROM hifz_revision WHERE date LIKE :monthPrefix || '%'")
    fun getHifzRevisionByMonth(monthPrefix: String): Flow<List<HifzRevision>>

    @Query("SELECT * FROM hifz_revision WHERE date LIKE :yearPrefix || '%'")
    fun getHifzRevisionByYear(yearPrefix: String): Flow<List<HifzRevision>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHifzRevision(revision: HifzRevision): Long

    @Delete
    suspend fun deleteHifzRevision(revision: HifzRevision)

    // --- Exam Fees ---
    @Query("SELECT * FROM exam_fees WHERE examType = :examType AND year = :year")
    fun getExamFees(examType: String, year: Int): Flow<List<ExamFee>>

    @Query("SELECT * FROM exam_fees WHERE year = :year")
    fun getExamFeesByYear(year: Int): Flow<List<ExamFee>>

    @Query("SELECT * FROM exam_fees WHERE studentId = :studentId")
    fun getExamFeesForStudent(studentId: Long): Flow<List<ExamFee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExamFee(fee: ExamFee): Long

    @Delete
    suspend fun deleteExamFee(fee: ExamFee)

    // --- Exam Results ---
    @Query("SELECT * FROM exam_results WHERE examType = :examType AND year = :year")
    fun getExamResults(examType: String, year: Int): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results WHERE year = :year")
    fun getExamResultsByYear(year: Int): Flow<List<ExamResult>>

    @Query("SELECT * FROM exam_results WHERE studentId = :studentId")
    fun getExamResultsForStudent(studentId: Long): Flow<List<ExamResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateExamResult(result: ExamResult): Long

    @Delete
    suspend fun deleteExamResult(result: ExamResult)

    // --- Financial Transactions (খরচ ও জমা) ---
    @Query("SELECT * FROM financial_transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<FinancialTransaction>>

    @Query("SELECT * FROM financial_transactions WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC, id DESC")
    fun getTransactionsByMonth(monthPrefix: String): Flow<List<FinancialTransaction>>

    @Query("SELECT * FROM financial_transactions WHERE date LIKE :yearPrefix || '%' ORDER BY date DESC, id DESC")
    fun getTransactionsByYear(yearPrefix: String): Flow<List<FinancialTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: FinancialTransaction)

    // --- Attendance Records ---
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :monthPrefix || '%'")
    fun getAttendanceByMonth(monthPrefix: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date LIKE :yearPrefix || '%'")
    fun getAttendanceByYear(yearPrefix: String): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(records: List<AttendanceRecord>)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteAttendanceByDate(date: String)
}
