package com.river.gowithamap.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataBaseFavorites extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "Favorites";
    public static final String DB_COLUMN_ID = "DB_COLUMN_ID";
    public static final String DB_COLUMN_NAME = "DB_COLUMN_NAME";
    public static final String DB_COLUMN_LATITUDE = "DB_COLUMN_LATITUDE";
    public static final String DB_COLUMN_LONGITUDE = "DB_COLUMN_LONGITUDE";
    public static final String DB_COLUMN_TIMESTAMP = "DB_COLUMN_TIMESTAMP";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Favorites.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (DB_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "DB_COLUMN_NAME TEXT NOT NULL, " +
            "DB_COLUMN_LATITUDE TEXT NOT NULL, " +
            "DB_COLUMN_LONGITUDE TEXT NOT NULL, " +
            "DB_COLUMN_TIMESTAMP BIGINT NOT NULL)";

    public DataBaseFavorites(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }

    // 保存收藏
    public static void saveFavorite(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        try {
            sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
            XLog.i("收藏保存成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 收藏插入错误: " + e.getMessage());
        }
    }

    // 删除收藏
    public static void deleteFavorite(SQLiteDatabase sqLiteDatabase, String id) {
        try {
            sqLiteDatabase.delete(TABLE_NAME, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("收藏删除成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 收藏删除错误: " + e.getMessage());
        }
    }

    // 删除所有收藏
    public static void deleteAllFavorites(SQLiteDatabase sqLiteDatabase) {
        try {
            sqLiteDatabase.delete(TABLE_NAME, null, null);
            XLog.i("所有收藏删除成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 删除所有收藏错误: " + e.getMessage());
        }
    }

    // 获取所有收藏
    public static List<Map<String, Object>> getAllFavorites(SQLiteDatabase sqLiteDatabase) {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, null, null, null, null, null,
                    DB_COLUMN_TIMESTAMP + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, Object> item = new HashMap<>();
                    item.put(DB_COLUMN_ID, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_ID)));
                    item.put(DB_COLUMN_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_NAME)));
                    item.put(DB_COLUMN_LATITUDE, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_LATITUDE)));
                    item.put(DB_COLUMN_LONGITUDE, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_LONGITUDE)));
                    item.put(DB_COLUMN_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(DB_COLUMN_TIMESTAMP)));
                    data.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            XLog.e("DATABASE: 获取收藏错误: " + e.getMessage());
        }
        return data;
    }
}
