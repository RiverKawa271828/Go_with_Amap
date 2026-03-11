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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FavoriteSelectAdapter extends BaseAdapter {

    private Context mContext;
    private List<Map<String, Object>> mData;
    private Set<Integer> mSelectedPositions;
    private OnSelectionChangeListener mListener;
    private OnItemClickListener mItemClickListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onCheckClick(int position);
    }

    public FavoriteSelectAdapter(Context context, List<Map<String, Object>> data) {
        mContext = context;
        mData = data;
        mSelectedPositions = new HashSet<>();
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        mListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_favorite_selectable, parent, false);
            holder = new ViewHolder();
            holder.cardView = (MaterialCardView) convertView;
            holder.itemContent = convertView.findViewById(R.id.item_content);
            holder.itemSelectArea = convertView.findViewById(R.id.item_select_area);
            holder.tvName = convertView.findViewById(R.id.tv_location_name);
            holder.tvCoords = convertView.findViewById(R.id.tv_coordinates);
            holder.ivCheck = convertView.findViewById(R.id.iv_check);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Map<String, Object> item = mData.get(position);
        String name = (String) item.get("name");
        String coords = (String) item.get("coords");

        holder.tvName.setText(name);
        holder.tvCoords.setText(coords);

        // 更新选中状态
        boolean isSelected = mSelectedPositions.contains(position);
        if (isSelected) {
            // 选中状态：圆圈亮起，卡片高亮
            holder.ivCheck.setImageResource(R.drawable.ic_check_circle_filled);
            if (holder.cardView != null) {
                holder.cardView.setStrokeColor(mContext.getColor(R.color.md_primary));
                holder.cardView.setStrokeWidth(2);
                holder.cardView.setCardBackgroundColor(mContext.getColor(R.color.md_primaryContainer));
            }
        } else {
            // 未选中状态
            holder.ivCheck.setImageResource(R.drawable.ic_check_circle);
            if (holder.cardView != null) {
                holder.cardView.setStrokeColor(mContext.getColor(R.color.md_outlineVariant));
                holder.cardView.setStrokeWidth(1);
                holder.cardView.setCardBackgroundColor(mContext.getColor(R.color.md_surfaceContainerLowest));
            }
        }

        // 设置点击事件
        // 1. 点击左侧内容区域 - 在地图上显示
        holder.itemContent.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(position);
            }
        });

        // 2. 点击右侧选择区域（包括圆圈和分隔条右侧）- 触发选择
        holder.itemSelectArea.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onCheckClick(position);
            } else {
                toggleSelection(position);
            }
        });

        return convertView;
    }

    public void toggleSelection(int position) {
        if (mSelectedPositions.contains(position)) {
            mSelectedPositions.remove(position);
        } else {
            mSelectedPositions.add(position);
        }
        notifyDataSetChanged();
        
        if (mListener != null) {
            mListener.onSelectionChanged(mSelectedPositions.size());
        }
    }

    public boolean isSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    public Set<Integer> getSelectedPositions() {
        return new HashSet<>(mSelectedPositions);
    }

    public List<Map<String, Object>> getSelectedItems() {
        List<Map<String, Object>> selectedItems = new ArrayList<>();
        for (int position : mSelectedPositions) {
            if (position >= 0 && position < mData.size()) {
                selectedItems.add(mData.get(position));
            }
        }
        return selectedItems;
    }

    public void clearSelection() {
        mSelectedPositions.clear();
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onSelectionChanged(0);
        }
    }

    public void selectAll() {
        mSelectedPositions.clear();
        for (int i = 0; i < mData.size(); i++) {
            mSelectedPositions.add(i);
        }
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onSelectionChanged(mSelectedPositions.size());
        }
    }

    public void updateData(List<Map<String, Object>> newData) {
        mData = newData;
        // 清理无效的选择
        Set<Integer> validSelections = new HashSet<>();
        for (int pos : mSelectedPositions) {
            if (pos >= 0 && pos < mData.size()) {
                validSelections.add(pos);
            }
        }
        mSelectedPositions = validSelections;
        notifyDataSetChanged();
        if (mListener != null) {
            mListener.onSelectionChanged(mSelectedPositions.size());
        }
    }

    static class ViewHolder {
        MaterialCardView cardView;
        LinearLayout itemContent;
        LinearLayout itemSelectArea;
        TextView tvName;
        TextView tvCoords;
        ImageView ivCheck;
    }
}
