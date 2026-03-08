package com.river.gowithamap;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.elvishew.xlog.XLog;
import com.river.gowithamap.database.DataBasePinnedLocations;
import com.river.gowithamap.utils.GoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图交互辅助类
 * 管理圆、坐标点的显示和交互
 */
public class MapInteractionHelper {
    
    private Context mContext;
    private AMap mAMap;
    
    // 多点定位圆
    private List<Circle> mMultiPointCircles = new ArrayList<>();
    private List<Marker> mMultiPointMarkers = new ArrayList<>();
    private List<LatLng> mMultiPointCenters = new ArrayList<>();
    private List<Double> mMultiPointRadius = new ArrayList<>();
    
    // 当前坐标点
    private Marker mCurrentLocationMarker = null;
    private LatLng mCurrentLocation = null;
    
    // 固定坐标点
    private List<Marker> mPinnedLocationMarkers = new ArrayList<>();
    private List<Map<String, Object>> mPinnedLocationData = new ArrayList<>();
    private SQLiteDatabase mPinnedLocationsDB;
    
    // 固定圆
    private List<Circle> mPinnedCircles = new ArrayList<>();
    private List<Marker> mPinnedCircleMarkers = new ArrayList<>();
    private List<Map<String, Object>> mPinnedCircleData = new ArrayList<>();
    
    // 当前选中的圆
    private Circle mSelectedCircle = null;
    private int mSelectedCircleIndex = -1;
    
    public MapInteractionHelper(Context context, AMap amap, SQLiteDatabase pinnedLocationsDB) {
        mContext = context;
        mAMap = amap;
        mPinnedLocationsDB = pinnedLocationsDB;
    }
    
    /**
     * 清除所有临时圆（非固定）
     */
    public void clearTemporaryCircles() {
        for (Circle circle : mMultiPointCircles) {
            if (circle != null) circle.remove();
        }
        mMultiPointCircles.clear();
        
        for (Marker marker : mMultiPointMarkers) {
            if (marker != null) marker.remove();
        }
        mMultiPointMarkers.clear();
        
        mMultiPointCenters.clear();
        mMultiPointRadius.clear();
        
        mSelectedCircle = null;
        mSelectedCircleIndex = -1;
        
        XLog.i("临时圆已清除");
    }
    
    /**
     * 添加可交互圆
     */
    public void addInteractiveCircle(double lat, double lon, double radiusM, String name) {
        LatLng center = new LatLng(lat, lon);
        mMultiPointCenters.add(center);
        mMultiPointRadius.add(radiusM);
        
        Circle circle = mAMap.addCircle(new CircleOptions()
            .center(center)
            .radius(radiusM)
            .fillColor(0x55FFFF00)
            .strokeColor(0xFFCCCC00)
            .strokeWidth(2));
        mMultiPointCircles.add(circle);
        
        addCircleCenterMarker(center, name, radiusM);
        
        XLog.i("添加圆: " + name + " (" + lat + ", " + lon + "), 半径: " + radiusM + "米");
    }
    
    /**
     * 添加圆心标记
     */
    private void addCircleCenterMarker(LatLng center, String name, double radiusM) {
        android.widget.LinearLayout markerView = new android.widget.LinearLayout(mContext);
        markerView.setOrientation(android.widget.LinearLayout.VERTICAL);
        markerView.setGravity(Gravity.CENTER);
        
        TextView nameView = new TextView(mContext);
        nameView.setText(name);
        nameView.setTextSize(11);
        nameView.setTextColor(Color.BLACK);
        nameView.setBackgroundColor(0xAAFFFFFF);
        nameView.setPadding(8, 3, 8, 3);
        nameView.setGravity(Gravity.CENTER);
        markerView.addView(nameView);
        
        TextView radiusView = new TextView(mContext);
        double radiusKm = radiusM / 1000.0;
        radiusView.setText(String.format("%.2f km", radiusKm));
        radiusView.setTextSize(10);
        radiusView.setTextColor(Color.DKGRAY);
        radiusView.setBackgroundColor(0xAAFFFFFF);
        radiusView.setPadding(8, 2, 8, 2);
        radiusView.setGravity(Gravity.CENTER);
        markerView.addView(radiusView);
        
        markerView.measure(android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
                          android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        
        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);
        
        BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(bitmap);
        
        Marker marker = mAMap.addMarker(new MarkerOptions()
            .position(center)
            .icon(customIcon)
            .anchor(0.5f, 0.5f)
            .setFlat(true));
        
        mMultiPointMarkers.add(marker);
    }
    
