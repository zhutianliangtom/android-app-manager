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