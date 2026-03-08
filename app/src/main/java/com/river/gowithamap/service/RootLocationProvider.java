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
 */

package com.river.gowithamap.service;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.river.gowithamap.utils.RootUtils;

import java.lang.reflect.Method;

/**
 * ROOT 级别位置提供者
 * 通过 ROOT 权限直接操作系统位置服务，绕过 Mock Location API
 */
public class RootLocationProvider {

    private static final String TAG = "RootLocationProvider";

    // 位置提供者名称
    public static final String PROVIDER_GPS = LocationManager.GPS_PROVIDER;
    public static final String PROVIDER_NETWORK = LocationManager.NETWORK_PROVIDER;
    public static final String PROVIDER_PASSIVE = LocationManager.PASSIVE_PROVIDER;

    // Android 12+ 的 ProviderProperties 常量
    private static final int POWER_USAGE_LOW = 1;
    private static final int ACCURACY_FINE = 1;
    private static final int ACCURACY_COARSE = 2;

    private Context mContext;
    private LocationManager mLocationManager;
    private Handler mHandler;
    private boolean mIsRunning = false;

    // 当前位置
    private double mCurLat = 36.667662;
    private double mCurLng = 117.027707;
    private double mCurAlt = 55.0;
    private float mAccuracy = 3.0f;
    private long mUpdateInterval = 1000; // 毫秒

    // 位置更新任务
    private Runnable mLocationUpdateTask;

    public RootLocationProvider(Context context) {
        mContext = context.getApplicationContext();
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());

