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