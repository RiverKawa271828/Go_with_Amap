<p align="center">
<img src="./docs/images/LOGO.png" height="80"/>
</p>

<div align="center">

[![license](https://img.shields.io/github/license/RiverKawa271828/Go_with_Amap)](https://github.com/RiverKawa271828/Go_with_Amap/blob/master/LICENSE)

</div>

<div align="center">
Go with Amap - Android Virtual Location Tool based on Amap SDK
</div>

## Project Statement

**Go with Amap** is an Android virtual location tool based on Android Debug API + Amap SDK.

- **Author**: River
- **Project URL**: https://github.com/RiverKawa271828/Go_with_Amap
- **License**: GPL-3.0-only

This project is inspired by **[GoGoGo (影梭)](https://github.com/ZCShou/GoGoGo)** by ZCShou. Special thanks to the original author for the open source contribution!

## Main Modifications

Compared to the original project, this fork includes the following changes and enhancements:

### 1. Map SDK Change
- **Original**: Baidu Map SDK
- **This Project**: Amap (Gaode) Map SDK (3D SDK 9.8.2)
- Coordinate system changed from BD09 to GCJ02

### 2. System Requirements
- **Original**: Android 8.0+ (API 26)
- **This Project**: Android 12+ (API 31)
- Supports Android 14 (API 34)

### 3. New Features

#### Multi-Point Positioning
- Select multiple coordinate points from history or favorites
- Draw circular areas around each point (radius in kilometers)
- Automatic intersection area calculation using geometric algorithms
- Support trilateration for finding optimal target point
- Visual display of circles and intersection areas

#### Favorites Feature
- Long-press map to add locations to favorites
- Favorites list management (view, delete)
- Quick positioning from favorites

#### Enhanced Floating Joystick
- Built-in mini-map (search and select locations without returning to main screen)
- Built-in history record list
- Support switching between joystick and directional button modes

### 4. Architecture Changes
- **Architecture**: arm64-v8a only (original supported more architectures)
- **Package Name**: `com.river.gowithamap` (original: `com.zcshou.gogogo`)
- **App Name**: Go with Amap (original: 影梭 / GoGoGo)

### 5. Dependency Upgrades
- Gradle: 9.1.0
- Android SDK: 34
- Amap SDK: 9.8.2
- OkHttp: 4.12.0
- Added XLog, Markwon libraries

### 6. UI/UX Improvements
- New navigation drawer menu
- Added welcome page and user agreement
- Improved settings interface
- New icons and theme colors

## Introduction

Go with Amap is an Android virtual location tool based on Android Debug API + Amap SDK, supporting joystick-controlled movement simulation without requiring ROOT access.

This project is for educational purposes only, for learning Android development and map SDK usage.

## Features

### Core Features
1. **Virtual Location** - Select target location by clicking on map or entering coordinates
2. **Joystick Control** - Floating window joystick with walk/run/bike speed modes
3. **Location Search** - Integrated Amap POI search
4. **History Records** - Automatic location history saving with quick replay

### New Features
5. **Multi-Point Positioning** - Multi-circle intersection calculation for target location
6. **Favorites Management** - Location bookmarking and quick access
7. **Floating Window Map** - Switch locations without returning to main screen
8. **Map Types** - Normal/Satellite map switch

## Tech Stack

| Item | Version |
|------|---------|
| Language | Java |
| Min SDK | Android 12 (API 31) |
| Target SDK | Android 14 (API 34) |
| Map SDK | Amap 3D SDK 9.8.2 |
| Search SDK | Amap Search SDK 9.7.0 |
| Build Tool | Gradle 9.1.0 |
| Architecture | arm64-v8a |

## Core Dependencies

```gradle
// AndroidX
implementation 'androidx.appcompat:appcompat:1.5.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1'
implementation 'com.google.android.material:material:1.7.0'

// Amap Maps
implementation 'com.amap.api:3dmap:9.8.2'
implementation 'com.amap.api:search:9.7.0'

// Network & Utilities
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.elvishew:xlog:1.11.1'
implementation 'io.noties.markwon:core:4.6.2'
```

## Project Structure

```
app/src/main/java/com/river/gowithamap/
├── MainActivity.java              # Main activity (added multi-point & favorites)
├── WelcomeActivity.java           # Welcome page (new)
├── SettingsActivity.java          # Settings page
├── HistoryActivity.java           # History page
├── FavoritesActivity.java         # Favorites page (new)
├── GoApplication.java             # Application entry
├── BaseActivity.java              # Base activity class
├── service/
│   └── ServiceGo.java             # Location simulation service
├── joystick/
│   ├── JoyStick.java              # Floating joystick (enhanced)
│   ├── RockerView.java            # Rocker view
│   └── ButtonView.java            # Directional buttons view
├── database/
│   ├── DataBaseHistoryLocation.java   # Location history database
│   ├── DataBaseHistorySearch.java     # Search history database
│   └── DataBaseFavorites.java         # Favorites database (new)
└── utils/
    ├── GoUtils.java               # General utilities
    ├── MapUtils.java              # Map coordinate conversion (GCJ02/WGS84)
    └── ShareUtils.java            # Share utilities
```

## Core Implementation

### Location Simulation Principle

Uses Android's `LocationManager.addTestProvider()` API:

```java
// Add GPS test provider
mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, ...);
mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

// Set mock location
Location loc = new Location(LocationManager.GPS_PROVIDER);
loc.setLatitude(latitude);
loc.setLongitude(longitude);
mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
```

### Coordinate Conversion

Supports conversion between WGS84 and GCJ02:

```java
// GCJ02 to WGS84
double[] wgs84 = MapUtils.gcj02ToWgs84(gcjLon, gcjLat);

// WGS84 to GCJ02  
double[] gcj02 = MapUtils.wgs84ToGcj02(wgsLon, wgsLat);
```

### Multi-Point Positioning Algorithm (New)

1. **Circle Intersection**: Calculate intersection points using planar geometry
2. **Optimal Target**: Find point with minimum distance to all circle circumferences
3. **Gradient Descent**: Iterative optimization for precise adjustment

```java
// Calculate two circle intersections
List<LatLng> intersections = calculateTwoCircleIntersections(c1, r1, c2, r2);

// Find best target point
LatLng bestPoint = findBestIntersectionPoint(allIntersections);
```

## Requirements

### Prerequisites

1. **Android 12+** (API 31)
2. **Developer Options Enabled**
3. **Set Mock Location App**: Select this app as mock location app in developer options
4. **Overlay Permission**: For displaying joystick

### Permissions

- `ACCESS_FINE_LOCATION` - Precise location
- `ACCESS_COARSE_LOCATION` - Approximate location
- `ACCESS_BACKGROUND_LOCATION` - Background location (Android 10+)
- `SYSTEM_ALERT_WINDOW` - Overlay window
- `ACCESS_MOCK_LOCATION` - Mock location
- `FOREGROUND_SERVICE_LOCATION` - Foreground service location

## Build Instructions

### Configure Amap API Key

Add to `local.properties`:

```properties
AMAP_API_KEY=your_amap_api_key
```

### Build APK

```bash
# Debug version
./gradlew assembleDebug

# Release version
./gradlew assembleRelease
```

### Output Files

- Debug: `GoWithAmap_{version}_arm64-v8a_debug.apk`
- Release: `GoWithAmap_{version}_arm64-v8a_release.apk`

## Usage Steps

1. Install APK and open the app
2. Read and accept the user agreement
3. Grant necessary permissions (location, overlay, etc.)
4. Enable developer options in system settings
5. Set this app as mock location app
6. Click on map to select target location
7. Click start button to begin location simulation
8. Use joystick to control movement when displayed

## Multi-Point Positioning Guide

1. Click "Multi-Point" in navigation menu
2. Select "History" or "Favorites" tab
3. Tap to select a coordinate point
4. Enter radius (in kilometers)
5. Click "Confirm" to add circle
6. Repeat steps 3-5 to add at least 3 circles
7. Select "Finish" to calculate intersection
8. System will automatically find the optimal target point

## Notes

⚠️ **For educational purposes only**

1. Do not use for illegal purposes
2. Do not use for game cheating
3. Do not use to invade others' privacy
4. Users bear all consequences of using this software

## Disclaimer

This software is for learning Android development:
- No user data collection
- All data stored locally
- Developer not responsible for any consequences

## License

GPL-3.0-only

## Credits

### Original Project
- **GoGoGo (影梭)** by [ZCShou](https://github.com/ZCShou)
- Project: https://github.com/ZCShou/GoGoGo

### Open Source Libraries
- Amap (Gaode) Map SDK
- Android Jetpack
- OkHttp
- XLog
- Markwon

## Changelog

### v2.20.1 (Current)
- Removed ineffective map cache feature
- Optimized search dialog style
- Added disclaimer to scrolling tips
- InfoWindow now uses semi-transparent background

### v2.20.0
- Optimized InfoWindow to display both GCJ02 and WGS84 coordinates
- Fixed crash in simulation records dialog
- Enhanced import/export functionality

### v0.1.2
- Changed Map SDK: Baidu Map → Amap
- Added multi-point positioning feature
- Added favorites feature
- Added floating window map
- Optimized joystick control
- Android 14 support
- Minimum system requirement raised to Android 12

---

**Author**: River  
**Project URL**: https://github.com/RiverKawa271828/Go_with_Amap  
**For educational use only. Use at your own risk!**
