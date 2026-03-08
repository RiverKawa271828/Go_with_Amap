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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
 * 区域收藏数据库
 * 用于存储用户收藏的圆形区域（圆心坐标和半径）
 */
public class DataBaseFavoriteRegions extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "FavoriteRegions";
    public static final String DB_COLUMN_ID = "DB_COLUMN_ID";
    public static final String DB_COLUMN_NAME = "DB_COLUMN_NAME";
    public static final String DB_COLUMN_CENTER_LAT = "DB_COLUMN_CENTER_LAT";
    public static final String DB_COLUMN_CENTER_LON = "DB_COLUMN_CENTER_LON";
    public static final String DB_COLUMN_RADIUS = "DB_COLUMN_RADIUS";
    public static final String DB_COLUMN_TIMESTAMP = "DB_COLUMN_TIMESTAMP";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "FavoriteRegions.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (DB_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "DB_COLUMN_NAME TEXT NOT NULL, " +
            "DB_COLUMN_CENTER_LAT TEXT NOT NULL, " +
            "DB_COLUMN_CENTER_LON TEXT NOT NULL, " +
            "DB_COLUMN_RADIUS TEXT NOT NULL, " +
            "DB_COLUMN_TIMESTAMP BIGINT NOT NULL)";

    public DataBaseFavoriteRegions(Context context) {
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

    /**
     * 保存区域收藏
     * @param sqLiteDatabase 数据库
     * @param contentValues 包含名称、纬度、经度、半径的数据
     */
    public static void saveFavoriteRegion(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        try {
            sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
            XLog.i("区域收藏保存成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 区域收藏插入错误: " + e.getMessage());
        }
    }

    /**
     * 删除区域收藏
     * @param sqLiteDatabase 数据库
     * @param id 区域ID
     */
    public static void deleteFavoriteRegion(SQLiteDatabase sqLiteDatabase, String id) {
        try {
            sqLiteDatabase.delete(TABLE_NAME, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("区域收藏删除成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 区域收藏删除错误: " + e.getMessage());
        }
    }

    /**
     * 获取所有区域收藏
     * @param sqLiteDatabase 数据库
     * @return 区域列表
     */
    public static List<Map<String, Object>> getAllFavoriteRegions(SQLiteDatabase sqLiteDatabase) {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, null, null, null, null, null,
                    DB_COLUMN_TIMESTAMP + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, Object> item = new HashMap<>();
                    item.put(DB_COLUMN_ID, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_ID)));
                    item.put(DB_COLUMN_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_NAME)));
                    item.put(DB_COLUMN_CENTER_LAT, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_CENTER_LAT)));
                    item.put(DB_COLUMN_CENTER_LON, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_CENTER_LON)));
                    item.put(DB_COLUMN_RADIUS, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_RADIUS)));
                    item.put(DB_COLUMN_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(DB_COLUMN_TIMESTAMP)));
                    data.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            XLog.e("DATABASE: 获取区域收藏错误: " + e.getMessage());
        }
        return data;
    }

    /**
     * 根据ID获取区域收藏
     * @param sqLiteDatabase 数据库
     * @param id 区域ID
     * @return 区域数据
     */
    public static Map<String, Object> getFavoriteRegionById(SQLiteDatabase sqLiteDatabase, String id) {
        Map<String, Object> item = null;
        try {
            Cursor cursor = sqLiteDatabase.query(TABLE_NAME, null, 
                    DB_COLUMN_ID + " = ?", new String[]{id}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                item = new HashMap<>();
                item.put(DB_COLUMN_ID, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_ID)));
                item.put(DB_COLUMN_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_NAME)));
                item.put(DB_COLUMN_CENTER_LAT, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_CENTER_LAT)));
                item.put(DB_COLUMN_CENTER_LON, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_CENTER_LON)));
                item.put(DB_COLUMN_RADIUS, cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_RADIUS)));
                item.put(DB_COLUMN_TIMESTAMP, cursor.getLong(cursor.getColumnIndexOrThrow(DB_COLUMN_TIMESTAMP)));
                cursor.close();
            }
        } catch (Exception e) {
            XLog.e("DATABASE: 获取区域收藏错误: " + e.getMessage());
        }
        return item;
    }

    /**
     * 清空所有区域收藏
     * @param sqLiteDatabase 数据库
     */
    public static void clearAllFavoriteRegions(SQLiteDatabase sqLiteDatabase) {
        try {
            sqLiteDatabase.delete(TABLE_NAME, null, null);
            XLog.i("所有区域收藏已清空");
        } catch (Exception e) {
            XLog.e("DATABASE: 清空区域收藏错误: " + e.getMessage());
        }
    }

    /**
     * 删除所有区域收藏（clearAllFavoriteRegions的别名）
     * @param sqLiteDatabase 数据库
     */
    public static void deleteAllFavoriteRegions(SQLiteDatabase sqLiteDatabase) {
        clearAllFavoriteRegions(sqLiteDatabase);
    }
}
