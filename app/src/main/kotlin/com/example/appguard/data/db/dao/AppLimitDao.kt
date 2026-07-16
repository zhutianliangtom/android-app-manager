package com.example.appguard.data.db.dao

import androidx.room.*
import com.example.appguard.data.db.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitDao {

    @Query("SELECT * FROM app_limits WHERE isEnabled = 1")
    fun getEnabledLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits")
    fun getAllLimits(): Flow<List<AppLimitEntity>>

    @Query("SELECT * FROM app_limits")
    suspend fun getAllLimitsList(): List<AppLimitEntity>

    @Query("SELECT * FROM app_limits WHERE packageName = :packageName")
    suspend fun getLimit(packageName: String): AppLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLimit(entity: AppLimitEntity)

    @Query("UPDATE app_limits SET todayUsedSeconds = :seconds, todayDate = :date WHERE packageName = :packageName")
    suspend fun updateUsage(packageName: String, seconds: Long, date: String)

    @Query("UPDATE app_limits SET isUnlocked = :unlocked, dailyLimitMinutes = dailyLimitMinutes + 30 WHERE packageName = :packageName")
    suspend fun unlockApp(packageName: String, unlocked: Boolean)

    @Query("UPDATE app_limits SET todayUsedSeconds = 0, todayDate = :date, isUnlocked = 0, hasExceededToday = 0")
    suspend fun resetDailyUsage(date: String)

    @Query("UPDATE app_limits SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE app_limits SET dailyLimitMinutes = :minutes WHERE packageName = :packageName")
    suspend fun setLimitMinutes(packageName: String, minutes: Int)

    @Query("UPDATE app_limits SET hasExceededToday = :exceeded WHERE packageName = :packageName")
    suspend fun setExceeded(packageName: String, exceeded: Boolean)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)
}