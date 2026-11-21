package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "madrasa_info")
data class MadrasaInfo(
    @PrimaryKey val id: Int = 1,
    val name: String = "দারুল উলুম হিফজুল কুরআন মাদ্রাসা",
    val address: String = "মিরপুর-১০, ঢাকা, বাংলাদেশ",
    val phone: String = "+৮৮০১৯৩৪৪১১১৯৯"
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roll: Int,
    val name: String,
    val fatherName: String = "",
    val address: String = "",
    val phone: String = "",
    val monthlyFee: Double = 0.0,
    val monthlyElectricityBill: Double = 0.0,
    val otherFee: Double = 0.0,
    val photoPath: String? = null,
    val admissionDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    val totalMonthlyFee: Double
        get() = monthlyFee + monthlyElectricityBill + otherFee
}

@Entity(tableName = "fee_payments")
data class FeePayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val monthName: String,
    val year: Int,
    val tuitionFee: Double = 0.0,
    val electricityBill: Double = 0.0,
    val otherFee: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "daily_sabaq")
data class DailySabaq(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val paraNumber: Int = 1, // ১ থেকে ৩০ পারা
    val lessonType: String, // "১ আয়াত", "৫ আয়াত", "আধা ছাপা", "১ ছাপা", "২ ছাপা"
    val isPassed: Boolean, // true = ✓, false = ✗
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String = ""
)

@Entity(tableName = "daily_sat_sabaq")
data class DailySatSabaq(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val paraNumber: Int = 1, // ১ থেকে ৩০ পারা
    val pagesCount: String, // "আধা ছাপা", "১ ছাপা", ..., "১৯ ছাপা"
    val isPassed: Boolean, // true = ✓, false = ✗
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String = ""
)

@Entity(tableName = "daily_amukhta")
data class DailyAmukhta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val paraNumber: Int = 1, // ১ থেকে ৩০ পারা
    val amount: String, // "৫ ছাপা", "আধা পারা", "১ পারা", "২ পারা"
    val isPassed: Boolean, // true = ✓, false = ✗
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String = ""
)

@Entity(tableName = "hifz_revision")
data class HifzRevision(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val paraNumber: Int = 1, // ১ থেকে ৩০ পারা
    val amount: String, // "আধা পারা", "১ পারা", "২ পারা", "৫ পারা"
    val isPassed: Boolean, // true = ✓, false = ✗
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String = ""
)

@Entity(tableName = "exam_fees")
data class ExamFee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val examType: String, // "প্রথম সাময়িক পরীক্ষা", "দ্বিতীয় সাময়িক পরীক্ষা", "বার্ষিক পরীক্ষা"
    val year: Int,
    val totalFee: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_results")
data class ExamResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val examType: String, // "প্রথম সাময়িক পরীক্ষা", "দ্বিতীয় সাময়িক পরীক্ষা", "বার্ষিক পরীক্ষা"
    val year: Int,
    // ১. কোরআন মাজীদ (মৌখিক)
    val quranPass: Int = 33,
    val quranObtained: Int = 0,
    val quranFull: Int = 100,
    // ২. তাজবীদ (মৌখিক)
    val tajweedPass: Int = 33,
    val tajweedObtained: Int = 0,
    val tajweedFull: Int = 100,
    // ৩. কালিমা ও মাসায়ালা (মৌখিক)
    val kalimaPass: Int = 33,
    val kalimaObtained: Int = 0,
    val kalimaFull: Int = 100,
    // ৪. দোয়া ও হাদিস (মৌখিক)
    val duaHadithPass: Int = 33,
    val duaHadithObtained: Int = 0,
    val duaHadithFull: Int = 100,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalMarks: Int
        get() = quranObtained + tajweedObtained + kalimaObtained + duaHadithObtained
    val totalFullMarks: Int
        get() = quranFull + tajweedFull + kalimaFull + duaHadithFull
    val isAllPassed: Boolean
        get() = quranObtained >= quranPass &&
                tajweedObtained >= tajweedPass &&
                kalimaObtained >= kalimaPass &&
                duaHadithObtained >= duaHadithPass
}

@Entity(tableName = "financial_transactions")
data class FinancialTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "খরচ" বা "মুহতামিম জমা"
    val category: String, // বিবরণ / খাত
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String, // YYYY-MM-DD
    val isPresent: Boolean, // true = উপস্থিত, false = অনুপস্থিত
    val timestamp: Long = System.currentTimeMillis()
)
