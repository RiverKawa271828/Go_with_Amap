package com.river.gowithamap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.elvishew.xlog.XLog;
import com.river.gowithamap.utils.FileSaveManager;
import com.river.gowithamap.utils.FileUtils;
import com.river.gowithamap.utils.GoUtils;
import com.river.gowithamap.utils.MapCacheManager;
import com.river.gowithamap.utils.RootUtils;
import com.river.gowithamap.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FragmentSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Set a non-empty decimal EditTextPreference
    private void setupDecimalEditTextPreference(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) EditTextPreference::getText);
            preference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                editText.setSelection(editText.length());
            });
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue.toString().trim().isEmpty()) {
                    GoUtils.DisplayToast(this.getContext(), getResources().getString(R.string.app_error_input_null));
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);

        ListPreference pfJoystick = findPreference("setting_joystick_type");
        if (pfJoystick != null) {
            // 使用自定义 SummaryProvider
            pfJoystick.setSummaryProvider((Preference.SummaryProvider<ListPreference>) preference -> Objects.requireNonNull(preference.getEntry()));
            pfJoystick.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().isEmpty());
        }

        EditTextPreference pfWalk = findPreference("setting_walk");
        setupDecimalEditTextPreference(pfWalk);

        EditTextPreference pfRun = findPreference("setting_run");
        setupDecimalEditTextPreference(pfRun);

        EditTextPreference pfBike = findPreference("setting_bike");
        setupDecimalEditTextPreference(pfBike);

        EditTextPreference pfAltitude = findPreference("setting_altitude");
        setupDecimalEditTextPreference(pfAltitude);

        EditTextPreference pfLatOffset = findPreference("setting_lat_max_offset");
        setupDecimalEditTextPreference(pfLatOffset);

        EditTextPreference pfLonOffset = findPreference("setting_lon_max_offset");
        setupDecimalEditTextPreference(pfLonOffset);

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener((preference, newValue) -> {
                if(((SwitchPreferenceCompat) preference).isChecked() != (Boolean) newValue) {
                    XLog.d(preference.getKey() + newValue);

                    if (Boolean.parseBoolean(newValue.toString())) {
                        XLog.d("on");
                    } else {
                        XLog.d("off");
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }

        EditTextPreference pfPosHisValid = findPreference("setting_history_expiration");
        setupDecimalEditTextPreference(pfPosHisValid);

        // 设置版本号
        String verName;
        verName = GoUtils.getVersionName(FragmentSettings.this.getContext());
        Preference pfVersion = findPreference("setting_version");
        if (pfVersion != null) {
            pfVersion.setSummary(verName);
        }

        // 初始化主题模式设置
        setupThemePreference();

        // 初始化地图缓存设置
        setupMapCachePreference();

        // 初始化地图缓存管理
        setupMapCacheManagePreference();

        // 初始化 ROOT 模式设置
        setupRootModePreference();
    }

    /**
     * 设置地图缓存偏好
     */
    private void setupMapCachePreference() {
        SwitchPreferenceCompat mapCachePreference = findPreference("setting_map_cache");
        if (mapCachePreference != null) {
            // 设置当前状态
            boolean isEnabled = MapCacheManager.isMapCacheEnabled(requireContext());
            mapCachePreference.setChecked(isEnabled);

            // 监听开关变化
            mapCachePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                MapCacheManager.setMapCacheEnabled(requireContext(), enabled);
                
                String message = enabled ? "地图缓存已启用" : "地图缓存已禁用，重启应用后生效";
                GoUtils.DisplayToast(requireContext(), message);
                
                return true;
            });
        }
    }

    /**
     * 设置地图缓存管理偏好
     */
    private void setupMapCacheManagePreference() {
        Preference cacheManagePreference = findPreference("setting_map_cache_manage");
        if (cacheManagePreference != null) {
            // 更新缓存大小摘要
            updateCacheSizeSummary(cacheManagePreference);

            // 点击打开缓存管理对话框
            cacheManagePreference.setOnPreferenceClickListener(preference -> {
                showCacheManageDialog();
                return true;
            });
        }
    }

    /**
     * 更新缓存大小摘要
     */
    private void updateCacheSizeSummary(Preference preference) {
        long cacheSize = MapCacheManager.getMapCacheSize(requireContext());
        String sizeStr = MapCacheManager.formatCacheSize(cacheSize);
        preference.setSummary("当前缓存大小: " + sizeStr);
    }

    /**
     * 显示缓存管理对话框
     */
    private void showCacheManageDialog() {
        // 获取缓存统计信息
        String stats = MapCacheManager.getCacheStats(requireContext());
        List<MapCacheManager.CacheAreaInfo> cachedAreas = MapCacheManager.getCachedAreaList(requireContext());

        // 创建对话框内容视图
        LinearLayout contentView = new LinearLayout(requireContext());
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setPadding(48, 24, 48, 24);

        // 缓存统计信息
        TextView statsText = new TextView(requireContext());
        statsText.setText(stats);
        statsText.setTextSize(14);
        statsText.setLineSpacing(0, 1.3f);
        contentView.addView(statsText);

        // 说明文字
        TextView infoText = new TextView(requireContext());
        infoText.setText("\n提示：地图瓦片缓存由系统管理，清除应用缓存后地图可能需要重新加载。\n");
        infoText.setTextSize(12);
        infoText.setAlpha(0.7f);
        contentView.addView(infoText);

        // 已缓存区域列表
        if (!cachedAreas.isEmpty()) {
            TextView areasTitle = new TextView(requireContext());
            areasTitle.setText("\n最近浏览过的区域：");
            areasTitle.setTextSize(14);
            contentView.addView(areasTitle);

            // 只显示前10个，避免列表过长
            int displayCount = Math.min(cachedAreas.size(), 10);
            for (int i = 0; i < displayCount; i++) {
                MapCacheManager.CacheAreaInfo area = cachedAreas.get(i);
                TextView areaText = new TextView(requireContext());
                String accessInfo = "访问" + area.accessCount + "次 • " +
                        MapCacheManager.formatAccessTime(area.lastAccessTime);
                areaText.setText("• " + area.name + "\n   " + accessInfo);
                areaText.setTextSize(13);
                areaText.setPadding(32, 8, 0, 8);
                areaText.setLineSpacing(0, 1.2f);
                contentView.addView(areaText);
            }

            if (cachedAreas.size() > 10) {
                TextView moreText = new TextView(requireContext());
                moreText.setText("...还有 " + (cachedAreas.size() - 10) + " 个区域");
                moreText.setTextSize(12);
                moreText.setAlpha(0.6f);
                moreText.setPadding(32, 8, 0, 0);
                contentView.addView(moreText);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.map_cache_title)
                .setView(contentView)
                .setPositiveButton(R.string.map_cache_clear, (dialog, which) -> {
                    showClearCacheConfirmDialog();
                })
                .setNegativeButton(android.R.string.cancel, null);

        // 如果有缓存区域，添加"选择清除"选项
        if (!cachedAreas.isEmpty()) {
            builder.setNeutralButton("选择清除", (dialog, which) -> {
                showSelectiveClearDialog(cachedAreas);
            });
        }

        builder.show();
    }

    /**
     * 显示选择性清除对话框
     */
    private void showSelectiveClearDialog(List<MapCacheManager.CacheAreaInfo> cachedAreas) {
        String[] areasArray = new String[cachedAreas.size()];
        boolean[] checkedItems = new boolean[cachedAreas.size()];

        for (int i = 0; i < cachedAreas.size(); i++) {
            MapCacheManager.CacheAreaInfo area = cachedAreas.get(i);
            areasArray[i] = area.name + " (" + area.accessCount + "次)";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("选择要清除的区域")
                .setMultiChoiceItems(areasArray, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("清除选中", (dialog, which) -> {
                    int clearedCount = 0;
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            MapCacheManager.clearCachedArea(requireContext(), cachedAreas.get(i).name);
                            clearedCount++;
                        }
                    }
                    if (clearedCount > 0) {
                        GoUtils.DisplayToast(requireContext(), "已清除 " + clearedCount + " 个区域的记录");
                        // 更新摘要
                        Preference preference = findPreference("setting_map_cache_manage");
                        if (preference != null) {
                            updateCacheSizeSummary(preference);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 显示清除缓存确认对话框
     */
    private void showClearCacheConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.map_cache_clear)
                .setMessage(R.string.map_cache_clear_confirm)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    boolean success = MapCacheManager.clearAllMapCache(requireContext());
                    if (success) {
                        GoUtils.DisplayToast(requireContext(), getString(R.string.map_cache_cleared));
                        // 更新摘要
                        Preference preference = findPreference("setting_map_cache_manage");
                        if (preference != null) {
                            updateCacheSizeSummary(preference);
                        }
                    } else {
                        GoUtils.DisplayToast(requireContext(), getString(R.string.map_cache_clear_failed));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 设置主题模式偏好
     */
    private void setupThemePreference() {
        ListPreference themePreference = findPreference("setting_theme_mode");
        if (themePreference != null) {
            // 设置当前值的摘要
            ThemeUtils.ThemeMode currentMode = ThemeUtils.getThemeMode(requireContext());
            switch (currentMode) {
                case SYSTEM_DEFAULT:
                    themePreference.setSummary(getString(R.string.setting_theme_follow_system));
                    break;
                case LIGHT:
                    themePreference.setSummary("浅色模式");
                    break;
                case DARK:
                    themePreference.setSummary("深色模式");
                    break;
            }

            // 监听主题切换
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String value = (String) newValue;
                ThemeUtils.ThemeMode mode;
                switch (value) {
                    case "light":
                        mode = ThemeUtils.ThemeMode.LIGHT;
                        preference.setSummary("浅色模式");
                        break;
                    case "dark":
                        mode = ThemeUtils.ThemeMode.DARK;
                        preference.setSummary("深色模式");
                        break;
                    case "system":
                    default:
                        mode = ThemeUtils.ThemeMode.SYSTEM_DEFAULT;
                        preference.setSummary(getString(R.string.setting_theme_follow_system));
                        break;
                }
                
                // 应用新主题
                ThemeUtils.setThemeMode(requireContext(), mode);
                
                // 立即刷新当前Activity以应用新主题
                requireActivity().recreate();
                
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // 可以在这里处理其他设置变化
    }

    /**
     * 设置 ROOT 模式偏好
     */
    private void setupRootModePreference() {
        // 检查并更新 ROOT 状态显示
        Preference rootStatusPreference = findPreference("setting_root_status");
        if (rootStatusPreference != null) {
            updateRootStatusSummary(rootStatusPreference);
        }

        // 设置 ROOT 模式开关
        SwitchPreferenceCompat rootModePreference = findPreference("use_root_mode");
        if (rootModePreference != null) {
            // 设置开关变化监听
            rootModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;

                // 如果启用 ROOT 模式，检查设备是否已 ROOT
                if (enabled) {
                    if (!RootUtils.isDeviceRooted()) {
                        GoUtils.DisplayToast(requireContext(), "设备未 ROOT，无法启用 ROOT 模式");
                        return false;
                    }

                    if (!RootUtils.hasRootAccess()) {
                        GoUtils.DisplayToast(requireContext(), "无法获取 ROOT 权限，请在 Superuser 应用中授权");
                        return false;
                    }

                    // 显示警告
                    new AlertDialog.Builder(requireContext())
                            .setTitle("警告")
                            .setMessage(R.string.setting_root_warning)
                            .setPositiveButton("确定", (dialog, which) -> {
                                // 提示需要重启
                                GoUtils.DisplayToast(requireContext(), getString(R.string.setting_root_restart_required));
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                rootModePreference.setChecked(false);
                            })
                            .show();

                    return true;
                } else {
                    // 禁用 ROOT 模式，提示需要重启
                    GoUtils.DisplayToast(requireContext(), getString(R.string.setting_root_restart_required));
                    return true;
                }
            });
        }
    }

    /**
     * 更新 ROOT 状态摘要
     */
    private void updateRootStatusSummary(Preference preference) {
        if (RootUtils.isDeviceRooted()) {
            if (RootUtils.hasRootAccess()) {
                preference.setSummary(R.string.setting_root_status_available);
            } else {
                preference.setSummary(R.string.setting_root_status_unavailable);
            }
        } else {
            preference.setSummary(R.string.setting_root_status_unavailable);
        }
    }
}
