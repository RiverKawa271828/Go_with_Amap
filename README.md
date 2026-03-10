<p align="center">
<img src="./docs/images/LOGO.png" height="80"/>
</p>

<div align="center">

[![license](https://img.shields.io/github/license/RiverKawa271828/Go_with_Amap)](https://github.com/RiverKawa271828/Go_with_Amap/blob/master/LICENSE)

</div>

<div align="center">
Go with Amap - 基于高德地图的 Android 虚拟定位工具
</div>

## 项目声明

**Go with Amap** 是一个基于 Android 调试 API + 高德地图 SDK 实现的安卓虚拟定位工具。

- **作者**: River
- **项目地址**: https://github.com/RiverKawa271828/Go_with_Amap
- **开源协议**: GPL-3.0-only

### 关于本项目

本项目是基于 **[影梭 (GoGoGo)](https://github.com/ZCShou/GoGoGo)** by ZCShou 的**二次开发项目**，感谢原作者的开源贡献！

**重要说明**：
- 本项目完全基于**个人需求**进行更改和增强
- 与原项目相比进行了大量架构级改动（地图 SDK 更换、系统要求提升、新增功能等）
- 由于改动幅度巨大，本项目作为**独立衍生项目**维护，不与原仓库保持同步
- 所有更改均遵循 GPL-3.0 开源协议，完整代码开源

**使用声明**：
- **本项目仅发布源码，不提供预构建的 APK 安装包**
- **本项目不接受 Issues 和 Pull Requests，仅为遵守 GPL 协议开源**
- 如有需求，请自行下载源码修改并构建
- 使用本源码产生的任何问题由使用者自行承担

如果您需要原项目的稳定版本，请访问 [GoGoGo 官方仓库](https://github.com/ZCShou/GoGoGo)。

## 主要修改内容

与原项目相比，本项目进行了以下修改和增强：

### 1. 地图 SDK 更换
- **原项目**: 百度地图 SDK
- **本项目**: 高德地图 SDK (Amap 3D SDK 9.8.2)
- 坐标系从 BD09 改为 GCJ02

### 2. 系统要求提升
- **原项目**: Android 8.0+ (API 26)
- **本项目**: Android 12+ (API 31)
- 支持 Android 14 (API 34)

### 3. 新增功能

#### ROOT 模式支持 (ROOT Mode)
- 新增 ROOT 模式选项，支持在已 ROOT 设备上使用
- 绕过部分应用的模拟位置检测
- 位置数据持久化，服务重启后自动恢复
- 自动检测 ROOT 状态并提示用户

#### 多点定位 (Multi-Point Positioning)
- 支持从历史记录或收藏中选择多个坐标点
- 以每个点为中心绘制圆形区域（可设置半径，单位：千米）
- 使用几何算法自动计算多个圆的相交区域
- 支持三边测量计算最佳目标点
- 可视化显示圆和相交区域

#### 收藏功能 (Favorites)
- 长按地图位置可添加到收藏
- 收藏列表管理（查看、删除）
- 支持从收藏快速定位到地图

#### 增强的悬浮窗摇杆
- 内置迷你地图（悬浮窗内可直接搜索和选点）
- 内置历史记录列表
- 支持方向键和摇杆两种控制方式切换

### 4. 架构变更
- **架构**: 仅支持 arm64-v8a（原项目支持更多架构）
- **包名**: `com.river.gowithamap`（原项目: `com.zcshou.gogogo`）
- **应用名**: Go with Amap（原项目: 影梭）

### 5. 依赖升级
- Gradle: 9.1.0
- Android SDK: 34
- 高德地图 SDK: 9.8.2
- OkHttp: 4.12.0
- 新增 XLog、Markwon 等库

### 6. UI/UX 改进
- 全新的导航抽屉菜单
- 添加欢迎页和用户协议
- 改进的设置界面
- 新的图标和主题色

## 简介

Go with Amap 是一个基于 Android 调试 API + 高德地图 SDK 实现的安卓虚拟定位工具，支持摇杆控制移动。无需 ROOT 权限即可修改当前位置并模拟移动。

本项目仅供学习 Android 开发和地图 SDK 使用。

## 功能特性

### 核心功能
1. **虚拟定位** - 点击地图或输入坐标选择目标位置
2. **摇杆控制** - 悬浮窗摇杆，支持步行/跑步/自行车三种速度
3. **位置搜索** - 集成高德地图 POI 搜索
4. **历史记录** - 自动保存定位历史，支持快速回放

### 新增功能
5. **ROOT 模式** - 支持 ROOT 设备，增强兼容性
6. **多点定位** - 多圆相交计算目标位置
7. **收藏管理** - 位置收藏和快速访问
8. **悬浮窗地图** - 无需返回主界面即可切换位置
9. **地图切换** - 普通地图/卫星地图

## 技术栈

| 项目 | 版本 |
|------|------|
| 开发语言 | Java |
| 最低 SDK | Android 12 (API 31) |
| 目标 SDK | Android 14 (API 34) |
| 地图 SDK | 高德地图 3D SDK 9.8.2 |
| 搜索 SDK | 高德地图搜索 SDK 9.7.0 |
| 构建工具 | Gradle 9.3.1 |
| 支持架构 | arm64-v8a |
| ROOT 支持 | 可选（增强模式）|

## 核心依赖

```gradle
// AndroidX
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
implementation 'com.google.android.material:material:1.11.0'

// 高德地图
implementation 'com.amap.api:3dmap:9.8.2'
implementation 'com.amap.api:search:9.7.0'

// 网络与工具
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.elvishew:xlog:1.11.1'
implementation 'io.noties.markwon:core:4.6.2'

// UI
implementation 'androidx.cardview:cardview:1.0.0'
```

## 项目结构

```
app/src/main/java/com/river/gowithamap/
├── MainActivity.java              # 主界面（新增多点定位、收藏功能）
├── WelcomeActivity.java           # 欢迎页（新增）
├── SettingsActivity.java          # 设置页
├── HistoryActivity.java           # 历史记录页
├── FavoritesActivity.java         # 收藏页（新增）
├── GoApplication.java             # 应用入口
├── BaseActivity.java              # 基类
├── service/
│   ├── ServiceGo.java             # 定位模拟服务（新增 ROOT 模式）
│   └── RootLocationProvider.java  # ROOT 模式位置提供者（新增）
├── joystick/
│   ├── JoyStick.java              # 悬浮窗摇杆（增强版）
│   ├── RockerView.java            # 摇杆视图
│   └── ButtonView.java            # 方向键视图
├── database/
│   ├── DataBaseHistoryLocation.java   # 定位历史数据库
│   ├── DataBaseHistorySearch.java     # 搜索历史数据库
│   └── DataBaseFavorites.java         # 收藏数据库（新增）
└── utils/
    ├── GoUtils.java               # 通用工具
    ├── MapUtils.java              # 地图坐标转换工具（GCJ02/WGS84）
    ├── RootUtils.java             # ROOT 检测与命令执行工具（新增）
    └── ShareUtils.java            # 分享工具
```

## 核心功能实现

### 位置模拟原理

使用 Android 的 `LocationManager.addTestProvider()` API：

```java
// 添加 GPS 测试提供者
mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, ...);
mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

// 设置模拟位置
Location loc = new Location(LocationManager.GPS_PROVIDER);
loc.setLatitude(latitude);
loc.setLongitude(longitude);
mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
```

### 坐标系转换

支持 WGS84 和 GCJ02 之间的相互转换：

```java
// GCJ02 转 WGS84
double[] wgs84 = MapUtils.gcj02ToWgs84(gcjLon, gcjLat);

// WGS84 转 GCJ02  
double[] gcj02 = MapUtils.wgs84ToGcj02(wgsLon, wgsLat);
```

### 多点定位算法（新增）

1. **两圆相交计算**：使用平面几何计算两个圆的相交点
2. **最优目标点**：在所有相交点中找到使得到各圆周距离最小的点
3. **梯度下降优化**：使用迭代优化算法精确调整目标点位置

```java
// 计算两圆相交点
List<LatLng> intersections = calculateTwoCircleIntersections(c1, r1, c2, r2);

// 找到最佳目标点
LatLng bestPoint = findBestIntersectionPoint(allIntersections);
```

## 使用要求

### 必要条件

1. **Android 12+** (API 31)
2. **开启开发者选项**
3. **设置模拟位置应用**：在开发者选项中选择本应用作为模拟位置应用（普通模式）
4. **悬浮窗权限**：用于显示摇杆控制

### ROOT 模式（可选）

如需使用 ROOT 模式增强兼容性：
1. 设备需要已 ROOT
2. 在设置中启用"ROOT 模式"
3. 授予 ROOT 权限（会弹出 Superuser 授权对话框）
4. 重启应用以应用更改

**注意**：ROOT 模式可绕过部分应用的模拟位置检测，但需要设备已 ROOT。

### 权限列表

- `ACCESS_FINE_LOCATION` - 精确位置
- `ACCESS_COARSE_LOCATION` - 粗略位置
- `ACCESS_BACKGROUND_LOCATION` - 后台位置（Android 10+）
- `SYSTEM_ALERT_WINDOW` - 悬浮窗
- `ACCESS_MOCK_LOCATION` - 模拟位置
- `FOREGROUND_SERVICE_LOCATION` - 前台服务位置

## 构建说明

### ⚠️ 配置高德地图 API Key（必需）

**编译前必须配置高德地图 API Key，否则应用无法正常运行！**

#### 快速开始

1. 复制示例配置文件：
```bash
cp local.properties.example local.properties
```

2. 编辑 `local.properties`，填入你的高德 API Key：
```properties
sdk.dir=/path/to/Android/Sdk
AMAP_API_KEY=你的高德地图API密钥
```

3. 高德 Key 申请步骤：
   - 前往 [高德开放平台](https://console.amap.com/dev/key/app)
   - 创建应用，添加「地图 SDK」「定位 SDK」「搜索 SDK」服务
   - 绑定包名 `com.river.gowithamap` 和你的调试签名 SHA1
   - 获取 Key 并填入 `local.properties`

#### 签名配置（可选）

- **调试构建**：自动使用 Android 默认调试签名，无需配置
- **发布构建**：如需使用自定义签名，配置 `KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`

#### 维护者本地开发

本项目维护者使用**外部密钥目录**方案：
- 公开仓库 (`Go_with_Amap`)：不包含任何敏感文件
- 本地开发目录 (`Go_with_Amap_with_key`)：包含签名密钥和 API Key

构建系统自动检测外部目录，存在时优先使用外部密钥。

### 构建 APK

```bash
# 清理并构建调试版本
./gradlew clean assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug
```

### 输出文件

- 调试版：`app/build/outputs/apk/debug/GoWithAmap_{version}_arm64-v8a_debug.apk`
- 发布版：`app/build/outputs/apk/release/GoWithAmap_{version}_arm64-v8a_release.apk`

## 使用步骤

### 普通模式

1. 安装 APK 并打开应用
2. 阅读并同意用户协议
3. 授予必要的权限（位置、悬浮窗等）
4. 在系统设置中开启开发者选项
5. 设置本应用为模拟位置应用：
   - 设置 → 系统 → 开发者选项 → 选择模拟位置信息应用
   - 选择 "Go with Amap"
6. 返回应用，在地图上点击选择目标位置
7. 点击启动按钮开始模拟定位
8. 显示悬浮窗摇杆后，可以控制移动

### ROOT 模式（可选增强）

1. 确保设备已 ROOT
2. 打开应用，进入"设置"
3. 开启"ROOT 模式"
4. 授权 ROOT 权限
5. 重启应用
6. 正常使用虚拟定位功能

## 多点定位使用说明

1. 点击导航菜单中的"多点定位"
2. 选择"历史"或"收藏"标签
3. 点击选择一个坐标点
4. 输入半径（单位：千米）
5. 点击"确定"添加圆
6. 重复步骤 3-5 添加至少 3 个圆
7. 选择"完成"计算相交区域
8. 系统会自动找到最佳目标点

## ROOT 模式使用说明

### 什么是 ROOT 模式

ROOT 模式是一种增强型的位置模拟方案，通过 ROOT 权限直接操作系统位置服务，相比普通模式具有以下优势：
- 位置更新更稳定
- 可绕过部分应用的模拟位置检测
- 服务重启后自动恢复上次位置

### 启用 ROOT 模式

1. **前提条件**
   - 设备已 ROOT（Magisk、SuperSU 等）
   - 已安装 Superuser 管理应用

2. **启用步骤**
   - 打开应用，进入"设置"
   - 找到"ROOT 模式"选项
   - 开启"启用 ROOT 模式"开关
   - 系统会弹出 ROOT 授权请求，点击"允许"
   - 阅读警告信息后点击"确定"
   - 重启应用以应用更改

3. **验证 ROOT 模式**
   - 查看设置中的"ROOT 状态"是否显示"已获取 ROOT 权限"
   - 启动虚拟定位后，位置图标会显示 ROOT 模式指示

### 注意事项

- ROOT 模式需要设备已 ROOT，未 ROOT 设备无法使用
- 部分设备可能需要额外的 SELinux 配置
- 使用 ROOT 模式产生的任何问题由用户自行承担
- 某些应用仍可能通过其他方式检测位置异常

## 注意事项

⚠️ **仅供学习 Android 开发和地图 SDK 使用**

1. 请勿用于任何违法用途
2. 请勿用于游戏作弊
3. 请勿用于侵犯他人隐私
4. 使用本软件产生的一切后果由用户自行承担

##  

本软件专为学习 Android 开发使用：
- 不会收集任何用户数据
- 所有数据均存储在本地
- 开发者不对使用本软件产生的任何后果负责

## 开源协议

GPL-3.0-only

## 致谢

### 原项目
- **影梭 (GoGoGo)** by [ZCShou](https://github.com/ZCShou)
- 项目地址: https://github.com/ZCShou/GoGoGo

### 使用的开源库
- 高德地图 SDK
- Android Jetpack
- OkHttp
- XLog
- Markwon

### 使用的资源
- **Maxwell the Cat** - 欢迎页GIF动画资源来自 [wilversings/maxwell](https://github.com/wilversings/maxwell) (KDE Plasma Widget)
  - Maxwell, the desktop cat, as a KDE Plasma Widget
  - 用于应用欢迎页加载动画展示

## 更新日志

### v2.17.7 (当前版本)
- 新增固定点和固定区域修改名称功能
- 固定圆圆心显示用户自定义名称
- 优化临时坐标和临时区域管理逻辑
- 修复固定点在设置临时点后消失的问题
- 修复固定点点击交互在应用刚打开时不响应的问题
- 修复固定点取消固定后图标样式未正确更新的问题
- 优化代码结构，减少重复代码
- 添加Maxwell猫GIF欢迎页动画

### v2.17.6
- 新增坐标点导入功能（支持文件和二维码）
- 增强导入功能健壮性，支持无type字段的坐标数据
- 优化欢迎页动画效果
- 修复区域操作相关问题

### v2.17.5
- 新增固定区域（圆）数据库持久化
- 临时区域和固定区域分离管理
- 优化区域交互逻辑
- 改进临时区域创建时间戳管理

### v2.17.4
- 新增 ROOT 模式支持（可选）
- 修复摇杆历史按钮闪退问题
- 修复服务重启后位置丢失问题
- 优化位置数据持久化存储
- 改进设置界面，添加 ROOT 状态检测
- 优化历史记录列表显示
- 更新 Gradle 至 9.3.1
- 更新依赖库版本

### v2.17.3
- 修复 Material Dialog 兼容性问题
- 优化摇杆历史记录显示

### v2.17.2
- 优化地图缓存管理
- 改进夜间模式切换

### v2.17.1
- 修复收藏功能相关问题
- 优化多点定位算法

### v2.17.0
- 更换地图 SDK：百度地图 → 高德地图
- 新增多点定位功能
- 新增收藏功能
- 新增悬浮窗地图
- 优化摇杆控制
- 适配 Android 14
- 最低系统要求提升至 Android 12

---

**作者**: River  
**项目地址**: https://github.com/RiverKawa271828/Go_with_Amap  
**仅供学习使用，否则后果自负！**
