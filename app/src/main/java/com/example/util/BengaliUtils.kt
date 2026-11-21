package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object BengaliUtils {
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    val months = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun toBn(number: Number): String {
        val s = number.toString()
        val sb = StringBuilder()
        for (ch in s) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toBn(str: String): String {
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatCurrency(amount: Double): String {
        val formatted = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
        return "${toBn(formatted)} ৳"
    }

    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getCurrentYear(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR)
    }

    fun getCurrentMonthName(): String {
        val cal = Calendar.getInstance()
        val monthIdx = cal.get(Calendar.MONTH)
        return months[monthIdx]
    }

    fun getCurrentMonthPrefix(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun formatDateToBn(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1].toIntOrNull() ?: 1
                val day = parts[2]
                val mName = if (month in 1..12) months[month - 1] else parts[1]
                "${toBn(day)} $mName, ${toBn(year)}"
            } else {
                toBn(dateStr)
            }
        } catch (e: Exception) {
            toBn(dateStr)
        }
    }
}
