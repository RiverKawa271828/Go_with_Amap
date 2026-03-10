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

/**
 * 固定坐标点数据库
 */
public class DataBasePinnedLocations extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "PinnedLocations";
    public static final String DB_COLUMN_ID = "id";
    public static final String DB_COLUMN_NAME = "name";
    public static final String DB_COLUMN_LATITUDE = "latitude";
    public static final String DB_COLUMN_LONGITUDE = "longitude";
    public static final String DB_COLUMN_TIMESTAMP = "timestamp";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "PinnedLocations.db";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
            " (" + DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DB_COLUMN_NAME + " TEXT NOT NULL, " +
            DB_COLUMN_LATITUDE + " TEXT NOT NULL, " +
            DB_COLUMN_LONGITUDE + " TEXT NOT NULL, " +
            DB_COLUMN_TIMESTAMP + " BIGINT NOT NULL)";

    public DataBasePinnedLocations(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * 保存固定坐标点
     */
    public static void savePinnedLocation(SQLiteDatabase db, ContentValues values) {
        try {
            db.insert(TABLE_NAME, null, values);
            XLog.i("固定坐标点保存成功");
        } catch (Exception e) {
            XLog.e("保存固定坐标点失败: " + e.getMessage());
        }
    }

    /**
     * 删除固定坐标点
     */
    public static void deletePinnedLocation(SQLiteDatabase db, String id) {
        try {
            db.delete(TABLE_NAME, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("固定坐标点删除成功");
        } catch (Exception e) {
            XLog.e("删除固定坐标点失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有固定坐标点
     */
    public static List<Map<String, Object>> getAllPinnedLocations(SQLiteDatabase db) {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
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
            XLog.e("获取固定坐标点失败: " + e.getMessage());
        }
        return data;
    }

    /**
     * 清除所有固定坐标点
     */
    public static void clearAllPinnedLocations(SQLiteDatabase db) {
        try {
            db.delete(TABLE_NAME, null, null);
            XLog.i("所有固定坐标点已清除");
        } catch (Exception e) {
            XLog.e("清除固定坐标点失败: " + e.getMessage());
        }
    }

    /**
     * 更新固定坐标点名称
     */
    public static void updatePinnedLocationName(SQLiteDatabase db, String id, String newName) {
        try {
            ContentValues values = new ContentValues();
            values.put(DB_COLUMN_NAME, newName);
            db.update(TABLE_NAME, values, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("固定坐标点名称更新成功: " + newName);
        } catch (Exception e) {
            XLog.e("更新固定坐标点名称失败: " + e.getMessage());
        }
    }
}