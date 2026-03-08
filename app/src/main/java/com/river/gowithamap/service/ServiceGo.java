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
 *
 * This file is based on GoGoGo (影梭) by ZCShou
 * Original project: https://github.com/ZCShou/GoGoGo
 * Modified to use Amap SDK instead of Baidu Map SDK
 */

package com.river.gowithamap.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.elvishew.xlog.XLog;
import com.river.gowithamap.MainActivity;
import com.river.gowithamap.R;
import com.river.gowithamap.joystick.JoyStick;

public class ServiceGo extends Service {
    // SharedPreferences 常量
    private static final String PREFS_NAME = "ServiceGoPrefs";
    private static final String PREF_LAT = "last_latitude";
    private static final String PREF_LNG = "last_longitude";
    private static final String PREF_ALT = "last_altitude";
    
    // 定位相关变量
    public static final double DEFAULT_LAT = 36.667662;
    public static final double DEFAULT_LNG = 117.027707;
    public static final double DEFAULT_ALT = 55.0D;
    public static final float DEFAULT_BEA = 0.0F;
    private double mCurLat = DEFAULT_LAT;
    private double mCurLng = DEFAULT_LNG;
    private double mCurAlt = DEFAULT_ALT;
    private float mCurBea = DEFAULT_BEA;
    private double mSpeed = 1.2;        /* 默认的速度，单位 m/s */
    private static final int HANDLER_MSG_ID = 0;
    private static final String SERVICE_GO_HANDLER_NAME = "ServiceGoLocation";
    private LocationManager mLocManager;
    private HandlerThread mLocHandlerThread;
    private Handler mLocHandler;
    private boolean isStop = false;
    // 通知栏消息
    private static final int SERVICE_GO_NOTE_ID = 1;
    private static final String SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW = "ShowJoyStick";
    private static final String SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE = "HideJoyStick";
    private static final String SERVICE_GO_NOTE_CHANNEL_ID = "SERVICE_GO_NOTE";
    private static final String SERVICE_GO_NOTE_CHANNEL_NAME = "SERVICE_GO_NOTE";
    private NoteActionReceiver mActReceiver;
    // 摇杆相关
    private JoyStick mJoyStick;
    // ROOT 模式相关
    private RootLocationProvider mRootLocationProvider;
    private boolean mUseRootMode = false;

    private final ServiceGoBinder mBinder = new ServiceGoBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 检查是否启用 ROOT 模式
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mUseRootMode = prefs.getBoolean("use_root_mode", false);
        XLog.i("SERVICEGO: Root mode: " + mUseRootMode);

        mLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (mUseRootMode) {
            // ROOT 模式：使用 RootLocationProvider
            initRootMode();
        } else {
            // 普通模式：使用标准 Mock Location API
            removeTestProviderNetwork();
            addTestProviderNetwork();
            removeTestProviderGPS();
            addTestProviderGPS();
            initGoLocation();
        }

