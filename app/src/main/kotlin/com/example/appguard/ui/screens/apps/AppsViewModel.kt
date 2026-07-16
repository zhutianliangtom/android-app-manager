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
            app.appLimitRepository.getAllLimitsList().forEach { limit ->
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