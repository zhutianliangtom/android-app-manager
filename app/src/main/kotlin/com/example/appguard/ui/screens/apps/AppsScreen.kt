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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
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
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
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