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
 * 固定圆数据库
 * 用于存储用户固定在地图上的圆形区域
 */
public class DataBasePinnedCircles extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "PinnedCircles";
    public static final String DB_COLUMN_ID = "DB_COLUMN_ID";
    public static final String DB_COLUMN_NAME = "DB_COLUMN_NAME";
    public static final String DB_COLUMN_CENTER_LAT = "DB_COLUMN_CENTER_LAT";
    public static final String DB_COLUMN_CENTER_LON = "DB_COLUMN_CENTER_LON";
    public static final String DB_COLUMN_RADIUS = "DB_COLUMN_RADIUS";
    public static final String DB_COLUMN_TIMESTAMP = "DB_COLUMN_TIMESTAMP";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "PinnedCircles.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (DB_COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "DB_COLUMN_NAME TEXT NOT NULL, " +
            "DB_COLUMN_CENTER_LAT TEXT NOT NULL, " +
            "DB_COLUMN_CENTER_LON TEXT NOT NULL, " +
            "DB_COLUMN_RADIUS TEXT NOT NULL, " +
            "DB_COLUMN_TIMESTAMP BIGINT NOT NULL)";

    public DataBasePinnedCircles(Context context) {
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
     * 保存固定圆
     * @param sqLiteDatabase 数据库
     * @param contentValues 包含名称、纬度、经度、半径的数据
     */
    public static void savePinnedCircle(SQLiteDatabase sqLiteDatabase, ContentValues contentValues) {
        try {
            sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
            XLog.i("固定圆保存成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 固定圆插入错误: " + e.getMessage());
        }
    }

    /**
     * 删除固定圆
     * @param sqLiteDatabase 数据库
     * @param id 圆ID
     */
    public static void deletePinnedCircle(SQLiteDatabase sqLiteDatabase, String id) {
        try {
            sqLiteDatabase.delete(TABLE_NAME, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("固定圆删除成功");
        } catch (Exception e) {
            XLog.e("DATABASE: 固定圆删除错误: " + e.getMessage());
        }
    }

    /**
     * 获取所有固定圆
     * @param sqLiteDatabase 数据库
     * @return 固定圆列表
     */
    public static List<Map<String, Object>> getAllPinnedCircles(SQLiteDatabase sqLiteDatabase) {
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
            XLog.e("DATABASE: 获取固定圆错误: " + e.getMessage());
        }
        return data;
    }

    /**
     * 根据ID获取固定圆
     * @param sqLiteDatabase 数据库
     * @param id 圆ID
     * @return 圆数据
     */
    public static Map<String, Object> getPinnedCircleById(SQLiteDatabase sqLiteDatabase, String id) {
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
            XLog.e("DATABASE: 获取固定圆错误: " + e.getMessage());
        }
        return item;
    }

    /**
     * 清空所有固定圆
     * @param sqLiteDatabase 数据库
     */
    public static void clearAllPinnedCircles(SQLiteDatabase sqLiteDatabase) {
        try {
            sqLiteDatabase.delete(TABLE_NAME, null, null);
            XLog.i("所有固定圆已清空");
        } catch (Exception e) {
            XLog.e("DATABASE: 清空固定圆错误: " + e.getMessage());
        }
    }
}