        initLocationUpdateTask();
    }

    /**
     * 初始化位置更新任务
     */
    private void initLocationUpdateTask() {
        mLocationUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (mIsRunning) {
                    injectLocation();
                    mHandler.postDelayed(this, mUpdateInterval);
                }
            }
        };
    }

    /**
     * 检查 ROOT 权限
     */
    public boolean checkRootAccess() {
        if (!RootUtils.isDeviceRooted()) {
            XLog.e(TAG + ": Device is not rooted");
            return false;
        }

        if (!RootUtils.hasRootAccess()) {
            XLog.e(TAG + ": No root access granted");
            return false;
        }

        return true;
    }

    /**
     * 设置测试提供者（通过反射和 ROOT）
     */
    public boolean setupTestProviders() {
        try {
            // 尝试使用反射调用系统方法
            removeTestProvider(PROVIDER_GPS);
            removeTestProvider(PROVIDER_NETWORK);

            addTestProvider(PROVIDER_GPS);
            addTestProvider(PROVIDER_NETWORK);

            setTestProviderEnabled(PROVIDER_GPS, true);
            setTestProviderEnabled(PROVIDER_NETWORK, true);

            XLog.i(TAG + ": Test providers setup successfully");
            return true;

        } catch (Exception e) {
            XLog.e(TAG + ": Failed to setup test providers: " + e.getMessage());
            return false;
        }
    }

    /**
     * 移除测试提供者
     */
    public void removeTestProviders() {
        try {
            removeTestProvider(PROVIDER_GPS);
            removeTestProvider(PROVIDER_NETWORK);
            XLog.i(TAG + ": Test providers removed");
        } catch (Exception e) {
            XLog.e(TAG + ": Error removing test providers: " + e.getMessage());
        }
    }

    /**
     * 开始位置模拟
     */
    public boolean start() {
        if (!checkRootAccess()) {
            return false;
        }

        if (!setupTestProviders()) {
            return false;
        }

        mIsRunning = true;
        mHandler.post(mLocationUpdateTask);

        XLog.i(TAG + ": Root location provider started");
        return true;
    }

    /**
     * 停止位置模拟
     */
    public void stop() {
        mIsRunning = false;
        mHandler.removeCallbacks(mLocationUpdateTask);
        removeTestProviders();
        XLog.i(TAG + ": Root location provider stopped");
    }

    /**
     * 设置位置
     */
    public void setLocation(double lng, double lat, double alt) {
        mCurLng = lng;
        mCurLat = lat;
        mCurAlt = alt;

        // 立即注入一次
        if (mIsRunning) {
            injectLocation();
        }
    }

    /**
     * 注入位置到系统
     */
    private void injectLocation() {
        try {
            // 创建 GPS 位置
            Location gpsLocation = createLocation(PROVIDER_GPS);
            setTestProviderLocation(PROVIDER_GPS, gpsLocation);

            // 创建 Network 位置
            Location networkLocation = createLocation(PROVIDER_NETWORK);
            setTestProviderLocation(PROVIDER_NETWORK, networkLocation);

        } catch (Exception e) {
            XLog.e(TAG + ": Failed to inject location: " + e.getMessage());
        }
    }

    /**
     * 创建位置对象
     */
    private Location createLocation(String provider) {
        Location location = new Location(provider);
        location.setLatitude(mCurLat);
        location.setLongitude(mCurLng);
        location.setAltitude(mCurAlt);
        location.setAccuracy(mAccuracy);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        location.setBearing(0f);
        location.setSpeed(0f);
        return location;
    }

    /**
     * 添加测试提供者（反射）
     */
    private void addTestProvider(String provider) throws Exception {
        try {
            // Android 12+ (API 31+) 使用 ProviderProperties
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                try {
                    // 尝试使用新的 API
                    Object properties = createProviderProperties();
                    Method method = LocationManager.class.getMethod("addTestProvider",
                            String.class, Class.forName("android.location.provider.ProviderProperties"));
                    method.invoke(mLocationManager, provider, properties);
                } catch (Exception e) {
                    // 回退到旧方法
                    addTestProviderLegacy(provider);
                }
            } else {
                addTestProviderLegacy(provider);
            }
        } catch (Exception e) {
            XLog.e(TAG + ": addTestProvider failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 旧版添加测试提供者
     */
    private void addTestProviderLegacy(String provider) throws Exception {
        // Android 6.0 - 11
        try {
            Method method = LocationManager.class.getMethod("addTestProvider",
                    String.class, boolean.class, boolean.class, boolean.class,
                    boolean.class, boolean.class, boolean.class, boolean.class,
                    int.class, int.class);
            method.invoke(mLocationManager, provider, false, false, false,
                    false, true, true, true, 1, 1);
        } catch (NoSuchMethodException e) {
            // 更早版本
            Method method = LocationManager.class.getMethod("addTestProvider",
                    String.class, boolean.class, boolean.class, boolean.class,
                    boolean.class, boolean.class, boolean.class, boolean.class,
                    int.class, int.class);
            method.invoke(mLocationManager, provider, false, false, false,
                    false, true, true, true, 1, 1);
        }
    }

    /**
     * 创建 ProviderProperties（Android 12+）
     */
    private Object createProviderProperties() throws Exception {
        // 使用 Builder 模式创建 ProviderProperties
        Class<?> propertiesClass = Class.forName("android.location.provider.ProviderProperties");
        Class<?> builderClass = Class.forName("android.location.provider.ProviderProperties$Builder");

        Object builder = builderClass.newInstance();

        // 设置属性
        Method setPowerUsage = builderClass.getMethod("setPowerUsage", int.class);
        setPowerUsage.invoke(builder, POWER_USAGE_LOW);

        Method setAccuracy = builderClass.getMethod("setAccuracy", int.class);
        setAccuracy.invoke(builder, ACCURACY_FINE);

        Method setSupportsAltitude = builderClass.getMethod("setHasAltitudeSupport", boolean.class);
        setSupportsAltitude.invoke(builder, true);

        Method setSupportsSpeed = builderClass.getMethod("setHasSpeedSupport", boolean.class);
        setSupportsSpeed.invoke(builder, true);

        Method setSupportsBearing = builderClass.getMethod("setHasBearingSupport", boolean.class);
        setSupportsBearing.invoke(builder, true);

        // 构建
        Method build = builderClass.getMethod("build");
        return build.invoke(builder);
    }

    /**
     * 移除测试提供者（反射）
     */
    private void removeTestProvider(String provider) {
        try {
            Method method = LocationManager.class.getMethod("removeTestProvider", String.class);
            method.invoke(mLocationManager, provider);
        } catch (Exception e) {
            // 可能提供者不存在，忽略错误
            Log.d(TAG, "removeTestProvider: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用测试提供者
     */
    private void setTestProviderEnabled(String provider, boolean enabled) throws Exception {
        try {
            Method method = LocationManager.class.getMethod("setTestProviderEnabled", String.class, boolean.class);
            method.invoke(mLocationManager, provider, enabled);
        } catch (NoSuchMethodException e) {
            // 某些版本可能没有这个方法的公开版本
            XLog.w(TAG + ": setTestProviderEnabled not available");
        }
    }

    /**
     * 设置测试提供者位置
     */
    private void setTestProviderLocation(String provider, Location location) throws Exception {
        Method method = LocationManager.class.getMethod("setTestProviderLocation", String.class, Location.class);
        method.invoke(mLocationManager, provider, location);
    }

    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * 设置更新间隔
     */
    public void setUpdateInterval(long intervalMs) {
        mUpdateInterval = intervalMs;
    }

    /**
     * 设置精度
     */
    public void setAccuracy(float accuracy) {
        mAccuracy = accuracy;
    }
}
