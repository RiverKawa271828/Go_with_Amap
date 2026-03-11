package com.river.gowithamap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

// 使用简单的XOR加密，兼容所有Android版本

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        
        // 初始化备份数据设置
        setupBackupDataPreference();
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
    
    /**
     * 设置备份数据偏好
     */
    private void setupBackupDataPreference() {
        Preference backupPreference = findPreference("setting_backup_data");
        if (backupPreference != null) {
            backupPreference.setOnPreferenceClickListener(preference -> {
                showBackupDialog();
                return true;
            });
        }
    }

    /**
     * 显示备份对话框（弹窗式）
     */
    private void showBackupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_backup, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // 获取视图
        ListView lvBackupFiles = view.findViewById(R.id.lv_backup_files);
        ListView lvImportFiles = view.findViewById(R.id.lv_import_files);
        View containerExport = view.findViewById(R.id.container_export);
        View containerImport = view.findViewById(R.id.container_import);
        com.google.android.material.button.MaterialButton btnExport = view.findViewById(R.id.btn_export);
        com.google.android.material.button.MaterialButton btnImport = view.findViewById(R.id.btn_import);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        LinearLayout layoutExportButtons = view.findViewById(R.id.layout_export_buttons);
        LinearLayout layoutImportButtons = view.findViewById(R.id.layout_import_buttons);
        com.google.android.material.button.MaterialButton btnDeleteBackup = view.findViewById(R.id.btn_delete_backup);
        com.google.android.material.button.MaterialButton btnExportBackup = view.findViewById(R.id.btn_export_backup);
        com.google.android.material.button.MaterialButton btnImportOverwrite = view.findViewById(R.id.btn_import_overwrite);
        com.google.android.material.button.MaterialButton btnImportIncremental = view.findViewById(R.id.btn_import_incremental);
        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.btn_close);

        // 创建适配器
        BackupFileAdapter exportAdapter = new BackupFileAdapter(requireContext());
        BackupFileAdapter importAdapter = new BackupFileAdapter(requireContext());
        lvBackupFiles.setAdapter(exportAdapter);
        lvImportFiles.setAdapter(importAdapter);

        // 加载备份文件列表
        loadBackupFiles(exportAdapter);
        loadBackupFiles(importAdapter);

        // 导出适配器选择监听
        exportAdapter.setOnSelectionChangeListener(count -> {
            if (count > 0) {
                btnDeleteBackup.setVisibility(View.VISIBLE);
                btnExportBackup.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                tvHint.setText("已选择 " + count + " 个文件");
            } else {
                btnDeleteBackup.setVisibility(View.GONE);
                btnExportBackup.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                tvHint.setText("点击选择备份文件，长按可多选");
            }
        });

        // 导入适配器选择监听
        importAdapter.setOnSelectionChangeListener(count -> {
            if (count > 1) {
                tvHint.setText("不支持同时导入多个备份文件");
                tvHint.setTextColor(getResources().getColor(R.color.md_error));
            } else if (count > 0) {
                tvHint.setText("已选择备份文件，点击覆盖或增量导入");
                tvHint.setTextColor(getResources().getColor(R.color.textTertiary));
            } else {
                tvHint.setText("点击选择要导入的备份文件");
                tvHint.setTextColor(getResources().getColor(R.color.textTertiary));
            }
        });

        // 列表点击事件
        lvBackupFiles.setOnItemClickListener((parent, v, position, id) -> {
            exportAdapter.toggleSelection(position);
        });

        lvBackupFiles.setOnItemLongClickListener((parent, v, position, id) -> {
            if (!exportAdapter.isSelectionMode()) {
                exportAdapter.setSelectionMode(true);
                exportAdapter.toggleSelection(position);
            }
            return true;
        });

        lvImportFiles.setOnItemClickListener((parent, v, position, id) -> {
            // 单选模式 - 清除之前的选择，只选择当前点击的
            importAdapter.clearSelection();
            importAdapter.toggleSelection(position);
        });

        // 导入列表不支持长按

        // 切换按钮点击事件
        btnExport.setOnClickListener(v -> {
            containerExport.setVisibility(View.VISIBLE);
            containerImport.setVisibility(View.GONE);
            layoutExportButtons.setVisibility(View.VISIBLE);
            layoutImportButtons.setVisibility(View.GONE);
            btnExport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.md_primary)));
            btnExport.setTextColor(getResources().getColor(R.color.md_onPrimary));
            btnImport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.R.color.transparent));
            btnImport.setTextColor(getResources().getColor(R.color.md_onSurfaceVariant));
            tvHint.setText("点击选择备份文件，长按可多选");
            exportAdapter.clearSelection();
        });

        btnImport.setOnClickListener(v -> {
            containerExport.setVisibility(View.GONE);
            containerImport.setVisibility(View.VISIBLE);
            layoutExportButtons.setVisibility(View.GONE);
            layoutImportButtons.setVisibility(View.VISIBLE);
            btnImport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.md_primary)));
            btnImport.setTextColor(getResources().getColor(R.color.md_onPrimary));
            btnExport.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.R.color.transparent));
            btnExport.setTextColor(getResources().getColor(R.color.md_onSurfaceVariant));
            tvHint.setText("点击选择要导入的备份文件");
            importAdapter.clearSelection();
        });

        // 删除备份按钮
        btnDeleteBackup.setOnClickListener(v -> {
            java.util.List<BackupFileAdapter.BackupFileInfo> selectedFiles = exportAdapter.getSelectedFiles();
            if (selectedFiles.isEmpty()) return;

            new AlertDialog.Builder(requireContext())
                .setTitle("删除备份")
                .setMessage("确定要删除选中的 " + selectedFiles.size() + " 个备份文件吗？")
                .setPositiveButton("确定", (d, which) -> {
                    for (BackupFileAdapter.BackupFileInfo info : selectedFiles) {
                        info.file.delete();
                    }
                    loadBackupFiles(exportAdapter);
                    exportAdapter.clearSelection();
                    GoUtils.DisplayToast(requireContext(), "已删除 " + selectedFiles.size() + " 个备份");
                })
                .setNegativeButton("取消", null)
                .show();
        });

        // 导出备份按钮
        btnExportBackup.setOnClickListener(v -> {
            showExportWithEncryptionDialog(dialog);
        });

        // 导入-覆盖按钮
        btnImportOverwrite.setOnClickListener(v -> {
            java.util.List<BackupFileAdapter.BackupFileInfo> selectedFiles = importAdapter.getSelectedFiles();
            if (selectedFiles.isEmpty()) {
                GoUtils.DisplayToast(requireContext(), "请先选择一个备份文件");
                return;
            }
            if (selectedFiles.size() > 1) {
                GoUtils.DisplayToast(requireContext(), "不支持同时导入多个备份文件，请只选择一个");
                return;
            }
            processImportWithMode(selectedFiles.get(0).file, true, dialog);
        });

        // 导入-增量按钮
        btnImportIncremental.setOnClickListener(v -> {
            java.util.List<BackupFileAdapter.BackupFileInfo> selectedFiles = importAdapter.getSelectedFiles();
            if (selectedFiles.isEmpty()) {
                GoUtils.DisplayToast(requireContext(), "请先选择一个备份文件");
                return;
            }
            if (selectedFiles.size() > 1) {
                GoUtils.DisplayToast(requireContext(), "不支持同时导入多个备份文件，请只选择一个");
                return;
            }
            processImportWithMode(selectedFiles.get(0).file, false, dialog);
        });

        // 从文件选择按钮 - 使用 SAF 选择任意位置的备份文件
        com.google.android.material.button.MaterialButton btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            startActivityForResult(intent, REQUEST_CODE_IMPORT_BACKUP);
            // 保存 dialog 引用，以便在 onActivityResult 中关闭
            mBackupDialog = dialog;
        });

        // 关闭按钮
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // 保存备份对话框引用，用于文件选择回调
    private AlertDialog mBackupDialog;

    /**
     * 加载备份文件列表
     */
    private void loadBackupFiles(BackupFileAdapter adapter) {
        java.util.List<BackupFileAdapter.BackupFileInfo> files = new java.util.ArrayList<>();

        // 从应用私有目录读取
        java.io.File exportDir = new java.io.File(requireContext().getExternalFilesDir(null), "Backups");
        if (exportDir.exists() && exportDir.isDirectory()) {
            java.io.File[] fileList = exportDir.listFiles((d, name) -> name.endsWith(".zip"));
            if (fileList != null) {
                for (java.io.File file : fileList) {
                    boolean isEncrypted = isBackupEncrypted(file);
                    files.add(new BackupFileAdapter.BackupFileInfo(file, isEncrypted));
                }
            }
        }

        // 同时兼容旧目录（Downloads/GoWithAmap/）
        String savePath = FileSaveManager.getSavePath(requireContext());
        java.io.File oldDir = new java.io.File(savePath);
        if (oldDir.exists() && oldDir.isDirectory()) {
            java.io.File[] fileList = oldDir.listFiles((d, name) -> name.endsWith(".zip"));
            if (fileList != null) {
                for (java.io.File file : fileList) {
                    boolean isEncrypted = isBackupEncrypted(file);
                    files.add(new BackupFileAdapter.BackupFileInfo(file, isEncrypted));
                }
            }
        }

        // 按修改时间排序，最新的在前
        files.sort((f1, f2) -> Long.compare(f2.file.lastModified(), f1.file.lastModified()));

        adapter.setFiles(files);
    }

    /**
     * 检查备份文件是否加密
     */
    private boolean isBackupEncrypted(java.io.File zipFile) {
        try {
            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile));
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("encrypted.flag")) {
                    zis.close();
                    return true;
                }
            }
            zis.close();
        } catch (Exception e) {
            XLog.e("检查加密状态失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 显示导出加密选项对话框
     */
    private void showExportWithEncryptionDialog(AlertDialog parentDialog) {
        String[] options = {"不加密", "加密备份"};

        new AlertDialog.Builder(requireContext())
            .setTitle("导出选项")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // 不加密导出
                    showExportDataSelectionDialog(null, parentDialog);
                } else {
                    // 加密导出 - 输入密码
                    showPasswordInputDialog(password -> {
                        showExportDataSelectionDialog(password, parentDialog);
                    });
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示密码输入对话框
     */
    private void showPasswordInputDialog(PasswordCallback callback) {
        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("请输入密码");

        new AlertDialog.Builder(requireContext())
            .setTitle("设置备份密码")
            .setView(input)
            .setPositiveButton("确定", (dialog, which) -> {
                String password = input.getText().toString().trim();
                if (password.isEmpty()) {
                    GoUtils.DisplayToast(requireContext(), "密码不能为空");
                    return;
                }
                if (password.length() < 6) {
                    GoUtils.DisplayToast(requireContext(), "密码长度至少6位");
                    return;
                }

                // 确认密码
                final EditText confirmInput = new EditText(requireContext());
                confirmInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                confirmInput.setHint("请再次输入密码");

                new AlertDialog.Builder(requireContext())
                    .setTitle("确认密码")
                    .setView(confirmInput)
                    .setPositiveButton("确定", (d, w) -> {
                        String confirmPassword = confirmInput.getText().toString().trim();
                        if (!password.equals(confirmPassword)) {
                            GoUtils.DisplayToast(requireContext(), "两次输入的密码不一致");
                            return;
                        }
                        callback.onPasswordEntered(password);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    interface PasswordCallback {
        void onPasswordEntered(String password);
    }

    /**
     * 显示导出数据选择对话框
     */
    private void showExportDataSelectionDialog(String password, AlertDialog parentDialog) {
        String[] dataTypes = {"模拟记录", "收藏坐标", "收藏区域", "固定坐标", "固定区域", "偏好设置"};
        boolean[] checkedItems = {true, true, true, true, true, true};

        new AlertDialog.Builder(requireContext())
            .setTitle("选择要导出的数据")
            .setMultiChoiceItems(dataTypes, checkedItems, (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;
            })
            .setPositiveButton("导出", (dialog, which) -> {
                performExport(checkedItems, password, parentDialog);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 执行导出
     */
    private void performExport(boolean[] checkedItems, String password, AlertDialog parentDialog) {
        try {
            // 使用应用私有目录导出，避免 Android 10+ 存储权限问题
            java.io.File exportDir = new java.io.File(requireContext().getExternalFilesDir(null), "Backups");
            if (!exportDir.exists()) {
                boolean created = exportDir.mkdirs();
                XLog.i("导出目录创建: " + created);
            }

            // 生成文件名：GWA_年月日_HHMMSS.zip（添加时间戳避免覆盖）
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
            String dateStr = sdf.format(new java.util.Date());
            String fileName = "GWA_" + dateStr + ".zip";
            java.io.File zipFile = new java.io.File(exportDir, fileName);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);

            // 导出数据
            if (checkedItems[0]) addJsonToZip(zos, "history.json", exportHistory());
            if (checkedItems[1]) addJsonToZip(zos, "favorites.json", exportFavorites());
            if (checkedItems[2]) addJsonToZip(zos, "favorite_regions.json", exportFavoriteRegions());
            if (checkedItems[3]) addJsonToZip(zos, "pinned_locations.json", exportPinnedLocations());
            if (checkedItems[4]) addJsonToZip(zos, "pinned_circles.json", exportPinnedCircles());
            if (checkedItems[5]) addJsonToZip(zos, "preferences.json", exportPreferences());

            // 元数据
            JSONObject metaData = new JSONObject();
            metaData.put("version", 2);
            metaData.put("exportTime", System.currentTimeMillis());
            metaData.put("encrypted", password != null);
            addJsonToZip(zos, "meta.json", metaData);

            // 加密标记
            if (password != null) {
                java.util.zip.ZipEntry flagEntry = new java.util.zip.ZipEntry("encrypted.flag");
                zos.putNextEntry(flagEntry);
                zos.write("1".getBytes());
                zos.closeEntry();
            }

            zos.close();

            byte[] zipData = baos.toByteArray();

            // 如果需要加密，对ZIP数据进行加密
            if (password != null) {
                zipData = encryptData(zipData, password);
            }

            // 写入文件
            java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
            fos.write(zipData);
            fos.close();

            XLog.i("备份已保存到: " + zipFile.getAbsolutePath());
            GoUtils.DisplayToast(requireContext(), "备份已保存: " + zipFile.getName());

            // 刷新列表
            if (parentDialog != null) {
                ListView lvBackupFiles = parentDialog.findViewById(R.id.lv_backup_files);
                if (lvBackupFiles != null && lvBackupFiles.getAdapter() instanceof BackupFileAdapter) {
                    loadBackupFiles((BackupFileAdapter) lvBackupFiles.getAdapter());
                }
            }

            // 分享文件
            shareBackupFile(zipFile);

        } catch (Exception e) {
            XLog.e("导出失败: " + e.getMessage(), e);
            GoUtils.DisplayToast(requireContext(), "导出失败: " + e.getMessage());
        }
    }

    /**
     * 加密数据（使用XOR加密）
     */
    private byte[] encryptData(byte[] data, String password) throws Exception {
        byte[] key = password.getBytes("UTF-8");
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        // 添加加密标记前缀
        byte[] marker = "ENCRYPTED:".getBytes("UTF-8");
        byte[] finalResult = new byte[marker.length + result.length];
        System.arraycopy(marker, 0, finalResult, 0, marker.length);
        System.arraycopy(result, 0, finalResult, marker.length, result.length);
        return finalResult;
    }

    /**
     * 解密数据（使用XOR解密）
     */
    private byte[] decryptData(byte[] encryptedData, String password) throws Exception {
        byte[] key = password.getBytes("UTF-8");
        // 移除加密标记前缀
        int markerLength = "ENCRYPTED:".getBytes("UTF-8").length;
        byte[] data = new byte[encryptedData.length - markerLength];
        System.arraycopy(encryptedData, markerLength, data, 0, data.length);
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    /**
     * 处理导入（支持加密）
     */
    private void processImportWithMode(java.io.File zipFile, boolean isOverwrite, AlertDialog parentDialog) {
        boolean isEncrypted = isBackupEncrypted(zipFile);

        if (isEncrypted) {
            // 需要输入密码
            final EditText input = new EditText(requireContext());
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            input.setHint("请输入备份密码");

            new AlertDialog.Builder(requireContext())
                .setTitle("输入密码")
                .setMessage("此备份文件已加密，请输入密码")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String password = input.getText().toString().trim();
                    if (password.isEmpty()) {
                        GoUtils.DisplayToast(requireContext(), "密码不能为空");
                        return;
                    }
                    performImport(zipFile, password, isOverwrite, parentDialog);
                })
                .setNegativeButton("取消", null)
                .show();
        } else {
            // 未加密直接导入
            performImport(zipFile, null, isOverwrite, parentDialog);
        }
    }

    /**
     * 执行导入
     */
    private void performImport(java.io.File zipFile, String password, boolean isOverwrite, AlertDialog parentDialog) {
        try {
            // 读取文件
            java.io.FileInputStream fis = new java.io.FileInputStream(zipFile);
            byte[] fileData = new byte[(int) zipFile.length()];
            fis.read(fileData);
            fis.close();

            // 解密
            if (password != null) {
                try {
                    fileData = decryptData(fileData, password);
                } catch (Exception e) {
                    GoUtils.DisplayToast(requireContext(), "密码错误或文件损坏");
                    return;
                }
            }

            // 解压到临时目录
            java.io.File tempDir = new java.io.File(requireContext().getCacheDir(), "import_temp_" + System.currentTimeMillis());
            tempDir.mkdirs();

            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileData));
            java.util.zip.ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("encrypted.flag")) {
                    zis.closeEntry();
                    continue;
                }
                java.io.File entryFile = new java.io.File(tempDir, entry.getName());
                java.io.FileOutputStream entryFos = new java.io.FileOutputStream(entryFile);
                byte[] buffer = new byte[1024];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    entryFos.write(buffer, 0, count);
                }
                entryFos.close();
                zis.closeEntry();
            }
            zis.close();

            // 显示数据选择对话框
            showImportDataSelectionDialog(tempDir, isOverwrite, parentDialog);

        } catch (Exception e) {
            XLog.e("导入失败: " + e.getMessage());
            GoUtils.DisplayToast(requireContext(), "导入失败: " + e.getMessage());
        }
    }

    /**
     * 显示导入数据选择对话框
     */
    private void showImportDataSelectionDialog(java.io.File tempDir, boolean isOverwrite, AlertDialog parentDialog) {
        java.io.File historyFile = new java.io.File(tempDir, "history.json");
        java.io.File favoritesFile = new java.io.File(tempDir, "favorites.json");
        java.io.File favoriteRegionsFile = new java.io.File(tempDir, "favorite_regions.json");
        java.io.File pinnedLocationsFile = new java.io.File(tempDir, "pinned_locations.json");
        java.io.File pinnedCirclesFile = new java.io.File(tempDir, "pinned_circles.json");
        java.io.File preferencesFile = new java.io.File(tempDir, "preferences.json");

        java.util.List<String> availableTypes = new java.util.ArrayList<>();
        java.util.List<Runnable> importActions = new java.util.ArrayList<>();

        if (historyFile.exists()) {
            availableTypes.add("模拟记录");
            importActions.add(() -> {
                try {
                    String content = readFileContent(historyFile);
                    importHistory(new JSONArray(content), isOverwrite);
                } catch (Exception e) {
                    XLog.e("导入历史记录失败: " + e.getMessage());
                }
            });
        }
        if (favoritesFile.exists()) {
            availableTypes.add("收藏坐标");
            importActions.add(() -> {
                try {
                    String content = readFileContent(favoritesFile);
                    importFavorites(new JSONArray(content), isOverwrite);
                } catch (Exception e) {
                    XLog.e("导入收藏失败: " + e.getMessage());
                }
            });
        }
        if (favoriteRegionsFile.exists()) {
            availableTypes.add("收藏区域");
            importActions.add(() -> {
                try {
                    String content = readFileContent(favoriteRegionsFile);
                    importFavoriteRegions(new JSONArray(content), isOverwrite);
                } catch (Exception e) {
                    XLog.e("导入收藏区域失败: " + e.getMessage());
                }
            });
        }
        if (pinnedLocationsFile.exists()) {
            availableTypes.add("固定坐标");
            importActions.add(() -> {
                try {
                    String content = readFileContent(pinnedLocationsFile);
                    importPinnedLocations(new JSONArray(content), isOverwrite);
                } catch (Exception e) {
                    XLog.e("导入固定坐标失败: " + e.getMessage());
                }
            });
        }
        if (pinnedCirclesFile.exists()) {
            availableTypes.add("固定区域");
            importActions.add(() -> {
                try {
                    String content = readFileContent(pinnedCirclesFile);
                    importPinnedCircles(new JSONArray(content), isOverwrite);
                } catch (Exception e) {
                    XLog.e("导入固定区域失败: " + e.getMessage());
                }
            });
        }
        if (preferencesFile.exists()) {
            availableTypes.add("偏好设置");
            importActions.add(() -> {
                try {
                    String content = readFileContent(preferencesFile);
                    importPreferences(new JSONObject(content));
                } catch (Exception e) {
                    XLog.e("导入偏好设置失败: " + e.getMessage());
                }
            });
        }

        if (availableTypes.isEmpty()) {
            GoUtils.DisplayToast(requireContext(), "备份文件中未找到有效数据");
            cleanupTempDir(tempDir);
            return;
        }

        boolean[] checkedItems = new boolean[availableTypes.size()];
        java.util.Arrays.fill(checkedItems, true);

        new AlertDialog.Builder(requireContext())
            .setTitle("选择要导入的数据")
            .setMultiChoiceItems(availableTypes.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;
            })
            .setPositiveButton("导入", (dialog, which) -> {
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        importActions.get(i).run();
                    }
                }
                GoUtils.DisplayToast(requireContext(), "导入完成");
                cleanupTempDir(tempDir);
                if (parentDialog != null) {
                    parentDialog.dismiss();
                }
            })
            .setNegativeButton("取消", (dialog, which) -> cleanupTempDir(tempDir))
            .show();
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(java.io.File file) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }

    /**
     * 清理临时目录
     */
    private void cleanupTempDir(java.io.File tempDir) {
        if (tempDir != null && tempDir.exists()) {
            deleteRecursive(tempDir);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteRecursive(java.io.File file) {
        if (file.isDirectory()) {
            java.io.File[] files = file.listFiles();
            if (files != null) {
                for (java.io.File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    /**
     * 添加JSON到ZIP
     */
    private void addJsonToZip(java.util.zip.ZipOutputStream zos, String fileName, JSONObject json) throws Exception {
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(fileName);
        zos.putNextEntry(entry);
        zos.write(json.toString(2).getBytes("UTF-8"));
        zos.closeEntry();
    }

    private void addJsonToZip(java.util.zip.ZipOutputStream zos, String fileName, JSONArray json) throws Exception {
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(fileName);
        zos.putNextEntry(entry);
        zos.write(json.toString(2).getBytes("UTF-8"));
        zos.closeEntry();
    }

    /**
     * 分享备份文件
     */
    private void shareBackupFile(java.io.File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");

        Uri uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            requireContext().getPackageName() + ".fileProvider",
            file);

        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "分享备份文件"));
    }

    private static final int REQUEST_CODE_IMPORT_BACKUP = 1001;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_IMPORT_BACKUP && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // 将选择的文件复制到临时目录
                try {
                    java.io.File tempFile = copyUriToTempFile(uri);
                    if (tempFile != null) {
                        // 关闭备份对话框
                        if (mBackupDialog != null && mBackupDialog.isShowing()) {
                            mBackupDialog.dismiss();
                        }
                        // 显示导入选项对话框
                        showImportOptionsDialog(tempFile);
                    } else {
                        GoUtils.DisplayToast(requireContext(), "文件读取失败");
                    }
                } catch (Exception e) {
                    XLog.e("处理选择的文件失败: " + e.getMessage());
                    GoUtils.DisplayToast(requireContext(), "文件处理失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 将 URI 复制到临时文件
     */
    private java.io.File copyUriToTempFile(Uri uri) throws Exception {
        java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
        if (is == null) return null;

        java.io.File tempFile = new java.io.File(requireContext().getCacheDir(), "import_" + System.currentTimeMillis() + ".zip");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);

        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }

        is.close();
        fos.close();

        return tempFile;
    }

    /**
     * 显示导入选项对话框
     */
    private void showImportOptionsDialog(java.io.File file) {
        new AlertDialog.Builder(requireContext())
            .setTitle("导入选项")
            .setMessage("选择导入模式：")
            .setPositiveButton("覆盖导入", (d, which) -> {
                processImportWithMode(file, true, null);
            })
            .setNegativeButton("增量导入", (d, which) -> {
                processImportWithMode(file, false, null);
            })
            .setNeutralButton("取消", null)
            .show();
    }

    /**
     * 导出设置偏好
     */
    private JSONObject exportPreferences() throws JSONException {
        JSONObject prefs = new JSONObject();
        SharedPreferences sharedPrefs = getPreferenceScreen().getSharedPreferences();
        
        prefs.put("joystick_type", sharedPrefs.getString("setting_joystick_type", "0"));
        prefs.put("walk_speed", sharedPrefs.getString("setting_walk", "1.0"));
        prefs.put("run_speed", sharedPrefs.getString("setting_run", "3.0"));
        prefs.put("bike_speed", sharedPrefs.getString("setting_bike", "6.0"));
        prefs.put("altitude", sharedPrefs.getString("setting_altitude", "0"));
        prefs.put("random_offset", sharedPrefs.getBoolean("setting_random_offset", false));
        prefs.put("lat_offset", sharedPrefs.getString("setting_lat_max_offset", "0.0001"));
        prefs.put("lon_offset", sharedPrefs.getString("setting_lon_max_offset", "0.0001"));
        prefs.put("log_enabled", sharedPrefs.getBoolean("setting_log_off", false));
        prefs.put("history_expiration", sharedPrefs.getString("setting_history_expiration", "30"));
        prefs.put("map_cache", sharedPrefs.getBoolean("setting_map_cache", true));
        prefs.put("theme_mode", sharedPrefs.getString("setting_theme_mode", "system"));
        prefs.put("use_root_mode", sharedPrefs.getBoolean("use_root_mode", false));
        
        return prefs;
    }
    
    /**
     * 导入设置偏好
     */
    private void importPreferences(JSONObject prefs) throws JSONException {
        SharedPreferences.Editor editor = getPreferenceScreen().getSharedPreferences().edit();
        
        if (prefs.has("joystick_type")) editor.putString("setting_joystick_type", prefs.getString("joystick_type"));
        if (prefs.has("walk_speed")) editor.putString("setting_walk", prefs.getString("walk_speed"));
        if (prefs.has("run_speed")) editor.putString("setting_run", prefs.getString("run_speed"));
        if (prefs.has("bike_speed")) editor.putString("setting_bike", prefs.getString("bike_speed"));
        if (prefs.has("altitude")) editor.putString("setting_altitude", prefs.getString("altitude"));
        if (prefs.has("random_offset")) editor.putBoolean("setting_random_offset", prefs.getBoolean("random_offset"));
        if (prefs.has("lat_offset")) editor.putString("setting_lat_max_offset", prefs.getString("lat_offset"));
        if (prefs.has("lon_offset")) editor.putString("setting_lon_max_offset", prefs.getString("lon_offset"));
        if (prefs.has("log_enabled")) editor.putBoolean("setting_log_off", prefs.getBoolean("log_enabled"));
        if (prefs.has("history_expiration")) editor.putString("setting_history_expiration", prefs.getString("history_expiration"));
        if (prefs.has("map_cache")) editor.putBoolean("setting_map_cache", prefs.getBoolean("map_cache"));
        if (prefs.has("theme_mode")) editor.putString("setting_theme_mode", prefs.getString("theme_mode"));
        if (prefs.has("use_root_mode")) editor.putBoolean("use_root_mode", prefs.getBoolean("use_root_mode"));
        
        editor.apply();
    }
    
    /**
     * 导出模拟记录
     */
    private JSONArray exportHistory() {
        JSONArray array = new JSONArray();
        try {
            com.river.gowithamap.database.DataBaseSimulationRecords dbHelper =
                new com.river.gowithamap.database.DataBaseSimulationRecords(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();

            android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBaseSimulationRecords.TABLE_NAME,
                null, null, null, null, null,
                com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_TIMESTAMP + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject item = new JSONObject();
                    item.put("location", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LOCATION)));
                    item.put("longitude", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LONGITUDE)));
                    item.put("latitude", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LATITUDE)));
                    item.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_TIMESTAMP)));
                    array.put(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导出模拟记录失败: " + e.getMessage());
        }
        return array;
    }

    /**
     * 导入模拟记录
     */
    private void importHistory(JSONArray array, boolean isOverwrite) {
        try {
            com.river.gowithamap.database.DataBaseSimulationRecords dbHelper =
                new com.river.gowithamap.database.DataBaseSimulationRecords(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 获取现有数据用于去重
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            if (!isOverwrite) {
                android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBaseSimulationRecords.TABLE_NAME,
                    null, null, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String loc = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LOCATION));
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LATITUDE));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LONGITUDE));
                        existingKeys.add(loc + "|" + lat + "|" + lon);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            if (isOverwrite) {
                db.delete(com.river.gowithamap.database.DataBaseSimulationRecords.TABLE_NAME, null, null);
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                // 读取坐标（兼容新旧格式）
                String longitude = item.optString("longitude", item.optString("longitudeCustom", ""));
                String latitude = item.optString("latitude", item.optString("latitudeCustom", ""));
                
                if (longitude.isEmpty() || latitude.isEmpty()) {
                    XLog.w("导入模拟记录: 坐标为空，跳过");
                    continue;
                }

                // 增量导入时检查是否已存在
                if (!isOverwrite) {
                    String loc = item.getString("location");
                    String key = loc + "|" + latitude + "|" + longitude;
                    if (existingKeys.contains(key)) {
                        continue; // 跳过已存在的数据
                    }
                }

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LOCATION, item.getString("location"));
                values.put(com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LONGITUDE, longitude);
                values.put(com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_LATITUDE, latitude);
                values.put(com.river.gowithamap.database.DataBaseSimulationRecords.DB_COLUMN_TIMESTAMP, item.optLong("timestamp", System.currentTimeMillis() / 1000));
                db.insert(com.river.gowithamap.database.DataBaseSimulationRecords.TABLE_NAME, null, values);
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导入模拟记录失败: " + e.getMessage());
        }
    }

    /**
     * 导出收藏坐标
     */
    private JSONArray exportFavorites() {
        JSONArray array = new JSONArray();
        try {
            com.river.gowithamap.database.DataBaseFavorites dbHelper = 
                new com.river.gowithamap.database.DataBaseFavorites(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBaseFavorites.TABLE_NAME, 
                null, null, null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject item = new JSONObject();
                    item.put("name", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_NAME)));
                    item.put("lat", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_LATITUDE)));
                    item.put("lon", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_LONGITUDE)));
                    item.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_TIMESTAMP)));
                    array.put(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导出收藏失败: " + e.getMessage());
        }
        return array;
    }
    
    /**
     * 导入收藏坐标
     */
    private void importFavorites(JSONArray array, boolean isOverwrite) {
        try {
            com.river.gowithamap.database.DataBaseFavorites dbHelper =
                new com.river.gowithamap.database.DataBaseFavorites(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 获取现有数据用于去重
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            if (!isOverwrite) {
                android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBaseFavorites.TABLE_NAME,
                    null, null, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_LATITUDE));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_LONGITUDE));
                        existingKeys.add(lat + "|" + lon);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            if (isOverwrite) {
                db.delete(com.river.gowithamap.database.DataBaseFavorites.TABLE_NAME, null, null);
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                // 增量导入时检查是否已存在
                if (!isOverwrite) {
                    String lat = item.getString("lat");
                    String lon = item.getString("lon");
                    String key = lat + "|" + lon;
                    if (existingKeys.contains(key)) {
                        continue;
                    }
                }

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_NAME, item.getString("name"));
                values.put(com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_LATITUDE, item.getString("lat"));
                values.put(com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_LONGITUDE, item.getString("lon"));
                values.put(com.river.gowithamap.database.DataBaseFavorites.DB_COLUMN_TIMESTAMP, item.getLong("timestamp"));
                db.insert(com.river.gowithamap.database.DataBaseFavorites.TABLE_NAME, null, values);
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导入收藏失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出收藏区域
     */
    private JSONArray exportFavoriteRegions() {
        JSONArray array = new JSONArray();
        try {
            com.river.gowithamap.database.DataBaseFavoriteRegions dbHelper = 
                new com.river.gowithamap.database.DataBaseFavoriteRegions(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBaseFavoriteRegions.TABLE_NAME, 
                null, null, null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject item = new JSONObject();
                    item.put("name", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_NAME)));
                    item.put("centerLat", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT)));
                    item.put("centerLon", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON)));
                    item.put("radius", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_RADIUS)));
                    item.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP)));
                    array.put(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导出收藏区域失败: " + e.getMessage());
        }
        return array;
    }
    
    /**
     * 导入收藏区域
     */
    private void importFavoriteRegions(JSONArray array, boolean isOverwrite) {
        try {
            com.river.gowithamap.database.DataBaseFavoriteRegions dbHelper =
                new com.river.gowithamap.database.DataBaseFavoriteRegions(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 获取现有数据用于去重
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            if (!isOverwrite) {
                android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBaseFavoriteRegions.TABLE_NAME,
                    null, null, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON));
                        String radius = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_RADIUS));
                        existingKeys.add(lat + "|" + lon + "|" + radius);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            if (isOverwrite) {
                db.delete(com.river.gowithamap.database.DataBaseFavoriteRegions.TABLE_NAME, null, null);
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                // 增量导入时检查是否已存在
                if (!isOverwrite) {
                    String lat = item.getString("centerLat");
                    String lon = item.getString("centerLon");
                    String radius = item.getString("radius");
                    String key = lat + "|" + lon + "|" + radius;
                    if (existingKeys.contains(key)) {
                        continue;
                    }
                }

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_NAME, item.getString("name"));
                values.put(com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_CENTER_LAT, item.getString("centerLat"));
                values.put(com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_CENTER_LON, item.getString("centerLon"));
                values.put(com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_RADIUS, item.getString("radius"));
                values.put(com.river.gowithamap.database.DataBaseFavoriteRegions.DB_COLUMN_TIMESTAMP, item.getLong("timestamp"));
                db.insert(com.river.gowithamap.database.DataBaseFavoriteRegions.TABLE_NAME, null, values);
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导入收藏区域失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出固定点
     */
    private JSONArray exportPinnedLocations() {
        JSONArray array = new JSONArray();
        try {
            com.river.gowithamap.database.DataBasePinnedLocations dbHelper = 
                new com.river.gowithamap.database.DataBasePinnedLocations(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBasePinnedLocations.TABLE_NAME, 
                null, null, null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject item = new JSONObject();
                    item.put("name", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_NAME)));
                    item.put("lat", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_LATITUDE)));
                    item.put("lon", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_LONGITUDE)));
                    item.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_TIMESTAMP)));
                    array.put(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导出固定点失败: " + e.getMessage());
        }
        return array;
    }
    
    /**
     * 导入固定点
     */
    private void importPinnedLocations(JSONArray array, boolean isOverwrite) {
        try {
            com.river.gowithamap.database.DataBasePinnedLocations dbHelper =
                new com.river.gowithamap.database.DataBasePinnedLocations(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 获取现有数据用于去重
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            if (!isOverwrite) {
                android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBasePinnedLocations.TABLE_NAME,
                    null, null, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_LATITUDE));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_LONGITUDE));
                        existingKeys.add(lat + "|" + lon);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            if (isOverwrite) {
                db.delete(com.river.gowithamap.database.DataBasePinnedLocations.TABLE_NAME, null, null);
            }

            // 导入新数据
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                // 增量导入时检查是否已存在
                if (!isOverwrite) {
                    String lat = item.getString("lat");
                    String lon = item.getString("lon");
                    String key = lat + "|" + lon;
                    if (existingKeys.contains(key)) {
                        continue;
                    }
                }

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_NAME, item.getString("name"));
                values.put(com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_LATITUDE, item.getString("lat"));
                values.put(com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_LONGITUDE, item.getString("lon"));
                values.put(com.river.gowithamap.database.DataBasePinnedLocations.DB_COLUMN_TIMESTAMP, item.getLong("timestamp"));
                db.insert(com.river.gowithamap.database.DataBasePinnedLocations.TABLE_NAME, null, values);
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导入固定点失败: " + e.getMessage());
        }
    }

    /**
     * 导出固定区域
     */
    private JSONArray exportPinnedCircles() {
        JSONArray array = new JSONArray();
        try {
            com.river.gowithamap.database.DataBasePinnedCircles dbHelper = 
                new com.river.gowithamap.database.DataBasePinnedCircles(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            
            android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBasePinnedCircles.TABLE_NAME, 
                null, null, null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject item = new JSONObject();
                    item.put("name", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_NAME)));
                    item.put("centerLat", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_CENTER_LAT)));
                    item.put("centerLon", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_CENTER_LON)));
                    item.put("radius", cursor.getString(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_RADIUS)));
                    item.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow(
                        com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_TIMESTAMP)));
                    array.put(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导出固定区域失败: " + e.getMessage());
        }
        return array;
    }
    
    /**
     * 导入固定区域
     */
    private void importPinnedCircles(JSONArray array, boolean isOverwrite) {
        try {
            com.river.gowithamap.database.DataBasePinnedCircles dbHelper =
                new com.river.gowithamap.database.DataBasePinnedCircles(requireContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 获取现有数据用于去重
            java.util.Set<String> existingKeys = new java.util.HashSet<>();
            if (!isOverwrite) {
                android.database.Cursor cursor = db.query(com.river.gowithamap.database.DataBasePinnedCircles.TABLE_NAME,
                    null, null, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String lat = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_CENTER_LAT));
                        String lon = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_CENTER_LON));
                        String radius = cursor.getString(cursor.getColumnIndexOrThrow(
                            com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_RADIUS));
                        existingKeys.add(lat + "|" + lon + "|" + radius);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            if (isOverwrite) {
                db.delete(com.river.gowithamap.database.DataBasePinnedCircles.TABLE_NAME, null, null);
            }

            // 导入新数据
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                // 增量导入时检查是否已存在
                if (!isOverwrite) {
                    String lat = item.getString("centerLat");
                    String lon = item.getString("centerLon");
                    String radius = item.getString("radius");
                    String key = lat + "|" + lon + "|" + radius;
                    if (existingKeys.contains(key)) {
                        continue;
                    }
                }

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_NAME, item.getString("name"));
                values.put(com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_CENTER_LAT, item.getString("centerLat"));
                values.put(com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_CENTER_LON, item.getString("centerLon"));
                values.put(com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_RADIUS, item.getString("radius"));
                values.put(com.river.gowithamap.database.DataBasePinnedCircles.DB_COLUMN_TIMESTAMP, item.getLong("timestamp"));
                db.insert(com.river.gowithamap.database.DataBasePinnedCircles.TABLE_NAME, null, values);
            }
            db.close();
        } catch (Exception e) {
            XLog.e("导入固定区域失败: " + e.getMessage());
        }
    }
}
