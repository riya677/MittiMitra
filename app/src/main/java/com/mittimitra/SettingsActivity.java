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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. SETUP TOOLBAR (Modern Green Style)
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // Set White Arrow
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_24);
            if (upArrow != null) {
                upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        // 2. SETUP RECYCLER VIEW
        RecyclerView recyclerView = findViewById(R.id.recycler_view_settings);
        // Note: LayoutManager (GridLayout) is already defined in activity_settings.xml

        // 3. PREPARE DATA
        List<SettingItem> items = new ArrayList<>();

        items.add(new SettingItem("language", "Language", android.R.drawable.ic_menu_mapmode));
        items.add(new SettingItem("accessibility", "Accessibility", android.R.drawable.ic_menu_sort_alphabetically));
        items.add(new SettingItem("theme", "App Theme", android.R.drawable.ic_menu_view));
        items.add(new SettingItem("unit_converter", "Unit Converter", android.R.drawable.ic_menu_sort_by_size));
        items.add(new SettingItem("kisan_call", "Kisan Helpline", android.R.drawable.ic_menu_call));
        items.add(new SettingItem("manage_data", "Manage Data", android.R.drawable.ic_menu_delete));
        items.add(new SettingItem("notifications", "Notifications", android.R.drawable.ic_popup_reminder));
        items.add(new SettingItem("share", "Share App", android.R.drawable.ic_menu_share));

        // 4. SETUP ADAPTER & CLICKS
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
                    startActivity(new Intent(this, UnitConverterActivity.class));
                    break;
                case "kisan_call":
                    // Call Govt Helpline: 1800-180-1551
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
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out Mitti Mitra - The AI Soil Doctor for Farmers!");
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                    break;
            }
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back when white arrow is clicked
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}