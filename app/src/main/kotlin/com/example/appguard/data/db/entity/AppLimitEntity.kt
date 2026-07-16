package com.example.appguard.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 30,
    val todayUsedSeconds: Long = 0,
    val todayDate: String = "",
    val isUnlocked: Boolean = false,
    val isEnabled: Boolean = true,
    val hasExceededToday: Boolean = false
)