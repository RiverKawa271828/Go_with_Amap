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

**Go with Amap** 是基于 Android 调试 API + 高德地图 SDK 的安卓虚拟定位工具，为 **[影梭 (GoGoGo)](https://github.com/ZCShou/GoGoGo)** 的二次开发版本。

- **作者**: River
- **项目地址**: https://github.com/RiverKawa271828/Go_with_Amap
- **开源协议**: GPL-3.0-only

**使用声明**:
- **本项目仅发布源码，不提供预构建的 APK 安装包**
- **本项目不接受 Issues 和 Pull Requests，仅为遵守 GPL 协议开源**
- 使用本源码产生的任何问题由使用者自行承担

## 主要特性

| 项目 | 原项目 (影梭) | 本项目 |
|------|--------------|--------|
| 地图 SDK | 百度地图 | 高德地图 (GCJ02) |
| 最低系统 | Android 8.0 | Android 12 |
| 架构支持 | 多架构 | 仅 arm64-v8a |

### 功能特性

- **虚拟定位** - 点击地图或输入坐标选择目标位置
- **摇杆控制** - 悬浮窗摇杆，支持步行/跑步/自行车三种速度
- **位置搜索** - 集成高德地图 POI 搜索
- **模拟记录** - 记录实际模拟过的位置
- **收藏管理** - 支持坐标和区域收藏
- **固定管理** - 管理地图固定点和区域
- **多点定位** - 多圆相交计算目标位置
- **ROOT 模式** - 支持 ROOT 设备
- **批量分享** - 支持多选后批量分享
- **数据备份** - ZIP 格式导出，支持加密

## 技术栈

| 项目 | 版本 |
|------|------|
| 开发语言 | Java |
| 最低 SDK | Android 12 (API 31) |
| 目标 SDK | Android 14 (API 34) |
| 地图 SDK | 高德地图 3D SDK 9.8.2 |
| 构建工具 | Gradle 9.3.1 |
| 支持架构 | arm64-v8a |

## 构建说明

### 配置高德地图 API Key

1. 复制配置文件：`cp local.properties.example local.properties`
2. 编辑 `local.properties`，填入高德 API Key
3. 前往 [高德开放平台](https://console.amap.com/dev/key/app) 申请 Key
4. 绑定包名 `com.river.gowithamap` 和调试签名 SHA1

### 构建命令

```bash
./gradlew assembleDebug   # 调试版本
./gradlew assembleRelease # 发布版本
```

## 使用要求

1. **Android 12+** (API 31)
2. 开启开发者选项
3. 设置本应用为模拟位置应用
4. 授予悬浮窗权限（摇杆功能）

## 更新日志

### v2.20.0
- 优化 InfoWindow 宽度，同时展示 GCJ02 和 WGS84 两套坐标
- 修复模拟记录对话框 SearchView 类型错误导致的闪退
- 优化导出导入功能，支持从任意位置选择备份文件
- 将"历史记录"统一改为"模拟记录"
- 其他细节优化

### v2.19.3
- 重构历史记录为模拟记录
- 多点定位"历史"改为"模拟"
- 导入分享对话框标题修正
- 夜间模式颜色优化

[查看更多](./CHANGELOG.md)

## 开源协议

GPL-3.0-only

## 致谢

- **[影梭 (GoGoGo)](https://github.com/ZCShou/GoGoGo)** by ZCShou
- 高德地图 SDK

---

**仅供学习 Android 开发和地图 SDK 使用**