    /**
     * 设置当前坐标点
     */
    public void setCurrentLocation(LatLng point, String name) {
        // 移除旧标记
        if (mCurrentLocationMarker != null) {
            mCurrentLocationMarker.remove();
        }
        
        mCurrentLocation = point;
        
        // 添加新标记
        mCurrentLocationMarker = mAMap.addMarker(new MarkerOptions()
            .position(point)
            .title(name != null ? name : "选定位置")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding)));
        
        XLog.i("设置当前位置: " + point.latitude + ", " + point.longitude);
    }
    
    /**
     * 固定当前坐标点
     */
    public void pinCurrentLocation(String name) {
        if (mCurrentLocation == null || mCurrentLocationMarker == null) return;
        
        // 保存到数据库
        ContentValues values = new ContentValues();
        values.put(DataBasePinnedLocations.DB_COLUMN_NAME, name);
        values.put(DataBasePinnedLocations.DB_COLUMN_LATITUDE, String.valueOf(mCurrentLocation.latitude));
        values.put(DataBasePinnedLocations.DB_COLUMN_LONGITUDE, String.valueOf(mCurrentLocation.longitude));
        values.put(DataBasePinnedLocations.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
        
        DataBasePinnedLocations.savePinnedLocation(mPinnedLocationsDB, values);
        
        // 添加到固定列表
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("lat", mCurrentLocation.latitude);
        data.put("lon", mCurrentLocation.longitude);
        mPinnedLocationData.add(data);
        mPinnedLocationMarkers.add(mCurrentLocationMarker);
        
        // 清除当前标记引用（但不清除地图上的标记）
        mCurrentLocationMarker = null;
        mCurrentLocation = null;
        
        GoUtils.DisplayToast(mContext, "坐标点已固定: " + name);
        XLog.i("固定坐标点: " + name);
    }
    
    /**
     * 检查点击位置是否在圆内
     */
    public int checkClickOnCircle(LatLng point) {
        for (int i = 0; i < mMultiPointCircles.size(); i++) {
            LatLng center = mMultiPointCenters.get(i);
            double radius = mMultiPointRadius.get(i);
            
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                point.latitude, point.longitude,
                center.latitude, center.longitude,
                results);
            
            if (results[0] <= radius) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 检查点击位置是否在固定圆内
     */
    public int checkClickOnPinnedCircle(LatLng point) {
        for (int i = 0; i < mPinnedCircles.size(); i++) {
            Map<String, Object> data = mPinnedCircleData.get(i);
            double lat = (double) data.get("lat");
            double lon = (double) data.get("lon");
            double radius = (double) data.get("radius");
            
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                point.latitude, point.longitude,
                lat, lon,
                results);
            
            if (results[0] <= radius) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 检查点击位置是否在当前坐标点上
     */
    public boolean checkClickOnCurrentLocation(LatLng point) {
        if (mCurrentLocationMarker == null || mCurrentLocation == null) return false;
        
        float[] results = new float[1];
        android.location.Location.distanceBetween(
            point.latitude, point.longitude,
            mCurrentLocation.latitude, mCurrentLocation.longitude,
            results);
        
        return results[0] <= 50; // 50米范围内认为是点击在标记上
    }
    
    /**
     * 检查点击位置是否在固定坐标点上
     */
    public int checkClickOnPinnedLocation(LatLng point) {
        for (int i = 0; i < mPinnedLocationMarkers.size(); i++) {
            Marker marker = mPinnedLocationMarkers.get(i);
            if (marker == null) continue;
            
            LatLng markerPos = marker.getPosition();
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                point.latitude, point.longitude,
                markerPos.latitude, markerPos.longitude,
                results);
            
            if (results[0] <= 50) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 选中圆
     */
    public void selectCircle(int index) {
        if (index < 0 || index >= mMultiPointCircles.size()) return;
        
        // 恢复之前选中的圆
        if (mSelectedCircle != null) {
            mSelectedCircle.setStrokeColor(0xFFCCCC00);
            mSelectedCircle.setFillColor(0x55FFFF00);
        }
        
        mSelectedCircle = mMultiPointCircles.get(index);
        mSelectedCircleIndex = index;
        
        mSelectedCircle.setStrokeColor(0xFF0000FF);
        mSelectedCircle.setFillColor(0x550000FF);
        
        XLog.i("选中圆 #" + index);
    }
    
    /**
     * 获取选中的圆信息
     */
    public Map<String, Object> getSelectedCircleInfo() {
        if (mSelectedCircle == null || mSelectedCircleIndex < 0) return null;
        
        Map<String, Object> info = new HashMap<>();
        info.put("center", mMultiPointCenters.get(mSelectedCircleIndex));
        info.put("radius", mMultiPointRadius.get(mSelectedCircleIndex));
        info.put("index", mSelectedCircleIndex);
        return info;
    }
    
    /**
     * 获取当前坐标
     */
    public LatLng getCurrentLocation() {
        return mCurrentLocation;
    }
    
    /**
     * 获取固定坐标点数据
     */
    public List<Map<String, Object>> getPinnedLocations() {
        return mPinnedLocationData;
    }
    
    /**
     * 获取固定圆数据
     */
    public List<Map<String, Object>> getPinnedCircles() {
        return mPinnedCircleData;
    }
    
    /**
     * 加载固定坐标点
     */
    public void loadPinnedLocations() {
        List<Map<String, Object>> locations = DataBasePinnedLocations.getAllPinnedLocations(mPinnedLocationsDB);
        for (Map<String, Object> data : locations) {
            String name = (String) data.get(DataBasePinnedLocations.DB_COLUMN_NAME);
            double lat = Double.parseDouble((String) data.get(DataBasePinnedLocations.DB_COLUMN_LATITUDE));
            double lon = Double.parseDouble((String) data.get(DataBasePinnedLocations.DB_COLUMN_LONGITUDE));
            
            Marker marker = mAMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title(name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding)));
            
            mPinnedLocationMarkers.add(marker);
            mPinnedLocationData.add(data);
        }
        XLog.i("加载了 " + locations.size() + " 个固定坐标点");
    }
}