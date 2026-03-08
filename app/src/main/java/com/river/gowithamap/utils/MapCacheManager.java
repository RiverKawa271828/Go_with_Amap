package com.river.gowithamap.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.webkit.WebStorage;
import android.webkit.WebView;

import androidx.preference.PreferenceManager;

import com.amap.api.maps.MapsInitializer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 地图缓存管理器
 * 用于管理高德地图的缓存设置和缓存文件
 */
public class MapCacheManager {

    private static final String KEY_MAP_CACHE_ENABLED = "setting_map_cache";
    private static final String KEY_CACHED_AREAS = "cached_areas_v2";
    private static final String KEY_CACHE_STATS = "cache_stats";

    /**
     * 缓存区域信息类
     */
    public static class CacheAreaInfo {
        public String name;
        public long lastAccessTime;
        public int accessCount;

        public CacheAreaInfo(String name, long lastAccessTime, int accessCount) {
            this.name = name;
            this.lastAccessTime = lastAccessTime;
            this.accessCount = accessCount;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                json.put("lastAccess", lastAccessTime);
                json.put("count", accessCount);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        public static CacheAreaInfo fromJson(JSONObject json) {
            try {
                return new CacheAreaInfo(
                    json.getString("name"),
                    json.getLong("lastAccess"),
                    json.getInt("count")
                );
            } catch (JSONException e) {
                return null;
            }
        }
    }

