# App Usage Manager - 设计文档

## 概述

一款 Android 手机应用使用管理 App，帮助用户自定义 App 每日使用时长，超时强制退出，并通过积分机制激励用户自律。

## 技术约束

- 兼容 Android 8 ~ 16
- 纯客户端，Kotlin 原生
- AGP 8.13.2，Kotlin 2.3.20
- R8 混淆开启，关闭 debug 调试
- 蓝白主题，简约 UI，过渡动画

## 架构

MVVM + Repository 模式，三层结构：

```
UI Layer (Jetpack Compose + ViewModel)
    ↓
Domain Layer (UseCase / Repository)
    ↓
Data Layer (Room DB + SharedPrefs)
```

## 核心模块

### 1. TimeService（时间服务）
- 通过 NTP 协议请求 `ntp.aliyun.com`（UDP 端口 123）
- 使用 OkHttp 或原生 Socket 实现 NTP 客户端
- 无网络时返回 null，调用方据此阻断所有目标App
- 绝不调用 `System.currentTimeMillis()` 判定时间

### 2. AccessibilityService（无障碍服务）
- 监听窗口切换事件（TYPE_WINDOW_STATE_CHANGED），检测目标App切换到前台
- 累计前台使用时长（基于 NTP 时间戳差值）
- 超时后执行 `performGlobalAction(GLOBAL_ACTION_HOME)` 强制退回桌面
- 发送通知："xxx应用已到时长"
- 跨天自动重置当日计时

### 3. AppUsageRepository（应用使用数据仓库）
- Room 数据库，表 `app_limits`：
  - package_name (TEXT PK)
  - app_name (TEXT)
  - daily_limit_minutes (INT)
  - today_used_seconds (INT)
  - today_date (TEXT, yyyy-MM-dd)
  - is_unlocked (INT, 0/1)
  - is_enabled (INT, 0/1)

### 4. PointsRepository（积分仓库）
- SharedPreferences 存储
- 字段：total_points (INT)
- 加分：当日所有目标App均未超时 → +10 分
- 扣分：用户解除限制 → -20 分
- 允许负数

### 5. 解除限制逻辑
- 用户可在 App 内点击解除按钮，扣 20 分
- 解除后追加 30 分钟时长
- 每天每个 App 只能解除一次
- 通知鼓励语：如"再坚持一下，你可以的！"、"自律给你自由，加油！"

### 6. 积分奖励逻辑
- 每日结束时（或次日首次打开时）结算
- 当日所有启用的目标App均未触发超时 → +10 分
- 通知鼓励语

## UI 页面

### 首页
- 顶部：今日积分数字（大号字体，带动画）
- 中部：鼓励语卡片（随机展示）
- 底部：今日已用时长概览（各目标App环形进度条或列表）

### App管理页
- 通过 `PackageManager.getInstalledApplications()` 获取真实已安装App列表
- 过滤系统应用（白名单：启动器、设置等保留）
- 每项显示：图标 + 名称 + 时长设置开关 + 时长选择器
- 支持搜索过滤

### 设置页
- 默认时长预设（15/30/60/90/120分钟）
- 通知开关
- 关于信息

## 技术选型

| 项 | 选择 |
|------|------|
| UI 框架 | Jetpack Compose BOM |
| 数据库 | Room |
| 网络 | java.net.Socket (NTP/UDP) |
| 异步 | Kotlin Coroutines + Flow |
| 导航 | Compose Navigation |
| 序列化 | kotlinx.serialization（已内置 Kotlin 2.x） |
| DI | 手动单例，Application 类初始化 |

## 构建配置

- AGP 8.13.2
- Kotlin 2.3.20
- minSdk 26 (Android 8)
- targetSdk 35 (Android 15)
- compileSdk 35
- R8 混淆：release 构建开启 minifyEnabled，proguard-rules.pro 配置
- debug 调试：release 构建 debuggable false