        initNotification();
        initJoyStick();
    }

    /**
     * 初始化 ROOT 模式
     */
    private void initRootMode() {
        mRootLocationProvider = new RootLocationProvider(this);
        if (mRootLocationProvider.checkRootAccess()) {
            boolean success = mRootLocationProvider.start();
            if (success) {
                XLog.i("SERVICEGO: Root mode initialized successfully");
            } else {
                XLog.e("SERVICEGO: Failed to start root provider, falling back to normal mode");
                mUseRootMode = false;
            }
        } else {
            XLog.e("SERVICEGO: Root access not available, falling back to normal mode");
            mUseRootMode = false;
            // 回退到普通模式
            removeTestProviderNetwork();
            addTestProviderNetwork();
            removeTestProviderGPS();
            addTestProviderGPS();
            initGoLocation();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        if (intent == null) {
            XLog.w("SERVICEGO: onStartCommand: intent is null, restoring from SharedPreferences");
            // 从 SharedPreferences 恢复位置
            mCurLng = Double.longBitsToDouble(prefs.getLong(PREF_LNG, Double.doubleToLongBits(DEFAULT_LNG)));
            mCurLat = Double.longBitsToDouble(prefs.getLong(PREF_LAT, Double.doubleToLongBits(DEFAULT_LAT)));
            mCurAlt = Double.longBitsToDouble(prefs.getLong(PREF_ALT, Double.doubleToLongBits(DEFAULT_ALT)));
            XLog.i("SERVICEGO: Restored location from prefs: " + mCurLat + ", " + mCurLng);
        } else {
            // 正常启动，获取位置并保存到 SharedPreferences
            mCurLng = intent.getDoubleExtra(MainActivity.LNG_MSG_ID, DEFAULT_LNG);
            mCurLat = intent.getDoubleExtra(MainActivity.LAT_MSG_ID, DEFAULT_LAT);
            mCurAlt = intent.getDoubleExtra(MainActivity.ALT_MSG_ID, DEFAULT_ALT);
            
            // 保存到 SharedPreferences
            prefs.edit()
                .putLong(PREF_LNG, Double.doubleToLongBits(mCurLng))
                .putLong(PREF_LAT, Double.doubleToLongBits(mCurLat))
                .putLong(PREF_ALT, Double.doubleToLongBits(mCurAlt))
                .apply();
            XLog.i("SERVICEGO: Saved location to prefs: " + mCurLat + ", " + mCurLng);
        }

        if (mJoyStick != null) {
            mJoyStick.setCurrentPosition(mCurLng, mCurLat, mCurAlt);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isStop = true;
        mLocHandler.removeMessages(HANDLER_MSG_ID);
        mLocHandlerThread.quit();

        mJoyStick.destroy();

        if (mUseRootMode && mRootLocationProvider != null) {
            // ROOT 模式：停止 RootLocationProvider
            mRootLocationProvider.stop();
        } else {
            // 普通模式：清理标准 Mock Location API
            removeTestProviderNetwork();
            removeTestProviderGPS();
        }

        unregisterReceiver(mActReceiver);
        stopForeground(STOP_FOREGROUND_REMOVE);

        super.onDestroy();
    }

    private void initNotification() {
        mActReceiver = new NoteActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW);
        filter.addAction(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE);
        // Android 14+ (API 34) 需要指定接收器导出标志，使用数值 4 表示 RECEIVER_NOT_EXPORTED
        if (Build.VERSION.SDK_INT >= 34) {
            registerReceiver(mActReceiver, filter, 4);
        } else {
            registerReceiver(mActReceiver, filter);
        }

        NotificationChannel mChannel = new NotificationChannel(SERVICE_GO_NOTE_CHANNEL_ID, SERVICE_GO_NOTE_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(mChannel);
        }

        //准备intent
        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPI = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent showIntent = new Intent(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW);
        PendingIntent showPendingPI = PendingIntent.getBroadcast(this, 0, showIntent, PendingIntent.FLAG_IMMUTABLE);
        Intent hideIntent = new Intent(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE);
        PendingIntent hidePendingPI = PendingIntent.getBroadcast(this, 0, hideIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, SERVICE_GO_NOTE_CHANNEL_ID)
                .setChannelId(SERVICE_GO_NOTE_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_service_tips))
                .setContentIntent(clickPI)
                .addAction(new NotificationCompat.Action(null, getResources().getString(R.string.note_show), showPendingPI))
                .addAction(new NotificationCompat.Action(null, getResources().getString(R.string.note_hide), hidePendingPI))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        // Android 14+ 需要指定前台服务类型
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(SERVICE_GO_NOTE_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(SERVICE_GO_NOTE_ID, notification);
        }
    }

    private void initJoyStick() {
        mJoyStick = new JoyStick(this);
        mJoyStick.setListener(new JoyStick.JoyStickClickListener() {
            @Override
            public void onMoveInfo(double speed, double disLng, double disLat, double angle) {
                mSpeed = speed;
                // 根据当前的经纬度和距离，计算下一个经纬度
                // Latitude: 1 deg = 110.574 km // 纬度的每度的距离大约为 110.574km
                // Longitude: 1 deg = 111.320*cos(latitude) km  // 经度的每度的距离从0km到111km不等
                // 具体见：http://wp.mlab.tw/?p=2200
                mCurLng += disLng / (111.320 * Math.cos(Math.abs(mCurLat) * Math.PI / 180));
                mCurLat += disLat / 110.574;
                mCurBea = (float) angle;
            }

            @Override
            public void onPositionInfo(double lng, double lat, double alt) {
                mCurLng = lng;
                mCurLat = lat;
                mCurAlt = alt;
            }
        });
        mJoyStick.show();
    }

    private void initGoLocation() {
        // 创建 HandlerThread 实例，第一个参数是线程的名字
        mLocHandlerThread = new HandlerThread(SERVICE_GO_HANDLER_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        // 启动 HandlerThread 线程
        mLocHandlerThread.start();
        // Handler 对象与 HandlerThread 的 Looper 对象的绑定
        mLocHandler = new Handler(mLocHandlerThread.getLooper()) {
            // 这里的Handler对象可以看作是绑定在HandlerThread子线程中，所以handlerMessage里的操作是在子线程中运行的
            @Override
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(100);

                    if (!isStop) {
                        setLocationNetwork();
                        setLocationGPS();

                        sendEmptyMessage(HANDLER_MSG_ID);
                    }
                } catch (InterruptedException e) {
                    XLog.e("SERVICEGO: ERROR - handleMessage");
                    Thread.currentThread().interrupt();
                }
            }
        };

        mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
    }

    private void removeTestProviderGPS() {
        try {
            if (mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - removeTestProviderGPS");
        }
    }

    // 注意下面临时添加 @SuppressLint("wrongconstant") 以处理 addTestProvider 参数值的 lint 错误
    @SuppressLint("wrongconstant")
    private void addTestProviderGPS() {
        try {
            // 注意，由于 android api 问题，下面的参数会提示错误(以下参数是通过相关API获取的真实GPS参数，不是随便写的)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else {
                @SuppressWarnings("deprecation")
                int powerHigh = Criteria.POWER_HIGH;
                @SuppressWarnings("deprecation")
                int accuracyFine = Criteria.ACCURACY_FINE;
                mLocManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, powerHigh, accuracyFine);
            }
            if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - addTestProviderGPS");
        }
    }

    private void setLocationGPS() {
        try {
            // 尽可能模拟真实的 GPS 数据
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            @SuppressWarnings("deprecation")
            int accuracyFine = Criteria.ACCURACY_FINE;
            loc.setAccuracy(accuracyFine);    // 设定此位置的估计水平精度，以米为单位。
            loc.setAltitude(mCurAlt);                     // 设置高度，在 WGS 84 参考坐标系中的米
            loc.setBearing(mCurBea);                       // 方向（度）
            loc.setLatitude(mCurLat);                   // 纬度（度）
            loc.setLongitude(mCurLng);                  // 经度（度）
            loc.setTime(System.currentTimeMillis());    // 本地时间
            loc.setSpeed((float) mSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            Bundle bundle = new Bundle();
            bundle.putInt("satellites", 7);
            loc.setExtras(bundle);

            mLocManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - setLocationGPS");
        }
    }

    private void removeTestProviderNetwork() {
        try {
            if (mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
                mLocManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - removeTestProviderNetwork");
        }
    }

    // 注意下面临时添加 @SuppressLint("wrongconstant") 以处理 addTestProvider 参数值的 lint 错误
    @SuppressLint("wrongconstant")
    private void addTestProviderNetwork() {
        try {
            // 注意，由于 android api 问题，下面的参数会提示错误(以下参数是通过相关API获取的真实NETWORK参数，不是随便写的)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mLocManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, ProviderProperties.POWER_USAGE_LOW, ProviderProperties.ACCURACY_COARSE);
            } else {
                @SuppressWarnings("deprecation")
                int powerLow = Criteria.POWER_LOW;
                @SuppressWarnings("deprecation")
                int accuracyCoarse = Criteria.ACCURACY_COARSE;
                mLocManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                        true, true, true, true,
                        true, powerLow, accuracyCoarse);
            }
            if (!mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            }
        } catch (SecurityException e) {
            XLog.e("SERVICEGO: ERROR - addTestProviderNetwork");
        }
    }

    private void setLocationNetwork() {
        try {
            // 尽可能模拟真实的 NETWORK 数据
            Location loc = new Location(LocationManager.NETWORK_PROVIDER);
            @SuppressWarnings("deprecation")
            int accuracyCoarse = Criteria.ACCURACY_COARSE;
            loc.setAccuracy(accuracyCoarse);  // 设定此位置的估计水平精度，以米为单位。
            loc.setAltitude(mCurAlt);                     // 设置高度，在 WGS 84 参考坐标系中的米
            loc.setBearing(mCurBea);                       // 方向（度）
            loc.setLatitude(mCurLat);                   // 纬度（度）
            loc.setLongitude(mCurLng);                  // 经度（度）
            loc.setTime(System.currentTimeMillis());    // 本地时间
            loc.setSpeed((float) mSpeed);
            loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            mLocManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, loc);
        } catch (Exception e) {
            XLog.e("SERVICEGO: ERROR - setLocationNetwork");
        }
    }

    public class NoteActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW)) {
                    mJoyStick.show();
                }

                if (action.equals(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE)) {
                    mJoyStick.hide();
                }
            }
        }
    }

    public class ServiceGoBinder extends Binder {
        public void setPosition(double lng, double lat, double alt) {
            mCurLng = lng;
            mCurLat = lat;
            mCurAlt = alt;

            if (mUseRootMode && mRootLocationProvider != null) {
                // ROOT 模式：使用 RootLocationProvider
                mRootLocationProvider.setLocation(lng, lat, alt);
            } else {
                // 普通模式：使用标准 Handler
                mLocHandler.removeMessages(HANDLER_MSG_ID);
                mLocHandler.sendEmptyMessage(HANDLER_MSG_ID);
            }

            if (mJoyStick != null) {
                mJoyStick.setCurrentPosition(mCurLng, mCurLat, mCurAlt);
            }

            // 保存到 SharedPreferences
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong(PREF_LNG, Double.doubleToLongBits(mCurLng))
                .putLong(PREF_LAT, Double.doubleToLongBits(mCurLat))
                .putLong(PREF_ALT, Double.doubleToLongBits(mCurAlt))
                .apply();
        }

        public double getLongitude() {
            return mCurLng;
        }

        public double getLatitude() {
            return mCurLat;
        }

        public double getAltitude() {
            return mCurAlt;
        }

        /**
         * 检查是否使用 ROOT 模式
         */
        public boolean isRootMode() {
            return mUseRootMode;
        }

        /**
         * 设置 ROOT 模式
         */
        public void setRootMode(boolean enabled) {
            if (mUseRootMode == enabled) {
                return; // 没有变化
            }

            // 保存设置
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean("use_root_mode", enabled)
                .apply();

            // 重启服务以应用新设置
            XLog.i("SERVICEGO: Root mode changed to " + enabled + ", restarting service...");
            // 实际重启需要在 Activity 中处理
        }
    }
}


