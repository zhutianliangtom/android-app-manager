# App Usage Manager - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app usage manager with time limits, forced exit via AccessibilityService, points system, and blue-white theme UI.

**Architecture:** MVVM + Repository, single-module Android project. Room for persistence, NTP for time, AccessibilityService for app monitoring, Jetpack Compose for UI.

**Tech Stack:** AGP 8.13.2, Kotlin 2.3.20, Gradle 8.11.1, Jetpack Compose BOM 2024.12.01, Room 2.6.1, Compose Navigation 2.8.5

---

## File Structure

```
/workspace/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/strings.xml
│       │   ├── values/colors.xml
│       │   ├── xml/accessibility_service_config.xml
│       │   └── drawable/ic_launcher_foreground.xml
│       └── kotlin/com/example/appguard/
│           ├── AppGuardApp.kt
│           ├── MainActivity.kt
│           ├── data/
│           │   ├── db/AppDatabase.kt
│           │   ├── db/entity/AppLimitEntity.kt
│           │   ├── db/dao/AppLimitDao.kt
│           │   ├── repository/AppLimitRepository.kt
│           │   └── repository/PointsRepository.kt
│           ├── domain/
│           │   ├── NtpTimeService.kt
│           │   └── PointsManager.kt
│           ├── service/AppGuardAccessibilityService.kt
│           ├── ui/
│           │   ├── theme/Color.kt
│           │   ├── theme/Theme.kt
│           │   ├── theme/Type.kt
│           │   ├── navigation/NavGraph.kt
│           │   ├── screens/home/HomeScreen.kt
│           │   ├── screens/home/HomeViewModel.kt
│           │   ├── screens/apps/AppsScreen.kt
│           │   ├── screens/apps/AppsViewModel.kt
│           │   ├── screens/settings/SettingsScreen.kt
│           │   └── components/AppLimitItem.kt
│           └── util/NotificationHelper.kt
```

---

### Task 1: Project Scaffold & Gradle Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Create settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppGuard"
include(":app")
```

- [ ] **Step 2: Create root build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
}
```

- [ ] **Step 3: Create gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 4: Create gradle/wrapper/gradle-wrapper.properties**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 5: Create app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.appguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.appguard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = "2024.12.01"
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 6: Create app/proguard-rules.pro**

```
# Keep Room entities
-keep class com.example.appguard.data.db.entity.** { *; }

# Keep AccessibilityService
-keep class com.example.appguard.service.AppGuardAccessibilityService { *; }

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.appguard.**$$serializer { *; }
-keepclassmembers class com.example.appguard.** { *** Companion; }
-keepclasseswithmembers class com.example.appguard.** { kotlinx.serialization.KSerializer serializer(...); }
```

---

### Task 2: Room Database - Entity & DAO

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/data/db/entity/AppLimitEntity.kt`
- Create: `app/src/main/kotlin/com/example/appguard/data/db/dao/AppLimitDao.kt`
- Create: `app/src/main/kotlin/com/example/appguard/data/db/AppDatabase.kt`

- [ ] **Step 1: Create AppLimitEntity.kt**

```kotlin
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
```

- [ ] **Step 2: Create AppLimitDao.kt**

```kotlin
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
```

- [ ] **Step 3: Create AppDatabase.kt**

```kotlin
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
```

---

### Task 3: Repository Layer

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/data/repository/AppLimitRepository.kt`
- Create: `app/src/main/kotlin/com/example/appguard/data/repository/PointsRepository.kt`

- [ ] **Step 1: Create AppLimitRepository.kt**

```kotlin
package com.example.appguard.data.repository

import com.example.appguard.data.db.AppDatabase
import com.example.appguard.data.db.entity.AppLimitEntity
import kotlinx.coroutines.flow.Flow

class AppLimitRepository(private val db: AppDatabase) {

    private val dao = db.appLimitDao()

    fun getEnabledLimits(): Flow<List<AppLimitEntity>> = dao.getEnabledLimits()

    fun getAllLimits(): Flow<List<AppLimitEntity>> = dao.getAllLimits()

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
        val limits = dao.getAllLimits()
        // This is a Flow, we need to collect. We'll handle cross-day reset in the service layer.
        dao.resetDailyUsage(todayDate)
    }
}
```

- [ ] **Step 2: Create PointsRepository.kt**

