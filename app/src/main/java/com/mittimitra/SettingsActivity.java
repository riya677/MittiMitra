package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view_settings);

        // --- UPDATED: Add new setting items ---
        List<SettingItem> items = new ArrayList<>();
        items.add(new SettingItem("language", getString(R.string.setting_language), android.R.drawable.ic_menu_mapmode));
        items.add(new SettingItem("font_size", getString(R.string.setting_font_size), android.R.drawable.ic_menu_sort_alphabetically));
        items.add(new SettingItem("theme", getString(R.string.setting_theme), android.R.drawable.ic_menu_view));
        items.add(new SettingItem("manage_data", getString(R.string.setting_manage_data), android.R.drawable.ic_menu_delete));

        SettingsAdapter adapter = new SettingsAdapter(items, item -> {
            // --- UPDATED: Handle new clicks ---
            if (item.id.equals("language")) {
                startActivity(new Intent(this, LanguageActivity.class));
            } else if (item.id.equals("font_size")) {
                startActivity(new Intent(this, FontSizeActivity.class));
            } else if (item.id.equals("theme")) {
                startActivity(new Intent(this, ThemeSettingsActivity.class));
            } else if (item.id.equals("manage_data")) {
                startActivity(new Intent(this, ManageDataActivity.class));
            }
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}