package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MadrasaInfo::class,
        Student::class,
        FeePayment::class,
        DailySabaq::class,
        DailySatSabaq::class,
        DailyAmukhta::class,
        HifzRevision::class,
        ExamFee::class,
        ExamResult::class,
        FinancialTransaction::class,
        AttendanceRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun madrasaDao(): MadrasaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_sabaq ADD COLUMN paraNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_sat_sabaq ADD COLUMN paraNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE daily_amukhta ADD COLUMN paraNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE hifz_revision ADD COLUMN paraNumber INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hafez_pro_madrasa.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
