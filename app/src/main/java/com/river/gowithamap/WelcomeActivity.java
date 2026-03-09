package com.river.gowithamap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.river.gowithamap.utils.GoUtils;

import java.util.ArrayList;

public class WelcomeActivity extends BaseActivity {
    private static SharedPreferences preferences;
    private static final String KEY_ACCEPT_AGREEMENT = "KEY_ACCEPT_AGREEMENT";
    private static final String KEY_ACCEPT_PRIVACY = "KEY_ACCEPT_PRIVACY";

    private static boolean isPermission = false;
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final ArrayList<String> ReqPermissions = new ArrayList<>();

    private CheckBox checkBox;
    private Boolean mAgreement;
    private Boolean mPrivacy;
    private boolean isFirstCreate = true;

    private MediaPlayer mMediaPlayer;
    private GestureDetector mGestureDetector;
    private Handler mDoubleTapHandler;
    private static final long DOUBLE_TAP_TIMEOUT = 300; // 双击超时时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        // 检查是否已经同意条款
        preferences = getSharedPreferences(KEY_ACCEPT_AGREEMENT, MODE_PRIVATE);
        boolean hasAcceptedPrivacy = preferences.getBoolean(KEY_ACCEPT_PRIVACY, false);
        boolean hasAcceptedAgreement = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);

        // 只有当从后台返回（savedInstanceState不为null）且已同意条款时，才跳过启动页
        if (savedInstanceState != null && hasAcceptedPrivacy && hasAcceptedAgreement) {
            // 直接启动MainActivity
            startMainActivityDirectly();
            return;
        }

        // 其他情况（包括完全关闭后重新打开）都显示启动页
        setContentView(R.layout.activity_welcome);

        // 加载 Maxwell 猫 GIF - 使用 WebView (硬件加速，更流畅)
        WebView webViewMaxwell = findViewById(R.id.webview_maxwell);

        // 启用硬件加速和优化设置
        webViewMaxwell.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings webSettings = webViewMaxwell.getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webViewMaxwell.setWebChromeClient(new WebChromeClient());
        webViewMaxwell.setWebViewClient(new WebViewClient());

        // 使用 HTML 加载 GIF，自适应大小
        String gifUrl = "file:///android_res/drawable/maxwell_spinning.gif";
        String html = "<html><body style=\"margin:0;padding:0;background-color:#FBFDF9;display:flex;justify-content:center;align-items:center;height:100vh;\">" +
                "<img src=\"" + gifUrl + "\" style=\"max-width:80%;max-height:80%;width:auto;height:auto;object-fit:contain;\" />" +
                "</body></html>";
        webViewMaxwell.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        // 初始化双击检测
        mDoubleTapHandler = new Handler(Looper.getMainLooper());
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                playStockMarketSound();
                return true;
            }
        });

        // 设置双击监听
        webViewMaxwell.setOnTouchListener((v, event) -> {
            if (mGestureDetector.onTouchEvent(event)) {
                return true;
            }
            return v.onTouchEvent(event);
        });

        Button startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());

        checkAgreementAndPrivacy();
    }

    /**
     * 直接启动MainActivity（用于已同意条款的情况）
     */
    private void startMainActivityDirectly() {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        WelcomeActivity.this.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果不是第一次创建（即从后台返回，Activity未被销毁），且已同意条款，直接跳转
        if (!isFirstCreate) {
            boolean hasAcceptedPrivacy = preferences.getBoolean(KEY_ACCEPT_PRIVACY, false);
            boolean hasAcceptedAgreement = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);
            if (hasAcceptedPrivacy && hasAcceptedAgreement) {
                startMainActivityDirectly();
            }
        }
        isFirstCreate = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放 MediaPlayer
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * 播放股市音效
     */
    private void playStockMarketSound() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        mMediaPlayer = MediaPlayer.create(this, R.raw.stockmarket);
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mMediaPlayer = null;
            });
            mMediaPlayer.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SDK_PERMISSION_REQUEST) {
            boolean hasLocationPermission = false;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        hasLocationPermission = true;
                    }
                }
            }

            // 至少需要一个定位权限
            if (hasLocationPermission) {
                isPermission = true;
            } else {
                GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_permission));
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkDefaultPermissions() {
        // 定位权限是必需的
        boolean hasFineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasFineLocation) {
            ReqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!hasCoarseLocation) {
            ReqPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Android 13+ 需要请求通知权限
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ReqPermissions.add("android.permission.POST_NOTIFICATIONS");
            }
        }

        // Android 12+ 需要请求近似位置权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ 权限处理，至少需要一个定位权限
            if (ReqPermissions.isEmpty() || (!ReqPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) && !ReqPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))) {
                // 如果已经有定位权限，允许继续
                isPermission = true;
            } else {
                requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
            }
        } else {
            // Android 12 以下版本正常处理
            if (ReqPermissions.isEmpty()) {
                isPermission = true;
            } else {
                requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    private void startMainActivity() {
        if (!checkBox.isChecked()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_agreement));
            return;
        }

        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        // GPS 检查已移除，允许不开启 GPS 使用软件
        // if (!GoUtils.isGpsOpened(this)) {
        //     GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_gps));
        //     return;
        // }

        // 保存同意状态（无论权限是否已获取）
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_ACCEPT_AGREEMENT, true);
        editor.putBoolean(KEY_ACCEPT_PRIVACY, true);
        editor.apply();

        // 启动主界面
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        WelcomeActivity.this.finish();
    }

    private void doAcceptation() {
        if (mAgreement && mPrivacy) {
            checkBox.setChecked(true);
            checkDefaultPermissions();
        } else {
            checkBox.setChecked(false);
        }
        //实例化Editor对象
        SharedPreferences.Editor editor = preferences.edit();
        //存入数据
        editor.putBoolean(KEY_ACCEPT_AGREEMENT, mAgreement);
        editor.putBoolean(KEY_ACCEPT_PRIVACY, mPrivacy);
        //提交修改
        editor.apply();
    }

    private void showAgreementDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_agreement);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_agreement_content));
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mAgreement = false;

                doAcceptation();

                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mAgreement = true;

                doAcceptation();

                alertDialog.cancel();
            });
        }
    }

    private void showPrivacyDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_privacy);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_privacy_content));
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mPrivacy = false;

                doAcceptation();

                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mPrivacy = true;

                doAcceptation();

                alertDialog.cancel();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void checkAgreementAndPrivacy() {
        preferences = getSharedPreferences(KEY_ACCEPT_AGREEMENT, MODE_PRIVATE);
        mPrivacy = preferences.getBoolean(KEY_ACCEPT_PRIVACY, false);
        mAgreement = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);

        checkBox = findViewById(R.id.check_agreement);
        // 拦截 CheckBox 的点击事件
        checkBox.setOnTouchListener((v, event) -> {
            if (v instanceof TextView) {
                TextView text = (TextView) v;
                MovementMethod method = text.getMovementMethod();
                if (method != null && text.getText() instanceof Spannable
                        && event.getAction() == MotionEvent.ACTION_UP) {
                    if (method.onTouchEvent(text, (Spannable) text.getText(), event)) {
                        event.setAction(MotionEvent.ACTION_CANCEL);
                    }
                }
            }
            return false;
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!mPrivacy || !mAgreement) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_read));
                    checkBox.setChecked(false);
                }
            } else {
                mPrivacy = false;
                mAgreement = false;
            }
        });

        String str = getString(R.string.app_agreement_privacy);
        SpannableStringBuilder builder = getSpannableStringBuilder(str);

        checkBox.setText(builder);
        checkBox.setMovementMethod(LinkMovementMethod.getInstance());

        if (mPrivacy && mAgreement) {
            checkBox.setChecked(true);
            checkDefaultPermissions();
        } else {
            checkBox.setChecked(false);
        }
    }

    @NonNull
    private SpannableStringBuilder getSpannableStringBuilder(String str) {
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        ClickableSpan clickSpanAgreement = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showAgreementDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, WelcomeActivity.this.getTheme()));
                ds.setUnderlineText(false);
            }
        };
        int agreement_start = str.indexOf("《");
        int agreement_end = str.indexOf("》") + 1;
        builder.setSpan(clickSpanAgreement, agreement_start,agreement_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClickableSpan clickSpanPrivacy = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPrivacyDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, WelcomeActivity.this.getTheme()));
                ds.setUnderlineText(false);
            }
        };
        int privacy_start = str.indexOf("《", agreement_end);
        int privacy_end = str.indexOf("》", agreement_end) + 1;
        builder.setSpan(clickSpanPrivacy, privacy_start, privacy_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
