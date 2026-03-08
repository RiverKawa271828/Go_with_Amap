/*
 * Copyright (C) 2024 River
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.river.gowithamap.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

/**
 * ROOT 工具类
 * 用于检测设备是否已 ROOT 以及执行 ROOT 命令
 */
public class RootUtils {

    private static final String TAG = "RootUtils";
    private static Boolean isRooted = null;

    /**
     * 检测设备是否已 ROOT
     */
    public static boolean isDeviceRooted() {
        if (isRooted != null) {
            return isRooted;
        }

        // 检查常见的 ROOT 标志
        isRooted = checkTestKeys()
                || checkSuperuserApk()
                || checkWhichSu()
                || checkSuBinary();

        Log.d(TAG, "Device rooted: " + isRooted);
        return isRooted;
    }

    /**
     * 检查测试签名（test-keys）
     */
    private static boolean checkTestKeys() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    /**
     * 检查是否存在 Superuser.apk
     */
    private static boolean checkSuperuserApk() {
        return new File("/system/app/Superuser.apk").exists()
                || new File("/system/app/SuperUser.apk").exists()
                || new File("/system/priv-app/Superuser.apk").exists();
    }

    /**
     * 检查 which su 命令
     */
    private static boolean checkWhichSu() {
        return executeCommand("which su");
    }

    /**
     * 检查 su 二进制文件
     */
    private static boolean checkSuBinary() {
        String[] paths = {
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行普通命令
     */
    private static boolean executeCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 执行 ROOT 命令
     *
     * @param commands 命令数组
     * @return 是否执行成功
     */
    public static boolean executeRootCommand(String[] commands) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader successReader = null;
        BufferedReader errorReader = null;

        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            // 执行命令
            for (String command : commands) {
                Log.d(TAG, "Executing: " + command);
                os.writeBytes(command + "\n");
                os.flush();
            }

            os.writeBytes("exit\n");
            os.flush();

            // 读取输出
            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = successReader.readLine()) != null) {
                Log.d(TAG, "Output: " + line);
            }

            while ((line = errorReader.readLine()) != null) {
                Log.e(TAG, "Error: " + line);
            }

            process.waitFor();
            return process.exitValue() == 0;

        } catch (Exception e) {
            Log.e(TAG, "Failed to execute root command", e);
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (successReader != null) successReader.close();
                if (errorReader != null) errorReader.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing streams", e);
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 执行单个 ROOT 命令
     */
    public static boolean executeRootCommand(String command) {
        return executeRootCommand(new String[]{command});
    }

    /**
     * 请求 ROOT 权限
     * 这会弹出 Superuser 授权对话框
     */
    public static boolean requestRootAccess() {
        return executeRootCommand("id");
    }

    /**
     * 使用 settings 命令设置位置
     * 需要系统签名或特定权限
     */
    public static boolean setLocationViaSettings(double lat, double lng, double alt) {
        // 尝试使用 settings put global 命令
        // 注意：这需要系统权限，普通 ROOT 可能无法使用
        String[] commands = {
                "settings put global mock_location_lat " + lat,
                "settings put global mock_location_lon " + lng,
                "settings put global mock_location_alt " + alt
        };
        return executeRootCommand(commands);
    }

    /**
     * 使用 app_process 注入位置
     * 这是一个高级的 ROOT 方案
     */
    public static boolean injectLocation(double lat, double lng, double alt, float accuracy) {
        // 构建位置注入命令
        String locationData = String.format("%f,%f,%f,%f", lat, lng, alt, accuracy);

        // 使用 am 命令广播位置信息
        String[] commands = {
                "am broadcast -a com.river.gowithamap.INJECT_LOCATION " +
                        "--es location \"" + locationData + "\""
        };

        return executeRootCommand(commands);
    }

    /**
     * 检查是否有 ROOT 权限（已授权）
     */
    public static boolean hasRootAccess() {
        return executeRootCommand("id");
    }

    /**
     * 清除 ROOT 检测结果（用于重新检测）
     */
    public static void clearRootCache() {
        isRooted = null;
    }
}
