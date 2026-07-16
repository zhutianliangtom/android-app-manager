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