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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BackupFileAdapter extends BaseAdapter {

    private Context mContext;
    private List<BackupFileInfo> mFiles;
    private Set<Integer> mSelectedPositions;
    private boolean mSelectionMode;
    private OnSelectionChangeListener mListener;

    public static class BackupFileInfo {
        public File file;
        public boolean isEncrypted;

        public BackupFileInfo(File file, boolean isEncrypted) {
            this.file = file;
            this.isEncrypted = isEncrypted;
        }
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    public BackupFileAdapter(Context context) {
        mContext = context;
        mFiles = new ArrayList<>();
        mSelectedPositions = new HashSet<>();
        mSelectionMode = false;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        mListener = listener;
    }

    public void setFiles(List<BackupFileInfo> files) {
        // 按时间从新到旧排序
        Collections.sort(files, new Comparator<BackupFileInfo>() {
            @Override
            public int compare(BackupFileInfo o1, BackupFileInfo o2) {
                return Long.compare(o2.file.lastModified(), o1.file.lastModified());
            }
        });
        mFiles = files;
        mSelectedPositions.clear();
        notifyDataSetChanged();
    }

    public List<BackupFileInfo> getSelectedFiles() {
        List<BackupFileInfo> selected = new ArrayList<>();
        for (int pos : mSelectedPositions) {
            if (pos >= 0 && pos < mFiles.size()) {
                selected.add(mFiles.get(pos));
            }
        }
        return selected;
    }

    public void toggleSelection(int position) {
        if (mSelectedPositions.contains(position)) {
            mSelectedPositions.remove(position);
        } else {
            mSelectedPositions.add(position);
        }

        // 如果没有选中项，退出选择模式
        if (mSelectedPositions.isEmpty()) {
            mSelectionMode = false;
        } else {
            mSelectionMode = true;
        }

        notifyDataSetChanged();

        if (mListener != null) {
            mListener.onSelectionChanged(mSelectedPositions.size());
        }
    }

    public void clearSelection() {
        mSelectedPositions.clear();
        mSelectionMode = false;
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onSelectionChanged(0);
        }
    }

    public boolean isSelectionMode() {
        return mSelectionMode;
    }

    public void setSelectionMode(boolean mode) {
        mSelectionMode = mode;
        if (!mode) {
            mSelectedPositions.clear();
        }
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onSelectionChanged(mSelectedPositions.size());
        }
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_backup_file, parent, false);
            holder = new ViewHolder();
            holder.cardView = (MaterialCardView) convertView;
            holder.ivCheck = convertView.findViewById(R.id.iv_check);
            holder.tvFileName = convertView.findViewById(R.id.tv_file_name);
            holder.tvFileTime = convertView.findViewById(R.id.tv_file_time);
            holder.tvFileSize = convertView.findViewById(R.id.tv_file_size);
            holder.ivEncrypted = convertView.findViewById(R.id.iv_encrypted);
            holder.divider = convertView.findViewById(R.id.divider);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BackupFileInfo info = mFiles.get(position);

        // 文件名
        holder.tvFileName.setText(info.file.getName());

        // 时间 - 年月日时分秒
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateStr = sdf.format(new Date(info.file.lastModified()));
        holder.tvFileTime.setText(dateStr);

        // 文件大小
        String sizeStr = formatFileSize(info.file.length());
        holder.tvFileSize.setText(sizeStr);

        // 加密图标
        holder.ivEncrypted.setVisibility(info.isEncrypted ? View.VISIBLE : View.GONE);

        // 选中状态
        boolean isSelected = mSelectedPositions.contains(position);
        if (mSelectionMode) {
            holder.ivCheck.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.VISIBLE);
            holder.ivCheck.setImageResource(isSelected ? R.drawable.ic_check_circle_filled : R.drawable.ic_check_circle);

            if (isSelected) {
                holder.cardView.setStrokeColor(mContext.getColor(R.color.md_primary));
                holder.cardView.setStrokeWidth(2);
                holder.cardView.setCardBackgroundColor(mContext.getColor(R.color.md_primaryContainer));
            } else {
                holder.cardView.setStrokeColor(mContext.getColor(R.color.md_outlineVariant));
                holder.cardView.setStrokeWidth(1);
                holder.cardView.setCardBackgroundColor(mContext.getColor(R.color.md_surfaceContainerLowest));
            }
        } else {
            holder.ivCheck.setVisibility(View.GONE);
            holder.divider.setVisibility(View.GONE);
            holder.cardView.setStrokeColor(mContext.getColor(R.color.md_outlineVariant));
            holder.cardView.setStrokeWidth(1);
            holder.cardView.setCardBackgroundColor(mContext.getColor(R.color.md_surfaceContainerLowest));
        }

        return convertView;
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    static class ViewHolder {
        MaterialCardView cardView;
        ImageView ivCheck;
        TextView tvFileName;
        TextView tvFileTime;
        TextView tvFileSize;
        ImageView ivEncrypted;
        View divider;
    }
}
