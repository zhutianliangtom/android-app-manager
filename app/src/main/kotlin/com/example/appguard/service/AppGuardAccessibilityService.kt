package com.example.appguard.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.appguard.data.db.AppDatabase
import com.example.appguard.data.repository.AppLimitRepository
import com.example.appguard.data.repository.PointsRepository
import com.example.appguard.domain.NtpTimeService
import com.example.appguard.domain.PointsManager
import com.example.appguard.util.NotificationHelper
import kotlinx.coroutines.*

class AppGuardAccessibilityService : AccessibilityService() {

    private lateinit var repository: AppLimitRepository
    private lateinit var pointsManager: PointsManager
    private lateinit var notificationHelper: NotificationHelper
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastForegroundPackage: String? = null
    private var foregroundStartTime: Long? = null
    private var currentDate: String = ""
    private var ntpAvailable: Boolean = false

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = AppLimitRepository(db)
        val pointsRepo = PointsRepository(this)
        notificationHelper = NotificationHelper(this)
        pointsManager = PointsManager(pointsRepo, notificationHelper)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        scope.launch {
            checkNtpAndDate()
            if (!ntpAvailable) {
                forceGoHome()
                return@launch
            }

            val now = System.currentTimeMillis()

            // 处理上一个前台App的时长累计
            lastForegroundPackage?.let { lastPkg ->
                foregroundStartTime?.let { startTime ->
                    val elapsed = (now - startTime) / 1000
                    if (elapsed > 0) {
                        accumulateUsage(lastPkg, elapsed)
                    }
                }
            }

            // 检查新App是否受限
            lastForegroundPackage = packageName
            foregroundStartTime = now

            val limit = repository.getLimit(packageName)
            if (limit != null && limit.isEnabled) {
                val totalLimit = if (limit.isUnlocked) {
                    (limit.dailyLimitMinutes + 30) * 60L
                } else {
                    limit.dailyLimitMinutes * 60L
                }

                if (limit.todayUsedSeconds >= totalLimit) {
                    forceGoHome()
                    notificationHelper.showTimeLimitNotification(limit.appName)
                    if (!limit.isUnlocked) {
                        repository.setExceeded(packageName, true)
                    }
                }
            }
        }
    }

    private suspend fun accumulateUsage(packageName: String, elapsedSeconds: Long) {
        val limit = repository.getLimit(packageName) ?: return
        if (!limit.isEnabled) return

        val newUsed = limit.todayUsedSeconds + elapsedSeconds
        repository.updateUsage(packageName, newUsed, currentDate)
    }

    private suspend fun checkNtpAndDate() {
        val ntpTime = NtpTimeService.getNtpTime()
        ntpAvailable = ntpTime != null
        if (ntpTime != null) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
            val date = sdf.format(java.util.Date(ntpTime))
            if (date != currentDate && currentDate.isNotEmpty()) {
                checkAndRewardDaily()
                repository.resetDailyIfNeeded(date)
            }
            if (date != currentDate) {
                currentDate = date
            }
        }
    }

    private suspend fun checkAndRewardDaily() {
        val limits = repository.getAllLimitsList()
        val enabledLimits = limits.filter { it.isEnabled }
        if (enabledLimits.isNotEmpty() && enabledLimits.none { it.hasExceededToday }) {
            pointsManager.rewardDailyCompliance()
        }
    }

    private fun forceGoHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}