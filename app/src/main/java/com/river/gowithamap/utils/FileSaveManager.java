package com.river.gowithamap.utils;

import android.content.Context;
import android.os.Environment;

import com.elvishew.xlog.XLog;

import java.io.File;

/**
 * 文件保存路径管理器
 * 仅使用默认目录 (Downloads/GoWithAmap/)
 */
public class FileSaveManager {

    /**
     * 获取保存路径
     * @param context 上下文
     * @return 默认路径 (Downloads/GoWithAmap/)
     */
    public static String getSavePath(Context context) {
        return getDefaultSavePath();
    }

    /**
     * 获取默认保存路径
     * @return 默认路径 (Downloads/GoWithAmap/)
     */
    public static String getDefaultSavePath() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String defaultPath = downloadsDir.getAbsolutePath() + "/GoWithAmap";

        // 确保目录存在
        File dir = new File(defaultPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            XLog.i("FileSaveManager: 创建默认目录=" + created);
        }

        return defaultPath;
    }

    /**
     * 获取完整的文件路径
     * @param context 上下文
     * @param fileName 文件名
     * @return 完整的文件路径
     */
    public static String getFullFilePath(Context context, String fileName) {
        return getSavePath(context) + "/" + fileName;
    }

    /**
     * 确保保存目录存在
     * @param context 上下文
     * @return 如果目录存在或创建成功返回true
     */
    public static boolean ensureDirectoryExists(Context context) {
        String savePath = getSavePath(context);
        File dir = new File(savePath);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
}
