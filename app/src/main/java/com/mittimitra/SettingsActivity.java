package com.mittimitra;

import android.content.Intent;
import android.net.Uri;
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

        List<SettingItem> items = new ArrayList<>();

        // 1. Language Settings
        items.add(new SettingItem("language", getString(R.string.setting_language), android.R.drawable.ic_menu_mapmode));

        // 2. Accessibility (Font Size & Dyslexic Font)
        items.add(new SettingItem("accessibility", getString(R.string.setting_accessibility), android.R.drawable.ic_menu_sort_alphabetically));

        // 3. App Theme
        items.add(new SettingItem("theme", getString(R.string.setting_theme), android.R.drawable.ic_menu_view));

        // 4. Land Unit Converter (New Feature)
        // Uses 'ic_menu_sort_by_size' as it relates to measurement/size
        items.add(new SettingItem("unit_converter", getString(R.string.setting_unit_converter), android.R.drawable.ic_menu_sort_by_size));

        // 5. Kisan Call Center (New Feature)
        // Uses 'ic_menu_call' for the phone action
        items.add(new SettingItem("kisan_call", getString(R.string.setting_kisan_call), android.R.drawable.ic_menu_call));

        // 6. Manage Data (Clear History)
        items.add(new SettingItem("manage_data", getString(R.string.setting_manage_data), android.R.drawable.ic_menu_delete));

        // 1. Add these items to your list:
        items.add(new SettingItem("notifications", getString(R.string.setting_notifications), android.R.drawable.ic_popup_reminder));
        items.add(new SettingItem("share", getString(R.string.setting_share_app), android.R.drawable.ic_menu_share));

        SettingsAdapter adapter = new SettingsAdapter(items, item -> {
            switch (item.id) {
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
                    // Launches the new Unit Converter Activity
                    startActivity(new Intent(this, UnitConverterActivity.class));
                    break;
                case "kisan_call":
                    // Initiates a call to the Govt. Kisan Helpline (1800-180-1551)
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