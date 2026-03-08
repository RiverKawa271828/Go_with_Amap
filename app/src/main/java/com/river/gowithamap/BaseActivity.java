package com.river.gowithamap;

import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.elvishew.xlog.XLog;
import com.river.gowithamap.utils.ThemeUtils;

/**
 * Activity 基类
 * 提供主题切换和高刷新率支持
 */
public class BaseActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在super.onCreate之前应用主题
        ThemeUtils.applyTheme(this);
        
        // 启用高刷新率
        enableHighRefreshRate();
        
        super.onCreate(savedInstanceState);
    }

    /**
     * 启用高刷新率支持（90Hz/120Hz/144Hz）
     * 让地图滑动和界面滚动更加流畅
     */
    protected void enableHighRefreshRate() {
        try {
            Window window = getWindow();
            if (window == null) return;

            // Android 11+ (API 30+) 使用 Window.setPreferredDisplayMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Display display = getDisplay();
                if (display != null) {
                    Display.Mode[] modes = display.getSupportedModes();
                    if (modes != null && modes.length > 0) {
                        // 找到最高刷新率的模式
                        Display.Mode bestMode = modes[0];
                        float maxRefreshRate = bestMode.getRefreshRate();
                        
                        for (Display.Mode mode : modes) {
                            float refreshRate = mode.getRefreshRate();
                            if (refreshRate > maxRefreshRate) {
                                maxRefreshRate = refreshRate;
                                bestMode = mode;
                            }
                        }
                        
                        // 如果设备支持高于60Hz的刷新率，则启用
                        if (maxRefreshRate > 60) {
                            WindowManager.LayoutParams params = window.getAttributes();
                            params.preferredDisplayModeId = bestMode.getModeId();
                            window.setAttributes(params);
                            XLog.i("[" + getClass().getSimpleName() + "] 高刷新率已启用: " + maxRefreshRate + "Hz");
                        } else {
                            XLog.i("[" + getClass().getSimpleName() + "] 设备不支持高刷新率");
                        }
                    }
                }
            } else {
                // Android 10 及以下，确保硬件加速已启用
                // 硬件加速对流畅度很重要
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                );
                XLog.i("[" + getClass().getSimpleName() + "] 硬件加速已启用");
            }
        } catch (Exception e) {
            XLog.e("[" + getClass().getSimpleName() + "] 启用高刷新率失败: " + e.getMessage());
        }
    }
}