    /**
     * 检查地图缓存是否启用
     * @param context 上下文
     * @return true 如果缓存已启用（默认启用）
     */
    public static boolean isMapCacheEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_MAP_CACHE_ENABLED, true);
    }

    /**
     * 设置地图缓存开关
     * @param context 上下文
     * @param enabled 是否启用缓存
     */
    public static void setMapCacheEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(KEY_MAP_CACHE_ENABLED, enabled).apply();

        // 应用缓存设置
        applyMapCacheSetting(enabled);
    }

    /**
     * 应用地图缓存设置
     * @param enabled 是否启用缓存
     */
    private static void applyMapCacheSetting(boolean enabled) {
        try {
            if (enabled) {
                MapsInitializer.setNetWorkEnable(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取应用可访问的缓存大小
     * 注意：高德地图SDK的瓦片缓存存储在系统私有目录，普通应用无法直接访问
     * @param context 上下文
     * @return 缓存大小（字节）
     */
    public static long getMapCacheSize(Context context) {
        long totalSize = 0;

        // 1. 获取应用缓存目录大小（网络缓存、WebView缓存等）
        totalSize += getFolderSize(context.getCacheDir());

        // 2. 获取外部缓存大小
        if (context.getExternalCacheDir() != null) {
            totalSize += getFolderSize(context.getExternalCacheDir());
        }

        // 3. 获取应用私有目录的amap相关缓存
        File amapCache = new File(context.getFilesDir(), "amap");
        if (amapCache.exists()) {
            totalSize += getFolderSize(amapCache);
        }

        // 4. 获取数据库文件大小
        File dbFile = context.getDatabasePath("webview.db");
        if (dbFile != null && dbFile.exists()) {
            totalSize += dbFile.length();
        }
        File dbCacheFile = context.getDatabasePath("webviewCache.db");
        if (dbCacheFile != null && dbCacheFile.exists()) {
            totalSize += dbCacheFile.length();
        }

        return totalSize;
    }

    /**
     * 获取缓存统计信息
     * @param context 上下文
     * @return 包含统计信息的字符串
     */
    public static String getCacheStats(Context context) {
        long appCacheSize = getMapCacheSize(context);
        int areaCount = getCachedAreas(context).size();

        StringBuilder stats = new StringBuilder();
        stats.append("应用缓存: ").append(formatCacheSize(appCacheSize));

        // 高德SDK缓存通常远大于应用缓存，这里给一个预估
        // 根据已记录的区域数量估算瓦片缓存
        if (areaCount > 0) {
            // 假设每个城市平均缓存约50MB（取决于浏览的详细程度）
            long estimatedTileCache = areaCount * 50L * 1024 * 1024;
            stats.append("\n预估地图瓦片缓存: ").append(formatCacheSize(estimatedTileCache));
            stats.append("\n（基于 ").append(areaCount).append(" 个浏览过的区域估算）");
        } else {
            stats.append("\n地图瓦片缓存: 存储在系统目录，无法直接计算");
        }

        return stats.toString();
    }

    /**
     * 格式化缓存大小为可读字符串
     * @param sizeBytes 字节数
     * @return 格式化后的字符串
     */
    public static String formatCacheSize(long sizeBytes) {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 添加缓存区域记录
     * 当用户浏览某个区域时调用此方法记录
     * @param context 上下文
     * @param cityName 城市名称
     */
    public static void addCachedArea(Context context, String cityName) {
        if (cityName == null || cityName.isEmpty()) return;

        // 标准化城市名称
        cityName = normalizeCityName(cityName);
        if (cityName.isEmpty()) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String areasJson = prefs.getString(KEY_CACHED_AREAS, "[]");

        try {
            JSONArray areas = new JSONArray(areasJson);
            boolean found = false;
            long currentTime = System.currentTimeMillis();

            // 查找是否已存在
            for (int i = 0; i < areas.length(); i++) {
                JSONObject obj = areas.getJSONObject(i);
                if (cityName.equals(obj.optString("name"))) {
                    // 更新访问时间和次数
                    obj.put("lastAccess", currentTime);
                    obj.put("count", obj.optInt("count", 1) + 1);
                    found = true;
                    break;
                }
            }

            // 如果不存在，添加新记录
            if (!found) {
                JSONObject newArea = new JSONObject();
                newArea.put("name", cityName);
                newArea.put("lastAccess", currentTime);
                newArea.put("count", 1);
                areas.put(newArea);
            }

            prefs.edit().putString(KEY_CACHED_AREAS, areas.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 标准化城市名称
     * @param name 原始名称
     * @return 标准化后的名称
     */
    private static String normalizeCityName(String name) {
        if (name == null) return "";
        name = name.trim();

        // 移除常见的后缀重复
        String[] suffixes = {"市", "地区", "盟", "州", "县", "区"};
        for (String suffix : suffixes) {
            if (name.endsWith(suffix + suffix)) {
                name = name.substring(0, name.length() - suffix.length());
            }
        }

        // 确保名称长度合理
        if (name.length() < 2 || name.length() > 20) {
            return "";
        }

        return name;
    }

    /**
     * 获取已缓存的区域列表（按最后访问时间排序）
     * @param context 上下文
     * @return 区域信息列表
     */
    public static List<CacheAreaInfo> getCachedAreaList(Context context) {
        List<CacheAreaInfo> list = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String areasJson = prefs.getString(KEY_CACHED_AREAS, "[]");

        try {
            JSONArray areas = new JSONArray(areasJson);
            for (int i = 0; i < areas.length(); i++) {
                CacheAreaInfo info = CacheAreaInfo.fromJson(areas.getJSONObject(i));
                if (info != null) {
                    list.add(info);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 按最后访问时间降序排序
        Collections.sort(list, (a, b) -> Long.compare(b.lastAccessTime, a.lastAccessTime));

        return list;
    }

    /**
     * 获取已缓存的区域名称集合
     * @param context 上下文
     * @return 区域名称集合
     */
    public static Set<String> getCachedAreas(Context context) {
        Set<String> set = new HashSet<>();
        for (CacheAreaInfo info : getCachedAreaList(context)) {
            set.add(info.name);
        }
        return set;
    }

    /**
     * 获取区域访问次数
     * @param context 上下文
     * @param cityName 城市名称
     * @return 访问次数
     */
    public static int getAreaAccessCount(Context context, String cityName) {
        for (CacheAreaInfo info : getCachedAreaList(context)) {
            if (info.name.equals(cityName)) {
                return info.accessCount;
            }
        }
        return 0;
    }

    /**
     * 获取最后访问时间
     * @param context 上下文
     * @param cityName 城市名称
     * @return 最后访问时间戳
     */
    public static long getAreaLastAccessTime(Context context, String cityName) {
        for (CacheAreaInfo info : getCachedAreaList(context)) {
            if (info.name.equals(cityName)) {
                return info.lastAccessTime;
            }
        }
        return 0;
    }

    /**
     * 格式化时间戳为可读字符串
     * @param timestamp 时间戳
     * @return 格式化后的字符串
     */
    public static String formatAccessTime(long timestamp) {
        if (timestamp == 0) return "从未";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 30) {
            return days / 30 + "个月前";
        } else if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }

    /**
     * 清除指定区域的缓存记录
     * @param context 上下文
     * @param cityName 城市名称
     */
    public static void clearCachedArea(Context context, String cityName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String areasJson = prefs.getString(KEY_CACHED_AREAS, "[]");

        try {
            JSONArray areas = new JSONArray(areasJson);
            JSONArray newAreas = new JSONArray();

            for (int i = 0; i < areas.length(); i++) {
                JSONObject obj = areas.getJSONObject(i);
                if (!cityName.equals(obj.optString("name"))) {
                    newAreas.put(obj);
                }
            }

            prefs.edit().putString(KEY_CACHED_AREAS, newAreas.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除所有缓存记录
     * @param context 上下文
     */
    public static void clearAllCachedAreas(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove(KEY_CACHED_AREAS).remove(KEY_CACHE_STATS).apply();
    }

    /**
     * 清除所有地图缓存
     * 包括：应用缓存、WebView缓存、网络缓存、以及已记录的区域
     * @param context 上下文
     * @return 是否清除成功
     */
    public static boolean clearAllMapCache(Context context) {
        boolean success = true;

        try {
            // 1. 清除应用缓存目录
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                success &= deleteFolderContents(cacheDir);
            }

            // 2. 清除外部缓存
            if (context.getExternalCacheDir() != null) {
                success &= deleteFolderContents(context.getExternalCacheDir());
            }

            // 3. 清除WebView缓存
            try {
                WebView webView = new WebView(context);
                webView.clearCache(true);
                webView.destroy();
            } catch (Exception e) {
                // WebView可能已在运行，尝试其他方式
            }

            // 4. 清除WebStorage
            try {
                WebStorage.getInstance().deleteAllData();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 5. 清除数据库缓存
            context.deleteDatabase("webview.db");
            context.deleteDatabase("webviewCache.db");

            // 6. 清除记录的区域
            clearAllCachedAreas(context);

            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递归获取文件夹大小
     * @param folder 文件夹
     * @return 大小（字节）
     */
    private static long getFolderSize(File folder) {
        long size = 0;
        if (folder == null || !folder.exists()) {
            return 0;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isFile()) {
                size += file.length();
            } else if (file.isDirectory()) {
                size += getFolderSize(file);
            }
        }
        return size;
    }

    /**
     * 删除文件夹内容（保留文件夹本身）
     * @param folder 文件夹
     * @return 是否删除成功
     */
    private static boolean deleteFolderContents(File folder) {
        if (folder == null || !folder.exists()) {
            return true;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return true;
        }

        boolean success = true;
        for (File file : files) {
            if (file.isFile()) {
                success &= file.delete();
            } else if (file.isDirectory()) {
                success &= deleteFolder(file);
            }
        }
        return success;
    }

    /**
     * 递归删除文件夹
     * @param folder 文件夹
     * @return 是否删除成功
     */
    private static boolean deleteFolder(File folder) {
        if (folder == null || !folder.exists()) {
            return true;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    file.delete();
                } else if (file.isDirectory()) {
                    deleteFolder(file);
                }
            }
        }

        return folder.delete();
    }

    /**
     * 清除地图缓存（旧方法，保留兼容性）
     * @param context 上下文
     */
    public static void clearMapCache(Context context) {
        clearAllMapCache(context);
    }
}