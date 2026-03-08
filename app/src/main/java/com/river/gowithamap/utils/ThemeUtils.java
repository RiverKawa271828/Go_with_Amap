package com.river.gowithamap.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.river.gowithamap.R;

/**
 * 主题工具类 - 管理夜间模式切换
 */
public class ThemeUtils {
    
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_SYSTEM_DEFAULT = "system_default";
    
    /**
     * 主题模式
     */
    public enum ThemeMode {
        SYSTEM_DEFAULT,  // 跟随系统
        LIGHT,           // 浅色模式
        DARK             // 深色模式
    }
    
    /**
     * 应用主题（应在Activity的super.onCreate之前调用）
     */
    public static void applyTheme(Context context) {
        ThemeMode mode = getThemeMode(context);
        applyThemeMode(mode);
    }
    
    /**
     * 应用指定主题模式
     */
    public static void applyThemeMode(ThemeMode mode) {
        switch (mode) {
            case SYSTEM_DEFAULT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }
    
    /**
     * 获取当前主题模式
     */
    public static ThemeMode getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean systemDefault = prefs.getBoolean(KEY_SYSTEM_DEFAULT, true);
        if (systemDefault) {
            return ThemeMode.SYSTEM_DEFAULT;
        }
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        return darkMode ? ThemeMode.DARK : ThemeMode.LIGHT;
    }
    
    /**
     * 设置主题模式
     */
    public static void setThemeMode(Context context, ThemeMode mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        switch (mode) {
            case SYSTEM_DEFAULT:
                editor.putBoolean(KEY_SYSTEM_DEFAULT, true);
                break;
            case LIGHT:
                editor.putBoolean(KEY_SYSTEM_DEFAULT, false);
                editor.putBoolean(KEY_DARK_MODE, false);
                break;
            case DARK:
                editor.putBoolean(KEY_SYSTEM_DEFAULT, false);
                editor.putBoolean(KEY_DARK_MODE, true);
                break;
        }
        editor.apply();
        
        // 立即应用主题
        applyThemeMode(mode);
    }
    
    /**
     * 检查当前是否为深色模式
     */
    public static boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * 重新创建所有Activity以应用新主题
     * 需要在调用此方法后调用recreate()
     */
    public static void recreateAllActivities(Activity activity) {
        // 对于单Activity架构，直接recreate即可
        // 如果需要跨Activity刷新，可以使用EventBus或LiveData通知
        activity.recreate();
    }
}
