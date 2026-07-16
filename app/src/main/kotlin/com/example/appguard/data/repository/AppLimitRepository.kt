package com.example.appguard.data.repository

import com.example.appguard.data.db.AppDatabase
import com.example.appguard.data.db.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

class AppLimitRepository(private val db: AppDatabase) {

    private val dao = db.appLimitDao()

    fun getEnabledLimits(): Flow<List<AppLimitEntity>> = dao.getEnabledLimits()

    fun getAllLimits(): Flow<List<AppLimitEntity>> = dao.getAllLimits()

    suspend fun getAllLimitsList(): List<AppLimitEntity> = dao.getAllLimitsList()

    suspend fun getLimit(packageName: String): AppLimitEntity? = dao.getLimit(packageName)

    suspend fun upsertLimit(entity: AppLimitEntity) = dao.upsertLimit(entity)

    suspend fun updateUsage(packageName: String, seconds: Long, date: String) =
        dao.updateUsage(packageName, seconds, date)

    suspend fun unlockApp(packageName: String) = dao.unlockApp(packageName, true)

    suspend fun setEnabled(packageName: String, enabled: Boolean) = dao.setEnabled(packageName, enabled)

    suspend fun setLimitMinutes(packageName: String, minutes: Int) = dao.setLimitMinutes(packageName, minutes)

    suspend fun deleteLimit(packageName: String) = dao.deleteLimit(packageName)

    suspend fun setExceeded(packageName: String, exceeded: Boolean) = dao.setExceeded(packageName, exceeded)

    suspend fun resetDailyIfNeeded(todayDate: String) {
        dao.resetDailyUsage(todayDate)
    }
}