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