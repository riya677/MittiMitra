package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import java.util.List;

public class HistoryActivity extends BaseActivity { // Extend BaseActivity for themes

    private RecyclerView recyclerHistory;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // --- FIX: Setup Toolbar & Back Button ---
        Toolbar toolbar = findViewById(R.id.history_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Analysis History");
        }

        recyclerHistory = findViewById(R.id.recycler_history);
        tvEmpty = findViewById(R.id.tv_empty_history);

        if (recyclerHistory != null) {
            recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
            loadHistory();
        }
    }

    private void loadHistory() {
        new Thread(() -> {
            List<SoilAnalysis> history = MittiMitraDatabase.getDatabase(this)
                    .soilDao()
                    .getAllSoilAnalysis();

            runOnUiThread(() -> {
                if (history != null && !history.isEmpty()) {
                    RecentAnalysisAdapter adapter = new RecentAnalysisAdapter(history);
                    recyclerHistory.setAdapter(adapter);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                } else {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    // --- FIX: Handle Back Click ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}