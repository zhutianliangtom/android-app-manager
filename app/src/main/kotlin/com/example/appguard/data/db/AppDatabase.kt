package com.example.appguard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appguard.data.db.dao.AppLimitDao
import com.example.appguard.data.db.entity.AppLimitEntity

@Database(entities = [AppLimitEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appLimitDao(): AppLimitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_guard_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}