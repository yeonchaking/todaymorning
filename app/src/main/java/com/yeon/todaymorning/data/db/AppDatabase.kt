package com.yeon.todaymorning.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MissionRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun missionDao(): MissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 → v2: date 컬럼에 유니크 인덱스 추가.
         * 혹시 중복 날짜 레코드가 있다면 id가 작은 것(먼저 들어온 것)만 남긴다.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 중복 날짜 제거 (최소 id만 보존)
                database.execSQL(
                    """
                    DELETE FROM mission_records
                    WHERE id NOT IN (
                        SELECT MIN(id) FROM mission_records GROUP BY date
                    )
                    """.trimIndent()
                )
                // 유니크 인덱스 생성
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_mission_records_date ON mission_records (date)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todaymorning.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
