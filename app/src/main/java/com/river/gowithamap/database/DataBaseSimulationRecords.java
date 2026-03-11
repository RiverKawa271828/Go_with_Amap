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
 * 模拟记录数据库
 * 只记录实际被模拟过的位置（点击模拟按钮的位置）
 */
public class DataBaseSimulationRecords extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "SimulationRecords";
    public static final String DB_COLUMN_ID = "DB_COLUMN_ID";
    public static final String DB_COLUMN_LOCATION = "DB_COLUMN_LOCATION";
    public static final String DB_COLUMN_LATITUDE = "DB_COLUMN_LATITUDE";
    public static final String DB_COLUMN_LONGITUDE = "DB_COLUMN_LONGITUDE";
    public static final String DB_COLUMN_TIMESTAMP = "DB_COLUMN_TIMESTAMP";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "SimulationRecords.db";
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
            " (" + DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            DB_COLUMN_LOCATION + " TEXT NOT NULL, " +
            DB_COLUMN_LATITUDE + " TEXT NOT NULL, " +
            DB_COLUMN_LONGITUDE + " TEXT NOT NULL, " +
            DB_COLUMN_TIMESTAMP + " BIGINT NOT NULL)";

    public DataBaseSimulationRecords(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * 保存模拟记录
     */
    public static void saveSimulationRecord(SQLiteDatabase db, ContentValues values) {
        try {
            db.insert(TABLE_NAME, null, values);
            XLog.i("模拟记录保存成功");
        } catch (Exception e) {
            XLog.e("保存模拟记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有模拟记录（按时间倒序）
     */
    public static List<Map<String, Object>> getAllSimulationRecords(SQLiteDatabase db) {
        List<Map<String, Object>> data = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NAME, null, null, null, null, null,
                    DB_COLUMN_TIMESTAMP + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        Map<String, Object> item = new HashMap<>();
                        int idInt = cursor.getInt(cursor.getColumnIndexOrThrow(DB_COLUMN_ID));
                        String id = String.valueOf(idInt);
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_LOCATION));
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_LATITUDE));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(DB_COLUMN_LONGITUDE));
                        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DB_COLUMN_TIMESTAMP));

                        // 验证数据有效性
                        if (lat == null || lon == null || lat.isEmpty() || lon.isEmpty()) {
                            XLog.w("跳过无效记录: ID=" + id + ", lat=" + lat + ", lon=" + lon);
                            continue;
                        }

                        double latVal = Double.parseDouble(lat);
                        double lonVal = Double.parseDouble(lon);

                        item.put("id", id);
                        item.put("name", name != null ? name : "未命名位置");
                        item.put("lat", latVal);
                        item.put("lon", lonVal);
                        item.put("timestamp", timestamp);
                        item.put("coords", String.format("%.6f, %.6f", lonVal, latVal));
                        data.add(item);
                    } catch (NumberFormatException e) {
                        XLog.e("解析坐标失败，跳过该记录: " + e.getMessage());
                    } catch (Exception e) {
                        XLog.e("处理记录时出错，跳过: " + e.getMessage());
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            XLog.e("获取模拟记录失败: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 更新记录名称
     */
    public static void updateSimulationRecordName(SQLiteDatabase db, String id, String newName) {
        try {
            ContentValues values = new ContentValues();
            values.put(DB_COLUMN_LOCATION, newName);
            db.update(TABLE_NAME, values, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("模拟记录名称更新成功");
        } catch (Exception e) {
            XLog.e("更新模拟记录名称失败: " + e.getMessage());
        }
    }

    /**
     * 删除单条记录
     */
    public static void deleteSimulationRecord(SQLiteDatabase db, String id) {
        try {
            db.delete(TABLE_NAME, DB_COLUMN_ID + " = ?", new String[]{id});
            XLog.i("模拟记录删除成功");
        } catch (Exception e) {
            XLog.e("删除模拟记录失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有记录
     */
    public static void clearAllSimulationRecords(SQLiteDatabase db) {
        try {
            db.delete(TABLE_NAME, null, null);
            XLog.i("所有模拟记录已清空");
        } catch (Exception e) {
            XLog.e("清空模拟记录失败: " + e.getMessage());
        }
    }
}
