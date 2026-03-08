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

package com.river.gowithamap.joystick;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SearchView;

import androidx.preference.PreferenceManager;

import com.elvishew.xlog.XLog;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Poi;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.river.gowithamap.database.DataBaseHistoryLocation;
import com.river.gowithamap.HistoryActivity;
import com.river.gowithamap.MainActivity;
import com.river.gowithamap.R;
import com.river.gowithamap.utils.GoUtils;
import com.river.gowithamap.utils.MapUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoyStick extends View {
    private static final int DivGo = 1000;    /* 移动的时间间隔，单位 ms */
    private static final int WINDOW_TYPE_JOYSTICK = 0;
    private static final int WINDOW_TYPE_MAP = 1;
    private static final int WINDOW_TYPE_HISTORY = 2;

    private final Context mContext;
    private WindowManager.LayoutParams mWindowParamCurrent;
    private WindowManager mWindowManager;
    private int mCurWin = WINDOW_TYPE_JOYSTICK;
    private final LayoutInflater inflater;
    private boolean isWalk;
    private ImageButton btnWalk;
    private boolean isRun;
    private ImageButton btnRun;
    private boolean isBike;
    private ImageButton btnBike;
    private JoyStickClickListener mListener;

    // 移动
    private View mJoystickLayout;
    private GoUtils.TimeCount mTimer;
    private boolean isMove;
    private double mSpeed = 1.2;        /* 默认的速度，单位 m/s */
    private double mAltitude = 55.0;
    private double mAngle = 0;
    private double mR = 0;
    private double disLng = 0;
    private double disLat = 0;
    private final SharedPreferences sharedPreferences;
    /* 历史记录悬浮窗相关 */
    private FrameLayout mHistoryLayout;
    private final List<Map<String, Object>> mAllRecord = new ArrayList<> ();
    private TextView noRecordText;
    private ListView mRecordListView;
    /* 地图悬浮窗相关 */
    private FrameLayout mMapLayout;
    private MapView mMapView;
    private AMap mAMap;
    private LatLng mCurMapLngLat;
    private LatLng mMarkMapLngLat;
    private Inputtips mInputtips;
    private ListView mSearchList;
    private LinearLayout mSearchLayout;

    public JoyStick(Context context) {
        super(context);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickMapView();

            initHistoryView();
        }
    }

    public JoyStick(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickMapView();

            initHistoryView();
        }
    }

    public JoyStick(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        initWindowManager();

        inflater = LayoutInflater.from(mContext);

        if (inflater != null) {
            initJoyStickView();

            initJoyStickMapView();

            initHistoryView();
        }
    }

    public void setCurrentPosition(double lng, double lat, double alt) {
        double[] lngLat = MapUtils.wgs84ToGcj02(lng, lat);
        mCurMapLngLat = new LatLng(lngLat[1], lngLat[0]);
        mAltitude = alt;

        resetAMap();
    }

    public void show() {
        switch (mCurWin) {
            case WINDOW_TYPE_MAP:
                if (mJoystickLayout.getParent() != null) {
                    mWindowManager.removeView(mJoystickLayout);
                }
                if (mHistoryLayout.getParent() != null) {
                    mWindowManager.removeView(mHistoryLayout);
                }
                if (mMapLayout.getParent() == null) {
                    resetAMap();
                    mWindowManager.addView(mMapLayout, mWindowParamCurrent);
                }
                break;
            case WINDOW_TYPE_HISTORY:
                if (mMapLayout.getParent() != null) {
                    mWindowManager.removeView(mMapLayout);
                }
                if (mJoystickLayout.getParent() != null) {
                    mWindowManager.removeView(mJoystickLayout);
                }
                if (mHistoryLayout.getParent() == null) {
                    mWindowManager.addView(mHistoryLayout, mWindowParamCurrent);
                }
                break;
            case WINDOW_TYPE_JOYSTICK:
                if (mMapLayout.getParent() != null) {
                    mWindowManager.removeView(mMapLayout);
                }
                if (mHistoryLayout.getParent() != null) {
                    mWindowManager.removeView(mHistoryLayout);
                }
                if (mJoystickLayout.getParent() == null) {
                    mWindowManager.addView(mJoystickLayout, mWindowParamCurrent);
                }
                break;
        }
    }

    public void hide() {
        if (mMapLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mMapLayout);
        }

        if (mJoystickLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mJoystickLayout);
        }

        if (mHistoryLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mHistoryLayout);
        }
    }

    public void destroy() {
        if (mMapLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mMapLayout);
        }

        if (mJoystickLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mJoystickLayout);
        }

        if (mHistoryLayout.getParent() != null) {
            mWindowManager.removeViewImmediate(mHistoryLayout);
        }

        if (mAMap != null) {
            mAMap.setMyLocationEnabled(false);
        }
        mMapView.onDestroy();
    }

    public void setListener(JoyStickClickListener mListener) {
        this.mListener = mListener;
    }

    private void initWindowManager() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParamCurrent = new WindowManager.LayoutParams();
        mWindowParamCurrent.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mWindowParamCurrent.format = PixelFormat.RGBA_8888;
        mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE      // 不添加这个将导致游戏无法启动（MIUI12）,添加之后导致键盘无法显示
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowParamCurrent.gravity = Gravity.START | Gravity.TOP;
        mWindowParamCurrent.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParamCurrent.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParamCurrent.x = 300;
        mWindowParamCurrent.y = 300;
    }

    @SuppressLint("InflateParams")
    private void initJoyStickView() {
        /* 移动计时器 */
        mTimer = new GoUtils.TimeCount(DivGo, DivGo);
        mTimer.setListener(new GoUtils.TimeCount.TimeCountListener() {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
                disLng = mSpeed * (double)(DivGo / 1000) * mR * Math.cos(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                disLat = mSpeed * (double)(DivGo / 1000) * mR * Math.sin(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                mListener.onMoveInfo(mSpeed, disLng, disLat, 90.0F-mAngle);
                mTimer.start();
            }
        });
        // 获取参数区设置的速度
        try {
            mSpeed = Double.parseDouble(sharedPreferences.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));
        } catch (NumberFormatException e) {  // GOOD: The exception is caught.
            mSpeed = 1.2;
        }
        mJoystickLayout = inflater.inflate(R.layout.joystick, null);

        /* 整个摇杆拖动事件处理 */
        mJoystickLayout.setOnTouchListener(new JoyStickOnTouchListener());

        /* 位置按钮点击事件处理 */
        ImageButton btnPosition = mJoystickLayout.findViewById(R.id.joystick_position);
        btnPosition.setOnClickListener(v -> {
            if (mMapLayout.getParent() == null) {
                mCurWin = WINDOW_TYPE_MAP;
                show();
            }
        });

        /* 历史按钮点击事件处理 */
        ImageButton btnHistory = mJoystickLayout.findViewById(R.id.joystick_history);
        btnHistory.setOnClickListener(v -> {
            Log.d("JOYSTICK", "History button clicked");
            if (mHistoryLayout == null) {
                Log.e("JOYSTICK", "mHistoryLayout is null!");
                return;
            }
            Log.d("JOYSTICK", "mHistoryLayout.getParent()=" + mHistoryLayout.getParent());
            if (mHistoryLayout.getParent() == null) {
                Log.d("JOYSTICK", "Fetching records...");
                // 每次打开历史记录时刷新数据
                fetchAllRecord();
                Log.d("JOYSTICK", "Showing history, mAllRecord size=" + mAllRecord.size());
                showHistory(mAllRecord);
                mCurWin = WINDOW_TYPE_HISTORY;
                Log.d("JOYSTICK", "Calling show()");
                show();
                Log.d("JOYSTICK", "History button click handled OK");
            } else {
                Log.d("JOYSTICK", "HistoryLayout already has parent, skipping");
            }
        });

        /* 步行按键的点击处理 */
        btnWalk = mJoystickLayout.findViewById(R.id.joystick_walk);
        btnWalk.setOnClickListener(v -> {
            if (!isWalk) {
                btnWalk.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
                isWalk = true;
                btnRun.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isRun = false;
                btnBike.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isBike = false;
                try {
                    mSpeed = Double.parseDouble(sharedPreferences.getString("setting_walk", getResources().getString(R.string.setting_walk_default)));
                } catch (NumberFormatException e) {  // GOOD: The exception is caught.
                    mSpeed = 1.2;
                }
            }
        });
        /* 默认为步行 */
        isWalk = true;
        btnWalk.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
        /* 跑步按键的点击处理 */
        isRun = false;
        btnRun = mJoystickLayout.findViewById(R.id.joystick_run);
        btnRun.setOnClickListener(v -> {
            if (!isRun) {
                btnRun.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
                isRun = true;
                btnWalk.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isWalk = false;
                btnBike.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isBike = false;
                try {
                    mSpeed = Double.parseDouble(sharedPreferences.getString("setting_run", getResources().getString(R.string.setting_run_default)));
                } catch (NumberFormatException e) {  // GOOD: The exception is caught.
                    mSpeed = 3.6;
                }
            }
        });
        /* 自行车按键的点击处理 */
        isBike = false;
        btnBike = mJoystickLayout.findViewById(R.id.joystick_bike);
        btnBike.setOnClickListener(v -> {
            if (!isBike) {
                btnBike.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));
                isBike = true;
                btnWalk.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isWalk = false;
                btnRun.setColorFilter(getResources().getColor(R.color.black, mContext.getTheme()));
                isRun = false;
                try {
                    mSpeed = Double.parseDouble(sharedPreferences.getString("setting_bike", getResources().getString(R.string.setting_bike_default)));
                } catch (NumberFormatException e) {  // GOOD: The exception is caught.
                    mSpeed = 10.0;
                }
            }
        });
        /* 方向键点击处理 */
        RockerView rckView = mJoystickLayout.findViewById(R.id.joystick_rocker);
        rckView.setListener(this::processDirection);

        /* 方向键点击处理 */
        ButtonView btnView = mJoystickLayout.findViewById(R.id.joystick_button);
        btnView.setListener(this::processDirection);

        /* 这里用来决定摇杆类型 */
        if (sharedPreferences.getString("setting_joystick_type", "0").equals("0")) {
            rckView.setVisibility(VISIBLE);
            btnView.setVisibility(GONE);
        } else {
            rckView.setVisibility(GONE);
            btnView.setVisibility(VISIBLE);
        }
    }

    private void processDirection(boolean auto, double angle, double r) {
        if (r <= 0) {
            mTimer.cancel();
            isMove = false;
        } else {
            mAngle = angle;
            mR = r;
            if (auto) {
                if (!isMove) {
                    mTimer.start();
                    isMove = true;
                }
            } else {
                mTimer.cancel();
                isMove = false;
                // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）且转换为 km
                disLng = mSpeed * (double)(DivGo / 1000) * mR * Math.cos(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                disLat = mSpeed * (double)(DivGo / 1000) * mR * Math.sin(mAngle * 2 * Math.PI / 360) / 1000;// 注意安卓中的三角函数使用的是弧度
                mListener.onMoveInfo(mSpeed, disLng, disLat, 90.0F-mAngle);
            }
        }
    }

    private class JoyStickOnTouchListener implements OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;

                    mWindowParamCurrent.x += movedX;
                    mWindowParamCurrent.y += movedY;
                    mWindowManager.updateViewLayout(view, mWindowParamCurrent);
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    public interface JoyStickClickListener {
        void onMoveInfo(double speed, double disLng, double disLat, double angle);
        void onPositionInfo(double lng, double lat, double alt);
    }


    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void initJoyStickMapView() {
        mMapLayout = (FrameLayout)inflater.inflate(R.layout.joystick_map, null);
        mMapLayout.setOnTouchListener(new JoyStickOnTouchListener());

        mSearchList = mMapLayout.findViewById(R.id.map_search_list_view);
        mSearchLayout = mMapLayout.findViewById(R.id.map_search_linear);
        try {
            //noinspection deprecation
            mInputtips = new Inputtips(mContext, (tips, returnCode) -> {
                if (returnCode != 1000 || tips == null) {
                    GoUtils.DisplayToast(mContext,getResources().getString(R.string.app_search_null));
                } else {
                    List<Map<String, Object>> data = new ArrayList<>();

                    for (Tip tip : tips) {
                        if (tip.getPoint() == null) {
                            continue;
                        }

                        Map<String, Object> poiItem = new HashMap<>();
                        poiItem.put(MainActivity.POI_NAME, tip.getName());
                        poiItem.put(MainActivity.POI_ADDRESS, tip.getDistrict());
                        poiItem.put(MainActivity.POI_LONGITUDE, "" + tip.getPoint().getLongitude());
                        poiItem.put(MainActivity.POI_LATITUDE, "" + tip.getPoint().getLatitude());
                        data.add(poiItem);
                }

                SimpleAdapter simAdapt = new SimpleAdapter(
                        mContext,
                        data,
                        R.layout.search_poi_item,
                        new String[] {MainActivity.POI_NAME, MainActivity.POI_ADDRESS, MainActivity.POI_LONGITUDE, MainActivity.POI_LATITUDE}, // 与下面数组元素要一一对应
                        new int[] {R.id.poi_name, R.id.poi_address, R.id.poi_longitude, R.id.poi_latitude});
                mSearchList.setAdapter(simAdapt);
                mSearchLayout.setVisibility(View.VISIBLE);
                }
            });
        } catch (com.amap.api.services.core.AMapException e) {
            XLog.e("Inputtips init error: " + e.getMessage());
        }
        mSearchList.setOnItemClickListener((parent, view, position, id) -> {
            mSearchLayout.setVisibility(View.GONE);

            String lng = ((TextView) view.findViewById(R.id.poi_longitude)).getText().toString();
            String lat = ((TextView) view.findViewById(R.id.poi_latitude)).getText().toString();
            markAMap(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
        });

        TextView tips = mMapLayout.findViewById(R.id.joystick_map_tips);
        SearchView mSearchView = mMapLayout.findViewById(R.id.joystick_map_searchView);
        mSearchView.setOnSearchClickListener(v -> {
            tips.setVisibility(GONE);

            // 特殊处理：这里让搜索框获取焦点，以显示输入法
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            mWindowManager.updateViewLayout(mMapLayout, mWindowParamCurrent);
        });
        mSearchView.setOnCloseListener(() -> {
            tips.setVisibility(VISIBLE);
            mSearchLayout.setVisibility(GONE);

            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            mWindowManager.updateViewLayout(mMapLayout, mWindowParamCurrent);

            return false;       /* 这里必须返回false，否则需要自行处理搜索框的折叠 */
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText != null && newText.length() > 0) {
                    try {
                        mInputtips.setQuery(new InputtipsQuery(newText, MainActivity.mCurrentCity));
                        mInputtips.requestInputtipsAsyn();
                    } catch (Exception e) {
                        GoUtils.DisplayToast(mContext,getResources().getString(R.string.app_error_search));
                        e.printStackTrace();
                    }
                } else {
                    mSearchLayout.setVisibility(GONE);
                }

                return true;
            }
        });

        ImageButton btnGo = mMapLayout.findViewById(R.id.btnGo);
        btnGo.setOnClickListener(v -> {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            mWindowManager.updateViewLayout(mMapLayout, mWindowParamCurrent);

            tips.setVisibility(VISIBLE);
            mSearchView.clearFocus();
            mSearchView.onActionViewCollapsed();

            if (mMarkMapLngLat == null) {
                GoUtils.DisplayToast(mContext, getResources().getString(R.string.app_error_location));
            } else {
                if (mCurMapLngLat != mMarkMapLngLat) {
                    mCurMapLngLat = mMarkMapLngLat;
                    mMarkMapLngLat = null;

                    double[] lngLat = MapUtils.gcj02ToWgs84(mCurMapLngLat.longitude, mCurMapLngLat.latitude);
                    mListener.onPositionInfo(lngLat[0], lngLat[1], mAltitude);

                    resetAMap();

                    GoUtils.DisplayToast(mContext, getResources().getString(R.string.app_location_ok));
                }
            }
        });
        btnGo.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

        ImageButton btnClose = mMapLayout.findViewById(R.id.map_close);
        btnClose.setOnClickListener(v -> {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            tips.setVisibility(VISIBLE);
            mSearchLayout.setVisibility(GONE);
            mSearchView.clearFocus();
            mSearchView.onActionViewCollapsed();

            mCurWin = WINDOW_TYPE_JOYSTICK;
            show();
        });

        ImageButton btnBack = mMapLayout.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> resetAMap());
        btnBack.setColorFilter(getResources().getColor(R.color.colorAccent, mContext.getTheme()));

        initAMap();
    }

    private void initAMap() {
        mMapView = mMapLayout.findViewById(R.id.map_joystick);
        // 2D SDK 没有 showZoomControls 方法
        mAMap = mMapView.getMap();
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        mAMap.setMyLocationStyle(myLocationStyle);
        mAMap.setMyLocationEnabled(true);

        mAMap.setOnMapClickListener(latLng -> markAMap(latLng));
        // 2D SDK 没有 setOnPOIClickListener 方法
        mAMap.setOnMapLongClickListener(latLng -> markAMap(latLng));
    }

    private void resetAMap() {
        mAMap.clear();

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mCurMapLngLat)
                .zoom(18)
                .build();
        mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void markAMap(LatLng latLng) {
        mMarkMapLngLat = latLng;

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(MainActivity.mMapIndicator);
        mAMap.clear();
        mAMap.addMarker(markerOptions);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(18)
                .build();
        mAMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }


    @SuppressLint({"InflateParams", "ClickableViewAccessibility"})
    private void initHistoryView() {
        Log.d("JOYSTICK", "initHistoryView START");
        
        Log.d("JOYSTICK", "Inflating joystick_history layout");
        mHistoryLayout = (FrameLayout)inflater.inflate(R.layout.joystick_history, null);
        if (mHistoryLayout == null) {
            Log.e("JOYSTICK", "ERROR - mHistoryLayout is null after inflate!");
            return;
        }
        Log.d("JOYSTICK", "mHistoryLayout inflated OK");
        
        mHistoryLayout.setOnTouchListener(new JoyStickOnTouchListener());
        Log.d("JOYSTICK", "OnTouchListener set OK");

        TextView tips = mHistoryLayout.findViewById(R.id.joystick_his_tips);
        SearchView mSearchView = mHistoryLayout.findViewById(R.id.joystick_his_searchView);
        mSearchView.setOnSearchClickListener(v -> {
            tips.setVisibility(GONE);

            // 特殊处理：这里让搜索框获取焦点，以显示输入法
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            mWindowManager.updateViewLayout(mHistoryLayout, mWindowParamCurrent);
        });
        mSearchView.setOnCloseListener(() -> {
            tips.setVisibility(VISIBLE);

            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            mWindowManager.updateViewLayout(mHistoryLayout, mWindowParamCurrent);

            return false;       /* 这里必须返回false，否则需要自行处理搜索框的折叠 */
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {// 当点击搜索按钮时触发该方法
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {// 当搜索内容改变时触发该方法
                try {
                    if (TextUtils.isEmpty(newText)) {
                        showHistory(mAllRecord);
                    } else {
                        List<Map<String, Object>> searchRet = new ArrayList<>();
                        for (int i = 0; i < mAllRecord.size(); i++){
                            if (mAllRecord.get(i).toString().indexOf(newText) > 0){
                                searchRet.add(mAllRecord.get(i));
                            }
                        }

                        if (searchRet.size() > 0) {
                            showHistory(searchRet);
                        } else {
                            GoUtils.DisplayToast(mContext, getResources().getString(R.string.app_search_null));
                            showHistory(mAllRecord);
                        }
                    }
                } catch (Exception e) {
                    Log.e("JOYSTICK", "ERROR - onQueryTextChange: " + e.getMessage());
                    e.printStackTrace();
                }
                return false;
            }
        });

        Log.d("JOYSTICK", "Finding noRecordText and mRecordListView");
        noRecordText = mHistoryLayout.findViewById(R.id.joystick_his_record_no_textview);
        mRecordListView = mHistoryLayout.findViewById(R.id.joystick_his_record_list_view);
        
        if (noRecordText == null) {
            Log.e("JOYSTICK", "ERROR - noRecordText is null!");
        }
        if (mRecordListView == null) {
            Log.e("JOYSTICK", "ERROR - mRecordListView is null!");
        }
        Log.d("JOYSTICK", "Views found: noRecordText=" + noRecordText + ", mRecordListView=" + mRecordListView);
        
        Log.d("JOYSTICK", "Setting OnItemClickListener");
        mRecordListView.setOnItemClickListener((adapterView, view, i, l) -> {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            mWindowManager.updateViewLayout(mHistoryLayout, mWindowParamCurrent);

            mSearchView.clearFocus();
            mSearchView.onActionViewCollapsed();
            tips.setVisibility(VISIBLE);

            // wgs84坐标
            String wgs84LatLng = (String) ((TextView) view.findViewById(R.id.WGSLatLngText)).getText();
            wgs84LatLng = wgs84LatLng.substring(wgs84LatLng.indexOf('[') + 1, wgs84LatLng.indexOf(']'));
            String[] wgs84latLngStr = wgs84LatLng.split(" ");
            String wgs84Longitude = wgs84latLngStr[0].substring(wgs84latLngStr[0].indexOf(':') + 1);
            String wgs84Latitude = wgs84latLngStr[1].substring(wgs84latLngStr[1].indexOf(':') + 1);

            mListener.onPositionInfo(Double.parseDouble(wgs84Longitude), Double.parseDouble(wgs84Latitude), mAltitude);

            // 注意这里在选择位置之后需要刷新地图
            String gcj02LatLng = (String) ((TextView) view.findViewById(R.id.BDLatLngText)).getText();
            gcj02LatLng = gcj02LatLng.substring(gcj02LatLng.indexOf('[') + 1, gcj02LatLng.indexOf(']'));
            String[] gcj02LatLngStr = gcj02LatLng.split(" ");
            String gcj02Longitude = gcj02LatLngStr[0].substring(gcj02LatLngStr[0].indexOf(':') + 1);
            String gcj02Latitude = gcj02LatLngStr[1].substring(gcj02LatLngStr[1].indexOf(':') + 1);
            mCurMapLngLat = new LatLng(Double.parseDouble(gcj02Latitude), Double.parseDouble(gcj02Longitude));

            GoUtils.DisplayToast(mContext, getResources().getString(R.string.app_location_ok));
        });

        Log.d("JOYSTICK", "Calling fetchAllRecord from initHistoryView");
        fetchAllRecord();

        Log.d("JOYSTICK", "Calling showHistory from initHistoryView");
        showHistory(mAllRecord);

        Log.d("JOYSTICK", "Finding btnClose");
        ImageButton btnClose = mHistoryLayout.findViewById(R.id.joystick_his_close);
        if (btnClose == null) {
            Log.e("JOYSTICK", "ERROR - btnClose is null!");
        }
        btnClose.setOnClickListener(v -> {
            // 关闭时清除焦点
            mWindowParamCurrent.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            mSearchView.clearFocus();
            mSearchView.onActionViewCollapsed();
            tips.setVisibility(VISIBLE);

            mCurWin = WINDOW_TYPE_JOYSTICK;
            show();
        });
        
        Log.d("JOYSTICK", "initHistoryView END SUCCESS");
    }

    private void fetchAllRecord() {
        Log.d("JOYSTICK", "fetchAllRecord START");
        SQLiteDatabase mHistoryLocationDB = null;
        Cursor cursor = null;

        try {
            // 先清空列表，防止数据重复累积
            Log.d("JOYSTICK", "Clearing mAllRecord, size=" + mAllRecord.size());
            mAllRecord.clear();
            
            Log.d("JOYSTICK", "Creating DataBaseHistoryLocation");
            DataBaseHistoryLocation hisLocDBHelper = new DataBaseHistoryLocation(mContext.getApplicationContext());
            
            Log.d("JOYSTICK", "Getting writable database");
            mHistoryLocationDB = hisLocDBHelper.getWritableDatabase();
            
            Log.d("JOYSTICK", "Querying database");
            cursor = mHistoryLocationDB.query(DataBaseHistoryLocation.TABLE_NAME, null,
                    DataBaseHistoryLocation.DB_COLUMN_ID + " > ?", new String[] {"0"},
                    null, null, DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP + " DESC", null);

            Log.d("JOYSTICK", "Cursor count=" + cursor.getCount());
            
            // 获取列索引，避免硬编码
            Log.d("JOYSTICK", "Getting column indices");
            int idIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_ID);
            int locationIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_LOCATION);
            int longitudeIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_WGS84);
            int latitudeIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_WGS84);
            int timestampIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_TIMESTAMP);
            int gcjLongitudeIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_LONGITUDE_CUSTOM);
            int gcjLatitudeIndex = cursor.getColumnIndexOrThrow(DataBaseHistoryLocation.DB_COLUMN_LATITUDE_CUSTOM);
            Log.d("JOYSTICK", "Column indices OK");

            int rowCount = 0;
            while (cursor.moveToNext()) {
                rowCount++;
                Log.d("JOYSTICK", "Processing row " + rowCount);
                
                Map<String, Object> item = new HashMap<>();
                int ID = cursor.getInt(idIndex);
                Log.d("JOYSTICK", "ID=" + ID);
                
                String Location = cursor.getString(locationIndex);
                Log.d("JOYSTICK", "Location=" + Location);
                
                String Longitude = cursor.getString(longitudeIndex);
                Log.d("JOYSTICK", "Longitude=" + Longitude);
                
                String Latitude = cursor.getString(latitudeIndex);
                Log.d("JOYSTICK", "Latitude=" + Latitude);
                
                long TimeStamp = cursor.getLong(timestampIndex);
                Log.d("JOYSTICK", "TimeStamp=" + TimeStamp);
                
                String GCJ02Longitude = cursor.getString(gcjLongitudeIndex);
                Log.d("JOYSTICK", "GCJ02Longitude=" + GCJ02Longitude);
                
                String GCJ02Latitude = cursor.getString(gcjLatitudeIndex);
                Log.d("JOYSTICK", "GCJ02Latitude=" + GCJ02Latitude);
                
                Log.d("TB", ID + "\t" + Location + "\t" + Longitude + "\t" + Latitude + "\t" + TimeStamp + "\t" + GCJ02Longitude + "\t" + GCJ02Latitude);
                
                Log.d("JOYSTICK", "Parsing BigDecimal");
                BigDecimal bigDecimalLongitude = BigDecimal.valueOf(Double.parseDouble(Longitude));
                BigDecimal bigDecimalLatitude = BigDecimal.valueOf(Double.parseDouble(Latitude));
                BigDecimal bigDecimalGCJLongitude = BigDecimal.valueOf(Double.parseDouble(GCJ02Longitude));
                BigDecimal bigDecimalGCJLatitude = BigDecimal.valueOf(Double.parseDouble(GCJ02Latitude));
                
                Log.d("JOYSTICK", "Setting scale");
                double doubleLongitude = bigDecimalLongitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleLatitude = bigDecimalLatitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleGCJLongitude = bigDecimalGCJLongitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                double doubleGCJLatitude = bigDecimalGCJLatitude.setScale(11, RoundingMode.HALF_UP).doubleValue();
                
                Log.d("JOYSTICK", "Putting item data");
                item.put(HistoryActivity.KEY_ID, Integer.toString(ID));
                item.put(HistoryActivity.KEY_LOCATION, Location);
                item.put(HistoryActivity.KEY_TIME, GoUtils.timeStamp2Date(Long.toString(TimeStamp)));
                item.put(HistoryActivity.KEY_LNG_LAT_WGS, "[经度:" + doubleLongitude + " 纬度:" + doubleLatitude + "]");
                item.put(HistoryActivity.KEY_LNG_LAT_CUSTOM, "[经度:" + doubleGCJLongitude + " 纬度:" + doubleGCJLatitude + "]");
                
                Log.d("JOYSTICK", "Adding item to mAllRecord");
                mAllRecord.add(item);
                Log.d("JOYSTICK", "Row " + rowCount + " processed OK");
            }
            Log.d("JOYSTICK", "Total rows processed=" + rowCount);
            
            if (cursor != null) {
                cursor.close();
            }
            if (mHistoryLocationDB != null) {
                mHistoryLocationDB.close();
            }
            Log.d("JOYSTICK", "fetchAllRecord END SUCCESS, mAllRecord size=" + mAllRecord.size());
        } catch (Exception e) {
            Log.e("JOYSTICK", "ERROR - fetchAllRecord: " + e.getMessage());
            Log.e("JOYSTICK", "ERROR - Stack trace: ");
            e.printStackTrace();
            // 确保关闭资源
            if (cursor != null) {
                try { cursor.close(); } catch (Exception ex) { }
            }
            if (mHistoryLocationDB != null) {
                try { mHistoryLocationDB.close(); } catch (Exception ex) { }
            }
        }
    }

    private void showHistory(List<Map<String, Object>> list) {
        Log.d("JOYSTICK", "showHistory START, list size=" + (list == null ? "null" : list.size()));
        
        // 添加null检查，防止视图未初始化时崩溃
        if (mRecordListView == null || noRecordText == null) {
            Log.e("JOYSTICK", "ERROR - showHistory: views not initialized, mRecordListView=" + mRecordListView + ", noRecordText=" + noRecordText);
            return;
        }
        Log.d("JOYSTICK", "Views are initialized OK");
        
        if (list == null || list.size() == 0) {
            Log.d("JOYSTICK", "List is empty, showing noRecordText");
            mRecordListView.setVisibility(View.GONE);
            noRecordText.setVisibility(View.VISIBLE);
        } else {
            Log.d("JOYSTICK", "List has " + list.size() + " items, showing list");
            noRecordText.setVisibility(View.GONE);
            mRecordListView.setVisibility(View.VISIBLE);

            try {
                Log.d("JOYSTICK", "Creating SimpleAdapter");
                SimpleAdapter simAdapt = new SimpleAdapter(
                        mContext,
                        list,
                        R.layout.history_item,
                        new String[]{HistoryActivity.KEY_ID, HistoryActivity.KEY_LOCATION, HistoryActivity.KEY_TIME, HistoryActivity.KEY_LNG_LAT_WGS, HistoryActivity.KEY_LNG_LAT_CUSTOM}, // 与下面数组元素要一一对应
                        new int[]{R.id.LocationID, R.id.LocationText, R.id.TimeText, R.id.WGSLatLngText, R.id.BDLatLngText});
                Log.d("JOYSTICK", "Setting adapter");
                mRecordListView.setAdapter(simAdapt);
                Log.d("JOYSTICK", "showHistory END SUCCESS");
            } catch (Exception e) {
                Log.e("JOYSTICK", "ERROR - showHistory: " + e.getMessage());
                Log.e("JOYSTICK", "ERROR - Stack trace:");
                e.printStackTrace();
            }
        }
    }
}