package com.mittimitra;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingViewHolder> {

    private final List<SettingItem> items;
    private final OnSettingClickListener listener;

    public interface OnSettingClickListener {
        void onSettingClick(SettingItem item);
    }

    public SettingsAdapter(List<SettingItem> items, OnSettingClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_setting_icon, parent, false);
        return new SettingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
        SettingItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SettingViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;

        public SettingViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.setting_icon);
            title = itemView.findViewById(R.id.setting_title);
        }

        public void bind(final SettingItem item, final OnSettingClickListener listener) {
            title.setText(item.title);
            icon.setImageResource(item.iconResId);
            itemView.setOnClickListener(v -> listener.onSettingClick(item));
        }
    }
}