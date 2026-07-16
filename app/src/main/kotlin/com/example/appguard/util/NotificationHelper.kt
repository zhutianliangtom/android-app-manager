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