```kotlin
package com.example.appguard.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PointsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("points_prefs", Context.MODE_PRIVATE)
    private val _points = MutableStateFlow(prefs.getInt(KEY_POINTS, 0))
    val points: StateFlow<Int> = _points

    fun addPoints(amount: Int) {
        val newValue = _points.value + amount
        _points.value = newValue
        prefs.edit().putInt(KEY_POINTS, newValue).apply()
    }

    fun deductPoints(amount: Int): Int {
        val newValue = _points.value - amount
        _points.value = newValue
        prefs.edit().putInt(KEY_POINTS, newValue).apply()
        return newValue
    }

    companion object {
        private const val KEY_POINTS = "total_points"
    }
}
```

---

### Task 4: NTP Time Service

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/domain/NtpTimeService.kt`

- [ ] **Step 1: Create NtpTimeService.kt**

```kotlin
package com.example.appguard.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object NtpTimeService {

    private const val NTP_SERVER = "ntp.aliyun.com"
    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48
    private const val NTP_OFFSET_SECONDS = 2208988800L
    private const val TIMEOUT_MS = 5000

    /**
     * 获取阿里云 NTP 时间，返回毫秒时间戳。
     * 无网络或超时时返回 null，调用方应据此阻断所有目标App。
     */
    suspend fun getNtpTime(): Long? = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = TIMEOUT_MS

            val address = InetAddress.getByName(NTP_SERVER)
            val buffer = ByteArray(NTP_PACKET_SIZE)
            buffer[0] = 0x1B.toByte()

            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(requestPacket)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            socket.close()

            val seconds = readTimestamp(buffer, 40)
            val fraction = readTimestamp(buffer, 44)
            val millis = (seconds - NTP_OFFSET_SECONDS) * 1000 + (fraction * 1000L) / 0x100000000L
            millis
        } catch (e: Exception) {
            null
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0..3) {
            value = (value shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        return value
    }
}
```

---

### Task 5: Notification Helper

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/util/NotificationHelper.kt`

- [ ] **Step 1: Create NotificationHelper.kt**

```kotlin
package com.example.appguard.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_TIME_LIMIT = "time_limit_channel"
        const val CHANNEL_POINTS = "points_channel"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val timeLimitChannel = NotificationChannel(
            CHANNEL_TIME_LIMIT,
            "时长提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "App使用时长超限通知"
        }

        val pointsChannel = NotificationChannel(
            CHANNEL_POINTS,
            "积分变动",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "积分变动通知"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(timeLimitChannel)
        manager.createNotificationChannel(pointsChannel)
    }

    fun showTimeLimitNotification(appName: String) {
        if (!hasNotificationPermission()) return

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIME_LIMIT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("时长提醒")
            .setContentText("${appName}应用已到时长")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }

    fun showPointsNotification(title: String, message: String) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_POINTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2001, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
```

---

### Task 6: PointsManager

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/domain/PointsManager.kt`

- [ ] **Step 1: Create PointsManager.kt**

```kotlin
package com.example.appguard.domain

import com.example.appguard.data.repository.PointsRepository
import com.example.appguard.util.NotificationHelper

class PointsManager(
    private val pointsRepo: PointsRepository,
    private val notificationHelper: NotificationHelper
) {
    companion object {
        const val REWARD_POINTS = 10
        const val UNLOCK_PENALTY = 20
        val ENCOURAGEMENTS = listOf(
            "自律给你自由，加油！",
            "再坚持一下，你可以的！",
            "今天的克制是为了更好的明天！",
            "每一次坚持都是自我超越！",
            "控制屏幕时间，掌控人生！"
        )
    }

    fun rewardDailyCompliance() {
        pointsRepo.addPoints(REWARD_POINTS)
        val msg = ENCOURAGEMENTS.random()
        notificationHelper.showPointsNotification(
            "积分奖励 +${REWARD_POINTS}",
            msg
        )
    }

    fun penalizeUnlock(): Int {
        val newPoints = pointsRepo.deductPoints(UNLOCK_PENALTY)
        notificationHelper.showPointsNotification(
            "积分扣除 -${UNLOCK_PENALTY}",
            "别灰心，明天继续加油！"
        )
        return newPoints
    }

    fun getCurrentPoints(): Int = pointsRepo.points.value
}
```

---

### Task 7: AccessibilityService

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/service/AppGuardAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`

