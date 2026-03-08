package com.river.gowithamap.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.os.Environment;
import android.os.Build;

import com.elvishew.xlog.XLog;

/**
 * 文件工具类
 */
public class FileUtils {
    
    /**
     * 从URI获取实际文件路径
     */
    public static String getPathFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        String uriStr = uri.toString();
        XLog.d("解析URI: " + uriStr);

        try {
            // 处理 Tree URI (如: content://com.android.externalstorage.documents/tree/primary:Documents)
            if (uriStr.startsWith("content://com.android.externalstorage.documents/tree/")) {
                String treePath = uriStr.substring(uriStr.lastIndexOf("/tree/") + 6);

                // 处理 primary:path 格式
                if (treePath.startsWith("primary:")) {
                    String relativePath = treePath.substring(8); // 去掉 "primary:"
                    return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + relativePath;
                }

                // 处理其他存储设备
                if (treePath.contains(":")) {
                    String[] parts = treePath.split(":", 2);
                    if (parts.length >= 2) {
                        if ("primary".equalsIgnoreCase(parts[0])) {
                            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + parts[1];
                        }
                    }
                }
            }

            // Android 10+ 使用 SAF (Storage Access Framework)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 对于 Document URI
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    String docId = DocumentsContract.getDocumentId(uri);
                    XLog.d("Document ID: " + docId);

                    if (docId.startsWith("raw:")) {
                        return docId.replaceFirst("raw:", "");
                    }

                    // 处理 Downloads 目录
                    if (uriStr.contains("com.android.providers.downloads")) {
                        return Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                    }

                    // 处理外部存储
                    if (docId.contains(":")) {
                        String[] split = docId.split(":");
                        if (split.length >= 2) {
                            String type = split[0];
                            String relativePath = split[1];

                            if ("primary".equalsIgnoreCase(type)) {
                                return Environment.getExternalStorageDirectory() + "/" + relativePath;
                            }
                        }
                    }
                }
            }

            // 尝试常规方法
            String[] projection = {MediaStore.MediaColumns.DATA};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                if (cursor.moveToFirst()) {
                    String path = cursor.getString(columnIndex);
                    cursor.close();
                    return path;
                }
                cursor.close();
            }

            // 如果上述方法都失败，返回URI的路径部分
            String path = uri.getPath();
            if (path != null && path.startsWith("/tree/")) {
                path = path.replace("/tree/primary:", "/storage/emulated/0/");
                path = path.replace("/tree/", "/storage/");
                path = path.replace(":", "/");
            }
            return path;

        } catch (Exception e) {
            XLog.e("获取路径失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从URI获取文件名
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    
    /**
     * 确保目录存在
     */
    public static boolean ensureDirectoryExists(String path) {
        if (path == null || path.isEmpty()) return false;
        
        java.io.File dir = new java.io.File(path);
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }
}