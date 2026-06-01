package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Главный класс базы данных Room.
 * Хранит таблицы Medications и IntakeLogs.
 */
@Database(
    entities = [Medication::class, IntakeLog::class],
    version = 3,
    exportSchema = false
)
abstract class PillsDatabase : RoomDatabase() {

    abstract fun pillsDao(): PillsDao

    companion object {
        @Volatile
        private var INSTANCE: PillsDatabase? = null

        /**
         * Получение инстанса базы данных (Singleton).
         */
        fun getDatabase(context: Context): PillsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PillsDatabase::class.java,
                    "smart_pills_database"
                )
                .fallbackToDestructiveMigration() // Позволяет избежать сбоев при изменении схемы разработчиком
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