- [ ] **Step 1: Create accessibility_service_config.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="100" />
```

- [ ] **Step 2: Create AppGuardAccessibilityService.kt**

```kotlin
package com.example.appguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
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
                // 无网络：强制退出所有目标App
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
                if (limit.isUnlocked) {
                    // 已解除限制，但追加时长已用完
                    val totalLimit = (limit.dailyLimitMinutes) * 60L
                    if (limit.todayUsedSeconds >= totalLimit) {
                        forceGoHome()
                        notificationHelper.showTimeLimitNotification(limit.appName)
                    }
                } else {
                    val totalLimit = limit.dailyLimitMinutes * 60L
                    if (limit.todayUsedSeconds >= totalLimit) {
                        forceGoHome()
                        notificationHelper.showTimeLimitNotification(limit.appName)
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

        val totalLimit = if (limit.isUnlocked) {
            limit.dailyLimitMinutes * 60L
        } else {
            limit.dailyLimitMinutes * 60L
        }

        if (newUsed >= totalLimit && !limit.isUnlocked) {
            repository.setExceeded(packageName, true)
        }
    }

    private suspend fun checkNtpAndDate() {
        val ntpTime = NtpTimeService.getNtpTime()
        ntpAvailable = ntpTime != null
        if (ntpTime != null) {
            // 用 NTP 时间计算日期
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
            val date = sdf.format(java.util.Date(ntpTime))
            if (date != currentDate) {
                currentDate = date
                repository.resetDailyIfNeeded(date)
            }
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
```

---

### Task 8: Application Class & AndroidManifest

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/AppGuardApp.kt`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create AppGuardApp.kt**

```kotlin
package com.example.appguard

import android.app.Application
import com.example.appguard.data.db.AppDatabase
import com.example.appguard.data.repository.AppLimitRepository
import com.example.appguard.data.repository.PointsRepository
import com.example.appguard.domain.PointsManager
import com.example.appguard.util.NotificationHelper

class AppGuardApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var appLimitRepository: AppLimitRepository
        private set
    lateinit var pointsRepository: PointsRepository
        private set
    lateinit var pointsManager: PointsManager
        private set
    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        appLimitRepository = AppLimitRepository(database)
        pointsRepository = PointsRepository(this)
        notificationHelper = NotificationHelper(this)
        pointsManager = PointsManager(pointsRepository, notificationHelper)
    }
}
```

- [ ] **Step 2: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <application
        android:name=".AppGuardApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="AppGuard"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppGuard"
        xmlns:tools="http://schemas.android.com/tools">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.AppGuard">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.AppGuardAccessibilityService"
            android:exported="true"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>
    </application>
</manifest>
```

---

### Task 9: UI Theme (Blue-White)

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/ui/theme/Color.kt`
- Create: `app/src/main/kotlin/com/example/appguard/ui/theme/Type.kt`
- Create: `app/src/main/kotlin/com/example/appguard/ui/theme/Theme.kt`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Create Color.kt**

```kotlin
package com.example.appguard.ui.theme

import androidx.compose.ui.graphics.Color

val Blue10 = Color(0xFF001F33)
val Blue20 = Color(0xFF003D66)
val Blue30 = Color(0xFF005C99)
val Blue40 = Color(0xFF007ACC)
val Blue80 = Color(0xFF99D1FF)
val Blue90 = Color(0xFFCCE8FF)

val White = Color(0xFFFFFFFF)
val BlueGray50 = Color(0xFFE8EDF2)
val BlueGray100 = Color(0xFFD0D9E0)
val BlueGray200 = Color(0xFFA0B0C0)
```

- [ ] **Step 2: Create Type.kt**

```kotlin
package com.example.appguard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
package com.example.appguard.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Blue30,
    onSecondary = White,
    secondaryContainer = Blue80,
    onSecondaryContainer = Blue10,
    background = White,
    onBackground = Blue10,
    surface = White,
    onSurface = Blue10,
    surfaceVariant = BlueGray50,
    onSurfaceVariant = Blue20,
    outline = BlueGray200
)

@Composable
fun AppGuardTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

- [ ] **Step 4: Create colors.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="blue_primary">#007ACC</color>
    <color name="blue_dark">#003D66</color>
    <color name="white">#FFFFFF</color>
    <color name="blue_light">#CCE8FF</color>
</resources>
```

- [ ] **Step 5: Create strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AppGuard</string>
    <string name="home_title">今日概览</string>
    <string name="apps_title">App管理</string>
    <string name="settings_title">设置</string>
    <string name="points_label">我的积分</string>
    <string name="unlock_button">解除限制 (-20分)</string>
    <string name="no_apps_limited">暂无限制的App</string>
    <string name="accessibility_guide">请前往设置开启无障碍服务</string>
</resources>
```

---

### Task 10: HomeScreen & HomeViewModel

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/ui/screens/home/HomeViewModel.kt`
- Create: `app/src/main/kotlin/com/example/appguard/ui/screens/home/HomeScreen.kt`

- [ ] **Step 1: Create HomeViewModel.kt**

```kotlin
package com.example.appguard.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appguard.AppGuardApp
import com.example.appguard.data.db.entity.AppLimitEntity
import com.example.appguard.domain.NtpTimeService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val points: Int = 0,
    val limitedApps: List<AppLimitEntity> = emptyList(),
    val encouragement: String = "自律给你自由",
    val isNtpAvailable: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppGuardApp
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.pointsRepository.points.collect { pts ->
                _uiState.update { it.copy(points = pts) }
            }
        }
        viewModelScope.launch {
            app.appLimitRepository.getEnabledLimits().collect { limits ->
                _uiState.update { it.copy(limitedApps = limits) }
            }
        }
        viewModelScope.launch {
            val ntpTime = NtpTimeService.getNtpTime()
            _uiState.update { it.copy(isNtpAvailable = ntpTime != null) }
        }
    }
}
```

- [ ] **Step 2: Create HomeScreen.kt**

```kotlin
package com.example.appguard.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 积分卡片
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically()
            ) {
                PointsCard(points = uiState.points)
            }
        }

        // 网络状态
        item {
            if (!uiState.isNtpAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "网络不可用，已限制所有目标App",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 鼓励语
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInHorizontally()
            ) {
                EncouragementCard(text = uiState.encouragement)
            }
        }

        // 今日已用时长
        item {
            Text(
                text = "今日使用情况",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (uiState.limitedApps.isEmpty()) {
            item {
                Text(
                    text = "暂无限制的App，前往管理页面添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(uiState.limitedApps, key = { it.packageName }) { app ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically()
                ) {
                    UsageProgressItem(app)
                }
            }
        }
    }
}

@Composable
fun PointsCard(points: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "我的积分",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$points",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EncouragementCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun UsageProgressItem(app: com.example.appguard.data.db.entity.AppLimitEntity) {
    val totalSeconds = app.dailyLimitMinutes * 60L
    val progress = if (totalSeconds > 0) {
        (app.todayUsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${app.todayUsedSeconds / 60} / ${app.dailyLimitMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress >= 1f)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            if (app.isUnlocked) {
                Text(
                    text = "已解除限制",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
```

---

### Task 11: AppsScreen & AppsViewModel

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/ui/screens/apps/AppsViewModel.kt`
- Create: `app/src/main/kotlin/com/example/appguard/ui/screens/apps/AppsScreen.kt`

- [ ] **Step 1: Create AppsViewModel.kt**

```kotlin
package com.example.appguard.ui.screens.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.appguard.AppGuardApp
import com.example.appguard.data.db.entity.AppLimitEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isLimited: Boolean = false,
    val dailyLimitMinutes: Int = 30,
    val isUnlocked: Boolean = false,
    val todayUsedSeconds: Long = 0
)

data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppGuardApp
    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pm = getApplication<AppGuardApp>().packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val limitMap = mutableMapOf<String, AppLimitEntity>()
            app.appLimitRepository.getAllLimits().first().forEach { limit ->
                limitMap[limit.packageName] = limit
            }

            val appList = installedApps
                .filter { it.packageName != getApplication<AppGuardApp>().packageName }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                .map { appInfo ->
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val limit = limitMap[appInfo.packageName]
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = name,
                        isLimited = limit != null && limit.isEnabled,
                        dailyLimitMinutes = limit?.dailyLimitMinutes ?: 30,
                        isUnlocked = limit?.isUnlocked ?: false,
                        todayUsedSeconds = limit?.todayUsedSeconds ?: 0
                    )
                }
                .sortedBy { it.appName }

            _uiState.update { it.copy(apps = appList, isLoading = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleLimit(appInfo: AppInfo) {
        viewModelScope.launch {
            if (appInfo.isLimited) {
                app.appLimitRepository.deleteLimit(appInfo.packageName)
            } else {
                app.appLimitRepository.upsertLimit(
                    AppLimitEntity(
                        packageName = appInfo.packageName,
                        appName = appInfo.appName,
                        dailyLimitMinutes = appInfo.dailyLimitMinutes
                    )
                )
            }
            loadApps()
        }
    }

    fun updateLimitMinutes(packageName: String, minutes: Int) {
        viewModelScope.launch {
            app.appLimitRepository.setLimitMinutes(packageName, minutes)
            loadApps()
        }
    }

    fun unlockApp(packageName: String) {
        viewModelScope.launch {
            app.appLimitRepository.unlockApp(packageName)
            app.pointsManager.penalizeUnlock()
            loadApps()
        }
    }
}
```

- [ ] **Step 2: Create AppsScreen.kt**

```kotlin
package com.example.appguard.ui.screens.apps

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: AppsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }

    val filteredApps = uiState.apps.filter {
        uiState.searchQuery.isEmpty() ||
        it.appName.contains(uiState.searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("搜索应用") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically()
                    ) {
                        AppListItem(
                            app = app,
                            onToggleLimit = { viewModel.toggleLimit(app) },
                            onChangeTime = {
                                selectedApp = app
                                showTimePicker = true
                            },
                            onUnlock = {
                                selectedApp = app
                                showUnlockDialog = true
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // 时长选择弹窗
    if (showTimePicker && selectedApp != null) {
        TimePickerDialog(
            currentMinutes = selectedApp!!.dailyLimitMinutes,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                viewModel.updateLimitMinutes(selectedApp!!.packageName, minutes)
                showTimePicker = false
            }
        )
    }

    // 解除限制确认弹窗
    if (showUnlockDialog && selectedApp != null) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("解除限制") },
            text = { Text("将扣除 20 积分，为「${selectedApp!!.appName}」追加 30 分钟使用时长。今天仅可解除一次，确认继续？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unlockApp(selectedApp!!.packageName)
                    showUnlockDialog = false
                }) {
                    Text("确认解除 (-20分)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    onToggleLimit: () -> Unit,
    onChangeTime: () -> Unit,
    onUnlock: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = app.isLimited,
                    onCheckedChange = { onToggleLimit() }
                )
            }

            if (app.isLimited) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onChangeTime) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${app.dailyLimitMinutes}分钟/天")
                    }

                    if (app.isUnlocked) {
                        Text(
                            text = "已解除",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        TextButton(onClick = onUnlock) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("解除限制")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(15, 30, 45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每日时长") },
        text = {
            Column {
                options.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(minutes) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMinutes == minutes,
                            onClick = { onConfirm(minutes) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${minutes} 分钟",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
```

---

### Task 12: SettingsScreen & Navigation

**Files:**
- Create: `app/src/main/kotlin/com/example/appguard/ui/screens/settings/SettingsScreen.kt`
- Create: `app/src/main/kotlin/com/example/appguard/ui/navigation/NavGraph.kt`
- Create: `app/src/main/kotlin/com/example/appguard/MainActivity.kt`

- [ ] **Step 1: Create SettingsScreen.kt**

```kotlin
package com.example.appguard.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // 无障碍服务入口
                ListItem(
                    headlineContent = { Text("无障碍服务") },
                    supportingContent = { Text("开启后AppGuard才能监控应用使用") },
                    leadingContent = {
                        Icon(Icons.Default.Accessibility, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 通知权限
                ListItem(
                    headlineContent = { Text("通知权限") },
                    supportingContent = { Text("允许AppGuard发送时长提醒和积分通知") },
                    leadingContent = {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("关于 AppGuard") },
                    supportingContent = { Text("版本 1.0.0 · 帮助您管理手机使用时间") },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create NavGraph.kt**

```kotlin
package com.example.appguard.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.appguard.ui.screens.apps.AppsScreen
import com.example.appguard.ui.screens.home.HomeScreen
import com.example.appguard.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Apps : Screen("apps", "管理", Icons.Default.Apps)
    data object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Apps, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(initialAlpha = 0.3f) + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut(targetAlpha = 0.3f) + slideOutHorizontally { -it / 4 } },
            popEnterTransition = { fadeIn(initialAlpha = 0.3f) + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut(targetAlpha = 0.3f) + slideOutHorizontally { it / 4 } }
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 3: Create MainActivity.kt**

```kotlin
package com.example.appguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appguard.ui.navigation.AppNavGraph
import com.example.appguard.ui.theme.AppGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGuardTheme {
                AppNavGraph()
            }
        }
    }
}
```

---

### Task 13: Resource Files

**Files:**
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

- [ ] **Step 1: Create themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AppGuard" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">#007ACC</item>
        <item name="android:navigationBarColor">#FFFFFF</item>
    </style>
</resources>
```

- [ ] **Step 2: Create ic_launcher_foreground.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#007ACC"
        android:pathData="M54,54m-40,0a40,40 0,1 1,80 0a40,40 0,1 1,-80 0" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M38,44L38,66L46,66L46,56L62,56L62,66L70,66L70,44L38,44ZM54,52m-4,0a4,4 0,1 1,8 0a4,4 0,1 1,-8 0" />
</vector>
```

- [ ] **Step 3: Create ic_launcher.xml (adaptive icon)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/blue_light" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

---

### Task 14: Daily Reward Check & Cross-Day Reset

- [ ] **Step 1: Update AppGuardAccessibilityService.kt - add daily reward logic**

In `AppGuardAccessibilityService.kt`, update the `checkNtpAndDate` function and add a daily reward check. The existing `checkNtpAndDate` function already handles cross-day reset. We need to add the reward logic: when a new day starts, check if all enabled apps were NOT exceeded the previous day, and if so, reward points.

Modify the `checkNtpAndDate` method in `AppGuardAccessibilityService.kt`:

Replace the existing `checkNtpAndDate` function with:

```kotlin
private suspend fun checkNtpAndDate() {
    val ntpTime = NtpTimeService.getNtpTime()
    ntpAvailable = ntpTime != null
    if (ntpTime != null) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Shanghai")
        val date = sdf.format(java.util.Date(ntpTime))
        if (date != currentDate && currentDate.isNotEmpty()) {
            // 跨天：检查昨日是否所有App都未超时
            checkAndRewardDaily()
            repository.resetDailyIfNeeded(date)
        }
        if (date != currentDate) {
            currentDate = date
        }
    }
}

private suspend fun checkAndRewardDaily() {
    val limits = repository.getAllLimits()
    // 由于getAllLimits返回Flow，需要调整为一次性查询
    val dao = AppDatabase.getInstance(this).appLimitDao()
    // 直接使用dao查询
}
```

Wait, the repository uses Flow. Let me fix the approach. The repository needs a suspend function to get all limits as a list. Let me add that.

Actually, let me update the AppLimitRepository to add a `getAllLimitsList()` suspend function.

- [ ] **Step 2: Update AppLimitRepository.kt - add sync query method**

Add to `AppLimitRepository`:

```kotlin
suspend fun getAllLimitsList(): List<AppLimitEntity> {
    // Need a DAO method that returns List directly
    // We'll add a suspend function to the DAO
}
```

Actually, let me add the DAO method and update the repository properly.

In `AppLimitDao.kt`, add:

```kotlin
@Query("SELECT * FROM app_limits")
suspend fun getAllLimitsList(): List<AppLimitEntity>
```

Then in `AppLimitRepository.kt`, add:

```kotlin
suspend fun getAllLimitsList(): List<AppLimitEntity> = dao.getAllLimitsList()
```

Then update the `checkNtpAndDate` in `AppGuardAccessibilityService.kt`:

```kotlin
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
```

---

### Task 15: Git Init & GitHub Push

**Files:**
- Create: `.gitignore`

- [ ] **Step 1: Create .gitignore**

```
*.iml
.gradle
/local.properties
/.idea
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
/app/build
/app/release
```

- [ ] **Step 2: Initialize git and commit**

```bash
cd /workspace
git init
git add .
git commit -m "feat: initial AppGuard - app usage manager with time limits and points system"
```

- [ ] **Step 3: Push to GitHub** (requires user to provide repo URL)

```bash
git remote add origin <github-repo-url>
git branch -M main
git push -u origin main
```

---

## Verification Checklist

After all tasks are complete, verify:
1. `./gradlew assembleRelease` builds successfully
2. APK is generated at `app/build/outputs/apk/release/app-release.apk`
3. R8 obfuscation is active (verify proguard mapping file exists)
4. `debuggable=false` in release APK
5. All Compose UI screens render without errors
6. AccessibilityService is properly declared in manifest