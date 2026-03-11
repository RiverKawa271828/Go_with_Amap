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

package com.river.gowithamap;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.river.gowithamap.service.ServiceGo;
import com.river.gowithamap.database.DataBaseFavorites;
import com.river.gowithamap.database.DataBaseFavoriteRegions;

import com.river.gowithamap.database.DataBaseHistorySearch;
import com.river.gowithamap.database.DataBaseSimulationRecords;
import com.river.gowithamap.database.DataBasePinnedCircles;
import com.river.gowithamap.database.DataBasePinnedLocations;
import com.river.gowithamap.utils.ShareUtils;
import com.river.gowithamap.utils.GoUtils;
import com.river.gowithamap.utils.MapCacheManager;
import com.river.gowithamap.utils.MapUtils;
import com.river.gowithamap.utils.FileSaveManager;

import com.elvishew.xlog.XLog;

import io.noties.markwon.Markwon;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends BaseActivity implements SensorEventListener {
    /* 对外 */
    public static final String LAT_MSG_ID = "LAT_VALUE";
    public static final String LNG_MSG_ID = "LNG_VALUE";
    public static final String ALT_MSG_ID = "ALT_VALUE";

    public static final String POI_NAME = "POI_NAME";
    public static final String POI_ADDRESS = "POI_ADDRESS";
    public static final String POI_LONGITUDE = "POI_LONGITUDE";
    public static final String POI_LATITUDE = "POI_LATITUDE";

    private OkHttpClient mOkHttpClient;
    private SharedPreferences sharedPreferences;

    /*============================== 主界面地图 相关 ==============================*/
    /************** 地图 *****************/
    public final static BitmapDescriptor mMapIndicator = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
    public static String mCurrentCity = null;
    private MapView mMapView;
    private static AMap mAMap = null;
    private static LatLng mMarkLatLngMap = new LatLng(36.547743718042415, 117.07018449827267); // 当前标记的地图点（高德坐标 GCJ02）
    private static String mMarkName = null;
    private GeocodeSearch mGeocodeSearch;
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetic;
    private float[] mAccValues = new float[3];//加速度传感器数据
    private float[] mMagValues = new float[3];//地磁传感器数据
    private final float[] mR = new float[9];//旋转矩阵，用来保存磁场和加速度的数据
    private final float[] mDirectionValues = new float[3];//模拟方向传感器的数据（原始数据为弧度）
    /************** 定位 *****************/
    private AMapLocationClient mLocClient = null;
    private double mCurrentLat = 0.0;       // 当前位置的高德纬度（GCJ02）
    private double mCurrentLon = 0.0;       // 当前位置的高德经度（GCJ02）
    private float mCurrentDirection = 0.0f;
    private boolean isFirstLoc = true; // 是否首次定位
    private boolean isMockServStart = false;
    private ServiceGo.ServiceGoBinder mServiceBinder;
    private ServiceConnection mConnection;
    private FloatingActionButton mButtonStart;
    /*============================== 历史记录 相关 ==============================*/
    private static SQLiteDatabase mSimulationRecordsDB;
    private SQLiteDatabase mSearchHistoryDB;
    /*============================== 收藏 相关 ==============================*/
    private SQLiteDatabase mFavoritesDB;
    /*============================== SearchView 相关 ==============================*/
    private SearchView searchView;
    private ListView mSearchList;
    private LinearLayout mSearchLayout;
    private ListView mSearchHistoryList;
    private LinearLayout mHistoryLayout;
    private MenuItem searchItem;
    private Inputtips mInputtips;
    /*============================== 更新 相关 ==============================*/
    private DownloadManager mDownloadManager = null;
    private long mDownloadId;
    private BroadcastReceiver mDownloadBdRcv;
    private String mUpdateFilename;
    
    /*============================== 多点定位 相关 ==============================*/
    private List<Circle> mMultiPointCircles = new ArrayList<>();
    private List<Marker> mMultiPointMarkers = new ArrayList<>();
    private List<LatLng> mMultiPointCenters = new ArrayList<>();
    private List<Double> mMultiPointRadius = new ArrayList<>();
    private Circle mIntersectionCircle = null;
    private Marker mIntersectionMarker = null;
    private Marker mIntersectionCenterMarker = null;
    
    /*============================== 圆交互 相关 ==============================*/
    // 圆的状态常量
    private static final int CIRCLE_STATE_NORMAL = 0;      // 正常状态（黄色）
    private static final int CIRCLE_STATE_SELECTED = 1;    // 选中状态（蓝色）
    private static final int CIRCLE_STATE_PINNED = 2;      // 固定状态（深蓝色）

    // 当前选中的圆（临时圆）
    private Circle mSelectedCircle = null;
    private int mSelectedCircleIndex = -1;
    private LatLng mSelectedCircleCenter = null;
    private double mSelectedCircleRadius = 0;
    private boolean mIsSelectedCirclePinned = false;  // 标记当前选中的圆是否已固定

    // 临时圆时间戳（用于管理临时区域，类似临时坐标）
    private static long sTempCircleTimestamp = 0;
    private long mSelectedCircleTimestamp = 0;

    // 固定圆管理
    private List<Circle> mPinnedCircles = new ArrayList<>();
    private List<Marker> mPinnedCircleMarkers = new ArrayList<>();
    private List<Map<String, Object>> mPinnedCircleData = new ArrayList<>();
    
    /*============================== 坐标点管理 相关 ==============================*/
    // 当前坐标点（未固定）
    private Marker mCurrentLocationMarker = null;
    private boolean mIsCurrentLocationPinned = false;
    // 临时坐标创建时间戳（用于比较哪个临时坐标先创建）
    private static long sTempMarkerTimestamp = 0;
    private long mCurrentLocationMarkerTimestamp = 0;

    // 固定坐标点管理
    private List<Marker> mPinnedLocationMarkers = new ArrayList<>();
    private List<Map<String, Object>> mPinnedLocationData = new ArrayList<>();
    private SQLiteDatabase mPinnedLocationsDB;
    
    // 数据库
    private SQLiteDatabase mFavoriteRegionsDB;
    private SQLiteDatabase mPinnedCirclesDB;
    
    // 导入请求码
    private static final int REQUEST_CODE_IMPORT_FILE = 2001;
    private static final int REQUEST_CODE_SCAN_QR = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        XLog.i("MainActivity: onCreate");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mOkHttpClient = new OkHttpClient();

        initNavigationView();

        initMap(savedInstanceState);

        initMapLocation();

        initMapButton();

        initGoBtn();

        mConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mServiceBinder = (ServiceGo.ServiceGoBinder)service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        initStoreHistory();
        
        initCircleDatabases();

        initSearchView();

        initUpdateVersion();

        // 暂时禁用升级检查
        // checkUpdateVersion(false);
    }

    @Override
    protected void onPause() {
        XLog.i("MainActivity: onPause");
        mMapView.onPause();
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        XLog.i("MainActivity: onResume");
        mMapView.onResume();
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI);

        // 重新启动定位服务
        if (mLocClient != null) {
            XLog.i("onResume: 重新启动定位服务");
            mLocClient.startLocation();
        }

        super.onResume();
    }

    @Override
    protected void onStop() {
        XLog.i("MainActivity: onStop");
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
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
            }
        }
    }

    @Override
    protected void onDestroy() {
        XLog.i("MainActivity: onDestroy");

        if (isMockServStart) {
            // 添加 try-catch 防止解绑未绑定的服务导致崩溃
            try {
                unbindService(mConnection); // 解绑服务，服务要记得解绑，不要造成内存泄漏
            } catch (IllegalArgumentException e) {
                XLog.w("Service was not registered in onDestroy: " + e.getMessage());
            }
            Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
            stopService(serviceGoIntent);
        }
        unregisterReceiver(mDownloadBdRcv);

        mSensorManager.unregisterListener(this);

        // 退出时销毁定位
        if (mLocClient != null) {
            mLocClient.stopLocation();
            mLocClient.onDestroy();
        }
        // 关闭定位图层
        if (mAMap != null) {
            mAMap.setMyLocationEnabled(false);
        }
        mMapView.onDestroy();

        // 清除临时坐标（临时坐标在应用关闭后应清除）
        clearTempLocation();

        //close db
        mSimulationRecordsDB.close();
        mSearchHistoryDB.close();
        if (mPinnedLocationsDB != null) {
            mPinnedLocationsDB.close();
        }
        if (mFavoriteRegionsDB != null) {
            mFavoriteRegionsDB.close();
        }
        if (mPinnedCirclesDB != null) {
            mPinnedCirclesDB.close();
        }
        if (mFavoritesDB != null) {
            mFavoritesDB.close();
        }

        super.onDestroy();
    }

    /**
     * 清除临时坐标和临时圆（应用关闭时调用）
     * 临时坐标/圆在应用关闭后应清除，固定坐标/圆不受影响
     */
    private void clearTempLocation() {
        XLog.i("清除临时坐标和临时圆");
        // 移除临时标记
        if (sTempMarker != null) {
            sTempMarker.remove();
            sTempMarker = null;
        }
        if (mCurrentLocationMarker != null && !mIsCurrentLocationPinned) {
            mCurrentLocationMarker.remove();
        }
        mCurrentLocationMarker = null;
        mIsCurrentLocationPinned = false;
        mMarkLatLngMap = null;
        // 清除坐标时间戳
        sTempMarkerTimestamp = 0;
        mCurrentLocationMarkerTimestamp = 0;

        // 清除临时圆（未固定的选中圆）
        if (mSelectedCircle != null && !mIsSelectedCirclePinned) {
            // 恢复圆为普通状态
            mSelectedCircle.setStrokeColor(0xFFCCCC00);
            mSelectedCircle.setFillColor(0x55FFFF00);
        }
        mSelectedCircle = null;
        mSelectedCircleIndex = -1;
        mSelectedCircleCenter = null;
        mSelectedCircleRadius = 0;
        mIsSelectedCirclePinned = false;
        // 清除圆时间戳
        sTempCircleTimestamp = 0;
        mSelectedCircleTimestamp = 0;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //找到searchView
        searchItem = menu.findItem(R.id.action_search);
        searchItem.setOnActionExpandListener(new  MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchLayout.setVisibility(View.INVISIBLE);
                mHistoryLayout.setVisibility(View.INVISIBLE);
                return true;  // Return true to collapse action view
            }
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchLayout.setVisibility(View.INVISIBLE);
                //展示搜索历史
                List<Map<String, Object>> data = getSearchHistory();

                if (!data.isEmpty()) {
                    SimpleAdapter simAdapt = new SimpleAdapter(
                            MainActivity.this,
                            data,
                            R.layout.search_item,
                            new String[] {DataBaseHistorySearch.DB_COLUMN_KEY,
                                    DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                                    DataBaseHistorySearch.DB_COLUMN_TIMESTAMP,
                                    DataBaseHistorySearch.DB_COLUMN_IS_LOCATION,
                                    DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM,
                                    DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM},
                            new int[] {R.id.search_key,
                                    R.id.search_description,
                                    R.id.search_timestamp,
                                    R.id.search_isLoc,
                                    R.id.search_longitude,
                                    R.id.search_latitude});
                    mSearchHistoryList.setAdapter(simAdapt);
                    mHistoryLayout.setVisibility(View.VISIBLE);
                }

                return true;  // Return true to expand action view
            }
        });

        searchView = (SearchView) searchItem.getActionView();
        searchView.setIconified(false);// 设置searchView处于展开状态
        searchView.onActionViewExpanded();// 当展开无输入内容的时候，没有关闭的图标
        searchView.setIconifiedByDefault(true);//默认为true在框内，设置false则在框外
        searchView.setSubmitButtonEnabled(false);//显示提交按钮
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                try {
                    String city = (mCurrentCity != null) ? mCurrentCity : "";
                    XLog.i("搜索关键字: " + query + ", 城市: " + city);
                    InputtipsQuery inputtipsQuery = new InputtipsQuery(query, city);
                    inputtipsQuery.setCityLimit(true);
                    mInputtips.setQuery(inputtipsQuery);
                    mInputtips.requestInputtipsAsyn();
                    //搜索历史 插表参数
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, query);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, "搜索关键字");
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_KEY);
                    contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
                    clearAllMapOverlays();
                    mSearchLayout.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.app_error_search));
                    XLog.e("搜索异常: " + e.getMessage());
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调
                //搜索历史置为不可见
                mHistoryLayout.setVisibility(View.INVISIBLE);

                if (newText != null && !newText.isEmpty()) {
                    try {
                        String city = (mCurrentCity != null) ? mCurrentCity : "";
                        InputtipsQuery inputtipsQuery = new InputtipsQuery(newText, city);
                        inputtipsQuery.setCityLimit(true);
                        mInputtips.setQuery(inputtipsQuery);
                        mInputtips.requestInputtipsAsyn();
                    } catch (Exception e) {
                        GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.app_error_search));
                        XLog.e("搜索异常: " + e.getMessage());
                    }
                }

                return true;
            }
        });

        // 搜索框的清除按钮(该按钮属于安卓系统图标)
        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            EditText et = findViewById(androidx.appcompat.R.id.search_src_text);
            et.setText("");
            searchView.setQuery("", false);
            mSearchLayout.setVisibility(View.INVISIBLE);
            mHistoryLayout.setVisibility(View.VISIBLE);
        });

        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mAccValues = sensorEvent.values;
        }
        else if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mMagValues = sensorEvent.values;
        }

        SensorManager.getRotationMatrix(mR, null, mAccValues, mMagValues);
        SensorManager.getOrientation(mR, mDirectionValues);
        mCurrentDirection = (float) Math.toDegrees(mDirectionValues[0]);    // 弧度转角度
        if (mCurrentDirection < 0) {    // 由 -180 ~ + 180 转为 0 ~ 360
            mCurrentDirection += 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /*============================== NavigationView 相关 ==============================*/
    private void initNavigationView() {
        /*============================== NavigationView 相关 ==============================*/
        NavigationView mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                showSimulationRecordsDialog();
            } else if (id == R.id.nav_multipoint) {
                showMultiPointDialog();
            } else if (id == R.id.nav_favorites) {
                showFavoritesDialog();
            } else if (id == R.id.nav_pinned_manager) {
                showPinnedManager();
            } else if (id == R.id.nav_import) {
                showImportDialog();
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_dev) {
                if (!GoUtils.isDeveloperOptionsEnabled(this)) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_dev));
                } else {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_dev));
                    }
                }
            } else if (id == R.id.nav_update) {
                // checkUpdateVersion(true);
                GoUtils.DisplayToast(this, "升级检查已暂时禁用");
            } else if (id == R.id.nav_feedback) {
                // 问题反馈功能暂时禁用
                GoUtils.DisplayToast(this, "问题反馈功能暂时禁用");
            }

            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

            return true;
        });

        // 直接获取第 0 个头部视图
        View headerView = mNavigationView.getHeaderView(0);
        TextView app_version = headerView.findViewById(R.id.app_version);
        app_version.setText(GoUtils.getVersionName(this));
    }

    /*============================== 主界面地图 相关 ==============================*/
    private void initMap(Bundle savedInstanceState) {
        // 地图初始化
        mMapView = findViewById(R.id.amapView);
        // 必须在地图创建前调用 onCreate
        mMapView.onCreate(savedInstanceState);
        mAMap = mMapView.getMap();
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);
        
        // 应用地图缓存设置
        boolean mapCacheEnabled = MapCacheManager.isMapCacheEnabled(this);
        if (!mapCacheEnabled) {
            // 如果禁用了缓存，设置网络模式
            XLog.i("地图缓存已禁用");
        } else {
            XLog.i("地图缓存已启用");
        }
        mAMap.setMyLocationEnabled(true);

        // 设置Marker点击监听器（全局设置，确保固定点和临时点都能响应点击）
        mAMap.setOnMarkerClickListener(marker -> {
            showMarkerInfoAndMenu(marker);
            return true;
        });

        // 设置自定义InfoWindow适配器
        mAMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        mAMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            /**
             * 单击地图
             * 优先检测是否点击在圆上/圆内，如果是则触发圆交互
             * 标记点点击由Marker点击监听处理
             * 点击空白处不移动标记点
             */
            @Override
            public void onMapClick(LatLng point) {
                // 检查是否点击在多点定位圆内（包括圆内部，不只是圆周）
                boolean clickedOnCircle = false;
                for (int i = 0; i < mMultiPointCircles.size(); i++) {
                    Circle circle = mMultiPointCircles.get(i);
                    LatLng center = mMultiPointCenters.get(i);
                    double radius = mMultiPointRadius.get(i);

                    // 检查点击位置是否在圆内部（包括圆心和圆周之间的任何位置）
                    if (isPointInsideCircle(point, center, radius)) {
                        selectCircle(circle, i);
                        clickedOnCircle = true;
                        XLog.i("点击在多点定位圆 #" + i + " 内部，触发圆选择");
                        break;
                    }
                }

                // 如果没有点击在多点定位圆上，检查是否点击在固定圆内
                if (!clickedOnCircle) {
                    for (int i = 0; i < mPinnedCircles.size(); i++) {
                        Circle pinnedCircle = mPinnedCircles.get(i);
                        Map<String, Object> data = mPinnedCircleData.get(i);
                        double lat = (double) data.get("lat");
                        double lon = (double) data.get("lon");
                        double radius = (double) data.get("radius");
                        LatLng center = new LatLng(lat, lon);

                        if (isPointInsideCircle(point, center, radius)) {
                            // 点击在固定圆内，显示操作菜单
                            clickedOnCircle = true;
                            showPinnedCircleActionMenu(i);
                            XLog.i("点击在固定圆 '" + data.get("name") + "' 内部，显示操作菜单");
                            break;
                        }
                    }
                }

                // 如果没有点击在任何圆上，恢复之前选中圆的状态
                if (!clickedOnCircle && mSelectedCircle != null) {
                    mSelectedCircle.setStrokeColor(0xFFCCCC00); // 恢复黄色边框
                    mSelectedCircle.setFillColor(0x55FFFF00); // 恢复黄色填充
                    mSelectedCircle = null;
                    mSelectedCircleIndex = -1;
                    mSelectedCircleCenter = null;
                    mSelectedCircleRadius = 0;
                }

                // 点击空白处关闭InfoWindow（通过显示一个不可见的标记或清除当前标记）
                if (mCurrentLocationMarker != null) {
                    mCurrentLocationMarker.hideInfoWindow();
                }
            }
        });
        // 2D SDK 没有 setOnPOIClickListener 方法
        mAMap.setOnMapLongClickListener(new AMap.OnMapLongClickListener() {
            /**
             * 长按地图 - 设置新的标记点
             */
            @Override
            public void onMapLongClick(LatLng point) {
                XLog.i("onMapLongClick - 设置新位置: " + point.toString());
                mMarkLatLngMap = point;
                markMap();
                LatLonPoint latLonPoint = new LatLonPoint(point.latitude, point.longitude);
                RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
                mGeocodeSearch.getFromLocationAsyn(query);
                
                // 长按设置新标记，收藏功能通过单击标记菜单访问
            }
        });

        try {
            mGeocodeSearch = new GeocodeSearch(this);
            mGeocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
                @Override
                public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
                    if (rCode != AMapException.CODE_AMAP_SUCCESS || regeocodeResult == null) {
                        XLog.i("逆地理位置失败! 错误码: " + rCode);
                    } else {
                        RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
                        mMarkName = address.getFormatAddress();
                        // 2D SDK 中 RegeocodeAddress 没有 getLatLonPoint 方法，使用 mMarkLatLngMap 的坐标
                        // 高德使用 Marker 的 info window 代替百度的 InfoWindow
                        String snippetText = String.format("%.6f,%.6f", mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(mMarkLatLngMap)
                                .icon(mMapIndicator)
                                .title(mMarkName)
                                .snippet(snippetText);
                        clearAllMapOverlays();
                        Marker marker = mAMap.addMarker(markerOptions);
                        // 更新临时标记引用
                        mCurrentLocationMarker = marker;
                        sTempMarker = marker;
                        mIsCurrentLocationPinned = false;
                        marker.showInfoWindow();
                    }
                }

                @Override
                public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {
                    if (rCode == AMapException.CODE_AMAP_SUCCESS && geocodeResult != null
                            && geocodeResult.getGeocodeAddressList() != null
                            && !geocodeResult.getGeocodeAddressList().isEmpty()) {
                        // 获取地理编码结果
                        com.amap.api.services.geocoder.GeocodeAddress address = geocodeResult.getGeocodeAddressList().get(0);
                        if (address.getLatLonPoint() != null) {
                            double lat = address.getLatLonPoint().getLatitude();
                            double lon = address.getLatLonPoint().getLongitude();
                            String name = geocodeResult.getGeocodeQuery().getLocationName();

                            XLog.i("地理编码成功: " + name + " -> " + lat + ", " + lon);

                            // 在地图上显示位置
                            mMarkName = name;
                            mMarkLatLngMap = new LatLng(lat, lon);
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(mMarkLatLngMap);
                            mAMap.moveCamera(cameraUpdate);
                            markMap();

                            // 记录缓存区域
                            String cityName = extractCityFromAddress(address.getFormatAddress());
                            if (cityName != null) {
                                MapCacheManager.addCachedArea(MainActivity.this, cityName);
                            }

                            // 保存到搜索历史
                            double[] latLngWgs84 = MapUtils.gcj02ToWgs84(lon, lat);
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, name);
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, address.getFormatAddress());
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, String.valueOf(lon));
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, String.valueOf(lat));
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(latLngWgs84[0]));
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(latLngWgs84[1]));
                            contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                            DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);

                            // 关闭搜索界面
                            mSearchLayout.setVisibility(View.INVISIBLE);
                            if (searchItem != null) {
                                searchItem.collapseActionView();
                            }

                            GoUtils.DisplayToast(MainActivity.this, "已定位: " + name);
                        } else {
                            GoUtils.DisplayToast(MainActivity.this, "无法获取该位置坐标");
                        }
                    } else {
                        XLog.w("地理编码失败: " + rCode);
                        GoUtils.DisplayToast(MainActivity.this, "无法定位该地址，请重试");
                    }
                }
            });
        } catch (AMapException e) {
            XLog.e("GeocodeSearch init error: " + e.getMessage());
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务
        if (mSensorManager != null) {
            mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mSensorAccelerometer != null) {
                mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            if (mSensorMagnetic != null) {
                mSensorManager.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    //开启地图的定位图层
    private void initMapLocation() {
        try {
            // 设置定位样式 - 只在初始化时设置一次
            MyLocationStyle myLocationStyle = new MyLocationStyle();
            myLocationStyle.showMyLocation(true);
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            mAMap.setMyLocationStyle(myLocationStyle);
            
            // 定位初始化
            mLocClient = new AMapLocationClient(this);
            mLocClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    if (aMapLocation == null || mMapView == null) {// mapview 销毁后不在处理新接收的位置
                        XLog.e("定位回调: aMapLocation 或 mMapView 为空");
                        return;
                    }

                    mCurrentCity = aMapLocation.getCity();
                    mCurrentLat = aMapLocation.getLatitude();
                    mCurrentLon = aMapLocation.getLongitude();

                    XLog.i("定位回调: lat=" + mCurrentLat + ", lon=" + mCurrentLon + ", city=" + mCurrentCity);

                    /* 如果出现错误，则需要重新请求位置 */
                    int err = aMapLocation.getErrorCode();
                    if (err != AMapLocation.LOCATION_SUCCESS) {
                        XLog.e("定位错误: " + err + " - " + aMapLocation.getErrorInfo());
                        if (err == AMapLocation.ERROR_CODE_FAILURE_CONNECTION || 
                            err == AMapLocation.ERROR_CODE_FAILURE_LOCATION_PARAMETER) {
                            mLocClient.startLocation();   /* 请求位置 */
                        }
                    } else {
                        XLog.i("定位成功: isFirstLoc=" + isFirstLoc);
                        if (isFirstLoc) {
                            isFirstLoc = false;
                            // 这里记录高德地图返回的位置
                            mMarkLatLngMap = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                            CameraPosition cameraPosition = new CameraPosition(mMarkLatLngMap, 18, 0, 0);
                            mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                            XLog.i("首次定位成功，移动到位置: " + mMarkLatLngMap);
                        }
                    }
                }
            });
            AMapLocationClientOption locationOption = getLocationClientOption();
            //需将配置好的AMapLocationClientOption对象，通过setLocOption方法传递给AMapLocationClient对象使用
            mLocClient.setLocationOption(locationOption);
            //开始定位
            mLocClient.startLocation();
        } catch (Exception e) {
            XLog.e("ERROR: initMapLocation: " + e.getMessage());
        }
    }

    @NonNull
    private static AMapLocationClientOption getLocationClientOption() {
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //可选，默认gcj02，设置返回的定位结果坐标系
        // 2D SDK 中没有 setCoordinateType 方法，GCJ02 是默认坐标系
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationOption.setInterval(1000);
        //可选，设置是否需要地址信息，默认不需要
        locationOption.setNeedAddress(true);
        //可选，设置是否需要设备方向结果
        // locationOption.setNeedDeviceDirect(false);
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setLocationCacheEnable(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setKillProcess(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        // locationOption.setIsNeedLocationDescribe(false);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setOnceLocation(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        // locationOption.setIgnoreCacheException(true);
        //可选，默认false，设置是否开启Gps定位
        // locationOption.setOpenGps(true);
        // locationOption.setOpenGnss(true);
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setSensorEnable(false);
        return locationOption;
    }

    //地图上各按键的监听
    private void initMapButton() {
        RadioGroup mGroupMapType = this.findViewById(R.id.RadioGroupMapType);
        mGroupMapType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.mapNormal) {
                mAMap.setMapType(AMap.MAP_TYPE_NORMAL);
            }

            if (checkedId == R.id.mapSatellite) {
                mAMap.setMapType(AMap.MAP_TYPE_SATELLITE);
            }
        });

        ImageButton curPosBtn = this.findViewById(R.id.cur_position);
        curPosBtn.setOnClickListener(v -> resetMap());

        ImageButton inputPosBtn = this.findViewById(R.id.input_pos);
        inputPosBtn.setOnClickListener(v -> {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("请输入经度和纬度");
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.location_input, null);
            builder.setView(view);
            dialog = builder.show();

            EditText dialog_lng = view.findViewById(R.id.joystick_longitude);
            EditText dialog_lat = view.findViewById(R.id.joystick_latitude);
            // 使用 pos_type_gcj02 作为坐标类型选择（GCJ02 是默认坐标系）
            RadioButton rbGCJ02 = view.findViewById(R.id.pos_type_gcj02);

            Button btnGo = view.findViewById(R.id.input_position_ok);
            btnGo.setOnClickListener(v2 -> {
                String dialog_lng_str = dialog_lng.getText().toString();
                String dialog_lat_str = dialog_lat.getText().toString();

                if (TextUtils.isEmpty(dialog_lng_str) || TextUtils.isEmpty(dialog_lat_str)) {
                    GoUtils.DisplayToast(MainActivity.this,getResources().getString(R.string.app_error_input));
                } else {
                    double dialog_lng_double = Double.parseDouble(dialog_lng_str);
                    double dialog_lat_double = Double.parseDouble(dialog_lat_str);

                    if (dialog_lng_double > 180.0 || dialog_lng_double < -180.0) {
                        GoUtils.DisplayToast(MainActivity.this,  getResources().getString(R.string.app_error_longitude));
                    } else {
                        if (dialog_lat_double > 90.0 || dialog_lat_double < -90.0) {
                            GoUtils.DisplayToast(MainActivity.this,  getResources().getString(R.string.app_error_latitude));
                        } else {
                            if (rbGCJ02.isChecked()) {
                                mMarkLatLngMap = new LatLng(dialog_lat_double, dialog_lng_double);
                            } else {
                                double[] gcjLonLat = MapUtils.wgs84ToGcj02(dialog_lng_double, dialog_lat_double);
                                mMarkLatLngMap = new LatLng(gcjLonLat[1], gcjLonLat[0]);
                            }
                            mMarkName = "手动输入的坐标";

                            markMap();

                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(mMarkLatLngMap);
                            mAMap.moveCamera(cameraUpdate);

                            dialog.dismiss();
                        }
                    }
                }
            });

            Button btnCancel = view.findViewById(R.id.input_position_cancel);
            btnCancel.setOnClickListener(v1 -> dialog.dismiss());
        });
    }

    //标定选择的位置
    // 静态变量保存当前临时标记，确保即使Activity重建也能追踪
    private static Marker sTempMarker = null;

    private void markMap() {
        if (mMarkLatLngMap != null) {
            XLog.i("markMap开始 - mCurrentLocationMarker: " + (mCurrentLocationMarker != null ? "存在" : "null") +
                    ", sTempMarker: " + (sTempMarker != null ? "存在" : "null"));

            // 删除旧临时标记（临时坐标同时只能存在一个）
            // 优先使用静态变量，其次使用成员变量
            Marker markerToRemove = null;
            if (sTempMarker != null) {
                markerToRemove = sTempMarker;
                XLog.i("将删除静态变量中的旧临时标记");
            } else if (mCurrentLocationMarker != null && !mIsCurrentLocationPinned) {
                markerToRemove = mCurrentLocationMarker;
                XLog.i("将删除成员变量中的旧临时标记");
            }

            if (markerToRemove != null) {
                markerToRemove.remove();
                XLog.i("已删除旧临时标记");
            }

            // 创建新临时标记
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(mMarkLatLngMap)
                    .icon(mMapIndicator)
                    .title(mMarkName != null ? mMarkName : "位置")
                    .snippet(String.format("%.6f, %.6f", mMarkLatLngMap.longitude, mMarkLatLngMap.latitude));

            Marker newMarker = mAMap.addMarker(markerOptions);
            newMarker.setObject("temp_location"); // 设置tag为临时标记

            // 记录创建时间戳
            long timestamp = System.currentTimeMillis();
            mCurrentLocationMarkerTimestamp = timestamp;
            sTempMarkerTimestamp = timestamp;

            // 同时更新成员变量和静态变量
            mCurrentLocationMarker = newMarker;
            sTempMarker = newMarker;
            mIsCurrentLocationPinned = false;

            XLog.i("创建新临时标记: " + mMarkLatLngMap.toString() + ", 时间戳: " + timestamp);
        }
    }

    /**
     * 显示标记点信息和交互菜单
     */
    private void showMarkerInfoAndMenu(Marker marker) {
        if (marker == null) return;

        // 更新当前标记点数据
        mMarkLatLngMap = marker.getPosition();
        mMarkName = marker.getTitle();

        // 检查是否是固定标记
        int pinnedIndex = -1;
        for (int i = 0; i < mPinnedLocationMarkers.size(); i++) {
            if (mPinnedLocationMarkers.get(i) == marker) {
                pinnedIndex = i;
                break;
            }
        }

        if (pinnedIndex >= 0) {
            // 固定标记显示不同菜单
            showPinnedLocationActionMenu(pinnedIndex);
        } else {
            // 普通标记显示默认菜单
            showMarkerActionMenu();
        }
    }

    private void resetMap() {
        clearAllMapOverlays();
        mMarkLatLngMap = null;

        // 检查定位是否有效（经纬度是否为0）
        if (mCurrentLat == 0.0 && mCurrentLon == 0.0) {
            // 定位尚未完成，尝试重新启动定位
            XLog.i("resetMap: 定位尚未完成，尝试重新启动定位");
            if (mLocClient != null) {
                mLocClient.startLocation();
                GoUtils.DisplayToast(this, "正在获取定位，请稍后再试");
            } else {
                GoUtils.DisplayToast(this, "定位服务未初始化");
            }
            // 使用默认位置（北京）
            CameraPosition cameraPosition = new CameraPosition(new LatLng(39.9042, 116.4074), 10, 0, 0);
            mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            XLog.i("resetMap: 定位有效，移动到当前位置: " + mCurrentLat + ", " + mCurrentLon);
            CameraPosition cameraPosition = new CameraPosition(new LatLng(mCurrentLat, mCurrentLon), 18, 0, 0);
            mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    // 在地图上显示位置
    public static boolean showLocation(String name, String gcj02Longitude, String gcj02Latitude) {
        boolean ret = true;

        try {
            if (!gcj02Longitude.isEmpty() && !gcj02Latitude.isEmpty()) {
                mMarkName = name;
                mMarkLatLngMap = new LatLng(Double.parseDouble(gcj02Latitude), Double.parseDouble(gcj02Longitude));

                // 清除之前的临时标记（不清除固定标记）
                if (sTempMarker != null) {
                    sTempMarker.remove();
                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(mMarkLatLngMap)
                        .icon(mMapIndicator)
                        .title(mMarkName != null ? mMarkName : "位置")
                        .snippet(String.format("%.6f, %.6f", mMarkLatLngMap.longitude, mMarkLatLngMap.latitude));

                Marker marker = mAMap.addMarker(markerOptions);
                marker.setObject("temp_location"); // 设置为临时标记

                // 记录创建时间戳
                long timestamp = System.currentTimeMillis();
                sTempMarkerTimestamp = timestamp;

                // 更新临时标记引用
                sTempMarker = marker;

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(mMarkLatLngMap);
                mAMap.moveCamera(cameraUpdate);
                
                // 保存模拟记录（只记录实际模拟的位置）
                saveSimulationRecord(name, gcj02Latitude, gcj02Longitude);
            }
        } catch (Exception e) {
            ret = false;
            XLog.e("ERROR: showLocation: " + e.getMessage());
        }

        return ret;
    }
    
    /**
     * 保存模拟记录
     */
    private static void saveSimulationRecord(String name, String lat, String lon) {
        try {
            if (mSimulationRecordsDB != null) {
                ContentValues values = new ContentValues();
                values.put(DataBaseSimulationRecords.DB_COLUMN_LOCATION, 
                    name != null && !name.isEmpty() ? name : "未命名位置");
                values.put(DataBaseSimulationRecords.DB_COLUMN_LATITUDE, lat);
                values.put(DataBaseSimulationRecords.DB_COLUMN_LONGITUDE, lon);
                values.put(DataBaseSimulationRecords.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                DataBaseSimulationRecords.saveSimulationRecord(mSimulationRecordsDB, values);
            }
        } catch (Exception e) {
            XLog.e("保存模拟记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 在地图上显示收藏的标记
     * 蓝色标志，上方半透明小字显示名称，下方半透明小字显示坐标
     */
    public void showFavoriteOnMap(String name, double lat, double lon) {
        try {
            LatLng position = new LatLng(lat, lon);
            
            // 蓝色圆点标记
            BitmapDescriptor blueDot = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            Marker marker = mAMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(blueDot)
                    .anchor(0.5f, 0.5f));
            
            // 上方显示名称（半透明背景）
            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(12);
            tvName.setBackgroundColor(0xAAFFFFFF);
            tvName.setPadding(10, 5, 10, 5);
            BitmapDescriptor nameIcon = BitmapDescriptorFactory.fromView(tvName);
            mAMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(nameIcon)
                    .anchor(0.5f, 1.2f)); // 上方偏移
            
            // 下方显示坐标（半透明背景）
            TextView tvCoord = new TextView(this);
            tvCoord.setText(String.format("%.6f, %.6f", lat, lon));
            tvCoord.setTextSize(10);
            tvCoord.setBackgroundColor(0xAAFFFFFF);
            tvCoord.setPadding(8, 3, 8, 3);
            BitmapDescriptor coordIcon = BitmapDescriptorFactory.fromView(tvCoord);
            mAMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(coordIcon)
                    .anchor(0.5f, -0.2f)); // 下方偏移
            
            // 移动相机到该位置
            mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16));
            
        } catch (Exception e) {
            XLog.e("显示收藏位置失败: " + e.getMessage());
        }
    }

    private void initGoBtn() {
        mButtonStart = findViewById(R.id.faBtnStart);
        mButtonStart.setOnClickListener(this::doGoLocation);
    }

    private void startGoLocation() {
        Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
        bindService(serviceGoIntent, mConnection, BIND_AUTO_CREATE);    // 绑定服务和活动，之后活动就可以去调服务的方法了
        double[] latLng = MapUtils.gcj02ToWgs84(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);
        serviceGoIntent.putExtra(LNG_MSG_ID, latLng[0]);
        serviceGoIntent.putExtra(LAT_MSG_ID, latLng[1]);
        double alt = Double.parseDouble(sharedPreferences.getString("setting_altitude", "55.0"));
        serviceGoIntent.putExtra(ALT_MSG_ID, alt);

        startForegroundService(serviceGoIntent);
        XLog.d("startForegroundService: ServiceGo");

        isMockServStart = true;
    }

    private void stopGoLocation() {
        // 添加 try-catch 防止解绑未绑定的服务导致崩溃
        try {
            unbindService(mConnection); // 解绑服务，服务要记得解绑，不要造成内存泄漏
        } catch (IllegalArgumentException e) {
            XLog.w("Service was not registered: " + e.getMessage());
        }
        Intent serviceGoIntent = new Intent(MainActivity.this, ServiceGo.class);
        stopService(serviceGoIntent);
        isMockServStart = false;
    }

    private void doGoLocation(View v) {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        // GPS 检查已移除，允许不开启 GPS 使用软件
        // if (!GoUtils.isGpsOpened(this)) {
        //     GoUtils.showEnableGpsDialog(this);
        //     return;
        // }

        if (!Settings.canDrawOverlays(getApplicationContext())) {//悬浮窗权限判断
            GoUtils.showEnableFloatWindowDialog(this);
            XLog.e("无悬浮窗权限!");
            return;
        }

        if (isMockServStart) {
            if (mMarkLatLngMap == null) {
                stopGoLocation();
                Snackbar.make(v, "模拟位置已终止", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mButtonStart.setImageResource(R.drawable.ic_position);
            } else {
                double[] latLng = MapUtils.gcj02ToWgs84(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);
                double alt = Double.parseDouble(sharedPreferences.getString("setting_altitude", "55.0"));
                if (mServiceBinder != null) {
                    mServiceBinder.setPosition(latLng[0], latLng[1], alt);
                    Snackbar.make(v, "已传送到新位置", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    GoUtils.DisplayToast(this, "服务连接中，请稍后重试");
                    XLog.e("mServiceBinder is null when trying to set position");
                }

                recordCurrentLocation(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);
                saveSimulationRecord(mMarkName, String.valueOf(mMarkLatLngMap.latitude), String.valueOf(mMarkLatLngMap.longitude));

                clearAllMapOverlays();
                mMarkLatLngMap = null;

                if (GoUtils.isWifiEnabled(MainActivity.this)) {
                    GoUtils.showDisableWifiDialog(MainActivity.this);
                }
            }
        } else {
            if (!GoUtils.isAllowMockLocation(this)) {
                GoUtils.showEnableMockLocationDialog(this);
                XLog.e("无模拟位置权限!");
            } else {
                if (mMarkLatLngMap == null) {
                    Snackbar.make(v, "请先点击地图位置或者搜索位置", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                    startGoLocation();
                    mButtonStart.setImageResource(R.drawable.ic_fly);
                    Snackbar.make(v, "模拟位置已启动", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    recordCurrentLocation(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);
                    saveSimulationRecord(mMarkName, String.valueOf(mMarkLatLngMap.latitude), String.valueOf(mMarkLatLngMap.longitude));
                    clearAllMapOverlays();
                    mMarkLatLngMap = null;

                    if (GoUtils.isWifiEnabled(MainActivity.this)) {
                        GoUtils.showDisableWifiDialog(MainActivity.this);
                    }
                }
            }
        }
    }

    /*============================== 历史记录 相关 ==============================*/
    private void initStoreHistory() {
        try {
            // 模拟记录（替代原历史记录）
            mSimulationRecordsDB = new DataBaseSimulationRecords(getApplicationContext()).getWritableDatabase();
            // 搜索历史
            mSearchHistoryDB = new DataBaseHistorySearch(getApplicationContext()).getWritableDatabase();
            // 收藏
            mFavoritesDB = new DataBaseFavorites(getApplicationContext()).getWritableDatabase();
            // 收藏区域
            mFavoriteRegionsDB = new DataBaseFavoriteRegions(getApplicationContext()).getWritableDatabase();
            // 固定坐标点
            mPinnedLocationsDB = new DataBasePinnedLocations(getApplicationContext()).getWritableDatabase();
            // 固定区域
            mPinnedCirclesDB = new DataBasePinnedCircles(getApplicationContext()).getWritableDatabase();
        } catch (Exception e) {
            XLog.e("ERROR: sqlite init error: " + e.getMessage());
        }
    }
    
    /**
     * 初始化圆交互相关数据库
     */
    private void initCircleDatabases() {
        try {
            // 区域收藏数据库
            DataBaseFavoriteRegions dbRegions = new DataBaseFavoriteRegions(getApplicationContext());
            mFavoriteRegionsDB = dbRegions.getWritableDatabase();

            // 固定圆数据库
            DataBasePinnedCircles dbPinned = new DataBasePinnedCircles(getApplicationContext());
            mPinnedCirclesDB = dbPinned.getWritableDatabase();

            // 加载已固定的圆
            loadPinnedCircles();

            // 加载已固定的坐标点
            loadPinnedLocations();
        } catch (Exception e) {
            XLog.e("ERROR: circle databases init error: " + e.getMessage());
        }
    }

    /**
     * 从数据库加载固定坐标点并显示在地图上
     */
    private void loadPinnedLocations() {
        try {
            List<Map<String, Object>> pinnedLocations = DataBasePinnedLocations.getAllPinnedLocations(mPinnedLocationsDB);
            XLog.i("加载固定坐标点: 共 " + pinnedLocations.size() + " 个");

            for (Map<String, Object> data : pinnedLocations) {
                String name = (String) data.get(DataBasePinnedLocations.DB_COLUMN_NAME);
                double lat = Double.parseDouble((String) data.get(DataBasePinnedLocations.DB_COLUMN_LATITUDE));
                double lon = Double.parseDouble((String) data.get(DataBasePinnedLocations.DB_COLUMN_LONGITUDE));

                // 创建固定标记（不移动相机，避免多个标记时相机乱跳）
                createPinnedLocationMarker(name, lat, lon, false);
            }
        } catch (Exception e) {
            XLog.e("加载固定坐标点失败: " + e.getMessage());
        }
    }

    /**
     * 创建固定坐标点标记（用于加载时）
     * @param moveCamera 是否移动相机到该位置
     */
    private void createPinnedLocationMarker(String name, double lat, double lon, boolean moveCamera) {
        LatLng position = new LatLng(lat, lon);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .icon(createPinnedMarkerIcon(name))
                .anchor(0.5f, 1.0f)
                .draggable(false)
                .title("已固定:" + name)
                .snippet(String.format("%.6f, %.6f", lon, lat));

        Marker marker = mAMap.addMarker(markerOptions);
        marker.setObject("pinned_location"); // 设置为固定标记tag

        // 添加到固定标记列表
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("lat", lat);
        data.put("lon", lon);
        data.put("marker", marker);

        mPinnedLocationMarkers.add(marker);
        mPinnedLocationData.add(data);

        // 如果需要，移动相机到该位置
        if (moveCamera) {
            mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16));
        }
    }

    //获取查询历史
    private List<Map<String, Object>> getSearchHistory() {
        List<Map<String, Object>> data = new ArrayList<>();

        try {
            Cursor cursor = mSearchHistoryDB.query(DataBaseHistorySearch.TABLE_NAME, null,
                    DataBaseHistorySearch.DB_COLUMN_ID + " > ?", new String[] {"0"},
                    null, null, DataBaseHistorySearch.DB_COLUMN_TIMESTAMP + " DESC", null);

            while (cursor.moveToNext()) {
                Map<String, Object> searchHistoryItem = new HashMap<>();
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_KEY, cursor.getString(1));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, cursor.getString(2));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, "" + cursor.getInt(3));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, "" + cursor.getInt(4));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, cursor.getString(7));
                searchHistoryItem.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, cursor.getString(8));
                data.add(searchHistoryItem);
            }
            cursor.close();
        } catch (Exception e) {
            XLog.e("ERROR: getSearchHistory");
        }

        return data;
    }

    // 记录请求的位置信息（此方法保留用于位置名称查询，不再保存到旧历史记录）
    private void recordCurrentLocation(double lng, double lat) {
        // 模拟记录现在由saveSimulationRecord直接保存
        // 此方法仅用于获取位置名称（如果需要）
        XLog.d("recordCurrentLocation: 位置已记录，模拟记录由saveSimulationRecord保存");
    }

    /*============================== SearchView 相关 ==============================*/
    private void initSearchView() {
        mSearchLayout = findViewById(R.id.search_linear);
        mHistoryLayout = findViewById(R.id.search_history_linear);

        mSearchList = findViewById(R.id.search_list_view);
        mSearchList.setOnItemClickListener((parent, view, position, id) -> {
            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            String poiName = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();
            String poiAddress = ((TextView) view.findViewById(R.id.poi_address)).getText().toString();

            // 检查是否需要地理编码查询
            if ("NEED_GEOCODE".equals(lng) || "NEED_GEOCODE".equals(lat)) {
                // 需要地理编码查询
                String fullAddress = poiAddress.trim();
                XLog.i("搜索结果无坐标，需要地理编码: " + poiName + ", 地址: " + fullAddress);
                performGeocodeSearch(poiName, fullAddress);
                return;
            }

            mMarkName = poiName;
            mMarkLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(mMarkLatLngMap);
            mAMap.moveCamera(cameraUpdate);

            markMap();

            // 记录缓存区域
            String cityName = extractCityFromAddress(poiAddress);
            if (cityName != null) {
                MapCacheManager.addCachedArea(this, cityName);
            }

            double[] latLng = MapUtils.gcj02ToWgs84(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);

            // mSearchList.setVisibility(View.GONE);
            //搜索历史 插表参数
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, mMarkName);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, poiAddress);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(latLng[0]));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(latLng[1]));
            contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

            DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
            mSearchLayout.setVisibility(View.INVISIBLE);
            searchItem.collapseActionView();
        });
        //搜索历史列表的点击监听
        mSearchHistoryList = findViewById(R.id.search_history_list_view);
        mSearchHistoryList.setOnItemClickListener((parent, view, position, id) -> {
            String searchDescription = ((TextView) view.findViewById(R.id.search_description)).getText().toString();
            String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();
            String searchIsLoc = ((TextView) view.findViewById(R.id.search_isLoc)).getText().toString();

            //如果是定位搜索
            if (searchIsLoc.equals("1")) {
                String lng = ((TextView) view.findViewById(R.id.search_longitude)).getText().toString();
                String lat = ((TextView) view.findViewById(R.id.search_latitude)).getText().toString();
                // mMarkName = ((TextView) view.findViewById(R.id.poi_name)).getText().toString();
                mMarkLatLngMap = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(mMarkLatLngMap);
                mAMap.moveCamera(cameraUpdate);

                markMap();

                // 记录缓存区域
                String cityName = extractCityFromAddress(searchDescription);
                if (cityName != null) {
                    MapCacheManager.addCachedArea(this, cityName);
                }

                double[] latLng = MapUtils.gcj02ToWgs84(mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);

                //设置列表不可见
                mHistoryLayout.setVisibility(View.INVISIBLE);
                searchItem.collapseActionView();
                //更新表
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_KEY, searchKey);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_DESCRIPTION, searchDescription);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_IS_LOCATION, DataBaseHistorySearch.DB_SEARCH_TYPE_RESULT);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM, lng);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM, lat);
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LONGITUDE_WGS84, String.valueOf(latLng[0]));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_LATITUDE_WGS84, String.valueOf(latLng[1]));
                contentValues.put(DataBaseHistorySearch.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                DataBaseHistorySearch.saveHistorySearch(mSearchHistoryDB, contentValues);
            } else if (searchIsLoc.equals("0")) { //如果仅仅是搜索
                try {
                    searchView.setQuery(searchKey, true);
                } catch (Exception e) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_search));
                    XLog.e(getResources().getString(R.string.app_error_search));
                }
            } else {
                XLog.e(getResources().getString(R.string.app_error_param));
            }
        });
        mSearchHistoryList.setOnItemLongClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("警告")//这里是表头的内容
                    .setMessage("确定要删除该项搜索记录吗?")//这里是中间显示的具体信息
                    .setPositiveButton("确定",(dialog, which) -> {
                        String searchKey = ((TextView) view.findViewById(R.id.search_key)).getText().toString();

                        try {
                            mSearchHistoryDB.delete(DataBaseHistorySearch.TABLE_NAME, DataBaseHistorySearch.DB_COLUMN_KEY + " = ?", new String[] {searchKey});
                            //删除成功
                            //展示搜索历史
                            List<Map<String, Object>> data = getSearchHistory();

                            if (!data.isEmpty()) {
                                SimpleAdapter simAdapt = new SimpleAdapter(
                                        MainActivity.this,
                                        data,
                                        R.layout.search_item,
                                        new String[] {DataBaseHistorySearch.DB_COLUMN_KEY,
                                                DataBaseHistorySearch.DB_COLUMN_DESCRIPTION,
                                                DataBaseHistorySearch.DB_COLUMN_TIMESTAMP,
                                                DataBaseHistorySearch.DB_COLUMN_IS_LOCATION,
                                                DataBaseHistorySearch.DB_COLUMN_LONGITUDE_CUSTOM,
                                                DataBaseHistorySearch.DB_COLUMN_LATITUDE_CUSTOM}, // 与下面数组元素要一一对应
                                        new int[] {R.id.search_key, R.id.search_description, R.id.search_timestamp, R.id.search_isLoc, R.id.search_longitude, R.id.search_latitude});
                                mSearchHistoryList.setAdapter(simAdapt);
                                mHistoryLayout.setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            XLog.e("ERROR: delete database error");
                            GoUtils.DisplayToast(MainActivity.this,getResources().getString(R.string.history_delete_error));
                        }
                    })
                    .setNegativeButton("取消",
                            (dialog, which) -> {
                            })
                    .show();
            return true;
        });
        //设置搜索建议返回值监听 - 高德地图 Inputtips
        try {
            //noinspection deprecation
            mInputtips = new Inputtips(this, (tipList, rCode) -> {
                if (rCode != AMapException.CODE_AMAP_SUCCESS || tipList == null) {
                    XLog.w("搜索请求失败，错误码: " + rCode);
                    // 错误码 1804 表示网络连接错误
                    if (rCode == 1804 || rCode == 1802) {
                        GoUtils.DisplayToast(this, "网络连接失败，请检查网络设置");
                    } else {
                        GoUtils.DisplayToast(this, "搜索失败，请重试");
                    }
                } else {
                    List<Map<String, Object>> data = getMapList(tipList);

                    if (data.isEmpty()) {
                        XLog.w("搜索结果为空，原始结果数量: " + tipList.size());
                        GoUtils.DisplayToast(this,getResources().getString(R.string.app_search_null));
                    } else {
                        XLog.i("搜索成功，返回 " + data.size() + " 条结果");
                        SimpleAdapter simAdapt = new SimpleAdapter(
                                MainActivity.this,
                                data,
                                R.layout.search_poi_item,
                                new String[] {POI_NAME, POI_ADDRESS, POI_LONGITUDE, POI_LATITUDE}, // 与下面数组元素要一一对应
                                new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
                        mSearchList.setAdapter(simAdapt);
                        // mSearchList.setVisibility(View.VISIBLE);
                        mSearchLayout.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (AMapException e) {
            XLog.e("Inputtips init error: " + e.getMessage());
        }
    }

    @NonNull
    private static List<Map<String, Object>> getMapList(List<Tip> tipList) {
        List<Map<String, Object>> data = new ArrayList<>();
        int retCnt = tipList.size();
        XLog.i("搜索原始返回 " + retCnt + " 条结果");

        int nullPointCount = 0;
        for (int i = 0; i < retCnt; i++) {
            Tip tip = tipList.get(i);

            Map<String, Object> poiItem = new HashMap<>();
            poiItem.put(POI_NAME, tip.getName());
            poiItem.put(POI_ADDRESS, tip.getDistrict() + " " + tip.getAddress());

            if (tip.getPoint() == null) {
                XLog.w("搜索结果项 " + i + " 的坐标为 null，关键字: " + tip.getName());
                nullPointCount++;
                // 保留无坐标结果，使用特殊标记
                poiItem.put(POI_LONGITUDE, "NEED_GEOCODE");
                poiItem.put(POI_LATITUDE, "NEED_GEOCODE");
                poiItem.put("NEED_GEOCODE", true);
                poiItem.put("FULL_ADDRESS", tip.getDistrict() + tip.getName());
            } else {
                poiItem.put(POI_LONGITUDE, "" + tip.getPoint().getLongitude());
                poiItem.put(POI_LATITUDE, "" + tip.getPoint().getLatitude());
                poiItem.put("NEED_GEOCODE", false);
            }
            data.add(poiItem);
        }

        XLog.i("搜索结果处理完成，有效坐标: " + (data.size() - nullPointCount) + ", 需地理编码: " + nullPointCount);
        return data;
    }

    /*============================== 更新 相关 ==============================*/
    private void initUpdateVersion() {
        mDownloadManager =(DownloadManager) MainActivity.this.getSystemService(DOWNLOAD_SERVICE);

        // 用于监听下载完成后，转到安装界面
        mDownloadBdRcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                installNewVersion();
            }
        };
        // Android 14+ (API 34) 需要指定接收器导出标志，使用数值 4 表示 RECEIVER_NOT_EXPORTED
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            registerReceiver(mDownloadBdRcv, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), 4);
        } else {
            registerReceiver(mDownloadBdRcv, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void checkUpdateVersion(boolean result) {
        // 暂时禁用自动升级检查
        if (true) return;

        String mapApiUrl = "";
        okhttp3.Request request = new okhttp3.Request.Builder().url(mapApiUrl).get().build();
        final Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                XLog.i("更新检测失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String resp = responseBody.string();
                    // 注意，该请求在子线程，不能直接操作界面
                    runOnUiThread(() -> {
                        try {
                            JSONObject getRetJson = new JSONObject(resp);
                            String curVersion = GoUtils.getVersionName(MainActivity.this);

                            if (curVersion != null
                                    && (!getRetJson.getString("name").contains(curVersion)
                                    || !getRetJson.getString("tag_name").contains(curVersion))) {
                                final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(MainActivity.this).create();
                                alertDialog.show();
                                alertDialog.setCancelable(false);
                                Window window = alertDialog.getWindow();
                                if (window != null) {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);      // 防止出现闪屏
                                    window.setContentView(R.layout.update);
                                    window.setGravity(Gravity.CENTER);
                                    window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

                                    TextView updateTitle = window.findViewById(R.id.update_title);
                                    updateTitle.setText(getRetJson.getString("name"));
                                    TextView updateTime = window.findViewById(R.id.update_time);
                                    updateTime.setText(getRetJson.getString("created_at"));
                                    TextView updateCommit = window.findViewById(R.id.update_commit);
                                    updateCommit.setText(getRetJson.getString("target_commitish"));

                                    TextView updateContent = window.findViewById(R.id.update_content);
                                    final Markwon markwon = Markwon.create(MainActivity.this);
                                    markwon.setMarkdown(updateContent, getRetJson.getString("body"));

                                    Button updateCancel = window.findViewById(R.id.update_ignore);
                                    updateCancel.setOnClickListener(v -> alertDialog.cancel());

                                    /* 这里用来保存下载地址 */
                                    JSONArray jsonArray = new JSONArray(getRetJson.getString("assets"));
                                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                                    String download_url = jsonObject.getString("browser_download_url");
                                    mUpdateFilename = jsonObject.getString("name");

                                    Button updateAgree = window.findViewById(R.id.update_agree);
                                    updateAgree.setOnClickListener(v -> {
                                        alertDialog.cancel();
                                        GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.update_downloading));
                                        downloadNewVersion(download_url);
                                    });
                                }
                            } else {
                                if (result) {
                                    GoUtils.DisplayToast(MainActivity.this, getResources().getString(R.string.update_last));
                                }
                            }
                        } catch (JSONException e) {
                            XLog.e("ERROR: resolve json");
                        }
                    });
                }
            }
        });
    }

    private void downloadNewVersion(String url) {
        if (mDownloadManager == null) {
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedOverRoaming(false);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(GoUtils.getAppName(this));
        request.setDescription("正在下载新版本...");
        request.setMimeType("application/vnd.android.package-archive");

        // DownloadManager不会覆盖已有的同名文件，需要自己来删除已存在的文件
        File file = new File(getExternalFilesDir("Updates"), mUpdateFilename);
        if (file.exists()) {
            if(!file.delete()) {
                return;
            }
        }
        request.setDestinationUri(Uri.fromFile(file));

        mDownloadId = mDownloadManager.enqueue(request);
    }

    private void installNewVersion() {
        Intent install = new Intent(Intent.ACTION_VIEW);
        Uri downloadFileUri = mDownloadManager.getUriForDownloadedFile(mDownloadId);
        File file = new File(getExternalFilesDir("Updates"), mUpdateFilename);
        if (downloadFileUri != null) {
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // 在Broadcast中启动活动需要添加Intent.FLAG_ACTIVITY_NEW_TASK
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);    //添加这一句表示对目标应用临时授权该Uri所代表的文件
            install.addCategory("android.intent.category.DEFAULT");
            install.setDataAndType(ShareUtils.getUriFromFile(MainActivity.this, file), "application/vnd.android.package-archive");
            startActivity(install);
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            intent.addCategory("android.intent.category.DEFAULT");
            startActivity(intent);
        }
    }

    /*============================== 多点定位 相关 ==============================*/
    
    /**
     * 显示多点定位对话框
     */
    private void showMultiPointDialog() {
        // 清除之前的多点定位标记
        clearMultiPointMarks();
        
        // 获取历史记录
        List<Map<String, Object>> historyData = getMultiPointHistoryData();
        if (historyData.isEmpty()) {
            GoUtils.DisplayToast(this, "模拟记录为空，请先保存位置");
            return;
        }
        
        // 显示选择对话框
        showMultiPointSelectionDialog(historyData, 0);
    }
    
    /**
     * 获取模拟记录数据用于多点定位
     */
    private List<Map<String, Object>> getMultiPointHistoryData() {
        // 使用模拟记录数据
        return getSimulationRecordsData();
    }
    
    /**
     * 获取模拟记录数据（保留旧方法名以兼容多点定位功能）
     */
    private List<Map<String, Object>> getHistoryData() {
        return getSimulationRecordsData();
    }
    
    /**
     * 获取收藏数据
     */
    private List<Map<String, Object>> getFavoritesData() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            if (mFavoritesDB == null) {
                XLog.e("获取收藏数据失败: mFavoritesDB 为 null");
                return data;
            }
            Cursor cursor = mFavoritesDB.query(DataBaseFavorites.TABLE_NAME,
                null, null, null, null, null,
                DataBaseFavorites.DB_COLUMN_TIMESTAMP + " DESC");

            if (cursor != null) {
                XLog.i("获取收藏数据: 查询返回 " + cursor.getCount() + " 条记录");
                if (cursor.moveToFirst()) {
                    do {
                        Map<String, Object> item = new HashMap<>();
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavorites.DB_COLUMN_ID));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavorites.DB_COLUMN_NAME));
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavorites.DB_COLUMN_LATITUDE));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavorites.DB_COLUMN_LONGITUDE));

                        item.put(DataBaseFavorites.DB_COLUMN_ID, id);
                        item.put("name", name);
                        double latVal = Double.parseDouble(lat);
                        double lonVal = Double.parseDouble(lon);
                        item.put("lat", latVal);
                        item.put("lon", lonVal);
                        // 显示格式：经度, 纬度
                        item.put("coords", String.format("%.6f, %.6f", lonVal, latVal));
                        data.add(item);
                        XLog.d("加载收藏: " + name + " (" + lat + ", " + lon + ")");
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }
        } catch (Exception e) {
            XLog.e("获取收藏数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return data;
    }
    
    /**
     * 获取区域收藏数据
     */
    private List<Map<String, Object>> getFavoriteRegionsData() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            Cursor cursor = mFavoriteRegionsDB.query(DataBaseFavoriteRegions.TABLE_NAME, 
                null, null, null, null, null, 
                DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP + " DESC");
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Map<String, Object> item = new HashMap<>();
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavoriteRegions.DB_COLUMN_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavoriteRegions.DB_COLUMN_NAME));
                    String lat = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT));
                    String lon = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON));
                    String radius = cursor.getString(cursor.getColumnIndexOrThrow(DataBaseFavoriteRegions.DB_COLUMN_RADIUS));
                    
                    item.put(DataBaseFavoriteRegions.DB_COLUMN_ID, id);
                    item.put("name", name);
                    double latVal = Double.parseDouble(lat);
                    double lonVal = Double.parseDouble(lon);
                    double radiusVal = Double.parseDouble(radius);
                    item.put("lat", latVal);
                    item.put("lon", lonVal);
                    item.put("radius", radiusVal);
                    // 显示格式：半径 X 米
                    item.put("coords", String.format("半径 %.0f 米", radiusVal));
                    data.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            XLog.e("获取区域收藏数据失败: " + e.getMessage());
        }
        return data;
    }

    /**
     * 获取模拟记录数据
     */
    private List<Map<String, Object>> getSimulationRecordsData() {
        return DataBaseSimulationRecords.getAllSimulationRecords(mSimulationRecordsDB);
    }

    /**
     * 显示多点定位选择对话框
     */
    private void showMultiPointSelectionDialog(List<Map<String, Object>> dataList, int currentCount) {
        showMultiPointSelectionDialog(dataList, currentCount, false);
    }
    
    private void showMultiPointSelectionDialog(List<Map<String, Object>> dataList, int currentCount, boolean isFavoriteMode) {
        showMultiPointSelectionDialog(dataList, currentCount, isFavoriteMode, null);
    }

    private void showMultiPointSelectionDialog(List<Map<String, Object>> dataList, int currentCount, boolean isFavoriteMode, AlertDialog existingDialog) {
        AlertDialog dialog;
        View view;

        if (existingDialog != null) {
            // 复用现有对话框
            dialog = existingDialog;
            view = dialog.findViewById(R.id.container_history).getRootView();
        } else {
            // 创建新对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            view = LayoutInflater.from(this).inflate(R.layout.dialog_multipoint, null);
            builder.setView(view);
            dialog = builder.create();
        }

        // 获取视图
        ListView lvHistory = view.findViewById(R.id.lv_history);
        ListView lvFavorites = view.findViewById(R.id.lv_favorites);
        View containerHistory = view.findViewById(R.id.container_history);
        View containerFavorites = view.findViewById(R.id.container_favorites);
        TextView tvSelectedCount = view.findViewById(R.id.tv_selected_count);
        EditText etRadius = view.findViewById(R.id.et_radius);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        com.google.android.material.button.MaterialButton btnHistory = view.findViewById(R.id.btn_history);
        com.google.android.material.button.MaterialButton btnFavorites = view.findViewById(R.id.btn_favorites);

        tvSelectedCount.setText("已选择: " + currentCount + " 个坐标");

        // 加载切换动画
        Animation switchFadeIn = AnimationUtils.loadAnimation(this, R.anim.switch_fade_in);
        Animation switchFadeOut = AnimationUtils.loadAnimation(this, R.anim.switch_fade_out);

        // 设置初始显示状态
        if (isFavoriteMode) {
            containerHistory.setVisibility(View.GONE);
            containerFavorites.setVisibility(View.VISIBLE);
        } else {
            containerHistory.setVisibility(View.VISIBLE);
            containerFavorites.setVisibility(View.GONE);
        }

        // 设置按钮样式 - 选中状态使用主色背景+白色文字，未选中使用透明背景+灰色文字
        updateMultiPointButtonStyles(btnHistory, btnFavorites, isFavoriteMode);

        // 获取历史数据
        List<Map<String, Object>> historyData = getMultiPointHistoryData();
        // 获取收藏数据
        List<Map<String, Object>> favoritesData = getFavoritesData();

        // 处理名称，空值时显示为附近地点
        processLocationNames(historyData);
        processLocationNames(favoritesData);

        // 使用自定义适配器
        MultiPointHistoryAdapter historyAdapter = new MultiPointHistoryAdapter(this, historyData);
        MultiPointHistoryAdapter favoritesAdapter = new MultiPointHistoryAdapter(this, favoritesData);
        lvHistory.setAdapter(historyAdapter);
        lvFavorites.setAdapter(favoritesAdapter);

        final Map<String, Object> selectedItem = new HashMap<>();
        final int[] selectedPosition = {-1};

        // 设置历史列表点击事件
        lvHistory.setOnItemClickListener((parent, itemView, position, id) -> {
            handleItemClick(historyData, position, selectedItem, selectedPosition, historyAdapter);
        });

        // 设置收藏列表点击事件
        lvFavorites.setOnItemClickListener((parent, itemView, position, id) -> {
            handleItemClick(favoritesData, position, selectedItem, selectedPosition, favoritesAdapter);
        });

        // 历史按钮点击 - 在同一对话框内切换
        btnHistory.setOnClickListener(v -> {
            if (containerFavorites.getVisibility() == View.VISIBLE) {
                // 使用切换动画
                containerFavorites.startAnimation(switchFadeOut);
                containerFavorites.setVisibility(View.GONE);
                containerHistory.setVisibility(View.VISIBLE);
                containerHistory.startAnimation(switchFadeIn);
                // 更新按钮样式
                updateMultiPointButtonStyles(btnHistory, btnFavorites, false);
            }
        });

        // 收藏按钮点击 - 在同一对话框内切换
        btnFavorites.setOnClickListener(v -> {
            if (containerHistory.getVisibility() == View.VISIBLE) {
                // 使用切换动画
                containerHistory.startAnimation(switchFadeOut);
                containerHistory.setVisibility(View.GONE);
                containerFavorites.setVisibility(View.VISIBLE);
                containerFavorites.startAnimation(switchFadeIn);
                // 更新按钮样式
                updateMultiPointButtonStyles(btnHistory, btnFavorites, true);
            }
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            // 如果已经选择了点，询问是否完成
            if (currentCount >= 3) {
                showFinishConfirmDialog();
            } else {
                clearMultiPointMarks();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedItem.isEmpty()) {
                GoUtils.DisplayToast(this, "请先选择一个坐标");
                return;
            }

            String radiusStr = etRadius.getText().toString().trim();
            if (radiusStr.isEmpty()) {
                GoUtils.DisplayToast(this, "请输入半径");
                return;
            }

            double radiusKm = Double.parseDouble(radiusStr); // 输入的是千米
            double radiusM = radiusKm * 1000; // 转换为米

            double lat = (Double) selectedItem.get("lat");
            double lon = (Double) selectedItem.get("lon");
            String name = (String) selectedItem.get("name");

            // 添加圆和标记
            addMultiPointCircle(lat, lon, radiusM, name);

            dialog.dismiss();

            // 询问是否继续添加 - 使用当前显示的数据
            int newCount = currentCount + 1;
            // 当有3个或更多圆时，可以计算相交区域
            boolean hasEnoughCircles = mMultiPointCircles.size() >= 3;
            // 根据当前显示的容器决定使用哪组数据
            List<Map<String, Object>> currentData = (containerHistory.getVisibility() == View.VISIBLE) ? historyData : favoritesData;
            boolean currentIsFavoriteMode = (containerFavorites.getVisibility() == View.VISIBLE);
            showAddMoreDialog(currentData, newCount, currentIsFavoriteMode, hasEnoughCircles);
        });
        
        dialog.show();
        GoUtils.setDialogWidth(dialog);

        // 设置缩放动画
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Animation_Zoom);
        }
    }

    /**
     * 根据坐标获取附近地点名称
     */
    private String getNearbyLocationName(double lat, double lon) {
        // 这里简化处理，实际应该根据坐标查询附近POI
        // 返回格式：经纬度附近
        return String.format("%.4f, %.4f 附近", lat, lon);
    }

    /**
     * 处理位置名称，空值时显示为附近地点
     */
    private void processLocationNames(List<Map<String, Object>> dataList) {
        for (Map<String, Object> item : dataList) {
            String name = (String) item.get("name");
            double lat = (Double) item.get("lat");
            double lon = (Double) item.get("lon");
            if (name == null || name.equals("null") || name.isEmpty()) {
                name = getNearbyLocationName(lat, lon);
                item.put("name", name);
            }
        }
    }

    /**
     * 更新多点定位对话框按钮样式
     */
    private void updateMultiPointButtonStyles(
            com.google.android.material.button.MaterialButton btnHistory,
            com.google.android.material.button.MaterialButton btnFavorites,
            boolean isFavoriteMode) {
        if (isFavoriteMode) {
            // 收藏按钮选中状态
            btnFavorites.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
            btnFavorites.setTextColor(getColor(R.color.md_onPrimary));
            btnFavorites.setElevation(getResources().getDimension(R.dimen.elevation_low));
            // 历史按钮未选中状态
            btnHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
            btnHistory.setTextColor(getColor(R.color.md_onSurfaceVariant));
            btnHistory.setElevation(0);
        } else {
            // 历史按钮选中状态
            btnHistory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
            btnHistory.setTextColor(getColor(R.color.md_onPrimary));
            btnHistory.setElevation(getResources().getDimension(R.dimen.elevation_low));
            // 收藏按钮未选中状态
            btnFavorites.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
            btnFavorites.setTextColor(getColor(R.color.md_onSurfaceVariant));
            btnFavorites.setElevation(0);
        }
    }

    /**
     * 处理列表项点击
     */
    private void handleItemClick(List<Map<String, Object>> dataList, int position,
                                 Map<String, Object> selectedItem, int[] selectedPosition,
                                 MultiPointHistoryAdapter adapter) {
        // 如果点击已选中的项，则取消选中
        if (selectedPosition[0] == position) {
            selectedPosition[0] = -1;
            selectedItem.clear();
            adapter.setSelectedPosition(-1);
            GoUtils.DisplayToast(this, "已取消选择");
        } else {
            selectedPosition[0] = position;
            selectedItem.putAll(dataList.get(position));
            adapter.setSelectedPosition(position);
            GoUtils.DisplayToast(this, "已选择: " + selectedItem.get("name"));
        }
    }
    
    /**
     * 多点定位历史记录适配器
     */
    private class MultiPointHistoryAdapter extends android.widget.BaseAdapter {
        private Context context;
        private List<Map<String, Object>> data;
        private int selectedPosition = -1;
        
        public MultiPointHistoryAdapter(Context context, List<Map<String, Object>> data) {
            this.context = context;
            this.data = data;
        }
        
        public void setSelectedPosition(int position) {
            this.selectedPosition = position;
            notifyDataSetChanged();
        }
        
        @Override
        public int getCount() {
            return data.size();
        }
        
        @Override
        public Object getItem(int position) {
            return data.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_multipoint_history, parent, false);
                holder = new ViewHolder();
                holder.container = convertView.findViewById(R.id.item_container);
                holder.tvName = convertView.findViewById(R.id.tv_location_name);
                holder.tvCoords = convertView.findViewById(R.id.tv_coordinates);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            Map<String, Object> item = data.get(position);
            String name = (String) item.get("name");
            double lat = (Double) item.get("lat");
            double lon = (Double) item.get("lon");
            
            holder.tvName.setText(name);
            // 显示格式：经度, 纬度
            holder.tvCoords.setText(String.format("%.6f, %.6f", lon, lat));
            
            // 设置选中状态的背景色 - 使用主题颜色
            if (position == selectedPosition) {
                holder.container.setBackgroundColor(getColor(R.color.md_primaryContainer));
            } else {
                holder.container.setBackgroundColor(getColor(android.R.color.transparent));
            }
            
            return convertView;
        }
        
        private class ViewHolder {
            LinearLayout container;
            TextView tvName;
            TextView tvCoords;
        }
    }
    
    /**
     * 显示是否继续添加对话框
     */
    private void showAddMoreDialog(List<Map<String, Object>> dataList, int count, boolean isFavoriteMode, boolean hasEnoughCircles) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_multipoint_confirm, null);
        builder.setView(view);
        
        AlertDialog dialog = builder.create();
        
        TextView tvCurrentCount = view.findViewById(R.id.tv_current_count);
        Button btnFinish = view.findViewById(R.id.btn_finish);
        Button btnAddMore = view.findViewById(R.id.btn_add_more);
        
        tvCurrentCount.setText("当前已选择 " + count + " 个坐标");
        
        // 完成按钮始终可点击
        btnFinish.setOnClickListener(v -> {
            dialog.dismiss();
            // 只有在有3个或更多圆时才计算相交区域
            if (mMultiPointCircles.size() >= 3) {
                calculateAndShowIntersection();
            } else {
                GoUtils.DisplayToast(this, "已添加 " + mMultiPointCircles.size() + " 个圆");
            }
        });
        
        btnAddMore.setOnClickListener(v -> {
            dialog.dismiss();
            showMultiPointSelectionDialog(dataList, count, isFavoriteMode);
        });
        
        dialog.show();
        GoUtils.setDialogWidth(dialog);
    }
    
    /**
     * 显示完成确认对话框（当用户取消时但已选点数>=3）
     */
    private void showFinishConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("多点定位")
            .setMessage("是否完成多点定位并计算相交区域？")
            .setPositiveButton("是", (dialog, which) -> {
                calculateAndShowIntersection();
            })
            .setNegativeButton("否", (dialog, which) -> {
                clearMultiPointMarks();
            })
            .show();
    }
    
    /**
     * 添加多点定位的圆和标记
     */
    private void addMultiPointCircle(double lat, double lon, double radiusM, String name) {
        LatLng center = new LatLng(lat, lon);
        mMultiPointCenters.add(center);
        mMultiPointRadius.add(radiusM);
        
        // 添加黄色圆形 - 高德地图Circle会自动随地图缩放
        Circle circle = mAMap.addCircle(new CircleOptions()
            .center(center)
            .radius(radiusM)  // 单位：米
            .fillColor(0x55FFFF00) // 黄色半透明填充
            .strokeColor(0xFFCCCC00) // 黄色边框
            .strokeWidth(2));
        mMultiPointCircles.add(circle);
        
        // 添加自定义圆心标记（显示名称和半径km）
        addCircleCenterMarker(center, name, radiusM);
        
        // 移动相机到圆中心，并缩放以适应圆的大小
        float zoomLevel = calculateZoomLevel(radiusM);
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoomLevel));
        
        XLog.i("添加多点定位圆: " + name + " (" + lat + ", " + lon + "), 半径: " + radiusM + "米");
    }
    
    /**
     * 根据半径计算合适的缩放级别
     */
    private float calculateZoomLevel(double radiusM) {
        return calculateZoomLevelStatic(radiusM);
    }
    
    /**
     * 计算并显示相交点
     * 找到三个圆周的共同交点或最接近圆周交点的位置
     */
    private void calculateAndShowIntersection() {
        if (mMultiPointCircles.size() < 3) {
            GoUtils.DisplayToast(this, getString(R.string.dialog_multipoint_minimum));
            return;
        }
        
        // 清除之前的相交标记
        clearIntersectionMarks();
        
        // 第一步：计算所有两两圆的交点
        List<LatLng> allIntersectionPoints = new ArrayList<>();
        for (int i = 0; i < mMultiPointCenters.size(); i++) {
            for (int j = i + 1; j < mMultiPointCenters.size(); j++) {
                List<LatLng> points = calculateTwoCircleIntersections(
                    mMultiPointCenters.get(i), mMultiPointRadius.get(i),
                    mMultiPointCenters.get(j), mMultiPointRadius.get(j)
                );
                allIntersectionPoints.addAll(points);
            }
        }
        
        if (allIntersectionPoints.isEmpty()) {
            GoUtils.DisplayToast(this, "圆之间没有相交点，请调整半径");
            return;
        }
        
        // 第二步：在所有交点中找到使得到所有圆周距离最小的点
        LatLng bestPoint = null;
        double minError = Double.MAX_VALUE;
        
        for (LatLng candidate : allIntersectionPoints) {
            double error = calculateCircleDistanceError(candidate);
            if (error < minError) {
                minError = error;
                bestPoint = candidate;
            }
        }
        
        // 第三步：如果最小误差太大，使用迭代优化找到更优的点
        if (minError > 50 && bestPoint != null) { // 如果误差大于50米
            LatLng optimized = optimizeToCircleIntersections(bestPoint);
            if (optimized != null) {
                bestPoint = optimized;
            }
        }
        
        if (bestPoint != null) {
            showEstimatedTargetPoint(bestPoint);
            
            // 显示误差信息
            double finalError = calculateCircleDistanceError(bestPoint);
            if (finalError < 10) {
                GoUtils.DisplayToast(this, "找到精确的圆周交点！误差: " + String.format("%.1f米", finalError));
            } else {
                GoUtils.DisplayToast(this, "估计的目标点，误差: " + String.format("%.1f米", finalError));
            }
        }
    }
    
    /**
     * 计算点到所有圆周的误差（使得到各圆心距离与半径的差的平方和最小）
     */
    private double calculateCircleDistanceError(LatLng point) {
        double error = 0;
        for (int i = 0; i < mMultiPointCenters.size(); i++) {
            double dist = calculateDistance(point, mMultiPointCenters.get(i));
            double radius = mMultiPointRadius.get(i);
            error += Math.abs(dist - radius); // 到圆周的距离误差
        }
        return error;
    }
    
    /**
     * 优化点到圆周交点的位置
     * 使用梯度下降找到使得到各圆周距离最小的点
     */
    private LatLng optimizeToCircleIntersections(LatLng initial) {
        double lat = initial.latitude;
        double lon = initial.longitude;
        double stepSize = 0.000001; // 初始步长（约0.1米）
        
        for (int iter = 0; iter < 500; iter++) {
            double gradLat = 0, gradLon = 0;
            
            for (int i = 0; i < mMultiPointCenters.size(); i++) {
                LatLng center = mMultiPointCenters.get(i);
                double radius = mMultiPointRadius.get(i);
                
                double dist = calculateDistance(new LatLng(lat, lon), center);
                if (dist < 0.1) continue;
                
                // 误差 = 实际距离 - 期望半径
                double error = dist - radius;
                
                // 计算梯度（指向使误差减小的方向）
                // 如果距离 > 半径，需要向圆心靠近
                // 如果距离 < 半径，需要远离圆心
                double latDiff = lat - center.latitude;
                double lonDiff = lon - center.longitude;
                
                // 转换到米制计算方向
                double latMeterPerDegree = 111000.0;
                double lonMeterPerDegree = 111000.0 * Math.cos(Math.toRadians(center.latitude));
                
                double distLat = latDiff * latMeterPerDegree;
                double distLon = lonDiff * lonMeterPerDegree;
                
                // 归一化方向向量
                double norm = Math.sqrt(distLat * distLat + distLon * distLon);
                if (norm > 0) {
                    // 梯度方向：如果误差>0（太远），向圆心移动；如果误差<0（太近），远离圆心
                    gradLat += (error / norm) * (distLat / norm) / latMeterPerDegree;
                    gradLon += (error / norm) * (distLon / norm) / lonMeterPerDegree;
                }
            }
            
            // 更新位置
            lat -= stepSize * gradLat;
            lon -= stepSize * gradLon;
            
            // 逐步减小步长
            if (iter % 100 == 0) {
                stepSize *= 0.5;
            }
            
            // 检查收敛
            if (Math.abs(gradLat) < 1e-12 && Math.abs(gradLon) < 1e-12) {
                break;
            }
        }
        
        return new LatLng(lat, lon);
    }
    
    /**
     * 计算两个圆的相交点（返回0、1或2个相交点）
     * 使用平面几何计算（小范围内可近似为平面）
     */
    private List<LatLng> calculateTwoCircleIntersections(LatLng c1, double r1, LatLng c2, double r2) {
        List<LatLng> intersections = new ArrayList<>();
        
        // 计算两圆心距离（米）
        double d = calculateDistance(c1, c2);
        
        if (d > r1 + r2 || d < Math.abs(r1 - r2) || d == 0) {
            // 不相交或内含或同心
            return intersections;
        }
        
        // 计算从c1到c2的方向向量（归一化）
        double dx = (c2.longitude - c1.longitude);
        double dy = (c2.latitude - c1.latitude);
        
        // 转换为米制距离（近似）
        // 纬度方向：1度 ≈ 111km
        // 经度方向：1度 ≈ 111km * cos(纬度)
        double latMeterPerDegree = 111000.0;
        double lonMeterPerDegree = 111000.0 * Math.cos(Math.toRadians(c1.latitude));
        
        double deltaX = dx * lonMeterPerDegree; // 东西方向距离（米）
        double deltaY = dy * latMeterPerDegree; // 南北方向距离（米）
        
        // 归一化方向向量
        double dirX = deltaX / d;
        double dirY = deltaY / d;
        
        // 垂直方向向量
        double perpX = -dirY;
        double perpY = dirX;
        
        // 使用余弦定理计算交点相对于c1的位置
        double a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
        double h = Math.sqrt(Math.max(0, r1 * r1 - a * a));
        
        // 交点在c1到c2连线上的投影点
        double projX = a * dirX;
        double projY = a * dirY;
        
        // 转换为经纬度偏移
        double projLatOffset = projY / latMeterPerDegree;
        double projLonOffset = projX / lonMeterPerDegree;
        
        // 垂直方向偏移（转换为经纬度）
        double perpLatOffset = (h * perpY) / latMeterPerDegree;
        double perpLonOffset = (h * perpX) / lonMeterPerDegree;
        
        // 交点1
        double lat3 = c1.latitude + projLatOffset + perpLatOffset;
        double lon3 = c1.longitude + projLonOffset + perpLonOffset;
        intersections.add(new LatLng(lat3, lon3));
        
        // 交点2（如果h>0）
        if (h > 0.001) {
            double lat4 = c1.latitude + projLatOffset - perpLatOffset;
            double lon4 = c1.longitude + projLonOffset - perpLonOffset;
            intersections.add(new LatLng(lat4, lon4));
        }
        
        return intersections;
    }
    
    /**
     * 检查点是否在所有圆内
     */
    private boolean isPointInAllCircles(LatLng point) {
        for (int i = 0; i < mMultiPointCenters.size(); i++) {
            double dist = calculateDistance(point, mMultiPointCenters.get(i));
            if (dist > mMultiPointRadius.get(i) + 0.1) { // 允许0.1米误差
                return false;
            }
        }
        return true;
    }
    
    /**
     * 找出所有同时在三个圆内的相交点
     */
    private List<LatLng> findCommonIntersectionPoints(List<LatLng> points) {
        List<LatLng> common = new ArrayList<>();
        for (LatLng point : points) {
            if (isPointInAllCircles(point)) {
                common.add(point);
            }
        }
        return common;
    }
    
    /**
     * 寻找三个圆共同覆盖区域的中心点
     * 通过逐步逼近法找到同时在三个圆内的点
     */
    private LatLng findCommonCircleCenter() {
        // 从第一个圆的圆心开始，逐步向其他圆心移动，直到找到共同点
        LatLng start = mMultiPointCenters.get(0);
        
        if (isPointInAllCircles(start)) {
            return start;
        }
        
        // 使用网格搜索找到共同区域内的点
        double minLat = Math.min(Math.min(mMultiPointCenters.get(0).latitude, mMultiPointCenters.get(1).latitude), 
                                  mMultiPointCenters.get(2).latitude);
        double maxLat = Math.max(Math.max(mMultiPointCenters.get(0).latitude, mMultiPointCenters.get(1).latitude), 
                                  mMultiPointCenters.get(2).latitude);
        double minLon = Math.min(Math.min(mMultiPointCenters.get(0).longitude, mMultiPointCenters.get(1).longitude), 
                                  mMultiPointCenters.get(2).longitude);
        double maxLon = Math.max(Math.max(mMultiPointCenters.get(0).longitude, mMultiPointCenters.get(1).longitude), 
                                  mMultiPointCenters.get(2).longitude);
        
        // 扩展搜索范围
        double latStep = (maxLat - minLat) / 20;
        double lonStep = (maxLon - minLon) / 20;
        
        for (double lat = minLat; lat <= maxLat; lat += latStep) {
            for (double lon = minLon; lon <= maxLon; lon += lonStep) {
                LatLng test = new LatLng(lat, lon);
                if (isPointInAllCircles(test)) {
                    return test;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 计算两点之间的距离（米）
     */
    private double calculateDistance(LatLng p1, LatLng p2) {
        double lat1 = Math.toRadians(p1.latitude);
        double lat2 = Math.toRadians(p2.latitude);
        double deltaLat = Math.toRadians(p2.latitude - p1.latitude);
        double deltaLon = Math.toRadians(p2.longitude - p1.longitude);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return 6371000 * c; // 地球半径6371km
    }
    
    /**
     * 显示相交点
     */
    private void showIntersectionPoint(LatLng point) {
        // 生成相交点名称
        String intersectionName = "相交点_" + System.currentTimeMillis();
        
        // 保存为固定点
        saveIntersectionAsPinnedLocation(intersectionName, point.latitude, point.longitude);
        
        // 创建固定点样式的标记
        BitmapDescriptor pinnedIcon = createPinnedMarkerIcon(intersectionName);
        mIntersectionMarker = mAMap.addMarker(new MarkerOptions()
            .position(point)
            .title("已固定:" + intersectionName)
            .snippet(String.format("%.6f, %.6f", point.longitude, point.latitude))
            .icon(pinnedIcon));
        mIntersectionMarker.setObject("pinned_location");
        
        // 添加到固定点列表
        Map<String, Object> data = new HashMap<>();
        data.put("name", intersectionName);
        data.put("lat", point.latitude);
        data.put("lon", point.longitude);
        data.put("marker", mIntersectionMarker);
        mPinnedLocationMarkers.add(mIntersectionMarker);
        mPinnedLocationData.add(data);
        
        // 移动到该点
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 18));
        
        GoUtils.DisplayToast(this, "相交点已固定: " + intersectionName);
    }
    
    /**
     * 保存相交点为固定位置
     */
    private void saveIntersectionAsPinnedLocation(String name, double lat, double lon) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DataBasePinnedLocations.DB_COLUMN_NAME, name);
        contentValues.put(DataBasePinnedLocations.DB_COLUMN_LATITUDE, String.valueOf(lat));
        contentValues.put(DataBasePinnedLocations.DB_COLUMN_LONGITUDE, String.valueOf(lon));
        contentValues.put(DataBasePinnedLocations.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
        
        DataBasePinnedLocations.savePinnedLocation(mPinnedLocationsDB, contentValues);
        XLog.i("相交点已保存为固定点: " + name);
    }
    
    /**
     * 显示相交区域（基于实际的相交点）
     * 当有多个圆周相交点时，显示这些点构成的区域
     */
    private void showIntersectionRegion(List<LatLng> points) {
        if (points.isEmpty()) return;
        
        // 计算相交区域的中心（所有相交点的重心）
        double avgLat = 0, avgLon = 0;
        for (LatLng point : points) {
            avgLat += point.latitude;
            avgLon += point.longitude;
        }
        avgLat /= points.size();
        avgLon /= points.size();
        LatLng center = new LatLng(avgLat, avgLon);
        
        // 计算相交区域的近似半径（取到各相交点距离的最大值）
        double maxDist = 0;
        for (LatLng point : points) {
            maxDist = Math.max(maxDist, calculateDistance(center, point));
        }
        
        // 显示相交区域
        showIntersectionArea(center, Math.max(maxDist, 10)); // 最小10米
        
        // 标记所有圆周相交点，并保存为固定点
        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            String pointName = "圆周相交点_" + (i + 1) + "_" + System.currentTimeMillis();
            
            // 保存为固定点
            saveIntersectionAsPinnedLocation(pointName, point.latitude, point.longitude);
            
            // 创建固定点样式的标记
            BitmapDescriptor pinnedIcon = createPinnedMarkerIcon(pointName);
            Marker marker = mAMap.addMarker(new MarkerOptions()
                .position(point)
                .title("已固定:" + pointName)
                .snippet(String.format("%.6f, %.6f", point.longitude, point.latitude))
                .icon(pinnedIcon));
            marker.setObject("pinned_location");
            
            // 添加到固定点列表
            Map<String, Object> data = new HashMap<>();
            data.put("name", pointName);
            data.put("lat", point.latitude);
            data.put("lon", point.longitude);
            data.put("marker", marker);
            mPinnedLocationMarkers.add(marker);
            mPinnedLocationData.add(data);
        }
        
        GoUtils.DisplayToast(this, "找到 " + points.size() + " 个圆周相交点");
    }
    
    /**
     * 显示相交区域
     * @param center 区域中心点
     * @param radius 区域半径（米）
     */
    private void showIntersectionArea(LatLng center, double radius) {
        // 绘制淡红色相交区域
        mIntersectionCircle = mAMap.addCircle(new CircleOptions()
            .center(center)
            .radius(radius)
            .fillColor(0x55FF4444) // 淡红色半透明填充
            .strokeColor(0xFFFF0000) // 红色边框
            .strokeWidth(2));
        
        // 生成相交中心点名称并保存为固定点
        String centerName = "相交中心_" + System.currentTimeMillis();
        saveIntersectionAsPinnedLocation(centerName, center.latitude, center.longitude);
        
        // 创建固定点样式的中心标记
        BitmapDescriptor pinnedIcon = createPinnedMarkerIcon(centerName);
        mIntersectionCenterMarker = mAMap.addMarker(new MarkerOptions()
            .position(center)
            .title("已固定:" + centerName)
            .snippet(String.format("%.6f, %.6f", center.longitude, center.latitude))
            .icon(pinnedIcon));
        mIntersectionCenterMarker.setObject("pinned_location");
        
        // 添加到固定点列表
        Map<String, Object> data = new HashMap<>();
        data.put("name", centerName);
        data.put("lat", center.latitude);
        data.put("lon", center.longitude);
        data.put("marker", mIntersectionCenterMarker);
        mPinnedLocationMarkers.add(mIntersectionCenterMarker);
        mPinnedLocationData.add(data);
        
        // 移动到该区域
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 16));
        
        GoUtils.DisplayToast(this, "相交区域中心已固定: " + centerName);
    }
    
    /**
     * 清除多点定位标记
     */
    private void clearMultiPointMarks() {
        // 移除所有圆
        for (Circle circle : mMultiPointCircles) {
            if (circle != null) {
                circle.remove();
            }
        }
        mMultiPointCircles.clear();
        
        // 移除所有标记
        for (Marker marker : mMultiPointMarkers) {
            if (marker != null) {
                marker.remove();
            }
        }
        mMultiPointMarkers.clear();
        
        // 清空中心点和半径数据
        mMultiPointCenters.clear();
        mMultiPointRadius.clear();
        
        // 重置选中状态
        mSelectedCircle = null;
        mSelectedCircleIndex = -1;
        mSelectedCircleCenter = null;
        mSelectedCircleRadius = 0;
        
        clearIntersectionMarks();
        
        XLog.i("多点定位标记已完全清除");
    }

    /**
     * 清除所有临时地图覆盖物
     * 注意：不清除固定点、固定区域和当前临时标记
     */
    private void clearAllMapOverlays() {
        // 清除多点定位相关（临时圆）
        for (Circle circle : mMultiPointCircles) {
            if (circle != null) {
                circle.remove();
            }
        }
        mMultiPointCircles.clear();
        mMultiPointCenters.clear();
        mMultiPointRadius.clear();

        // 清除圆标记（临时圆标记）
        for (Marker marker : mMultiPointMarkers) {
            if (marker != null) {
                marker.remove();
            }
        }
        mMultiPointMarkers.clear();

        // 注意：不清除固定圆和固定位置标记
        // 固定内容应该一直显示在地图上，除非用户主动取消固定

        // 清除当前临时标记
        if (mCurrentLocationMarker != null) {
            mCurrentLocationMarker.remove();
            mCurrentLocationMarker = null;
        }
        sTempMarker = null; // 同时清空静态变量
        mIsCurrentLocationPinned = false;

        // 重置选中状态
        mSelectedCircle = null;
        mSelectedCircleIndex = -1;
        mSelectedCircleCenter = null;
        mSelectedCircleRadius = 0;

        clearIntersectionMarks();

        XLog.i("临时地图覆盖物已清除（固定点和固定区域保留）");
    }

    /**
     * 清除相交区域标记
     */
    private void clearIntersectionMarks() {
        if (mIntersectionCircle != null) {
            mIntersectionCircle.remove();
            mIntersectionCircle = null;
        }
        if (mIntersectionMarker != null) {
            mIntersectionMarker.remove();
            mIntersectionMarker = null;
        }
        if (mIntersectionCenterMarker != null) {
            mIntersectionCenterMarker.remove();
            mIntersectionCenterMarker = null;
        }
    }

    /**
     * 使用三边测量/多边测量计算目标点
     * 通过最小二乘法找到使得到各圆心距离与半径差最小的点
     */
    private LatLng calculateTrilaterationPoint() {
        if (mMultiPointCenters.size() < 3) return null;

        // 初始猜测：圆心的平均值
        double lat = 0, lon = 0;
        for (LatLng center : mMultiPointCenters) {
            lat += center.latitude;
            lon += center.longitude;
        }
        lat /= mMultiPointCenters.size();
        lon /= mMultiPointCenters.size();

        // 迭代优化（梯度下降）
        double stepSize = 0.00001; // 约1米的步长
        int maxIterations = 1000;

        for (int iter = 0; iter < maxIterations; iter++) {
            double gradLat = 0, gradLon = 0;

            for (int i = 0; i < mMultiPointCenters.size(); i++) {
                LatLng center = mMultiPointCenters.get(i);
                double radius = mMultiPointRadius.get(i);

                double dist = calculateDistance(new LatLng(lat, lon), center);
                if (dist < 0.1) continue; // 避免除零

                double error = dist - radius;

                // 计算梯度
                gradLat += 2 * error * (lat - center.latitude) / dist;
                gradLon += 2 * error * (lon - center.longitude) / dist;
            }

            // 更新位置
            lat -= stepSize * gradLat;
            lon -= stepSize * gradLon;

            // 检查收敛
            if (Math.abs(gradLat) < 1e-10 && Math.abs(gradLon) < 1e-10) {
                break;
            }
        }

        return new LatLng(lat, lon);
    }

    /**
     * 计算多个点的平均值
     */
    private LatLng calculateAveragePoint(List<LatLng> points) {
        double lat = 0, lon = 0;
        for (LatLng point : points) {
            lat += point.latitude;
            lon += point.longitude;
        }
        return new LatLng(lat / points.size(), lon / points.size());
    }

    /**
     * 显示估计的目标点
     */
    private void showEstimatedTargetPoint(LatLng point) {
        // 添加红色目标点标记
        BitmapDescriptor redDot = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        mIntersectionMarker = mAMap.addMarker(new MarkerOptions()
            .position(point)
            .title("估计目标点")
            .snippet("坐标: " + String.format("%.6f, %.6f", point.latitude, point.longitude))
            .icon(redDot));

        // 计算与各圆心的距离误差
        StringBuilder errorInfo = new StringBuilder();
        for (int i = 0; i < mMultiPointCenters.size(); i++) {
            double actualDist = calculateDistance(point, mMultiPointCenters.get(i));
            double expectedDist = mMultiPointRadius.get(i);
            double error = Math.abs(actualDist - expectedDist);
            errorInfo.append(String.format("圆%d: 误差%.1fm\n", i + 1, error));
        }

        // 移动到该位置
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 17));

        XLog.i("估计目标点: " + point + ", 误差:\n" + errorInfo.toString());
        GoUtils.DisplayToast(this, "目标点: " + String.format("%.6f, %.6f", point.latitude, point.longitude));
    }

    /*============================== 收藏功能 相关 ==============================*/
    
    /**
     * 显示添加收藏按钮（长按地图后显示）
     */
    private void showAddToFavoriteButton(LatLng point) {
        // 使用Snackbar显示添加收藏按钮
        com.google.android.material.snackbar.Snackbar snackbar = com.google.android.material.snackbar.Snackbar
                .make(mMapView, "坐标: " + String.format("%.4f, %.4f", point.latitude, point.longitude), 
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.btn_add_to_favorite), v -> {
                    showAddFavoriteDialog(point);
                });
        snackbar.show();
    }
    
    /**
     * 显示添加收藏对话框
     */
    private void showAddFavoriteDialog(LatLng point) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView tvCoordinates = view.findViewById(R.id.tv_coordinates);
        EditText etName = view.findViewById(R.id.et_favorite_name);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);

        // 直接使用地图上显示的 GCJ02 坐标，不进行转换
        double lon = point.longitude;
        double lat = point.latitude;

        // 显示格式：经度 纬度
        tvCoordinates.setText(String.format("%.6f %.6f", lon, lat));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                name = String.format("%.4f %.4f 附近", lon, lat);
            }

            // 保存到收藏（直接使用 GCJ02 坐标，与地图上显示的一致）
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataBaseFavorites.DB_COLUMN_NAME, name);
            contentValues.put(DataBaseFavorites.DB_COLUMN_LONGITUDE, String.valueOf(lon));
            contentValues.put(DataBaseFavorites.DB_COLUMN_LATITUDE, String.valueOf(lat));
            contentValues.put(DataBaseFavorites.DB_COLUMN_TIMESTAMP, System.currentTimeMillis());

            DataBaseFavorites.saveFavorite(mFavoritesDB, contentValues);

            GoUtils.DisplayToast(this, getString(R.string.favorite_added));
            dialog.dismiss();
        });

        dialog.show();
        GoUtils.setDialogWidth(dialog);
    }

    /**
     * 显示收藏列表对话框（包含坐标和区域），支持多选删除
     */
    private void showFavoritesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_favorites, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // 获取视图
        ListView lvFavorites = view.findViewById(R.id.lv_favorites);
        ListView lvRegions = view.findViewById(R.id.lv_regions);
        View containerCoordinates = view.findViewById(R.id.container_coordinates);
        View containerRegions = view.findViewById(R.id.container_regions);
        com.google.android.material.button.MaterialButton btnCoordinates = view.findViewById(R.id.btn_coordinates);
        com.google.android.material.button.MaterialButton btnRegions = view.findViewById(R.id.btn_regions);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        com.google.android.material.button.MaterialButton btnClear = view.findViewById(R.id.btn_clear);
        com.google.android.material.button.MaterialButton btnShare = view.findViewById(R.id.btn_share);
        Button btnClose = view.findViewById(R.id.btn_close);

        // 加载坐标收藏数据
        List<Map<String, Object>> favorites = getFavoritesData();
        // 加载区域收藏数据
        List<Map<String, Object>> regions = getFavoriteRegionsData();

        XLog.i("我的收藏: 加载 " + favorites.size() + " 个坐标收藏, " + regions.size() + " 个区域收藏");

        if (favorites.isEmpty() && regions.isEmpty()) {
            GoUtils.DisplayToast(this, "暂无收藏");
            dialog.dismiss();
            return;
        }

        // 创建适配器（使用final数组以便在监听器中访问）
        final FavoriteSelectAdapter[] favoritesAdapter = new FavoriteSelectAdapter[1];
        final FavoriteSelectAdapter[] regionsAdapter = new FavoriteSelectAdapter[1];

        favoritesAdapter[0] = new FavoriteSelectAdapter(this, favorites);
        regionsAdapter[0] = new FavoriteSelectAdapter(this, regions);

        lvFavorites.setAdapter(favoritesAdapter[0]);
        lvRegions.setAdapter(regionsAdapter[0]);

        // 选择状态变化监听，更新按钮和提示
        FavoriteSelectAdapter.OnSelectionChangeListener selectionListener = selectedCount -> {
            boolean showingCoordinates = containerCoordinates.getVisibility() == View.VISIBLE;
            FavoriteSelectAdapter currentAdapter = showingCoordinates ? favoritesAdapter[0] : regionsAdapter[0];
            int count = currentAdapter.getSelectedPositions().size();

            if (count > 0) {
                btnClear.setText("删除 (" + count + ")");
                btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_error)));
                btnClear.setTextColor(getColor(R.color.md_onError));
                tvHint.setText("已选择 " + count + " 项，点击删除或分享按钮");
                // 当选中数>=1时显示分享按钮
                btnShare.setVisibility(View.VISIBLE);
            } else {
                btnClear.setText("清除");
                btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnClear.setTextColor(getColor(R.color.md_primary));
                tvHint.setText("点击右侧圆圈选择，点击条目在地图上显示，长按编辑名称");
                btnShare.setVisibility(View.GONE);
            }
        };

        favoritesAdapter[0].setOnSelectionChangeListener(selectionListener);
        regionsAdapter[0].setOnSelectionChangeListener(selectionListener);

        // 设置点击监听器 - 点击圆圈选择，点击条目内容在地图上显示
        favoritesAdapter[0].setOnItemClickListener(new FavoriteSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // 点击条目内容区域 - 在地图上显示
                Map<String, Object> item = favorites.get(position);
                String name = (String) item.get("name");
                double lat = (Double) item.get("lat");
                double lon = (Double) item.get("lon");
                showLocation(name, String.valueOf(lon), String.valueOf(lat));
                dialog.dismiss();
            }

            @Override
            public void onCheckClick(int position) {
                // 点击圆圈 - 触发选择
                favoritesAdapter[0].toggleSelection(position);
            }
        });

        regionsAdapter[0].setOnItemClickListener(new FavoriteSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // 点击条目内容区域 - 在地图上显示
                Map<String, Object> item = regions.get(position);
                String name = (String) item.get("name");
                double lat = (double) item.get("lat");
                double lon = (double) item.get("lon");
                double radius = (double) item.get("radius");
                showRegion(name, lon, lat, radius);
                dialog.dismiss();
            }

            @Override
            public void onCheckClick(int position) {
                // 点击圆圈 - 触发选择
                regionsAdapter[0].toggleSelection(position);
            }
        });
        
        // 清除ListView的默认点击监听（因为我们已经在adapter中处理了）
        lvFavorites.setOnItemClickListener(null);
        lvRegions.setOnItemClickListener(null);

        // 长按编辑坐标收藏名称
        lvFavorites.setOnItemLongClickListener((parent, v, position, id) -> {
            Map<String, Object> item = favorites.get(position);
            String favId = (String) item.get(DataBaseFavorites.DB_COLUMN_ID);
            String currentName = (String) item.get("name");

            final EditText input = new EditText(this);
            input.setText(currentName);
            input.setSelection(currentName.length());

            new AlertDialog.Builder(this)
                    .setTitle("修改名称")
                    .setView(input)
                    .setPositiveButton("确认", (d, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !newName.equals(currentName)) {
                            DataBaseFavorites.updateFavoriteName(mFavoritesDB, favId, newName);
                            // 刷新数据
                            favorites.clear();
                            favorites.addAll(getFavoritesData());
                            favoritesAdapter[0].updateData(favorites);
                            GoUtils.DisplayToast(this, "名称已修改");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 长按编辑区域收藏名称
        lvRegions.setOnItemLongClickListener((parent, v, position, id) -> {
            Map<String, Object> item = regions.get(position);
            String regionId = (String) item.get(DataBaseFavoriteRegions.DB_COLUMN_ID);
            String currentName = (String) item.get("name");

            final EditText input = new EditText(this);
            input.setText(currentName);
            input.setSelection(currentName.length());

            new AlertDialog.Builder(this)
                    .setTitle("修改名称")
                    .setView(input)
                    .setPositiveButton("确认", (d, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !newName.equals(currentName)) {
                            DataBaseFavoriteRegions.updateFavoriteRegionName(mFavoriteRegionsDB, regionId, newName);
                            // 刷新数据
                            regions.clear();
                            regions.addAll(getFavoriteRegionsData());
                            regionsAdapter[0].updateData(regions);
                            GoUtils.DisplayToast(this, "名称已修改");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 加载切换动画
        Animation switchFadeIn = AnimationUtils.loadAnimation(this, R.anim.switch_fade_in);
        Animation switchFadeOut = AnimationUtils.loadAnimation(this, R.anim.switch_fade_out);

        // 切换按钮点击事件
        btnCoordinates.setOnClickListener(v -> {
            if (containerRegions.getVisibility() == View.VISIBLE) {
                // 使用切换动画
                containerRegions.startAnimation(switchFadeOut);
                containerRegions.setVisibility(View.GONE);
                containerCoordinates.setVisibility(View.VISIBLE);
                containerCoordinates.startAnimation(switchFadeIn);
                // 更新按钮样式
                btnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
                btnCoordinates.setTextColor(getColor(R.color.md_onPrimary));
                btnCoordinates.setElevation(getResources().getDimension(R.dimen.elevation_low));
                btnRegions.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnRegions.setTextColor(getColor(R.color.md_onSurfaceVariant));
                btnRegions.setElevation(0);
                // 更新提示文字
                selectionListener.onSelectionChanged(0);
            }
        });

        btnRegions.setOnClickListener(v -> {
            if (containerCoordinates.getVisibility() == View.VISIBLE) {
                // 使用切换动画
                containerCoordinates.startAnimation(switchFadeOut);
                containerCoordinates.setVisibility(View.GONE);
                containerRegions.setVisibility(View.VISIBLE);
                containerRegions.startAnimation(switchFadeIn);
                // 更新按钮样式
                btnRegions.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
                btnRegions.setTextColor(getColor(R.color.md_onPrimary));
                btnRegions.setElevation(getResources().getDimension(R.dimen.elevation_low));
                btnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnCoordinates.setTextColor(getColor(R.color.md_onSurfaceVariant));
                btnCoordinates.setElevation(0);
                // 更新提示文字
                selectionListener.onSelectionChanged(0);
            }
        });

        // 清除/删除按钮点击事件
        btnClear.setOnClickListener(v -> {
            boolean showingCoordinates = containerCoordinates.getVisibility() == View.VISIBLE;
            FavoriteSelectAdapter currentAdapter = showingCoordinates ? favoritesAdapter[0] : regionsAdapter[0];
            List<Map<String, Object>> currentList = showingCoordinates ? favorites : regions;
            String typeName = showingCoordinates ? "坐标" : "区域";
            
            List<Map<String, Object>> selectedItems = currentAdapter.getSelectedItems();
            
            if (selectedItems.isEmpty()) {
                // 没有选中项，执行清除全部操作
                new AlertDialog.Builder(this)
                        .setTitle("清除" + typeName + "收藏")
                        .setMessage("确定要删除所有" + typeName + "收藏吗？此操作不可恢复。")
                        .setPositiveButton("确定", (d, which) -> {
                            if (showingCoordinates) {
                                DataBaseFavorites.deleteAllFavorites(mFavoritesDB);
                                currentList.clear();
                                currentList.addAll(getFavoritesData());
                            } else {
                                DataBaseFavoriteRegions.deleteAllFavoriteRegions(mFavoriteRegionsDB);
                                currentList.clear();
                                currentList.addAll(getFavoriteRegionsData());
                            }
                            currentAdapter.updateData(currentList);

                            if (favorites.isEmpty() && regions.isEmpty()) {
                                dialog.dismiss();
                                GoUtils.DisplayToast(this, "暂无收藏");
                            } else {
                                GoUtils.DisplayToast(this, "已清除所有" + typeName + "收藏");
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                // 有选中项，删除选中的条目
                new AlertDialog.Builder(this)
                        .setTitle("删除选中" + typeName)
                        .setMessage("确定要删除选中的 " + selectedItems.size() + " 个" + typeName + "吗？")
                        .setPositiveButton("确定", (d, which) -> {
                            if (showingCoordinates) {
                                for (Map<String, Object> item : selectedItems) {
                                    String favId = (String) item.get(DataBaseFavorites.DB_COLUMN_ID);
                                    DataBaseFavorites.deleteFavorite(mFavoritesDB, favId);
                                }
                                currentList.clear();
                                currentList.addAll(getFavoritesData());
                            } else {
                                for (Map<String, Object> item : selectedItems) {
                                    String regionId = (String) item.get(DataBaseFavoriteRegions.DB_COLUMN_ID);
                                    DataBaseFavoriteRegions.deleteFavoriteRegion(mFavoriteRegionsDB, regionId);
                                }
                                currentList.clear();
                                currentList.addAll(getFavoriteRegionsData());
                            }
                            currentAdapter.clearSelection();
                            currentAdapter.updateData(currentList);

                            if (favorites.isEmpty() && regions.isEmpty()) {
                                dialog.dismiss();
                                GoUtils.DisplayToast(this, "暂无收藏");
                            } else {
                                GoUtils.DisplayToast(this, "已删除 " + selectedItems.size() + " 个" + typeName);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        // 分享按钮点击 - 批量分享选中的条目
        btnShare.setOnClickListener(v -> {
            boolean showingCoordinates = containerCoordinates.getVisibility() == View.VISIBLE;
            FavoriteSelectAdapter currentAdapter = showingCoordinates ? favoritesAdapter[0] : regionsAdapter[0];
            List<Map<String, Object>> selectedItems = currentAdapter.getSelectedItems();
            String typeName = showingCoordinates ? "坐标" : "区域";

            if (selectedItems.size() < 2) {
                GoUtils.DisplayToast(this, "请至少选择2个" + typeName + "进行批量分享");
                return;
            }

            // 显示批量分享选项
            showBatchShareDialog(selectedItems, typeName);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 初始化提示文字
        tvHint.setText("点击右侧圆圈选择，点击条目在地图上显示");

        dialog.show();
        GoUtils.setDialogWidth(dialog);

        // 设置缩放动画
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Animation_Zoom);
        }
    }

    /**
     * 显示固定管理对话框
     * 使用与我的收藏相同的多选UI风格
     */
    private void showPinnedManager() {
        // 获取固定坐标数据
        List<Map<String, Object>> pinnedLocations = getPinnedLocationsData();
        // 获取固定区域数据
        List<Map<String, Object>> pinnedCircles = getPinnedCirclesData();

        if (pinnedLocations.isEmpty() && pinnedCircles.isEmpty()) {
            GoUtils.DisplayToast(this, "暂无固定项");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_favorites, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // 获取视图
        ListView lvFavorites = view.findViewById(R.id.lv_favorites);
        ListView lvRegions = view.findViewById(R.id.lv_regions);
        View containerCoordinates = view.findViewById(R.id.container_coordinates);
        View containerRegions = view.findViewById(R.id.container_regions);
        com.google.android.material.button.MaterialButton btnCoordinates = view.findViewById(R.id.btn_coordinates);
        com.google.android.material.button.MaterialButton btnRegions = view.findViewById(R.id.btn_regions);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        com.google.android.material.button.MaterialButton btnClear = view.findViewById(R.id.btn_clear);
        com.google.android.material.button.MaterialButton btnShare = view.findViewById(R.id.btn_share);
        Button btnClose = view.findViewById(R.id.btn_close);

        // 修改标题为"固定管理"
        try {
            // 从dialog_favorites.xml中找到标题TextView（第一个TextView子元素）
            if (view instanceof android.view.ViewGroup) {
                android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View child = viewGroup.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setText("固定管理");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            XLog.e("设置固定管理标题失败: " + e.getMessage());
        }

        // 修改按钮文字
        btnCoordinates.setText("坐标");
        btnRegions.setText("区域");

        // 加载数据
        XLog.i("固定管理: 加载 " + pinnedLocations.size() + " 个固定坐标, " + pinnedCircles.size() + " 个固定区域");

        // 创建适配器
        final FavoriteSelectAdapter[] locationsAdapter = new FavoriteSelectAdapter[1];
        final FavoriteSelectAdapter[] circlesAdapter = new FavoriteSelectAdapter[1];

        locationsAdapter[0] = new FavoriteSelectAdapter(this, pinnedLocations);
        circlesAdapter[0] = new FavoriteSelectAdapter(this, pinnedCircles);

        lvFavorites.setAdapter(locationsAdapter[0]);
        lvRegions.setAdapter(circlesAdapter[0]);

        // 选择状态变化监听
        FavoriteSelectAdapter.OnSelectionChangeListener selectionListener = selectedCount -> {
            boolean showingCoordinates = containerCoordinates.getVisibility() == View.VISIBLE;
            FavoriteSelectAdapter currentAdapter = showingCoordinates ? locationsAdapter[0] : circlesAdapter[0];
            int count = currentAdapter.getSelectedPositions().size();
            
            if (count > 0) {
                btnClear.setText("取消固定 (" + count + ")");
                btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_error)));
                btnClear.setTextColor(getColor(R.color.md_onError));
                tvHint.setText("已选择 " + count + " 项，点击取消固定按钮解除固定");
                // 当选中数>=1时显示分享按钮
                btnShare.setVisibility(View.VISIBLE);
            } else {
                btnClear.setText("全部取消固定");
                btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnClear.setTextColor(getColor(R.color.md_primary));
                tvHint.setText("点击右侧圆圈选择，点击左侧在地图上显示，长按编辑名称");
                btnShare.setVisibility(View.GONE);
            }
        };

        locationsAdapter[0].setOnSelectionChangeListener(selectionListener);
        circlesAdapter[0].setOnSelectionChangeListener(selectionListener);

        // 设置点击监听器 - 点击圆圈选择，点击左侧内容在地图上显示
        locationsAdapter[0].setOnItemClickListener(new FavoriteSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // 点击左侧内容区域 - 在地图上显示
                Map<String, Object> item = pinnedLocations.get(position);
                String name = (String) item.get("name");
                double lat = (double) item.get("lat");
                double lon = (double) item.get("lon");
                showPinnedLocationOnMap(name, lat, lon);
                dialog.dismiss();
            }

            @Override
            public void onCheckClick(int position) {
                // 点击圆圈 - 触发选择
                locationsAdapter[0].toggleSelection(position);
            }
        });

        circlesAdapter[0].setOnItemClickListener(new FavoriteSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // 点击左侧内容区域 - 在地图上显示
                Map<String, Object> item = pinnedCircles.get(position);
                String name = (String) item.get("name");
                double lat = (double) item.get("lat");
                double lon = (double) item.get("lon");
                double radius = (double) item.get("radius");
                showPinnedRegionOnMap(name, lat, lon, radius);
                dialog.dismiss();
            }

            @Override
            public void onCheckClick(int position) {
                // 点击圆圈 - 触发选择
                circlesAdapter[0].toggleSelection(position);
            }
        });
        
        // 清除ListView的默认点击监听
        lvFavorites.setOnItemClickListener(null);
        lvRegions.setOnItemClickListener(null);

        // 长按编辑固定坐标名称
        lvFavorites.setOnItemLongClickListener((parent, v, position, id) -> {
            Map<String, Object> item = pinnedLocations.get(position);
            String locId = (String) item.get("id");
            String currentName = (String) item.get("name");

            final EditText input = new EditText(this);
            input.setText(currentName);
            input.setSelection(currentName.length());

            new AlertDialog.Builder(this)
                    .setTitle("修改名称")
                    .setView(input)
                    .setPositiveButton("确认", (d, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !newName.equals(currentName)) {
                            DataBasePinnedLocations.updatePinnedLocationName(mPinnedLocationsDB, locId, newName);
                            // 刷新数据
                            pinnedLocations.clear();
                            pinnedLocations.addAll(getPinnedLocationsData());
                            locationsAdapter[0].updateData(pinnedLocations);
                            GoUtils.DisplayToast(this, "名称已修改");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 长按编辑固定区域名称
        lvRegions.setOnItemLongClickListener((parent, v, position, id) -> {
            Map<String, Object> item = pinnedCircles.get(position);
            String circleId = (String) item.get("id");
            String currentName = (String) item.get("name");

            final EditText input = new EditText(this);
            input.setText(currentName);
            input.setSelection(currentName.length());

            new AlertDialog.Builder(this)
                    .setTitle("修改名称")
                    .setView(input)
                    .setPositiveButton("确认", (d, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !newName.equals(currentName)) {
                            DataBasePinnedCircles.updatePinnedCircleName(mPinnedCirclesDB, circleId, newName);
                            // 刷新数据
                            pinnedCircles.clear();
                            pinnedCircles.addAll(getPinnedCirclesData());
                            circlesAdapter[0].updateData(pinnedCircles);
                            GoUtils.DisplayToast(this, "名称已修改");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 加载切换动画
        Animation switchFadeIn = AnimationUtils.loadAnimation(this, R.anim.switch_fade_in);
        Animation switchFadeOut = AnimationUtils.loadAnimation(this, R.anim.switch_fade_out);

        // 默认显示坐标列表
        containerCoordinates.setVisibility(View.VISIBLE);
        containerRegions.setVisibility(View.GONE);
        // 使用dialog_favorites.xml的按钮样式
        btnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
        btnCoordinates.setTextColor(getColor(R.color.md_onPrimary));

        // 坐标按钮点击
        btnCoordinates.setOnClickListener(v -> {
            if (containerRegions.getVisibility() == View.VISIBLE) {
                containerRegions.startAnimation(switchFadeOut);
                containerRegions.setVisibility(View.GONE);
                containerCoordinates.setVisibility(View.VISIBLE);
                containerCoordinates.startAnimation(switchFadeIn);
                // 更新按钮样式
                btnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
                btnCoordinates.setTextColor(getColor(R.color.md_onPrimary));
                btnRegions.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnRegions.setTextColor(getColor(R.color.md_onSurfaceVariant));
                // 清除选择状态
                circlesAdapter[0].clearSelection();
                selectionListener.onSelectionChanged(0);
            }
        });

        // 区域按钮点击
        btnRegions.setOnClickListener(v -> {
            if (containerCoordinates.getVisibility() == View.VISIBLE) {
                containerCoordinates.startAnimation(switchFadeOut);
                containerCoordinates.setVisibility(View.GONE);
                containerRegions.setVisibility(View.VISIBLE);
                containerRegions.startAnimation(switchFadeIn);
                // 更新按钮样式
                btnRegions.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_primary)));
                btnRegions.setTextColor(getColor(R.color.md_onPrimary));
                btnCoordinates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnCoordinates.setTextColor(getColor(R.color.md_onSurfaceVariant));
                // 清除选择状态
                locationsAdapter[0].clearSelection();
                selectionListener.onSelectionChanged(0);
            }
        });

        // 取消固定按钮 - 取消选中的固定项
        btnClear.setOnClickListener(v -> {
            boolean showingCoordinates = containerCoordinates.getVisibility() == View.VISIBLE;
            FavoriteSelectAdapter currentAdapter = showingCoordinates ? locationsAdapter[0] : circlesAdapter[0];
            List<Map<String, Object>> currentList = showingCoordinates ? pinnedLocations : pinnedCircles;
            String typeName = showingCoordinates ? "坐标" : "区域";
            
            List<Map<String, Object>> selectedItems = currentAdapter.getSelectedItems();
            
            if (selectedItems.isEmpty()) {
                // 没有选中项，执行全部取消固定
                new AlertDialog.Builder(this)
                        .setTitle("取消固定所有" + typeName)
                        .setMessage("确定要取消固定所有" + typeName + "吗？此操作不可恢复。")
                        .setPositiveButton("确定", (d, which) -> {
                            if (showingCoordinates) {
                                // 取消固定所有坐标
                                for (Map<String, Object> item : currentList) {
                                    unpinLocationByData(item);
                                }
                                pinnedLocations.clear();
                                pinnedLocations.addAll(getPinnedLocationsData());
                            } else {
                                // 取消固定所有区域
                                for (Map<String, Object> item : currentList) {
                                    unpinCircleByData(item);
                                }
                                pinnedCircles.clear();
                                pinnedCircles.addAll(getPinnedCirclesData());
                            }
                            currentAdapter.updateData(currentList);
                            
                            if (pinnedLocations.isEmpty() && pinnedCircles.isEmpty()) {
                                dialog.dismiss();
                                GoUtils.DisplayToast(this, "暂无固定项");
                            } else {
                                GoUtils.DisplayToast(this, "已取消固定所有" + typeName);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                // 有选中项，取消固定选中的条目
                new AlertDialog.Builder(this)
                        .setTitle("取消固定选中" + typeName)
                        .setMessage("确定要取消固定选中的 " + selectedItems.size() + " 个" + typeName + "吗？")
                        .setPositiveButton("确定", (d, which) -> {
                            if (showingCoordinates) {
                                for (Map<String, Object> item : selectedItems) {
                                    unpinLocationByData(item);
                                }
                                pinnedLocations.clear();
                                pinnedLocations.addAll(getPinnedLocationsData());
                            } else {
                                for (Map<String, Object> item : selectedItems) {
                                    unpinCircleByData(item);
                                }
                                pinnedCircles.clear();
                                pinnedCircles.addAll(getPinnedCirclesData());
                            }
                            currentAdapter.clearSelection();
                            currentAdapter.updateData(currentList);
                            
                            if (pinnedLocations.isEmpty() && pinnedCircles.isEmpty()) {
                                dialog.dismiss();
                                GoUtils.DisplayToast(this, "暂无固定项");
                            } else {
                                GoUtils.DisplayToast(this, "已取消固定 " + selectedItems.size() + " 个" + typeName);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        // 分享按钮点击 - 批量分享选中的条目
        btnShare.setOnClickListener(v -> {
            boolean showingCoordinates = containerCoordinates.getVisibility() == View.VISIBLE;
            FavoriteSelectAdapter currentAdapter = showingCoordinates ? locationsAdapter[0] : circlesAdapter[0];
            List<Map<String, Object>> selectedItems = currentAdapter.getSelectedItems();
            String typeName = showingCoordinates ? "坐标" : "区域";

            if (selectedItems.size() < 2) {
                GoUtils.DisplayToast(this, "请至少选择2个" + typeName + "进行批量分享");
                return;
            }

            // 显示批量分享选项
            showBatchShareDialog(selectedItems, typeName);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 初始化提示文字
        tvHint.setText("点击右侧圆圈选择，点击条目在地图上显示，长按编辑名称");

        dialog.show();
        GoUtils.setDialogWidth(dialog);

        // 设置缩放动画
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Animation_Zoom);
        }
    }

    /**
     * 显示模拟记录对话框
     * 使用与我的收藏相同的多选UI风格
     */
    private void showSimulationRecordsDialog() {
        // 检查数据库是否初始化
        if (mSimulationRecordsDB == null) {
            GoUtils.DisplayToast(this, "数据库未初始化，请重启应用");
            return;
        }

        // 获取模拟记录数据
        List<Map<String, Object>> recordsData = getSimulationRecordsData();

        if (recordsData == null || recordsData.isEmpty()) {
            GoUtils.DisplayToast(this, "暂无模拟记录");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_history, null);
        // 修改标题
        TextView tvTitle = view.findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setText("模拟记录");
        }
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // 获取视图
        ListView lvHistory = view.findViewById(R.id.lv_history);
        View containerHistory = view.findViewById(R.id.container_history);
        TextView tvNoRecord = view.findViewById(R.id.tv_no_record);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        SearchView searchView = view.findViewById(R.id.search_view);
        com.google.android.material.button.MaterialButton btnClear = view.findViewById(R.id.btn_clear);
        com.google.android.material.button.MaterialButton btnShare = view.findViewById(R.id.btn_share);
        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.btn_close);

        // 加载数据
        XLog.i("模拟记录: 加载 " + recordsData.size() + " 条记录");

        // 创建适配器
        final FavoriteSelectAdapter[] historyAdapter = new FavoriteSelectAdapter[1];
        historyAdapter[0] = new FavoriteSelectAdapter(this, recordsData);
        lvHistory.setAdapter(historyAdapter[0]);

        // 存储原始数据用于搜索过滤
        final List<Map<String, Object>> originalData = new ArrayList<>(recordsData);

        // 选择状态变化监听
        FavoriteSelectAdapter.OnSelectionChangeListener selectionListener = selectedCount -> {
            int count = historyAdapter[0].getSelectedPositions().size();

            if (count > 0) {
                btnClear.setText("删除 (" + count + ")");
                btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.md_error)));
                btnClear.setTextColor(getColor(R.color.md_onError));
                tvHint.setText("已选择 " + count + " 项，点击删除或分享按钮");
                // 当选中数>=1时显示分享按钮
                btnShare.setVisibility(View.VISIBLE);
            } else {
                btnClear.setText("全部删除");
                btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(android.R.color.transparent)));
                btnClear.setTextColor(getColor(R.color.md_primary));
                tvHint.setText("点击右侧圆圈选择，点击条目在地图上显示，长按编辑名称");
                btnShare.setVisibility(View.GONE);
            }
        };

        historyAdapter[0].setOnSelectionChangeListener(selectionListener);

        // 设置点击监听器 - 点击圆圈选择，点击条目内容在地图上显示
        historyAdapter[0].setOnItemClickListener(new FavoriteSelectAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // 点击条目内容区域 - 在地图上显示
                Map<String, Object> item = recordsData.get(position);
                String name = (String) item.get("name");
                double lat = (double) item.get("lat");
                double lon = (double) item.get("lon");
                showLocation(name, String.valueOf(lon), String.valueOf(lat));
                dialog.dismiss();
            }

            @Override
            public void onCheckClick(int position) {
                // 点击圆圈 - 触发选择
                historyAdapter[0].toggleSelection(position);
            }
        });

        // 清除ListView的默认点击监听
        lvHistory.setOnItemClickListener(null);

        // 长按编辑模拟记录名称
        lvHistory.setOnItemLongClickListener((parent, v, position, id) -> {
            Map<String, Object> item = recordsData.get(position);
            String locId = (String) item.get("id");
            String currentName = (String) item.get("name");

            final EditText input = new EditText(this);
            input.setText(currentName);
            input.setSelection(currentName.length());

            new AlertDialog.Builder(this)
                    .setTitle("修改名称")
                    .setView(input)
                    .setPositiveButton("确认", (d, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty() && !newName.equals(currentName)) {
                            DataBaseSimulationRecords.updateSimulationRecordName(mSimulationRecordsDB, locId, newName);
                            // 刷新数据
                            recordsData.clear();
                            recordsData.addAll(getSimulationRecordsData());
                            originalData.clear();
                            originalData.addAll(recordsData);
                            historyAdapter[0].clearSelection();
                            historyAdapter[0].updateData(recordsData);
                            GoUtils.DisplayToast(this, "名称已修改");
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });

        // 搜索功能
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    // 恢复原始数据
                    recordsData.clear();
                    recordsData.addAll(originalData);
                } else {
                    // 过滤数据
                    List<Map<String, Object>> filteredData = new ArrayList<>();
                    for (Map<String, Object> item : originalData) {
                        String name = (String) item.get("name");
                        String coords = (String) item.get("coords");
                        if ((name != null && name.toLowerCase().contains(newText.toLowerCase())) ||
                            (coords != null && coords.contains(newText))) {
                            filteredData.add(item);
                        }
                    }
                    recordsData.clear();
                    recordsData.addAll(filteredData);
                }
                historyAdapter[0].clearSelection();
                historyAdapter[0].updateData(recordsData);
                return false;
            }
        });

        // 删除按钮点击 - 删除选中的模拟记录
        btnClear.setOnClickListener(v -> {
            List<Map<String, Object>> selectedItems = historyAdapter[0].getSelectedItems();

            if (selectedItems.isEmpty()) {
                // 没有选中项，执行全部删除
                new AlertDialog.Builder(this)
                        .setTitle("删除全部模拟记录")
                        .setMessage("确定要删除全部模拟记录吗？此操作不可恢复。")
                        .setPositiveButton("确定", (d, which) -> {
                            try {
                                DataBaseSimulationRecords.clearAllSimulationRecords(mSimulationRecordsDB);
                                GoUtils.DisplayToast(this, "已删除全部模拟记录");
                                dialog.dismiss();
                            } catch (Exception e) {
                                XLog.e("删除全部模拟记录失败: " + e.getMessage());
                                GoUtils.DisplayToast(this, "删除失败");
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                // 删除选中的记录
                new AlertDialog.Builder(this)
                        .setTitle("删除选中记录")
                        .setMessage("确定要删除选中的 " + selectedItems.size() + " 条模拟记录吗？")
                        .setPositiveButton("确定", (d, which) -> {
                            try {
                                for (Map<String, Object> item : selectedItems) {
                                    String id = (String) item.get("id");
                                    DataBaseSimulationRecords.deleteSimulationRecord(mSimulationRecordsDB, id);
                                }
                                GoUtils.DisplayToast(this, "已删除 " + selectedItems.size() + " 条记录");
                                // 刷新数据
                                recordsData.clear();
                                recordsData.addAll(getSimulationRecordsData());
                                originalData.clear();
                                originalData.addAll(recordsData);
                                historyAdapter[0].clearSelection();
                                historyAdapter[0].updateData(recordsData);
                                if (recordsData.isEmpty()) {
                                    dialog.dismiss();
                                }
                            } catch (Exception e) {
                                XLog.e("删除历史记录失败: " + e.getMessage());
                                GoUtils.DisplayToast(this, "删除失败");
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        // 分享按钮点击 - 批量分享选中的条目
        btnShare.setOnClickListener(v -> {
            List<Map<String, Object>> selectedItems = historyAdapter[0].getSelectedItems();

            if (selectedItems.isEmpty()) {
                GoUtils.DisplayToast(this, "请至少选择1条记录进行分享");
                return;
            }

            // 显示批量分享选项
            showBatchShareDialog(selectedItems, "历史记录");
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // 设置缩放动画
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.AppTheme_Animation_Zoom);
        }
    }

    /**
     * 获取固定坐标数据
     */
    private List<Map<String, Object>> getPinnedLocationsData() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            List<Map<String, Object>> pinnedLocations = DataBasePinnedLocations.getAllPinnedLocations(mPinnedLocationsDB);
            for (Map<String, Object> pinned : pinnedLocations) {
                Map<String, Object> item = new HashMap<>();
                String id = (String) pinned.get(DataBasePinnedLocations.DB_COLUMN_ID);
                String name = (String) pinned.get(DataBasePinnedLocations.DB_COLUMN_NAME);
                double lat = Double.parseDouble((String) pinned.get(DataBasePinnedLocations.DB_COLUMN_LATITUDE));
                double lon = Double.parseDouble((String) pinned.get(DataBasePinnedLocations.DB_COLUMN_LONGITUDE));

                item.put("id", id);
                item.put("name", name);
                item.put("lat", lat);
                item.put("lon", lon);
                item.put("coords", String.format("%.6f, %.6f", lat, lon));
                data.add(item);
            }
        } catch (Exception e) {
            XLog.e("获取固定坐标数据失败: " + e.getMessage());
        }
        return data;
    }

    /**
     * 获取固定区域数据
     */
    private List<Map<String, Object>> getPinnedCirclesData() {
        List<Map<String, Object>> data = new ArrayList<>();
        try {
            List<Map<String, Object>> pinnedCircles = DataBasePinnedCircles.getAllPinnedCircles(mPinnedCirclesDB);
            for (Map<String, Object> pinned : pinnedCircles) {
                Map<String, Object> item = new HashMap<>();
                String id = (String) pinned.get(DataBasePinnedCircles.DB_COLUMN_ID);
                String name = (String) pinned.get(DataBasePinnedCircles.DB_COLUMN_NAME);
                double lat = Double.parseDouble((String) pinned.get(DataBasePinnedCircles.DB_COLUMN_CENTER_LAT));
                double lon = Double.parseDouble((String) pinned.get(DataBasePinnedCircles.DB_COLUMN_CENTER_LON));
                double radius = Double.parseDouble((String) pinned.get(DataBasePinnedCircles.DB_COLUMN_RADIUS));

                item.put("id", id);
                item.put("name", name);
                item.put("lat", lat);
                item.put("lon", lon);
                item.put("radius", radius);
                item.put("coords", String.format("半径 %.0f 米", radius));
                data.add(item);
            }
        } catch (Exception e) {
            XLog.e("获取固定区域数据失败: " + e.getMessage());
        }
        return data;
    }

    /**
     * 根据数据取消固定坐标点
     */
    private void unpinLocationByData(Map<String, Object> item) {
        String id = (String) item.get("id");
        String name = (String) item.get("name");
        double lat = (double) item.get("lat");
        double lon = (double) item.get("lon");

        // 从数据库删除
        DataBasePinnedLocations.deletePinnedLocation(mPinnedLocationsDB, id);

        // 在列表中查找并移除
        int index = -1;
        for (int i = 0; i < mPinnedLocationData.size(); i++) {
            Map<String, Object> data = mPinnedLocationData.get(i);
            String pinnedName = (String) data.get("name");
            if (pinnedName.equals(name)) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            Marker marker = mPinnedLocationMarkers.get(index);

            // 将标记变回未固定状态
            marker.setIcon(mMapIndicator);
            marker.setTitle(name);
            marker.setSnippet(String.format("%.6f, %.6f", marker.getPosition().longitude, marker.getPosition().latitude));
            marker.setObject("temp_location");

            // 从固定列表移除
            mPinnedLocationMarkers.remove(index);
            mPinnedLocationData.remove(index);

            // 设置为临时标记
            long newTimestamp = System.currentTimeMillis();
            if (sTempMarker != null && sTempMarker != marker && sTempMarkerTimestamp > 0) {
                sTempMarker.remove();
            }

            mCurrentLocationMarker = marker;
            sTempMarker = marker;
            mCurrentLocationMarkerTimestamp = newTimestamp;
            sTempMarkerTimestamp = newTimestamp;
            mIsCurrentLocationPinned = false;
            mMarkLatLngMap = marker.getPosition();
            mMarkName = name;
        }

        XLog.i("取消固定坐标: " + name);
    }

    /**
     * 根据数据取消固定圆
     */
    private void unpinCircleByData(Map<String, Object> item) {
        String id = (String) item.get("id");
        String name = (String) item.get("name");
        double lat = (double) item.get("lat");
        double lon = (double) item.get("lon");
        double radius = (double) item.get("radius");

        // 从数据库删除
        DataBasePinnedCircles.deletePinnedCircle(mPinnedCirclesDB, id);

        // 在列表中查找并移除
        int index = -1;
        for (int i = 0; i < mPinnedCircleData.size(); i++) {
            Map<String, Object> data = mPinnedCircleData.get(i);
            String pinnedName = (String) data.get("name");
            if (pinnedName.equals(name)) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            Circle circle = mPinnedCircles.get(index);
            Marker marker = mPinnedCircleMarkers.get(index);

            // 从固定列表移除
            mPinnedCircles.remove(index);
            mPinnedCircleMarkers.remove(index);
            mPinnedCircleData.remove(index);

            // 将圆变回临时圆状态（黄色）
            circle.setStrokeColor(0xFFCCCC00);
            circle.setFillColor(0x55FFFF00);

            // 添加到多点定位列表
            LatLng center = new LatLng(lat, lon);
            mMultiPointCircles.add(circle);
            mMultiPointCenters.add(center);
            mMultiPointRadius.add(radius);
            mMultiPointMarkers.add(marker);

            // 设置为当前临时圆
            long newTimestamp = System.currentTimeMillis();
            mSelectedCircle = circle;
            mSelectedCircleIndex = mMultiPointCircles.size() - 1;
            mSelectedCircleCenter = center;
            mSelectedCircleRadius = radius;
            mIsSelectedCirclePinned = false;
            mSelectedCircleTimestamp = newTimestamp;
            sTempCircleTimestamp = newTimestamp;

            marker.setTitle(name);
            marker.setSnippet("半径: " + String.format("%.0f", radius) + "米");
        }

        XLog.i("取消固定区域: " + name);
    }

    /**
     * 显示导入对话框
     */
    private void showImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_import, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        Button btnImportFile = view.findViewById(R.id.btn_import_file);
        Button btnImportQRCode = view.findViewById(R.id.btn_import_qrcode);
        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnImportFile.setOnClickListener(v -> {
            dialog.dismiss();
            importFromFile();
        });

        btnImportQRCode.setOnClickListener(v -> {
            dialog.dismiss();
            importFromQRCode();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        GoUtils.setDialogWidth(dialog);
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
        Intent intent = new Intent(this, QRCodeScanActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN_QR);
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
            GoUtils.DisplayToast(this, "导入失败: " + e.getMessage());
        }
    }

    /**
     * 处理二维码内容
     */
    private void processQRContent(String content) {
        XLog.i("导入内容: " + content);
        try {
            JSONObject json = new JSONObject(content);
            String type = json.optString("type");
            // 处理可能存在的空白字符和大小写问题
            if (type != null) {
                type = type.trim().toLowerCase();
            }
            XLog.i("导入类型: '" + type + "'");

            if ("batch".equals(type)) {
                // 批量导入
                int count = json.optInt("count", 0);
                String itemType = json.optString("itemType", "");
                JSONArray items = json.optJSONArray("items");

                if (items != null && items.length() > 0) {
                    int successCount = 0;
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        if (processImportItem(item)) {
                            successCount++;
                        }
                    }
                    GoUtils.DisplayToast(this, "批量导入完成: " + successCount + "/" + items.length() + " 个成功");
                } else {
                    GoUtils.DisplayToast(this, "批量导入数据为空");
                }
            } else if ("circle".equals(type)) {
                // 导入单个圆形区域
                if (processImportItem(json)) {
                    GoUtils.DisplayToast(this, "区域导入成功");
                } else {
                    GoUtils.DisplayToast(this, "区域导入失败");
                }
            } else if ("location".equals(type)) {
                // 导入单个坐标点
                if (processImportItem(json)) {
                    GoUtils.DisplayToast(this, "坐标导入成功");
                } else {
                    GoUtils.DisplayToast(this, "坐标导入失败");
                }
            } else if (type == null || type.isEmpty()) {
                // 没有type字段，检查是否有坐标数据，有则默认当作坐标点导入
                double lat = json.optDouble("lat", 0);
                double lon = json.optDouble("lon", 0);
                if (lat != 0 && lon != 0) {
                    XLog.i("无type字段，检测到坐标数据，默认按坐标点导入");
                    String name = json.optString("name", "导入的位置");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseFavorites.DB_COLUMN_NAME, name);
                    contentValues.put(DataBaseFavorites.DB_COLUMN_LATITUDE, String.valueOf(lat));
                    contentValues.put(DataBaseFavorites.DB_COLUMN_LONGITUDE, String.valueOf(lon));
                    contentValues.put(DataBaseFavorites.DB_COLUMN_TIMESTAMP, System.currentTimeMillis());
                    DataBaseFavorites.saveFavorite(mFavoritesDB, contentValues);

                    // 在地图上显示
                    LatLng latLng = new LatLng(lat, lon);
                    mMarkLatLngMap = latLng;
                    mMarkName = name;
                    mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

                    GoUtils.DisplayToast(this, "坐标导入成功: " + name);
                } else {
                    GoUtils.DisplayToast(this, "无法识别数据格式");
                }
            } else {
                String actualType = json.optString("type", "(无type字段)");
                XLog.w("未知的数据类型: '" + actualType + "'");
                GoUtils.DisplayToast(this, "未知的数据类型: '" + actualType + "'");
            }
        } catch (JSONException e) {
            XLog.e("解析导入数据失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "数据格式错误");
        }
    }

    /**
     * 处理单个导入项
     * @return 是否导入成功
     */
    private boolean processImportItem(JSONObject json) {
        try {
            String type = json.optString("type", "").trim().toLowerCase();

            if ("circle".equals(type)) {
                // 导入圆形区域
                String name = json.optString("name", "导入的区域");
                double lat = json.optDouble("lat", 0);
                double lon = json.optDouble("lon", 0);
                double radius = json.optDouble("radius", 1000);

                if (lat != 0 && lon != 0) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_NAME, name);
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT, String.valueOf(lat));
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON, String.valueOf(lon));
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_RADIUS, String.valueOf(radius));
                    contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                    DataBaseFavoriteRegions.saveFavoriteRegion(mFavoriteRegionsDB, contentValues);
                    XLog.i("导入区域: " + name + " (" + lat + ", " + lon + "), 半径: " + radius + "米");
                    return true;
                }
            } else if ("location".equals(type)) {
                // 导入坐标点
                String name = json.optString("name", "导入的位置");
                double lat = json.optDouble("lat", 0);
                double lon = json.optDouble("lon", 0);

                if (lat != 0 && lon != 0) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DataBaseFavorites.DB_COLUMN_NAME, name);
                    contentValues.put(DataBaseFavorites.DB_COLUMN_LATITUDE, String.valueOf(lat));
                    contentValues.put(DataBaseFavorites.DB_COLUMN_LONGITUDE, String.valueOf(lon));
                    contentValues.put(DataBaseFavorites.DB_COLUMN_TIMESTAMP, System.currentTimeMillis());

                    DataBaseFavorites.saveFavorite(mFavoritesDB, contentValues);
                    XLog.i("导入坐标: " + name + " (" + lat + ", " + lon + ")");
                    return true;
                }
            }
        } catch (Exception e) {
            XLog.e("处理导入项失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 从地址字符串中提取城市名称
     * @param address 完整地址
     * @return 城市名称，如果无法提取则返回null
     */
    private String extractCityFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        // 常见城市后缀
        String[] citySuffixes = {"市", "地区", "盟", "州"};
        String[] provinceSuffixes = {"省", "自治区", "特别行政区"};

        // 直辖市列表
        String[] directCities = {"北京", "上海", "天津", "重庆"};

        // 1. 首先检查是否是直辖市
        for (String city : directCities) {
            if (address.startsWith(city)) {
                return city + "市";
            }
        }

        // 2. 尝试匹配"XX市"格式
        for (String suffix : citySuffixes) {
            int index = address.indexOf(suffix);
            if (index > 0 && index < 15) { // 城市名通常在地址前15个字符内
                // 向前查找城市名称起点
                int start = index - 1;
                while (start >= 0 && isChineseChar(address.charAt(start)) && (index - start) < 10) {
                    start--;
                }
                start++;

                String cityName = address.substring(start, index + suffix.length());
                // 验证城市名长度（2-10个字符比较合理）
                if (cityName.length() >= 2 && cityName.length() <= 12) {
                    return cityName;
                }
            }
        }

        // 3. 按分隔符分割，查找可能的城市名
        String[] parts = address.split("[省市区县旗盟州镇乡街道]");
        for (int i = 0; i < parts.length && i < 2; i++) { // 只检查前两部分
            String part = parts[i].trim();
            if (part.length() >= 2 && part.length() <= 10) {
                // 检查是否包含省份名，如果是则继续
                boolean isProvince = false;
                for (String provSuffix : provinceSuffixes) {
                    if (part.endsWith(provSuffix) || address.startsWith(part + provSuffix)) {
                        isProvince = true;
                        break;
                    }
                }
                if (!isProvince && i == 0) {
                    // 第一部分不是省份，可能是城市
                    return part + "市";
                }
            }
        }

        // 4. 如果前面的方法都失败，尝试提取前2-4个字符
        if (address.length() >= 2) {
            // 跳过省份名
            int start = 0;
            for (String provSuffix : provinceSuffixes) {
                int idx = address.indexOf(provSuffix);
                if (idx >= 0 && idx < 10) {
                    start = idx + provSuffix.length();
                    break;
                }
            }

            // 提取2-4个字符作为城市名
            int end = Math.min(start + 4, address.length());
            for (int i = start + 2; i <= end; i++) {
                String candidate = address.substring(start, i);
                if (isValidCityName(candidate)) {
                    return candidate + "市";
                }
            }
        }

        return null;
    }

    /**
     * 检查字符是否为中文字符
     */
    private boolean isChineseChar(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 验证城市名称是否有效
     */
    private boolean isValidCityName(String name) {
        if (name == null || name.length() < 2 || name.length() > 10) {
            return false;
        }
        // 至少包含一个中文字符
        for (char c : name.toCharArray()) {
            if (isChineseChar(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行地理编码查询（将地址转换为坐标）
     * @param name 位置名称
     * @param address 完整地址
     */
    private void performGeocodeSearch(String name, String address) {
        try {
            if (mGeocodeSearch == null) {
                mGeocodeSearch = new GeocodeSearch(this);
            }

            // 显示加载提示
            GoUtils.DisplayToast(this, "正在定位: " + name + "...");

            // 创建地理编码查询
            com.amap.api.services.geocoder.GeocodeQuery query =
                    new com.amap.api.services.geocoder.GeocodeQuery(address, mCurrentCity);
            mGeocodeSearch.getFromLocationNameAsyn(query);

            XLog.i("发起地理编码查询: " + name + ", 地址: " + address + ", 城市: " + mCurrentCity);
        } catch (Exception e) {
            XLog.e("地理编码查询失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "定位失败，请检查网络");
        }
    }
    
    /*============================== 圆交互功能 ==============================*/
    
    /**
     * 加载已固定的圆
     */
    private void loadPinnedCircles() {
        try {
            List<Map<String, Object>> pinnedCircles = DataBasePinnedCircles.getAllPinnedCircles(mPinnedCirclesDB);
            for (Map<String, Object> circleData : pinnedCircles) {
                String name = (String) circleData.get(DataBasePinnedCircles.DB_COLUMN_NAME);
                double lat = Double.parseDouble((String) circleData.get(DataBasePinnedCircles.DB_COLUMN_CENTER_LAT));
                double lon = Double.parseDouble((String) circleData.get(DataBasePinnedCircles.DB_COLUMN_CENTER_LON));
                double radius = Double.parseDouble((String) circleData.get(DataBasePinnedCircles.DB_COLUMN_RADIUS));
                
                // 绘制固定圆（深蓝色）
                drawPinnedCircle(name, lat, lon, radius);
            }
            XLog.i("已加载 " + pinnedCircles.size() + " 个固定圆");
        } catch (Exception e) {
            XLog.e("加载固定圆失败: " + e.getMessage());
        }
    }
    
    /**
     * 绘制固定圆（深蓝色）
     */
    private void drawPinnedCircle(String name, double lat, double lon, double radius) {
        LatLng center = new LatLng(lat, lon);
        
        // 深蓝色圆形
        Circle circle = mAMap.addCircle(new CircleOptions()
            .center(center)
            .radius(radius)
            .fillColor(0x55000080) // 深蓝色半透明填充
            .strokeColor(0xFF000080) // 深蓝色边框
            .strokeWidth(3));
        
        mPinnedCircles.add(circle);

        // 创建自定义圆心标记（显示名称）
        LinearLayout markerView = new LinearLayout(this);
        markerView.setOrientation(LinearLayout.VERTICAL);
        markerView.setGravity(Gravity.CENTER);

        // 名称文本（上方）
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(12);
        nameView.setTextColor(Color.WHITE);
        nameView.setBackgroundColor(0xAA000080); // 深蓝色半透明背景
        nameView.setPadding(10, 5, 10, 5);
        nameView.setGravity(Gravity.CENTER);
        markerView.addView(nameView);

        // 半径文本（下方）
        TextView radiusView = new TextView(this);
        double radiusKm = radius / 1000.0;
        radiusView.setText(String.format("%.2f km", radiusKm));
        radiusView.setTextSize(10);
        radiusView.setTextColor(Color.WHITE);
        radiusView.setBackgroundColor(0xAA000080);
        radiusView.setPadding(8, 2, 8, 2);
        radiusView.setGravity(Gravity.CENTER);
        markerView.addView(radiusView);

        // 转换为BitmapDescriptor
        markerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(bitmap);

        Marker marker = mAMap.addMarker(new MarkerOptions()
            .position(center)
            .title(name)
            .snippet("半径: " + String.format("%.0f", radius) + "米 [已固定]")
            .icon(customIcon)
            .anchor(0.5f, 0.5f));
        mPinnedCircleMarkers.add(marker);
        
        // 保存数据
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("lat", lat);
        data.put("lon", lon);
        data.put("radius", radius);
        mPinnedCircleData.add(data);
        
        XLog.i("绘制固定圆: " + name + " (" + lat + ", " + lon + "), 半径: " + radius + "米");
    }
    
    /**
     * 在地图上显示区域（用于收藏-区域点击）- 添加为可交互圆
     * @param name 区域名称
     * @param lon 经度
     * @param lat 纬度
     * @param radius 半径（米）
     */
    public void showRegion(String name, double lon, double lat, double radius) {
        try {
            if (mAMap != null) {
                // 清除之前的圆（保留固定圆）
                clearMultiPointMarks();
                
                // 添加为可交互圆（与多点定位圆一致）
                addInteractiveCircle(lat, lon, radius, name);
                
                XLog.i("显示区域: " + name + " (" + lat + ", " + lon + "), 半径: " + radius + "米");
            }
        } catch (Exception e) {
            XLog.e("显示区域失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加可交互圆（支持点击）
     */
    private void addInteractiveCircle(double lat, double lon, double radiusM, String name) {
        LatLng center = new LatLng(lat, lon);
        mMultiPointCenters.add(center);
        mMultiPointRadius.add(radiusM);
        
        // 添加黄色圆形
        Circle circle = mAMap.addCircle(new CircleOptions()
            .center(center)
            .radius(radiusM)
            .fillColor(0x55FFFF00) // 黄色半透明填充
            .strokeColor(0xFFCCCC00) // 黄色边框
            .strokeWidth(2));
        mMultiPointCircles.add(circle);
        
        // 添加自定义圆心标记（显示名称和半径km）
        addCircleCenterMarker(center, name, radiusM);
        
        // 移动相机
        float zoomLevel = calculateZoomLevel(radiusM);
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, zoomLevel));
        
        XLog.i("添加可交互圆: " + name + " (" + lat + ", " + lon + "), 半径: " + radiusM + "米");
    }
    
    /**
     * 添加圆心标记，显示名称和半径（km）
     */
    private void addCircleCenterMarker(LatLng center, String name, double radiusM) {
        // 创建自定义视图来显示名称和半径
        LinearLayout markerView = new LinearLayout(this);
        markerView.setOrientation(LinearLayout.VERTICAL);
        markerView.setGravity(Gravity.CENTER);
        
        // 名称文本（上方）
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(11);
        nameView.setTextColor(Color.BLACK);
        nameView.setBackgroundColor(0xAAFFFFFF);
        nameView.setPadding(8, 3, 8, 3);
        nameView.setGravity(Gravity.CENTER);
        markerView.addView(nameView);
        
        // 半径文本（下方）
        TextView radiusView = new TextView(this);
        double radiusKm = radiusM / 1000.0;
        radiusView.setText(String.format("%.2f km", radiusKm));
        radiusView.setTextSize(10);
        radiusView.setTextColor(Color.DKGRAY);
        radiusView.setBackgroundColor(0xAAFFFFFF);
        radiusView.setPadding(8, 2, 8, 2);
        radiusView.setGravity(Gravity.CENTER);
        markerView.addView(radiusView);
        
        // 转换为BitmapDescriptor
        markerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        
        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);
        
        BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(bitmap);
        
        // 添加标记
        Marker marker = mAMap.addMarker(new MarkerOptions()
            .position(center)
            .icon(customIcon)
            .anchor(0.5f, 0.5f)
            .setFlat(true));
        
        mMultiPointMarkers.add(marker);
    }
    
    /**
     * 静态方法计算缩放级别
     */
    private static float calculateZoomLevelStatic(double radiusM) {
        if (radiusM < 100) return 18f;
        else if (radiusM < 500) return 16f;
        else if (radiusM < 1000) return 15f;
        else if (radiusM < 5000) return 13f;
        else if (radiusM < 10000) return 12f;
        else return 10f;
    }
    
    /**
     * 检查点击位置是否在圆上
     * @param clickPoint 点击位置
     * @param center 圆心
     * @param radius 半径（米）
     * @param tolerance 容差（米）
     * @return 是否在圆上
     */
    private boolean isPointOnCircle(LatLng clickPoint, LatLng center, double radius, double tolerance) {
        double distance = calculateDistance(clickPoint, center);
        return Math.abs(distance - radius) <= tolerance;
    }
    
    /**
     * 检查点击位置是否在圆内
     * @param clickPoint 点击位置
     * @param center 圆心
     * @param radius 半径（米）
     * @return 是否在圆内
     */
    private boolean isPointInsideCircle(LatLng clickPoint, LatLng center, double radius) {
        double distance = calculateDistance(clickPoint, center);
        return distance <= radius;
    }
    
    /**
     * 选中圆（圆周变蓝色）
     * @param circle 要选的圆
     * @param index 圆在列表中的索引
     */
    private void selectCircle(Circle circle, int index) {
        // 恢复之前选中圆的状态（如果它还是临时圆且未固定）
        if (mSelectedCircle != null && mSelectedCircleIndex >= 0 && mSelectedCircleIndex < mMultiPointCircles.size()) {
            mSelectedCircle.setStrokeColor(0xFFCCCC00); // 恢复黄色边框
            mSelectedCircle.setFillColor(0x55FFFF00); // 恢复黄色填充
        }

        // 选中新的圆
        mSelectedCircle = circle;
        mSelectedCircleIndex = index;
        mSelectedCircleCenter = mMultiPointCenters.get(index);
        mSelectedCircleRadius = mMultiPointRadius.get(index);
        mIsSelectedCirclePinned = false;  // 标记为未固定的临时圆

        // 记录时间戳（作为当前临时圆）
        long timestamp = System.currentTimeMillis();
        mSelectedCircleTimestamp = timestamp;
        sTempCircleTimestamp = timestamp;

        // 设置选中状态（蓝色）
        circle.setStrokeColor(0xFF0000FF); // 蓝色边框
        circle.setFillColor(0x550000FF); // 蓝色填充

        // 显示操作菜单
        showCircleActionMenu();

        XLog.i("选中圆 #" + index + ", 半径: " + mSelectedCircleRadius + "米, 时间戳: " + timestamp);
    }
    
    /**
     * 显示圆操作菜单（收藏、分享、固定）
     */
    private void showCircleActionMenu() {
        if (mSelectedCircle == null) return;

        String[] options = {"收藏", "分享", "固定"};

        new AlertDialog.Builder(this)
            .setTitle("区域操作")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 收藏
                        favoriteSelectedCircle();
                        break;
                    case 1: // 分享
                        shareSelectedCircle();
                        break;
                    case 2: // 固定
                        pinSelectedCircle();
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 收藏选中的圆
     */
    private void favoriteSelectedCircle() {
        if (mSelectedCircle == null) return;
        
        // 显示输入名称对话框
        EditText etName = new EditText(this);
        etName.setHint("输入区域名称");
        
        new AlertDialog.Builder(this)
            .setTitle("收藏区域")
            .setView(etName)
            .setPositiveButton("确定", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    name = "区域" + System.currentTimeMillis();
                }
                
                // 保存到数据库
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_NAME, name);
                contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT, String.valueOf(mSelectedCircleCenter.latitude));
                contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON, String.valueOf(mSelectedCircleCenter.longitude));
                contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_RADIUS, String.valueOf(mSelectedCircleRadius));
                contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
                
                DataBaseFavoriteRegions.saveFavoriteRegion(mFavoriteRegionsDB, contentValues);
                GoUtils.DisplayToast(this, "已收藏区域: " + name);
                XLog.i("收藏区域: " + name + ", 中心: (" + mSelectedCircleCenter.latitude + ", " + mSelectedCircleCenter.longitude + "), 半径: " + mSelectedCircleRadius + "米");
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 分享选中的圆
     */
    private void shareSelectedCircle() {
        if (mSelectedCircle == null) return;
        
        String[] options = {"生成二维码", "保存为文件"};
        
        new AlertDialog.Builder(this)
            .setTitle("分享区域")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 生成二维码
                    generateQRCodeForCircle();
                } else {
                    // 保存为文件
                    saveCircleToFile();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 为圆生成二维码
     */
    private void generateQRCodeForCircle() {
        try {
            // 创建JSON数据
            String name = "区域" + System.currentTimeMillis();
            JSONObject circleData = new JSONObject();
            circleData.put("type", "circle");
            circleData.put("name", name);
            circleData.put("lat", mSelectedCircleCenter.latitude);
            circleData.put("lon", mSelectedCircleCenter.longitude);
            circleData.put("radius", mSelectedCircleRadius);
            circleData.put("timestamp", System.currentTimeMillis());

            String qrContent = circleData.toString();

            // 显示二维码对话框
            showQRCodeDialog(qrContent, name);

        } catch (JSONException e) {
            XLog.e("生成二维码数据失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "生成二维码失败");
        }
    }
    
    /**
     * 显示二维码对话框
     */
    private void showQRCodeDialog(String content, String name) {
        try {
            // 生成二维码位图
            Bitmap qrBitmap = generateQRCodeBitmap(content, 500, 500);

            if (qrBitmap != null) {
                // 创建ImageView显示二维码
                ImageView ivQR = new ImageView(this);
                ivQR.setImageBitmap(qrBitmap);
                ivQR.setPadding(20, 20, 20, 20);

                new AlertDialog.Builder(this)
                    .setTitle("扫描二维码分享区域")
                    .setView(ivQR)
                    .setPositiveButton("导出/分享", (dialog, which) -> {
                        showExportQRCodeOptions(qrBitmap, name);
                    })
                    .setNegativeButton("关闭", null)
                    .show();
            }
        } catch (Exception e) {
            XLog.e("显示二维码失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "显示二维码失败");
        }
    }
    
    /**
     * 生成二维码位图
     */
    private Bitmap generateQRCodeBitmap(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            
            return bitmap;
        } catch (Exception e) {
            XLog.e("生成二维码位图失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 保存二维码到文件（使用自定义目录）
     */
    private void saveQRCodeToFile(Bitmap bitmap) {
        saveQRCodeToFile(bitmap, null);
    }

    /**
     * 保存二维码到自定义目录
     * @param bitmap 二维码位图
     * @param name 文件名称前缀（可选）
     */
    private void saveQRCodeToFile(Bitmap bitmap, String name) {
        try {
            String fileName = (name != null ? name : "QR") + "_" + System.currentTimeMillis() + ".png";
            String savePath = com.river.gowithamap.utils.FileSaveManager.getSavePath(this);
            
            // 确保目录存在
            File dir = new File(savePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(savePath, fileName);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            GoUtils.DisplayToast(this, "二维码已保存到: " + file.getAbsolutePath());
            XLog.i("二维码已保存到: " + file.getAbsolutePath());
        } catch (Exception e) {
            XLog.e("保存二维码失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "保存失败: " + e.getMessage());
        }
    }

    /**
     * 显示导出二维码选项对话框
     */
    private void showExportQRCodeOptions(Bitmap bitmap, String name) {
        String[] options = {"导出到自定义目录", "分享"};
        new AlertDialog.Builder(this)
                .setTitle("导出二维码")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 导出到自定义目录
                        saveQRCodeToFile(bitmap, name);
                    } else if (which == 1) {
                        // 分享
                        ShareUtils.shareImage(this, bitmap, "二维码");
                    }
                })
                .show();
    }
    
    /**
     * 将圆保存为文件
     */
    private void saveCircleToFile() {
        saveCircleToFileWithPath(FileSaveManager.getSavePath(this));
    }

    /**
     * 将圆保存到指定路径
     */
    private void saveCircleToFileWithPath(String savePath) {
        try {
            // 创建JSON数据
            JSONObject circleData = new JSONObject();
            circleData.put("type", "circle");
            circleData.put("name", "区域" + System.currentTimeMillis());
            circleData.put("lat", mSelectedCircleCenter.latitude);
            circleData.put("lon", mSelectedCircleCenter.longitude);
            circleData.put("radius", mSelectedCircleRadius);
            circleData.put("timestamp", System.currentTimeMillis());

            String fileName = "region_" + System.currentTimeMillis() + ".json";

            // 确保目录存在
            File dir = new File(savePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(savePath, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(circleData.toString(2));
            writer.close();

            GoUtils.DisplayToast(this, "区域已保存到: " + savePath);
            XLog.i("区域已保存到: " + file.getAbsolutePath());

            // 分享文件
            ShareUtils.shareFile(this, file, "区域数据");

        } catch (JSONException e) {
            XLog.e("创建JSON失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "保存失败");
        } catch (IOException e) {
            XLog.e("保存区域失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "保存失败");
        }
    }
    
    /**
     * 固定选中的圆
     */
    private void pinSelectedCircle() {
        if (mSelectedCircle == null) return;

        // 显示输入名称对话框
        EditText etName = new EditText(this);
        etName.setHint("输入固定圆名称");

        new AlertDialog.Builder(this)
            .setTitle("固定圆")
            .setView(etName)
            .setPositiveButton("确定", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    name = "固定圆" + System.currentTimeMillis();
                }

                // 保存到数据库
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBasePinnedCircles.DB_COLUMN_NAME, name);
                contentValues.put(DataBasePinnedCircles.DB_COLUMN_CENTER_LAT, String.valueOf(mSelectedCircleCenter.latitude));
                contentValues.put(DataBasePinnedCircles.DB_COLUMN_CENTER_LON, String.valueOf(mSelectedCircleCenter.longitude));
                contentValues.put(DataBasePinnedCircles.DB_COLUMN_RADIUS, String.valueOf(mSelectedCircleRadius));
                contentValues.put(DataBasePinnedCircles.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                DataBasePinnedCircles.savePinnedCircle(mPinnedCirclesDB, contentValues);

                // 绘制固定圆
                drawPinnedCircle(name, mSelectedCircleCenter.latitude, mSelectedCircleCenter.longitude, mSelectedCircleRadius);

                // 从多点定位列表中移除（因为已经固定）
                if (mSelectedCircleIndex >= 0 && mSelectedCircleIndex < mMultiPointCircles.size()) {
                    mMultiPointCircles.remove(mSelectedCircleIndex);
                    mMultiPointCenters.remove(mSelectedCircleIndex);
                    mMultiPointRadius.remove(mSelectedCircleIndex);
                    if (mSelectedCircleIndex < mMultiPointMarkers.size()) {
                        mMultiPointMarkers.get(mSelectedCircleIndex).remove();
                        mMultiPointMarkers.remove(mSelectedCircleIndex);
                    }
                }

                // 清除临时圆状态和时间戳（该圆已固定，不再是临时圆）
                sTempCircleTimestamp = 0;
                mSelectedCircleTimestamp = 0;

                // 清除选中状态
                mSelectedCircle = null;
                mSelectedCircleIndex = -1;
                mSelectedCircleCenter = null;
                mSelectedCircleRadius = 0;
                mIsSelectedCirclePinned = false;

                GoUtils.DisplayToast(this, "圆已固定: " + name);
                XLog.i("固定圆: " + name);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 清除所有固定圆
     */
    private void clearAllPinnedCircles() {
        for (Circle circle : mPinnedCircles) {
            circle.remove();
        }
        mPinnedCircles.clear();
        
        for (Marker marker : mPinnedCircleMarkers) {
            marker.remove();
        }
        mPinnedCircleMarkers.clear();
        mPinnedCircleData.clear();
        
        DataBasePinnedCircles.clearAllPinnedCircles(mPinnedCirclesDB);
        
        GoUtils.DisplayToast(this, "已清除所有固定圆");
        XLog.i("已清除所有固定圆");
    }
    
    /**
     * 显示固定圆操作菜单（修改名称、收藏、分享、取消固定）
     * UI风格与坐标操作一致
     * @param index 固定圆在列表中的索引
     */
    private void showPinnedCircleActionMenu(int index) {
        if (index < 0 || index >= mPinnedCircles.size()) return;

        Map<String, Object> data = mPinnedCircleData.get(index);
        String name = (String) data.get("name");
        double lat = (double) data.get("lat");
        double lon = (double) data.get("lon");
        double radius = (double) data.get("radius");

        String[] options = {"修改名称", "收藏", "分享", "取消固定"};
        // 显示格式：中心：经度 纬度
        String coordText = String.format("区域: %s\n中心：%.6f %.6f\n半径: %.0f米", name, lon, lat, radius);

        AlertDialog menuDialog = new AlertDialog.Builder(this)
            .setTitle(coordText)
            .setItems(options, (dialog, which) -> {
                // 先关闭菜单对话框
                dialog.dismiss();
                switch (which) {
                    case 0: // 修改名称
                        renamePinnedCircle(index);
                        break;
                    case 1: // 收藏
                        favoritePinnedCircle(name, lat, lon, radius);
                        break;
                    case 2: // 分享
                        sharePinnedCircle(name, lat, lon, radius);
                        break;
                    case 3: // 取消固定
                        unpinCircle(index);
                        break;
                }
            })
            .create();
        menuDialog.show();
        GoUtils.setDialogWidth(menuDialog);
    }
    
    /**
     * 收藏固定圆
     */
    private void favoritePinnedCircle(String name, double lat, double lon, double radius) {
        // 保存到区域收藏数据库
        ContentValues contentValues = new ContentValues();
        contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_NAME, name);
        contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT, String.valueOf(lat));
        contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON, String.valueOf(lon));
        contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_RADIUS, String.valueOf(radius));
        contentValues.put(DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
        
        DataBaseFavoriteRegions.saveFavoriteRegion(mFavoriteRegionsDB, contentValues);
        GoUtils.DisplayToast(this, "已收藏区域: " + name);
        XLog.i("收藏固定圆到区域: " + name);
    }
    
    /**
     * 分享固定圆
     */
    private void sharePinnedCircle(String name, double lat, double lon, double radius) {
        try {
            JSONObject circleData = new JSONObject();
            circleData.put("type", "circle");
            circleData.put("name", name);
            circleData.put("lat", lat);
            circleData.put("lon", lon);
            circleData.put("radius", radius);
            circleData.put("timestamp", System.currentTimeMillis());
            
            String[] options = {"生成二维码", "保存为文件"};
            new AlertDialog.Builder(this)
                .setTitle("分享区域")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showQRCodeDialog(circleData.toString(), name);
                    } else {
                        saveJsonToFile(circleData, name, "区域数据");
                    }
                })
                .show();
        } catch (JSONException e) {
            XLog.e("创建分享数据失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "分享失败");
        }
    }
    
    /**
     * 通用保存JSON到文件方法
     */
    private void saveJsonToFile(JSONObject jsonData, String fileNamePrefix, String shareTitle) {
        try {
            String fileName = fileNamePrefix + "_" + System.currentTimeMillis() + ".json";
            String savePath = com.river.gowithamap.utils.FileSaveManager.getSavePath(this);
            
            // 确保目录存在
            File dir = new File(savePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(savePath, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonData.toString(2));
            writer.close();

            GoUtils.DisplayToast(this, "已保存到: " + file.getAbsolutePath());
            ShareUtils.shareFile(this, file, shareTitle);
        } catch (Exception e) {
            XLog.e("保存文件失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "保存失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消固定圆
     */
    private void unpinCircle(int index) {
        if (index < 0 || index >= mPinnedCircles.size()) return;

        Map<String, Object> data = mPinnedCircleData.get(index);
        String name = (String) data.get("name");
        double lat = (double) data.get("lat");
        double lon = (double) data.get("lon");
        double radius = (double) data.get("radius");

        new AlertDialog.Builder(this)
            .setTitle("取消固定")
            .setMessage("确定要取消固定 '" + name + "' 吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                // 从数据库删除
                java.util.List<Map<String, Object>> allPinned = DataBasePinnedCircles.getAllPinnedCircles(mPinnedCirclesDB);
                for (Map<String, Object> pinned : allPinned) {
                    String pinnedName = (String) pinned.get(DataBasePinnedCircles.DB_COLUMN_NAME);
                    if (pinnedName.equals(name)) {
                        String id = (String) pinned.get(DataBasePinnedCircles.DB_COLUMN_ID);
                        DataBasePinnedCircles.deletePinnedCircle(mPinnedCirclesDB, id);
                        break;
                    }
                }

                // 从固定列表移除（但保留在地图上作为临时圆）
                Circle circle = mPinnedCircles.get(index);
                Marker marker = mPinnedCircleMarkers.get(index);

                // 从固定列表移除
                mPinnedCircles.remove(index);
                mPinnedCircleMarkers.remove(index);
                mPinnedCircleData.remove(index);

                // 将圆变回临时圆状态（黄色）
                circle.setStrokeColor(0xFFCCCC00); // 黄色边框
                circle.setFillColor(0x55FFFF00); // 黄色填充

                // 将圆添加到多点定位列表（作为临时圆管理）
                LatLng center = new LatLng(lat, lon);
                mMultiPointCircles.add(circle);
                mMultiPointCenters.add(center);
                mMultiPointRadius.add(radius);
                mMultiPointMarkers.add(marker);

                // 分配新的时间戳给这个取消固定的圆（作为新的临时圆）
                long newTimestamp = System.currentTimeMillis();

                // 检查是否存在其他临时圆，如果存在则比较时间戳，清除较早的
                if (mSelectedCircle != null && !mIsSelectedCirclePinned && sTempCircleTimestamp > 0) {
                    // 存在其他临时圆，比较时间戳
                    // 新取消固定的圆使用当前时间戳，总是比已存在的临时圆新
                    // 所以清除已存在的临时圆（它的时间戳较小）
                    // 恢复之前临时圆的状态为普通黄色
                    mSelectedCircle.setStrokeColor(0xFFCCCC00);
                    mSelectedCircle.setFillColor(0x55FFFF00);
                    XLog.i("取消固定圆：已存在的临时圆时间戳 " + sTempCircleTimestamp + " < 新临时圆时间戳 " + newTimestamp + "，清除已存在的");
                }

                // 设置当前选中的圆为这个取消固定的圆
                mSelectedCircle = circle;
                mSelectedCircleIndex = mMultiPointCircles.size() - 1;
                mSelectedCircleCenter = center;
                mSelectedCircleRadius = radius;
                mIsSelectedCirclePinned = false;
                mSelectedCircleTimestamp = newTimestamp;
                sTempCircleTimestamp = newTimestamp;

                // 更新标记为未固定状态
                marker.setTitle(name);
                marker.setSnippet("半径: " + String.format("%.0f", radius) + "米");

                GoUtils.DisplayToast(this, "已取消固定: " + name);
                XLog.i("取消固定圆: " + name + ", 变为临时圆, 时间戳: " + newTimestamp);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 重命名固定圆
     */
    private void renamePinnedCircle(int index) {
        if (index < 0 || index >= mPinnedCircleData.size()) return;

        Map<String, Object> data = mPinnedCircleData.get(index);
        String oldName = (String) data.get("name");
        double lat = (double) data.get("lat");
        double lon = (double) data.get("lon");
        double radius = (double) data.get("radius");
        Circle circle = mPinnedCircles.get(index);
        Marker marker = mPinnedCircleMarkers.get(index);

        // 显示输入对话框
        EditText etName = new EditText(this);
        etName.setText(oldName);
        etName.selectAll();

        new AlertDialog.Builder(this)
            .setTitle("修改区域名称")
            .setView(etName)
            .setPositiveButton("确定", (dialog, which) -> {
                String newName = etName.getText().toString().trim();
                if (newName.isEmpty()) {
                    GoUtils.DisplayToast(this, "名称不能为空");
                    return;
                }
                if (newName.equals(oldName)) {
                    return; // 名称未改变
                }

                // 从数据库中找到对应记录并更新
                java.util.List<Map<String, Object>> allPinned = DataBasePinnedCircles.getAllPinnedCircles(mPinnedCirclesDB);
                for (Map<String, Object> pinned : allPinned) {
                    String pinnedName = (String) pinned.get(DataBasePinnedCircles.DB_COLUMN_NAME);
                    if (pinnedName.equals(oldName)) {
                        String id = (String) pinned.get(DataBasePinnedCircles.DB_COLUMN_ID);
                        DataBasePinnedCircles.updatePinnedCircleName(mPinnedCirclesDB, id, newName);
                        break;
                    }
                }

                // 更新数据
                data.put("name", newName);
                // 更新标记
                marker.setTitle(newName);
                marker.setSnippet("半径: " + String.format("%.0f", radius) + "米 [已固定]");

                GoUtils.DisplayToast(this, "已修改区域名称为: " + newName);
                XLog.i("固定圆重命名: " + oldName + " -> " + newName);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 设置地图点击监听，用于检测圆的点击
     * 已整合到 initMap 方法中
     */
    private void setupCircleClickListener() {
        // 此方法已合并到 initMap 的 onMapClick 监听器中
    }

    /**
     * 显示标记点的操作菜单（收藏、分享、设置区域、固定）
     * UI风格与点击圆操作一致
     */
    private void showMarkerActionMenu() {
        if (mMarkLatLngMap == null) return;

        String[] options = {"收藏", "分享", "设置区域", "固定"};
        // 显示格式：坐标：经度 纬度
        String coordText = String.format("坐标：%.6f %.6f", mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);

        AlertDialog menuDialog = new AlertDialog.Builder(this)
            .setTitle(coordText)
            .setItems(options, (dialog, which) -> {
                // 先关闭菜单对话框
                dialog.dismiss();
                switch (which) {
                    case 0: // 收藏 - 弹窗输入名称
                        addCurrentMarkerToFavorite();
                        break;
                    case 1: // 分享 - 使用与固定圆相同的选项
                        shareCurrentMarkerLikeCircle();
                        break;
                    case 2: // 设置区域 - 以当前点为中心画临时圆
                        showSetRegionDialog(mMarkLatLngMap.latitude, mMarkLatLngMap.longitude, false);
                        break;
                    case 3: // 固定 - 弹窗输入名称
                        pinCurrentMarker();
                        break;
                }
            })
            .create();
        menuDialog.show();
        GoUtils.setDialogWidth(menuDialog);
    }

    /**
     * 收藏当前标记点（弹窗输入名称）
     */
    private void addCurrentMarkerToFavorite() {
        if (mMarkLatLngMap == null) return;

        // 显示输入对话框
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null);
        EditText etName = view.findViewById(R.id.et_favorite_name);
        // 设置默认名称
        etName.setText(mMarkName != null && !mMarkName.isEmpty() && !mMarkName.equals("位置")
                ? mMarkName
                : "");
        // 不设置hint，使用布局文件中的默认hint

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("保存", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    name = "收藏点_" + System.currentTimeMillis();
                }

                // 直接使用 GCJ02 坐标存储（与地图上显示的一致）
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBaseFavorites.DB_COLUMN_NAME, name);
                contentValues.put(DataBaseFavorites.DB_COLUMN_LONGITUDE, String.valueOf(mMarkLatLngMap.longitude));
                contentValues.put(DataBaseFavorites.DB_COLUMN_LATITUDE, String.valueOf(mMarkLatLngMap.latitude));
                contentValues.put(DataBaseFavorites.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                DataBaseFavorites.saveFavorite(mFavoritesDB, contentValues);
                GoUtils.DisplayToast(this, "已收藏: " + name);
                XLog.i("添加收藏: " + name + " 坐标: " + mMarkLatLngMap.longitude + " " + mMarkLatLngMap.latitude);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 分享当前标记点（使用与固定圆相同的UI）
     */
    private void shareCurrentMarkerLikeCircle() {
        if (mMarkLatLngMap == null) return;

        String[] options = {"生成二维码", "保存为文件"};
        // 显示格式：坐标：经度 纬度
        String shareText = String.format("位置坐标：%.6f %.6f", mMarkLatLngMap.longitude, mMarkLatLngMap.latitude);

        // 创建JSON数据
        String locationName = mMarkName != null ? mMarkName : "位置";
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("type", "location");
            jsonData.put("name", locationName);
            jsonData.put("lat", mMarkLatLngMap.latitude);
            jsonData.put("lon", mMarkLatLngMap.longitude);
        } catch (JSONException e) {
            XLog.e("创建JSON失败: " + e.getMessage());
        }

        new AlertDialog.Builder(this)
            .setTitle("分享位置")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 生成二维码
                    generateQRCodeForCurrentMarker(jsonData.toString(), locationName);
                } else {
                    // 保存为文件
                    saveJsonToFile(jsonData, locationName, "坐标数据");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示批量分享对话框
     */
    private void showBatchShareDialog(List<Map<String, Object>> items, String typeName) {
        // 构建分享文本
        StringBuilder shareTextBuilder = new StringBuilder();
        shareTextBuilder.append("批量分享").append(typeName).append(" (").append(items.size()).append(" 个)\n\n");

        // 创建JSON数组
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String name = (String) item.get("name");

            if (typeName.equals("坐标")) {
                double lat = (double) item.get("lat");
                double lon = (double) item.get("lon");
                shareTextBuilder.append(i + 1).append(". ").append(name).append("\n");
                shareTextBuilder.append("   坐标: ").append(String.format("%.6f, %.6f", lat, lon)).append("\n\n");

                // 添加到JSON数组
                JSONObject jsonItem = new JSONObject();
                try {
                    jsonItem.put("type", "location");
                    jsonItem.put("name", name);
                    jsonItem.put("lat", lat);
                    jsonItem.put("lon", lon);
                    jsonArray.put(jsonItem);
                } catch (JSONException e) {
                    XLog.e("创建JSON失败: " + e.getMessage());
                }
            } else {
                double lat = (double) item.get("lat");
                double lon = (double) item.get("lon");
                double radius = (double) item.get("radius");
                shareTextBuilder.append(i + 1).append(". ").append(name).append("\n");
                shareTextBuilder.append("   中心: ").append(String.format("%.6f, %.6f", lat, lon)).append("\n");
                shareTextBuilder.append("   半径: ").append(String.format("%.0f", radius)).append(" 米\n\n");

                // 添加到JSON数组
                JSONObject jsonItem = new JSONObject();
                try {
                    jsonItem.put("type", "circle");
                    jsonItem.put("name", name);
                    jsonItem.put("lat", lat);
                    jsonItem.put("lon", lon);
                    jsonItem.put("radius", radius);
                    jsonArray.put(jsonItem);
                } catch (JSONException e) {
                    XLog.e("创建JSON失败: " + e.getMessage());
                }
            }
        }

        // 创建外层JSON对象
        JSONObject batchJson = new JSONObject();
        try {
            batchJson.put("type", "batch");
            batchJson.put("count", items.size());
            batchJson.put("itemType", typeName.equals("坐标") ? "locations" : "circles");
            batchJson.put("items", jsonArray);
        } catch (JSONException e) {
            XLog.e("创建批量JSON失败: " + e.getMessage());
        }

        String shareText = shareTextBuilder.toString();
        String jsonString = batchJson.toString();

        new AlertDialog.Builder(this)
                .setTitle("批量分享 " + items.size() + " 个" + typeName)
                .setItems(new String[]{"分享文本", "生成二维码", "保存为文件"}, (dialog, which) -> {
                    if (which == 0) {
                        ShareUtils.shareText(this, "批量分享" + typeName, shareText);
                    } else if (which == 1) {
                        generateQRCodeForCurrentMarker(jsonString, "批量分享" + typeName);
                    } else {
                        saveJsonToFile(batchJson, "批量" + typeName, "批量" + typeName + "数据");
                    }
                })
                .show();
    }

    /**
     * 显示固定位置的操作菜单（修改名称、收藏、分享、设置区域、取消固定）
     */
    private void showPinnedLocationActionMenu(int index) {
        if (index < 0 || index >= mPinnedLocationData.size()) return;

        Map<String, Object> data = mPinnedLocationData.get(index);
        String name = (String) data.get("name");
        double lat = (double) data.get("lat");
        double lon = (double) data.get("lon");

        String[] options = {"修改名称", "收藏", "分享", "设置区域", "取消固定"};
        String coordText = String.format("已固定: %s\n%.6f, %.6f", name, lon, lat);

        AlertDialog menuDialog = new AlertDialog.Builder(this)
            .setTitle(coordText)
            .setItems(options, (dialog, which) -> {
                // 先关闭菜单对话框
                dialog.dismiss();
                switch (which) {
                    case 0: // 修改名称
                        renamePinnedLocation(index);
                        break;
                    case 1: // 收藏
                        addPinnedLocationToFavorite(name, lat, lon);
                        break;
                    case 2: // 分享
                        shareLocation(name, lon, lat);
                        break;
                    case 3: // 设置区域 - 以当前点为中心画固定圆
                        showSetRegionDialog(lat, lon, true);
                        break;
                    case 4: // 取消固定
                        unpinLocation(index);
                        break;
                }
            })
            .create();
        menuDialog.show();
        GoUtils.setDialogWidth(menuDialog);
    }

    /**
     * 取消固定位置
     */
    private void unpinLocation(int index) {
        if (index < 0 || index >= mPinnedLocationMarkers.size()) return;

        Marker marker = mPinnedLocationMarkers.get(index);
        Map<String, Object> data = mPinnedLocationData.get(index);
        String name = (String) data.get("name");

        new AlertDialog.Builder(this)
            .setTitle("取消固定")
            .setMessage("确定要取消固定 '" + name + "' 吗？")
            .setPositiveButton("确定", (dialog, which) -> {
                // 从数据库删除
                java.util.List<Map<String, Object>> allPinned = DataBasePinnedLocations.getAllPinnedLocations(mPinnedLocationsDB);
                for (Map<String, Object> pinned : allPinned) {
                    String pinnedName = (String) pinned.get(DataBasePinnedLocations.DB_COLUMN_NAME);
                    if (pinnedName.equals(name)) {
                        String id = (String) pinned.get(DataBasePinnedLocations.DB_COLUMN_ID);
                        DataBasePinnedLocations.deletePinnedLocation(mPinnedLocationsDB, id);
                        break;
                    }
                }

                // 将标记变回未固定状态（蓝色图标）
                marker.setIcon(mMapIndicator);
                marker.setTitle(name);
                marker.setSnippet(String.format("%.6f, %.6f", marker.getPosition().longitude, marker.getPosition().latitude));
                marker.setObject("temp_location"); // 重新设置为临时标记

                // 从固定列表移除
                mPinnedLocationMarkers.remove(index);
                mPinnedLocationData.remove(index);

                // 分配新的时间戳给这个取消固定的坐标（作为新的临时坐标）
                long newTimestamp = System.currentTimeMillis();

                // 检查是否存在其他临时坐标，如果存在则比较时间戳，清除较早的
                if (sTempMarker != null && sTempMarker != marker && sTempMarkerTimestamp > 0) {
                    // 存在其他临时坐标，比较时间戳
                    // 新取消固定的坐标使用当前时间戳，总是比已存在的临时坐标新
                    // 所以清除已存在的临时坐标（它的时间戳较小）
                    sTempMarker.remove();
                    XLog.i("取消固定：已存在的临时坐标时间戳 " + sTempMarkerTimestamp + " < 新临时坐标时间戳 " + newTimestamp + "，清除已存在的");
                }
                if (mCurrentLocationMarker != null && mCurrentLocationMarker != marker && !mIsCurrentLocationPinned) {
                    mCurrentLocationMarker.remove();
                }

                // 设置为当前未固定标记（成为新的临时坐标，使用新的时间戳）
                mCurrentLocationMarker = marker;
                sTempMarker = marker; // 更新静态变量
                mCurrentLocationMarkerTimestamp = newTimestamp;
                sTempMarkerTimestamp = newTimestamp;
                mIsCurrentLocationPinned = false;
                mMarkLatLngMap = marker.getPosition();
                mMarkName = name;

                GoUtils.DisplayToast(this, "已取消固定: " + name);
                XLog.i("取消固定位置: " + name + ", 新临时坐标时间戳: " + newTimestamp);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示设置区域对话框
     * @param lat 中心点纬度
     * @param lon 中心点经度
     * @param isPinned 是否为固定点（决定画出的是固定区域还是临时区域）
     */
    private void showSetRegionDialog(double lat, double lon, boolean isPinned) {
        // 创建简单的输入对话框
        EditText etInput = new EditText(this);
        etInput.setHint("请输入半径（km）");
        etInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
            .setTitle(isPinned ? "设置固定区域" : "设置临时区域")
            .setView(etInput)
            .setPositiveButton("确定", (dialog, which) -> {
                String radiusStr = etInput.getText().toString().trim();
                if (radiusStr.isEmpty()) {
                    GoUtils.DisplayToast(this, "请输入半径");
                    return;
                }
                
                try {
                    double radiusKm = Double.parseDouble(radiusStr);
                    if (radiusKm <= 0) {
                        GoUtils.DisplayToast(this, "半径必须大于0");
                        return;
                    }
                    
                    // 将km转换为米
                    double radiusM = radiusKm * 1000;
                    
                    if (isPinned) {
                        // 固定点：画出固定圆（深蓝色）
                        drawPinnedCircleFromPoint(lat, lon, radiusM);
                    } else {
                        // 临时点：画出临时圆（黄色）
                        drawTempCircleFromPoint(lat, lon, radiusM);
                    }
                } catch (NumberFormatException e) {
                    GoUtils.DisplayToast(this, "请输入有效的数字");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 从点坐标绘制临时圆（黄色）
     */
    private void drawTempCircleFromPoint(double lat, double lon, double radius) {
        LatLng center = new LatLng(lat, lon);
        
        // 绘制黄色临时圆
        Circle circle = mAMap.addCircle(new CircleOptions()
            .center(center)
            .radius(radius)
            .fillColor(0x55FFFF00) // 黄色半透明填充
            .strokeColor(0xFFCCCC00) // 黄色边框
            .strokeWidth(2));
        
        // 添加到多点定位列表中管理（作为临时圆）
        mMultiPointCircles.add(circle);
        mMultiPointCenters.add(center);
        mMultiPointRadius.add(radius);
        
        // 添加中心标记
        Marker marker = mAMap.addMarker(new MarkerOptions()
            .position(center)
            .title("临时区域中心")
            .snippet("半径: " + String.format("%.0f", radius) + "米")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        mMultiPointMarkers.add(marker);
        
        GoUtils.DisplayToast(this, "临时区域已设置，半径: " + String.format("%.0f", radius) + "米");
        XLog.i("绘制临时圆: 中心(" + lat + ", " + lon + "), 半径: " + radius + "米");
    }
    
    /**
     * 从点坐标绘制固定圆（深蓝色）
     */
    private void drawPinnedCircleFromPoint(double lat, double lon, double radius) {
        String regionName = "区域_" + System.currentTimeMillis();
        
        // 保存到固定圆数据库
        ContentValues contentValues = new ContentValues();
        contentValues.put(DataBasePinnedCircles.DB_COLUMN_NAME, regionName);
        contentValues.put(DataBasePinnedCircles.DB_COLUMN_CENTER_LAT, String.valueOf(lat));
        contentValues.put(DataBasePinnedCircles.DB_COLUMN_CENTER_LON, String.valueOf(lon));
        contentValues.put(DataBasePinnedCircles.DB_COLUMN_RADIUS, String.valueOf(radius));
        contentValues.put(DataBasePinnedCircles.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);
        
        DataBasePinnedCircles.savePinnedCircle(mPinnedCirclesDB, contentValues);
        
        // 绘制固定圆
        drawPinnedCircle(regionName, lat, lon, radius);
        
        GoUtils.DisplayToast(this, "固定区域已设置: " + regionName);
        XLog.i("绘制固定圆: " + regionName + " 中心(" + lat + ", " + lon + "), 半径: " + radius + "米");
    }

    /**
     * 重命名固定位置
     */
    private void renamePinnedLocation(int index) {
        if (index < 0 || index >= mPinnedLocationData.size()) return;

        Map<String, Object> data = mPinnedLocationData.get(index);
        String oldName = (String) data.get("name");
        double lat = (double) data.get("lat");
        double lon = (double) data.get("lon");
        Marker marker = mPinnedLocationMarkers.get(index);

        // 显示输入对话框
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null);
        EditText etName = view.findViewById(R.id.et_favorite_name);
        etName.setText(oldName);
        etName.selectAll();

        new AlertDialog.Builder(this)
            .setTitle("修改名称")
            .setView(view)
            .setPositiveButton("确定", (dialog, which) -> {
                String newName = etName.getText().toString().trim();
                if (newName.isEmpty()) {
                    GoUtils.DisplayToast(this, "名称不能为空");
                    return;
                }
                if (newName.equals(oldName)) {
                    return; // 名称未改变
                }

                // 从数据库中找到对应记录并更新
                java.util.List<Map<String, Object>> allPinned = DataBasePinnedLocations.getAllPinnedLocations(mPinnedLocationsDB);
                for (Map<String, Object> pinned : allPinned) {
                    String pinnedName = (String) pinned.get(DataBasePinnedLocations.DB_COLUMN_NAME);
                    if (pinnedName.equals(oldName)) {
                        String id = (String) pinned.get(DataBasePinnedLocations.DB_COLUMN_ID);
                        DataBasePinnedLocations.updatePinnedLocationName(mPinnedLocationsDB, id, newName);
                        break;
                    }
                }

                // 更新UI
                // 更新数据
                data.put("name", newName);
                // 更新标记
                marker.setIcon(createPinnedMarkerIcon(newName));
                marker.setTitle("已固定:" + newName);

                GoUtils.DisplayToast(this, "已修改名称为: " + newName);
                XLog.i("固定位置重命名: " + oldName + " -> " + newName);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 将固定位置添加到收藏
     */
    private void addPinnedLocationToFavorite(String name, double lat, double lon) {
        // 直接使用 GCJ02 坐标存储（与地图上显示的一致）
        ContentValues contentValues = new ContentValues();
        contentValues.put(DataBaseFavorites.DB_COLUMN_NAME, name);
        contentValues.put(DataBaseFavorites.DB_COLUMN_LONGITUDE, String.valueOf(lon));
        contentValues.put(DataBaseFavorites.DB_COLUMN_LATITUDE, String.valueOf(lat));
        contentValues.put(DataBaseFavorites.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

        DataBaseFavorites.saveFavorite(mFavoritesDB, contentValues);
        GoUtils.DisplayToast(this, "已添加到收藏: " + name);
        XLog.i("添加收藏: " + name + " 坐标: " + lon + " " + lat);
    }

    /**
     * 分享位置（供固定位置使用）
     */
    private void shareLocation(String name, double lon, double lat) {
        String[] options = {"分享文本", "生成二维码", "保存为文件"};
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

        new AlertDialog.Builder(this)
            .setTitle("分享位置")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    ShareUtils.shareText(this, "分享位置", shareText);
                } else if (which == 1) {
                    generateQRCodeForCurrentMarker(jsonData.toString(), name);
                } else {
                    saveJsonToFile(jsonData, name, "坐标数据");
                }
            })
            .show();
    }

    /**
     * 分享当前标记点
     */
    private void shareCurrentMarker() {
        if (mMarkLatLngMap == null) return;

        String[] options = {"分享文本", "生成二维码", "保存为文件"};
        String shareText = String.format("位置坐标: %.6f, %.6f", mMarkLatLngMap.latitude, mMarkLatLngMap.longitude);

        // 创建JSON数据
        String locationName = mMarkName != null ? mMarkName : "位置";
        JSONObject jsonData = new JSONObject();
        try {
            jsonData.put("type", "location");
            jsonData.put("name", locationName);
            jsonData.put("lat", mMarkLatLngMap.latitude);
            jsonData.put("lon", mMarkLatLngMap.longitude);
        } catch (JSONException e) {
            XLog.e("创建JSON失败: " + e.getMessage());
        }

        new AlertDialog.Builder(this)
            .setTitle("分享坐标")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 分享文本
                    ShareUtils.shareText(this, "分享位置", shareText);
                } else if (which == 1) {
                    // 生成二维码
                    generateQRCodeForCurrentMarker(jsonData.toString(), locationName);
                } else {
                    // 保存为文件
                    saveJsonToFile(jsonData, locationName, "坐标数据");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 为当前标记点生成二维码
     */
    private void generateQRCodeForCurrentMarker(String content, String name) {
        try {
            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(content,
                    com.google.zxing.BarcodeFormat.QR_CODE, 500, 500);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            // 显示二维码对话框
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setPadding(32, 32, 32, 32);

            new AlertDialog.Builder(this)
                .setTitle("扫描二维码获取位置")
                .setView(imageView)
                .setPositiveButton("导出/分享", (dialog, which) -> {
                    showExportQRCodeOptions(bitmap, name);
                })
                .setNegativeButton("关闭", null)
                .show();

        } catch (Exception e) {
            XLog.e("生成二维码失败: " + e.getMessage());
            GoUtils.DisplayToast(this, "生成二维码失败");
        }
    }

    /**
     * 在地图上显示固定位置标记
     */
    private void showPinnedLocationOnMap(String name, double lat, double lon) {
        LatLng position = new LatLng(lat, lon);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .icon(createPinnedMarkerIcon(name))
                .anchor(0.5f, 1.0f)
                .draggable(false)
                .title("已固定:" + name)
                .snippet(String.format("%.6f, %.6f", lon, lat));

        Marker marker = mAMap.addMarker(markerOptions);
        marker.setObject("pinned_location"); // 设置为固定标记tag

        // 添加到固定标记列表
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("lat", lat);
        data.put("lon", lon);
        data.put("marker", marker);

        mPinnedLocationMarkers.add(marker);
        mPinnedLocationData.add(data);

        // 移动到该位置
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16));
    }

    /**
     * 在地图上显示固定区域
     */
    private void showPinnedRegionOnMap(String name, double lat, double lon, double radius) {
        LatLng center = new LatLng(lat, lon);

        // 绘制固定圆（深蓝色）
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(5)
                .strokeColor(0xFF00008B) // 深蓝色边框
                .fillColor(0x5500008B); // 深蓝色填充

        Circle circle = mAMap.addCircle(circleOptions);

        // 添加中心点标记
        MarkerOptions markerOptions = new MarkerOptions()
                .position(center)
                .title(name)
                .snippet("半径: " + String.format("%.0f", radius) + "米 [已固定]")
                .draggable(false)
                .anchor(0.5f, 0.5f);

        Marker marker = mAMap.addMarker(markerOptions);

        // 添加到固定列表
        mPinnedCircles.add(circle);
        mPinnedCircleMarkers.add(marker);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("lat", lat);
        data.put("lon", lon);
        data.put("radius", radius);
        data.put("circle", circle);
        data.put("marker", marker);

        mPinnedCircleData.add(data);

        // 移动到该区域
        mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, calculateZoomLevel(radius)));

        XLog.i("显示固定区域: " + name + " (" + lat + ", " + lon + "), 半径: " + radius + "米");
    }

    /**
     * 处理取消固定坐标点（从固定管理返回）
     */
    private void handleUnpinLocation(String name, double lat, double lon) {
        // 在固定列表中查找该坐标点
        int index = -1;
        for (int i = 0; i < mPinnedLocationData.size(); i++) {
            Map<String, Object> data = mPinnedLocationData.get(i);
            String pinnedName = (String) data.get("name");
            if (pinnedName.equals(name)) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            XLog.w("未找到固定坐标点: " + name);
            return;
        }

        Marker marker = mPinnedLocationMarkers.get(index);

        // 将标记变回未固定状态（蓝色图标）
        marker.setIcon(mMapIndicator);
        marker.setTitle(name);
        marker.setSnippet(String.format("%.6f, %.6f", marker.getPosition().longitude, marker.getPosition().latitude));
        marker.setObject("temp_location"); // 重新设置为临时标记

        // 从固定列表移除
        mPinnedLocationMarkers.remove(index);
        mPinnedLocationData.remove(index);

        // 分配新的时间戳给这个取消固定的坐标（作为新的临时坐标）
        long newTimestamp = System.currentTimeMillis();

        // 检查是否存在其他临时坐标，如果存在则比较时间戳，清除较早的
        if (sTempMarker != null && sTempMarker != marker && sTempMarkerTimestamp > 0) {
            sTempMarker.remove();
            XLog.i("取消固定：已存在的临时坐标时间戳 " + sTempMarkerTimestamp + " < 新临时坐标时间戳 " + newTimestamp + "，清除已存在的");
        }
        if (mCurrentLocationMarker != null && mCurrentLocationMarker != marker && !mIsCurrentLocationPinned) {
            mCurrentLocationMarker.remove();
        }

        // 设置为当前未固定标记（成为新的临时坐标，使用新的时间戳）
        mCurrentLocationMarker = marker;
        sTempMarker = marker;
        mCurrentLocationMarkerTimestamp = newTimestamp;
        sTempMarkerTimestamp = newTimestamp;
        mIsCurrentLocationPinned = false;
        mMarkLatLngMap = marker.getPosition();
        mMarkName = name;

        GoUtils.DisplayToast(this, "已取消固定: " + name);
        XLog.i("取消固定位置: " + name + ", 新临时坐标时间戳: " + newTimestamp);
    }

    /**
     * 处理取消固定圆（从固定管理返回）
     */
    private void handleUnpinCircle(String name, double lat, double lon, double radius) {
        // 在固定列表中查找该圆
        int index = -1;
        for (int i = 0; i < mPinnedCircleData.size(); i++) {
            Map<String, Object> data = mPinnedCircleData.get(i);
            String pinnedName = (String) data.get("name");
            if (pinnedName.equals(name)) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            XLog.w("未找到固定圆: " + name);
            return;
        }

        // 从固定列表移除（但保留在地图上作为临时圆）
        Circle circle = mPinnedCircles.get(index);
        Marker marker = mPinnedCircleMarkers.get(index);

        // 从固定列表移除
        mPinnedCircles.remove(index);
        mPinnedCircleMarkers.remove(index);
        mPinnedCircleData.remove(index);

        // 将圆变回临时圆状态（黄色）
        circle.setStrokeColor(0xFFCCCC00); // 黄色边框
        circle.setFillColor(0x55FFFF00); // 黄色填充

        // 将圆添加到多点定位列表（作为临时圆管理）
        LatLng center = new LatLng(lat, lon);
        mMultiPointCircles.add(circle);
        mMultiPointCenters.add(center);
        mMultiPointRadius.add(radius);
        mMultiPointMarkers.add(marker);

        // 分配新的时间戳给这个取消固定的圆（作为新的临时圆）
        long newTimestamp = System.currentTimeMillis();

        // 检查是否存在其他临时圆，如果存在则比较时间戳，清除较早的
        if (mSelectedCircle != null && !mIsSelectedCirclePinned && sTempCircleTimestamp > 0) {
            mSelectedCircle.setStrokeColor(0xFFCCCC00);
            mSelectedCircle.setFillColor(0x55FFFF00);
            XLog.i("取消固定圆：已存在的临时圆时间戳 " + sTempCircleTimestamp + " < 新临时圆时间戳 " + newTimestamp + "，清除已存在的");
        }

        // 设置当前选中的圆为这个取消固定的圆
        mSelectedCircle = circle;
        mSelectedCircleIndex = mMultiPointCircles.size() - 1;
        mSelectedCircleCenter = center;
        mSelectedCircleRadius = radius;
        mIsSelectedCirclePinned = false;
        mSelectedCircleTimestamp = newTimestamp;
        sTempCircleTimestamp = newTimestamp;

        // 更新标记为未固定状态
        marker.setTitle(name);
        marker.setSnippet("半径: " + String.format("%.0f", radius) + "米");

        GoUtils.DisplayToast(this, "已取消固定: " + name);
        XLog.i("取消固定圆: " + name + ", 变为临时圆, 时间戳: " + newTimestamp);
    }

    /**
     * 将View转换为Bitmap
     */
    private Bitmap createBitmapFromView(View view) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * 创建固定标记的图标
     * 样式与区域文字框类似，但使用红色主题
     */
    private BitmapDescriptor createPinnedMarkerIcon(String name) {
        // 创建垂直布局的标记视图（与区域标记类似）
        LinearLayout markerView = new LinearLayout(this);
        markerView.setOrientation(LinearLayout.VERTICAL);
        markerView.setGravity(Gravity.CENTER);
        
        // 名称文本（上方）- 红色背景
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(12);
        nameView.setTextColor(Color.WHITE);
        nameView.setBackgroundColor(0xAAC62828); // 红色半透明背景（与区域深蓝色对应）
        nameView.setPadding(10, 5, 10, 5);
        nameView.setGravity(Gravity.CENTER);
        nameView.setMaxWidth(200);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setMaxLines(1);
        markerView.addView(nameView);
        
        // 转换为BitmapDescriptor
        markerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        
        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);
        
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * 固定当前标记点
     */
    private void pinCurrentMarker() {
        if (mMarkLatLngMap == null) return;

        // 显示输入对话框
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null);
        EditText etName = view.findViewById(R.id.et_favorite_name);
        // 不设置hint，使用布局文件中的默认hint

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("固定", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    name = "固定点_" + System.currentTimeMillis();
                }

                // 保存到固定位置数据库
                ContentValues contentValues = new ContentValues();
                contentValues.put(DataBasePinnedLocations.DB_COLUMN_NAME, name);
                contentValues.put(DataBasePinnedLocations.DB_COLUMN_LATITUDE, String.valueOf(mMarkLatLngMap.latitude));
                contentValues.put(DataBasePinnedLocations.DB_COLUMN_LONGITUDE, String.valueOf(mMarkLatLngMap.longitude));
                contentValues.put(DataBasePinnedLocations.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

                DataBasePinnedLocations.savePinnedLocation(mPinnedLocationsDB, contentValues);

                // 将当前标记转换为固定标记
                if (mCurrentLocationMarker != null) {
                    // 更新标记为固定样式
                    mCurrentLocationMarker.setIcon(createPinnedMarkerIcon(name));
                    // 设置title为"已固定:xx地点"，snippet为经纬度（经度 纬度）
                    mCurrentLocationMarker.setTitle("已固定:" + name);
                    mCurrentLocationMarker.setSnippet(String.format("%.6f %.6f", mMarkLatLngMap.longitude, mMarkLatLngMap.latitude));
                    mCurrentLocationMarker.setObject("pinned_location"); // 更改tag为固定标记

                    // 添加到固定标记列表
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("lat", mMarkLatLngMap.latitude);
                    data.put("lon", mMarkLatLngMap.longitude);
                    data.put("marker", mCurrentLocationMarker);

                    mPinnedLocationMarkers.add(mCurrentLocationMarker);
                    mPinnedLocationData.add(data);

                    // 标记为已固定
                    mIsCurrentLocationPinned = true;
                    mCurrentLocationMarker = null; // 清空成员变量
                    sTempMarker = null; // 清空静态变量，因为标记已转为固定状态，不再由临时标记管理
                } else {
                    // 如果没有当前标记，创建新的固定标记
                    showPinnedLocationOnMap(name, mMarkLatLngMap.latitude, mMarkLatLngMap.longitude);
                }

                GoUtils.DisplayToast(this, "已固定位置: " + name);
                XLog.i("固定位置: " + name);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 自定义InfoWindow适配器
     */
    private class CustomInfoWindowAdapter implements AMap.InfoWindowAdapter {
        private View mInfoWindowView;
        private Marker mCurrentMarker;

        @Override
        public View getInfoWindow(Marker marker) {
            mCurrentMarker = marker;
            if (mInfoWindowView == null) {
                mInfoWindowView = LayoutInflater.from(MainActivity.this).inflate(R.layout.info_window_location, null);
            }
            
            // 设置 InfoWindow 宽度为屏幕宽度的60%
            android.view.WindowManager windowManager = (android.view.WindowManager) getSystemService(Context.WINDOW_SERVICE);
            android.view.Display display = windowManager.getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);
            int screenWidth = size.x;
            mInfoWindowView.setLayoutParams(new android.view.ViewGroup.LayoutParams((int) (screenWidth * 0.6), 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            
            bindInfoWindowData(marker);
            return mInfoWindowView;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // 使用自定义窗口，不返回内容视图
            return null;
        }

        private void bindInfoWindowData(Marker marker) {
            if (mInfoWindowView == null || marker == null) return;

            // 获取视图
            TextView tvLocationName = mInfoWindowView.findViewById(R.id.tv_location_name);
            TextView tvAddress = mInfoWindowView.findViewById(R.id.tv_address);
            TextView tvGcj02 = mInfoWindowView.findViewById(R.id.tv_gcj02);
            TextView tvWgs84 = mInfoWindowView.findViewById(R.id.tv_wgs84);
            View ivSimulationStatus = mInfoWindowView.findViewById(R.id.iv_simulation_status);
            ImageView ivFavoriteStatus = mInfoWindowView.findViewById(R.id.iv_favorite_status);
            LinearLayout layoutRegionInfo = mInfoWindowView.findViewById(R.id.layout_region_info);
            TextView tvCenterPoint = mInfoWindowView.findViewById(R.id.tv_center_point);
            TextView tvRadius = mInfoWindowView.findViewById(R.id.tv_radius);
            ImageButton btnCopy = mInfoWindowView.findViewById(R.id.btn_copy);
            ImageButton btnFavorite = mInfoWindowView.findViewById(R.id.btn_favorite);
            ImageButton btnSimulate = mInfoWindowView.findViewById(R.id.btn_simulate);

            // 设置位置名称
            String rawTitle = marker.getTitle();
            final String title = (rawTitle == null || rawTitle.isEmpty() || rawTitle.equals("位置")) 
                ? "未命名位置" : rawTitle;
            tvLocationName.setText(title);

            // 获取坐标
            final LatLng position = marker.getPosition();
            final double lat = position.latitude;
            final double lon = position.longitude;

            // 转换为WGS84
            double[] wgs84 = MapUtils.gcj02ToWgs84(lon, lat);

            // 设置坐标显示
            tvGcj02.setText(String.format("GCJ02: %.6f, %.6f", lon, lat));
            tvWgs84.setText(String.format("WGS84: %.6f, %.6f", wgs84[0], wgs84[1]));

            // 检查是否是固定区域（通过marker的object判断）
            boolean isRegion = false;
            double radius = 0;
            Object obj = marker.getObject();
            if (obj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) obj;
                if (data.containsKey("radius")) {
                    isRegion = true;
                    radius = (double) data.get("radius");
                }
            }

            // 检查是否是模拟状态
            boolean isSimulating = isMockServStart && mMarkLatLngMap != null 
                && Math.abs(mMarkLatLngMap.latitude - lat) < 0.00001 
                && Math.abs(mMarkLatLngMap.longitude - lon) < 0.00001;
            ivSimulationStatus.setVisibility(isSimulating ? View.VISIBLE : View.GONE);
            if (isSimulating) {
                ivSimulationStatus.setBackgroundResource(R.drawable.circle_shape_pressed);
            }

            // 检查是否已收藏
            final boolean[] isFavorited = {checkIsFavorited(lat, lon)};
            ivFavoriteStatus.setVisibility(isFavorited[0] ? View.VISIBLE : View.GONE);
            btnFavorite.setImageResource(isFavorited[0] ? R.drawable.ic_favorite : R.drawable.ic_favorite);
            btnFavorite.setAlpha(isFavorited[0] ? 1.0f : 0.5f);

            // 区域信息显示
            if (isRegion) {
                layoutRegionInfo.setVisibility(View.VISIBLE);
                tvCenterPoint.setText(String.format("中心: %.3f, %.3f", lon, lat));
                tvRadius.setText(String.format("半径: %.0fm", radius));
            } else {
                layoutRegionInfo.setVisibility(View.GONE);
            }

            // 设置地址（使用snippet）
            String snippet = marker.getSnippet();
            tvAddress.setText(snippet != null && !snippet.isEmpty() ? snippet : "暂无地址信息");

            // 复制按钮点击
            btnCopy.setOnClickListener(v -> {
                String coordText = String.format("%.6f,%.6f", lon, lat);
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("坐标", coordText);
                cm.setPrimaryClip(clip);
                GoUtils.DisplayToast(MainActivity.this, "坐标已复制");
            });

            // 收藏按钮点击
            btnFavorite.setOnClickListener(v -> {
                if (isFavorited[0]) {
                    GoUtils.DisplayToast(MainActivity.this, "该位置已收藏");
                } else {
                    addMarkerToFavorite(marker, title, lat, lon);
                    isFavorited[0] = true;
                    ivFavoriteStatus.setVisibility(View.VISIBLE);
                    btnFavorite.setAlpha(1.0f);
                }
            });

            // 模拟按钮点击
            btnSimulate.setOnClickListener(v -> {
                // 更新当前标记
                mMarkLatLngMap = position;
                mMarkName = title;
                // 调用模拟方法
                doGoLocation(mInfoWindowView);
            });
        }
    }

    /**
     * 检查位置是否已收藏
     */
    private boolean checkIsFavorited(double lat, double lon) {
        try {
            List<Map<String, Object>> favorites = getFavoritesData();
            for (Map<String, Object> fav : favorites) {
                double favLat = (double) fav.get("lat");
                double favLon = (double) fav.get("lon");
                if (Math.abs(favLat - lat) < 0.00001 && Math.abs(favLon - lon) < 0.00001) {
                    return true;
                }
            }
        } catch (Exception e) {
            XLog.e("检查收藏状态失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 将标记添加到收藏
     */
    private void addMarkerToFavorite(Marker marker, String name, double lat, double lon) {
        // 直接使用 GCJ02 坐标存储
        ContentValues contentValues = new ContentValues();
        contentValues.put(DataBaseFavorites.DB_COLUMN_NAME, name);
        contentValues.put(DataBaseFavorites.DB_COLUMN_LONGITUDE, String.valueOf(lon));
        contentValues.put(DataBaseFavorites.DB_COLUMN_LATITUDE, String.valueOf(lat));
        contentValues.put(DataBaseFavorites.DB_COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000);

        DataBaseFavorites.saveFavorite(mFavoritesDB, contentValues);
        GoUtils.DisplayToast(this, "已收藏: " + name);
        XLog.i("添加收藏: " + name + " 坐标: " + lon + " " + lat);
        
        // 刷新InfoWindow显示
        marker.showInfoWindow();
    }

}
