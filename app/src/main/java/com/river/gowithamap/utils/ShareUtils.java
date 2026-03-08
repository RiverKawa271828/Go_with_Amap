package com.river.gowithamap.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class ShareUtils {
    /**
     * 返回uri
     */
    public static Uri getUriFromFile(Context context, File file) {
        String authority = context.getPackageName().concat(".fileProvider");
        return FileProvider.getUriForFile(context, authority, file);
    }

    public static void shareFile(Context context, File file, String title) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.putExtra(Intent.EXTRA_STREAM, getUriFromFile(context, file));
        share.setType("application/octet-stream");
        context.startActivity(Intent.createChooser(share, title));
    }

    public static void shareText(Context context, String title, String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        context.startActivity(Intent.createChooser(share, title));
    }

    /**
     * 分享图片
     */
    public static void shareImage(Context context, Bitmap bitmap, String title) {
        try {
            // 保存图片到临时文件
            File cacheDir = context.getCacheDir();
            File imageFile = new File(cacheDir, "share_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            // 获取URI并分享
            Uri uri = getUriFromFile(context, imageFile);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setType("image/png");
            context.startActivity(Intent.createChooser(share, title));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

