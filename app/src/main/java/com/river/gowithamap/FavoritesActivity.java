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

package com.river.gowithamap;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.elvishew.xlog.XLog;
import com.river.gowithamap.database.DataBaseFavorites;
import com.river.gowithamap.database.DataBaseFavoriteRegions;
import com.river.gowithamap.utils.GoUtils;
import com.river.gowithamap.utils.ShareUtils;
import com.river.gowithamap.utils.FileSaveManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FavoritesActivity extends BaseActivity {
    private static final int REQUEST_CODE_IMPORT_FILE = 1001;
    private static final int REQUEST_CODE_SCAN_QR = 1002;

    private SQLiteDatabase mFavoritesDB;
    private SQLiteDatabase mFavoriteRegionsDB;
    private ListView mListView;
    private ListView mRegionsListView;
    private View mImportSection;
    private com.google.android.material.button.MaterialButton mBtnCoordinates;
    private com.google.android.material.button.MaterialButton mBtnRegions;
    private View mCardCoordinates;
    private View mCardRegions;

    private boolean isShowingCoordinates = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // 初始化数据库
        DataBaseFavorites dbHelper = new DataBaseFavorites(this);
        mFavoritesDB = dbHelper.getWritableDatabase();
        
        DataBaseFavoriteRegions regionsDbHelper = new DataBaseFavoriteRegions(this);
        mFavoriteRegionsDB = regionsDbHelper.getWritableDatabase();

        mListView = findViewById(R.id.favorites_list);
        mRegionsListView = findViewById(R.id.regions_list);
        mImportSection = findViewById(R.id.import_section);
        mBtnCoordinates = findViewById(R.id.btn_coordinates);
        mBtnRegions = findViewById(R.id.btn_regions);
        mCardCoordinates = findViewById(R.id.card_coordinates);
        mCardRegions = findViewById(R.id.card_regions);

        // 设置Toolbar为ActionBar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 初始化按钮切换
        initButtonSwitch();

        // 加载收藏列表
        loadFavorites();
        loadFavoriteRegions();

        // 点击坐标收藏项在地图上显示
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(position);
            String name = (String) item.get(DataBaseFavorites.DB_COLUMN_NAME);
            double lat = Double.parseDouble((String) item.get(DataBaseFavorites.DB_COLUMN_LATITUDE));
            double lon = Double.parseDouble((String) item.get(DataBaseFavorites.DB_COLUMN_LONGITUDE));
            
            // 返回主界面并显示该位置
            showLocationOnMap(name, lon, lat);
        });

        // 长按坐标显示菜单（分享、删除）
        mListView.setOnItemLongClickListener((parent, view, position, id) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(position);
            String favId = (String) item.get(DataBaseFavorites.DB_COLUMN_ID);
            String name = (String) item.get(DataBaseFavorites.DB_COLUMN_NAME);
            double lat = Double.parseDouble((String) item.get(DataBaseFavorites.DB_COLUMN_LATITUDE));
            double lon = Double.parseDouble((String) item.get(DataBaseFavorites.DB_COLUMN_LONGITUDE));
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(name)
                    .setItems(new String[]{"分享", "删除"}, (dialog, which) -> {
                        if (which == 0) {
                            // 分享坐标
                            shareLocation(name, lon, lat);
                        } else if (which == 1) {
                            // 删除
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("删除收藏")
                                    .setMessage("确定要删除 " + name + " 吗？")
                                    .setPositiveButton("确定", (d, w) -> {
                                        DataBaseFavorites.deleteFavorite(mFavoritesDB, favId);
                                        loadFavorites();
                                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    })
                    .show();
            return true;
        });

        // 点击区域收藏项在地图上显示圆
        mRegionsListView.setOnItemClickListener((parent, view, position, id) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(position);
            String name = (String) item.get(DataBaseFavoriteRegions.DB_COLUMN_NAME);
            double lat = Double.parseDouble((String) item.get(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT));
            double lon = Double.parseDouble((String) item.get(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON));
            double radius = Double.parseDouble((String) item.get(DataBaseFavoriteRegions.DB_COLUMN_RADIUS));
            
            // 返回主界面并显示该区域
            showRegionOnMap(name, lon, lat, radius);
        });

        // 长按区域显示菜单（分享、删除）
        mRegionsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(position);
            String regionId = (String) item.get(DataBaseFavoriteRegions.DB_COLUMN_ID);
            String name = (String) item.get(DataBaseFavoriteRegions.DB_COLUMN_NAME);
            double lat = Double.parseDouble((String) item.get(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT));
            double lon = Double.parseDouble((String) item.get(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON));
            double radius = Double.parseDouble((String) item.get(DataBaseFavoriteRegions.DB_COLUMN_RADIUS));
            
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(name)
                    .setItems(new String[]{"分享", "删除"}, (dialog, which) -> {
                        if (which == 0) {
                            // 分享区域
                            shareRegion(name, lon, lat, radius);
                        } else if (which == 1) {
                            // 删除
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("删除区域收藏")
                                    .setMessage("确定要删除 " + name + " 吗？")
                                    .setPositiveButton("确定", (d, w) -> {
                                        DataBaseFavoriteRegions.deleteFavoriteRegion(mFavoriteRegionsDB, regionId);
                                        loadFavoriteRegions();
                                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    })
                    .show();
            return true;
        });

        // 导入文件按钮
        findViewById(R.id.btn_import_file).setOnClickListener(v -> importFromFile());

        // 扫描二维码按钮
        findViewById(R.id.btn_import_qrcode).setOnClickListener(v -> importFromQRCode());
    }

    private void initButtonSwitch() {
        // 坐标按钮点击
        mBtnCoordinates.setOnClickListener(v -> {
            showCoordinatesView();
        });

        // 区域按钮点击
        mBtnRegions.setOnClickListener(v -> {
            showRegionsView();
        });

        // 默认显示坐标
        showCoordinatesView();
    }

    private void showCoordinatesView() {
        isShowingCoordinates = true;

        // 更新按钮样式
        mBtnCoordinates.setTextColor(getColor(R.color.md_onPrimary));
        mBtnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
        mBtnCoordinates.setElevation(getResources().getDimension(R.dimen.elevation_low));

        mBtnRegions.setTextColor(getColor(R.color.md_onSurfaceVariant));
        mBtnRegions.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
        mBtnRegions.setElevation(0);

        // 显示坐标列表，隐藏区域列表
        mCardCoordinates.setVisibility(View.VISIBLE);
        mCardRegions.setVisibility(View.GONE);
        mImportSection.setVisibility(View.GONE);

        loadFavorites();
    }

    private void showRegionsView() {
        isShowingCoordinates = false;

        // 更新按钮样式
        mBtnRegions.setTextColor(getColor(R.color.md_onPrimary));
        mBtnRegions.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
        mBtnRegions.setElevation(getResources().getDimension(R.dimen.elevation_low));

        mBtnCoordinates.setTextColor(getColor(R.color.md_onSurfaceVariant));
        mBtnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
        mBtnCoordinates.setElevation(0);

        // 显示区域列表，隐藏坐标列表
        mCardCoordinates.setVisibility(View.GONE);
        mCardRegions.setVisibility(View.VISIBLE);
        mImportSection.setVisibility(View.VISIBLE);

        loadFavoriteRegions();
    }

    private void loadFavorites() {
        List<Map<String, Object>> favorites = DataBaseFavorites.getAllFavorites(mFavoritesDB);
        
        if (favorites.isEmpty()) {
            // 显示空提示
        }

        SimpleAdapter adapter = new SimpleAdapter(this, favorites,
                R.layout.item_favorite,
                new String[]{DataBaseFavorites.DB_COLUMN_NAME, DataBaseFavorites.DB_COLUMN_LATITUDE},
                new int[]{R.id.text1, R.id.text2});
        
        mListView.setAdapter(adapter);
    }

    private void loadFavoriteRegions() {
        List<Map<String, Object>> regions = DataBaseFavoriteRegions.getAllFavoriteRegions(mFavoriteRegionsDB);
        
        if (regions.isEmpty()) {
            // 显示空提示
        }

        SimpleAdapter adapter = new SimpleAdapter(this, regions,
                R.layout.item_favorite,
                new String[]{DataBaseFavoriteRegions.DB_COLUMN_NAME, DataBaseFavoriteRegions.DB_COLUMN_RADIUS},
                new int[]{R.id.text1, R.id.text2});
        
        mRegionsListView.setAdapter(adapter);
    }

    private void showLocationOnMap(String name, double lon, double lat) {
        // 返回坐标数据给MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("type", "location");
        resultIntent.putExtra("name", name);
        resultIntent.putExtra("lon", lon);
        resultIntent.putExtra("lat", lat);
        setResult(RESULT_OK, resultIntent);
        finish(); // 关闭当前Activity返回主界面
    }

    private void showRegionOnMap(String name, double lon, double lat, double radius) {
        // 返回区域数据给MainActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("type", "region");
        resultIntent.putExtra("name", name);
        resultIntent.putExtra("lon", lon);
        resultIntent.putExtra("lat", lat);
        resultIntent.putExtra("radius", radius);
        setResult(RESULT_OK, resultIntent);
        finish(); // 关闭当前Activity返回主界面
    }

    /**
     * 分享坐标
     */
    private void shareLocation(String name, double lon, double lat) {
        // 创建分享内容
        String shareText = String.format("位置: %s\n坐标: %.6f, %.6f", name, lat, lon);
        
        // 创建JSON数据
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("type", "location");
            jsonData.put("name", name);
            jsonData.put("lat", lat);
            jsonData.put("lon", lon);
        } catch (JSONException e) {
            XLog.e("创建JSON失败: " + e.getMessage());
        }
        
        // 显示分享选项
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("分享坐标")
                .setItems(new String[]{"分享文本", "生成二维码", "保存为文件"}, (dialog, which) -> {
                    if (which == 0) {
                        // 分享文本
                        ShareUtils.shareText(this, "分享位置", shareText);
                    } else if (which == 1) {
                        // 生成二维码
                        generateAndShareQR(jsonData.toString(), name);
                    } else {
                        // 保存为文件
                        saveLocationToFile(name, jsonData);
                    }
                })
                .show();
    }

    /**
     * 分享区域
     */
    private void shareRegion(String name, double lon, double lat, double radius) {
        // 创建分享内容
        String shareText = String.format("区域: %s\n中心: %.6f, %.6f\n半径: %.0f米", name, lat, lon, radius);
        
        // 创建JSON数据
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("type", "circle");
            jsonData.put("name", name);
            jsonData.put("lat", lat);
            jsonData.put("lon", lon);
            jsonData.put("radius", radius);
        } catch (JSONException e) {
            XLog.e("创建JSON失败: " + e.getMessage());
        }
        
        // 显示分享选项
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("分享区域")
                .setItems(new String[]{"分享文本", "生成二维码", "保存为文件"}, (dialog, which) -> {
                    if (which == 0) {
                        // 分享文本
                        ShareUtils.shareText(this, "分享区域", shareText);
                    } else if (which == 1) {
                        // 生成二维码
                        generateAndShareQR(jsonData.toString(), name);
                    } else {
                        // 保存为文件
                        saveRegionToFile(name, jsonData);
                    }
                })
                .show();
    }

    /**
     * 生成并分享二维码
     */
    private void generateAndShareQR(String content, String name) {
        try {
            // 生成二维码位图
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(content,
                    com.google.zxing.BarcodeFormat.QR_CODE, 500, 500);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            final android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(width, height,
                    android.graphics.Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // 显示选项对话框
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("导出二维码")
                    .setItems(new String[]{"导出到自定义目录", "分享"}, (dialog, which) -> {
                        if (which == 0) {
                            // 导出到自定义目录
                            saveQRCodeToCustomPath(bmp, name);
                        } else {
                            // 分享
                            shareQRCodeBitmap(bmp, name);
                        }
                    })
                    .show();

        } catch (Exception e) {
            XLog.e("生成二维码失败: " + e.getMessage());
            Toast.makeText(this, "生成二维码失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存二维码到默认目录
     */
    private void saveQRCodeToCustomPath(android.graphics.Bitmap bitmap, String name) {
        try {
            String fileName = "QR_" + name + "_" + System.currentTimeMillis() + ".png";

            String savePath = com.river.gowithamap.utils.FileSaveManager.getSavePath(this);
            java.io.File file = new java.io.File(savePath, fileName);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Toast.makeText(this, "二维码已保存到: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            XLog.i("二维码已保存到: " + file.getAbsolutePath());
        } catch (Exception e) {
            XLog.e("保存二维码失败: " + e.getMessage());
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 分享二维码
     */
    private void shareQRCodeBitmap(android.graphics.Bitmap bitmap, String name) {
        try {
            String fileName = "QR_" + name + "_" + System.currentTimeMillis() + ".png";
            java.io.File file = new java.io.File(getCacheDir(), fileName);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            ShareUtils.shareFile(this, file, "二维码");
        } catch (Exception e) {
            XLog.e("分享二维码失败: " + e.getMessage());
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存坐标到文件
     */
    private void saveLocationToFile(String name, JSONObject jsonData) {
        try {
            String fileName = name + "_" + System.currentTimeMillis() + ".json";

            String savePath = com.river.gowithamap.utils.FileSaveManager.getSavePath(this);
            java.io.File file = new java.io.File(savePath, fileName);

            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(jsonData.toString(2));
            writer.close();

            Toast.makeText(this, "已保存到: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            ShareUtils.shareFile(this, file, "坐标数据");

        } catch (Exception e) {
            XLog.e("保存文件失败: " + e.getMessage());
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存区域到文件
     */
    private void saveRegionToFile(String name, JSONObject jsonData) {
        try {
            String fileName = name + "_" + System.currentTimeMillis() + ".json";

            String savePath = com.river.gowithamap.utils.FileSaveManager.getSavePath(this);
            java.io.File file = new java.io.File(savePath, fileName);

            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(jsonData.toString(2));
            writer.close();

            Toast.makeText(this, "已保存到: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            ShareUtils.shareFile(this, file, "区域数据");

        } catch (Exception e) {
            XLog.e("保存文件失败: " + e.getMessage());
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从文件导入
     */
    private void importFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
        startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE);
    }

    /**
     * 从二维码导入
     */
    private void importFromQRCode() {
        // 显示选项对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("导入二维码")
                .setItems(new String[]{"扫描二维码", "从图片选择"}, (dialog, which) -> {
                    if (which == 0) {
                        // 启动二维码扫描Activity
                        Intent intent = new Intent(this, QRCodeScanActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_SCAN_QR);
                    } else {
                        // 从图片选择二维码
                        pickQRCodeFromImage();
                    }
                })
                .show();
    }

    /**
     * 从图片选择二维码
     */
    private void pickQRCodeFromImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_QR_IMAGE);
    }

    private static final int REQUEST_CODE_PICK_QR_IMAGE = 1003;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_IMPORT_FILE && data != null) {
                // 处理文件导入
                Uri uri = data.getData();
                if (uri != null) {
                    processImportedFile(uri);
                }
            } else if (requestCode == REQUEST_CODE_SCAN_QR && data != null) {
                // 处理二维码扫描结果
                String qrContent = data.getStringExtra("qr_content");
                if (qrContent != null) {
                    processQRContent(qrContent);
                }
            } else if (requestCode == REQUEST_CODE_PICK_QR_IMAGE && data != null) {
                // 处理从图片选择的二维码
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    decodeQRCodeFromImage(imageUri);
                }
            }
        }
    }

    /**
     * 从图片解码二维码
     */
    private void decodeQRCodeFromImage(Uri imageUri) {
        try {
            // 加载图片
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(imageUri));
            if (bitmap == null) {
                Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // 解码二维码
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            com.google.zxing.RGBLuminanceSource source = new com.google.zxing.RGBLuminanceSource(width, height, pixels);
            com.google.zxing.BinaryBitmap binaryBitmap = new com.google.zxing.BinaryBitmap(
                    new com.google.zxing.common.HybridBinarizer(source));

            com.google.zxing.MultiFormatReader reader = new com.google.zxing.MultiFormatReader();
            com.google.zxing.Result result = reader.decode(binaryBitmap);

            if (result != null) {
                String content = result.getText();
                processQRContent(content);
            } else {
                Toast.makeText(this, "未在图片中找到二维码", Toast.LENGTH_SHORT).show();
            }

        } catch (com.google.zxing.NotFoundException e) {
            Toast.makeText(this, "未在图片中找到二维码", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            XLog.e("解码二维码失败: " + e.getMessage());
            Toast.makeText(this, "解码二维码失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理导入的文件
     */
    private void processImportedFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                inputStream.close();
                
                String content = stringBuilder.toString();
                processQRContent(content);
            }
        } catch (Exception e) {
            XLog.e("导入文件失败: " + e.getMessage());
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理二维码内容
     */
    private void processQRContent(String content) {
        try {
            JSONObject json = new JSONObject(content);
            String type = json.optString("type");
            
            if ("circle".equals(type)) {
                // 导入圆形区域
                String name = json.optString("name", "导入的区域");
                double lat = json.optDouble("lat", 0);
                double lon = json.optDouble("lon", 0);
                double radius = json.optDouble("radius", 1000);
                
                if (lat != 0 && lon != 0) {
                    // 保存到数据库
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_NAME, name);
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT, String.valueOf(lat));
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON, String.valueOf(lon));
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_RADIUS, String.valueOf(radius));
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                    
                    DataBaseFavoriteRegions.saveFavoriteRegion(mFavoriteRegionsDB, contentValues);
                    loadFavoriteRegions();
                    
                    Toast.makeText(this, "区域导入成功", Toast.LENGTH_SHORT).show();
                    
                    // 切换到区域标签页
                    showRegionsView();
                } else {
                    Toast.makeText(this, "无效的区域数据", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "未知的数据类型", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            XLog.e("解析导入数据失败: " + e.getMessage());
            Toast.makeText(this, "数据格式错误", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_favorites, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_all) {
            if (isShowingCoordinates) {
                // 清空坐标收藏
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("清空收藏")
                        .setMessage("确定要清空所有坐标收藏吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            mFavoritesDB.delete(DataBaseFavorites.TABLE_NAME, null, null);
                            loadFavorites();
                            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                // 清空区域收藏
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("清空区域收藏")
                        .setMessage("确定要清空所有区域收藏吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            mFavoriteRegionsDB.delete(DataBaseFavoriteRegions.TABLE_NAME, null, null);
                            loadFavoriteRegions();
                            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFavoritesDB != null) {
            mFavoritesDB.close();
        }
        if (mFavoriteRegionsDB != null) {
            mFavoriteRegionsDB.close();
        }
    }
}