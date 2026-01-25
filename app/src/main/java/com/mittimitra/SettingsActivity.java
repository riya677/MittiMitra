package com.mittimitra;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. SETUP TOOLBAR
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_24);
            if (upArrow != null) {
                upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        // 2. INIT RECYCLERVIEW
        recyclerView = findViewById(R.id.recycler_view_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 3. REFRESH DATA ON RESUME (Fixes "Names not changing" issue)
        loadSettingsList();
    }

    private void loadSettingsList() {
        List<SettingItem> items = new ArrayList<>();

        // Use getString() here so it fetches the new language string every time
        items.add(new SettingItem("account", getString(R.string.setting_account_security), android.R.drawable.ic_menu_my_calendar));
        items.add(new SettingItem("language", getString(R.string.setting_language), android.R.drawable.ic_menu_mapmode));
        items.add(new SettingItem("accessibility", getString(R.string.setting_accessibility), android.R.drawable.ic_menu_sort_alphabetically));
        items.add(new SettingItem("theme", getString(R.string.setting_theme), android.R.drawable.ic_menu_view));
        items.add(new SettingItem("unit_converter", getString(R.string.setting_unit_converter), android.R.drawable.ic_menu_sort_by_size));
        items.add(new SettingItem("kisan_call", getString(R.string.setting_kisan_call), android.R.drawable.ic_menu_call));
        items.add(new SettingItem("manage_data", getString(R.string.setting_manage_data), android.R.drawable.ic_menu_delete));
        items.add(new SettingItem("notifications", getString(R.string.setting_notifications), android.R.drawable.ic_popup_reminder));
        items.add(new SettingItem("share", getString(R.string.setting_share_app), android.R.drawable.ic_menu_share));

        SettingsAdapter adapter = new SettingsAdapter(items, item -> {
            switch (item.id) {
                case "account":
                    // TODO: Create AccountSecurityActivity for linking credentials
                    android.widget.Toast.makeText(this, getString(R.string.toast_account_linking_soon), android.widget.Toast.LENGTH_SHORT).show();
                    break;
                case "language":
                    startActivity(new Intent(this, LanguageActivity.class));
                    break;
                case "accessibility":
                    startActivity(new Intent(this, AccessibilityActivity.class));
                    break;
                case "theme":
                    startActivity(new Intent(this, ThemeSettingsActivity.class));
                    break;
                case "unit_converter":
                    startActivity(new Intent(this, UnitConverterActivity.class));
                    break;
                case "kisan_call":
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:18001801551"));
                    startActivity(callIntent);
                    break;
                case "manage_data":
                    startActivity(new Intent(this, ManageDataActivity.class));
                    break;
                case "notifications":
                    startActivity(new Intent(this, NotificationSettingsActivity.class));
                    break;
                case "share":
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                    break;
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