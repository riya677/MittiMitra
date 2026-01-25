package com.mittimitra;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.snackbar.Snackbar;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    private RecyclerView recyclerHistory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmpty;
    private List<SoilAnalysis> historyList = new ArrayList<>();
    private RecentAnalysisAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.history_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.history);
        }

        recyclerHistory = findViewById(R.id.recycler_history);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_history);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.brand_green);
            swipeRefreshLayout.setOnRefreshListener(this::loadHistory);
        }

        // Initialize tvEmpty to soil empty view to keep existing logic working
        tvEmpty = findViewById(R.id.tv_empty_soil);

        if (recyclerHistory != null) {
            recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
            setupSwipeToDelete();
            loadHistory();
        }
    }

    private void loadHistory() {
        FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            MittiMitraDatabase db = MittiMitraDatabase.getDatabase(this);

            // 1. Soil Analysis
            List<SoilAnalysis> soilHistory = db.soilDao().getAnalysisForUser(user.getUid());

            // 2. Plant Analysis (All)
            List<com.mittimitra.database.entity.PlantHealth> plantHistory = 
                    db.plantDao().getAllByUserId(user.getUid());

            // 3. Crop Calendar (All)
            List<com.mittimitra.database.entity.CropSchedule> cropHistory = 
                    db.cropDao().getAllByUserId(user.getUid());

            runOnUiThread(() -> {
                // Stop refresh animation
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                // --- Soil Analysis ---
                historyList.clear();
                TextView tvEmptySoil = findViewById(R.id.tv_empty_soil);
                if (soilHistory != null && !soilHistory.isEmpty()) {
                    historyList.addAll(soilHistory);
                    adapter = new RecentAnalysisAdapter(historyList);
                    recyclerHistory.setAdapter(adapter);
                    if (tvEmptySoil != null) tvEmptySoil.setVisibility(View.GONE);
                } else {
                    if (tvEmptySoil != null) tvEmptySoil.setVisibility(View.VISIBLE);
                }

                // --- Plant Doctor ---
                RecyclerView recyclerPlant = findViewById(R.id.recycler_plant_history);
                TextView tvEmptyPlant = findViewById(R.id.tv_empty_plant);
                if (recyclerPlant != null) {
                    recyclerPlant.setLayoutManager(new LinearLayoutManager(this));
                    if (plantHistory != null && !plantHistory.isEmpty()) {
                        com.mittimitra.ui.adapters.PlantHistoryAdapter plantAdapter = 
                                new com.mittimitra.ui.adapters.PlantHistoryAdapter(this, plantHistory);
                        recyclerPlant.setAdapter(plantAdapter);
                        recyclerPlant.setVisibility(View.VISIBLE);
                        if (tvEmptyPlant != null) tvEmptyPlant.setVisibility(View.GONE);
                    } else {
                        recyclerPlant.setVisibility(View.GONE);
                        if (tvEmptyPlant != null) tvEmptyPlant.setVisibility(View.VISIBLE);
                    }
                }

                // --- Crop Calendar ---
                RecyclerView recyclerCrop = findViewById(R.id.recycler_crop_history);
                TextView tvEmptyCrop = findViewById(R.id.tv_empty_crop);
                if (recyclerCrop != null) {
                    recyclerCrop.setLayoutManager(new LinearLayoutManager(this));
                    if (cropHistory != null && !cropHistory.isEmpty()) {
                        com.mittimitra.ui.adapters.CropHistoryAdapter cropAdapter = 
                                new com.mittimitra.ui.adapters.CropHistoryAdapter(cropHistory);
                        recyclerCrop.setAdapter(cropAdapter);
                        recyclerCrop.setVisibility(View.VISIBLE);
                        if (tvEmptyCrop != null) tvEmptyCrop.setVisibility(View.GONE);
                    } else {
                        recyclerCrop.setVisibility(View.GONE);
                        if (tvEmptyCrop != null) tvEmptyCrop.setVisibility(View.VISIBLE);
                    }
                }
            });
        }).start();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.parseColor("#FF5252"));

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Haptic feedback for delete action
                viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                
                int position = viewHolder.getAdapterPosition();
                SoilAnalysis deletedItem = historyList.get(position);
                
                // Remove from list immediately (better UX)
                historyList.remove(position);
                adapter.notifyItemRemoved(position);
                
                if (historyList.isEmpty() && tvEmpty != null) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
                
                // Show Snackbar with Undo option
                Snackbar snackbar = Snackbar.make(recyclerHistory, R.string.record_deleted, Snackbar.LENGTH_LONG);
                snackbar.setAction(R.string.undo, v -> {
                    // Restore the item
                    historyList.add(position, deletedItem);
                    adapter.notifyItemInserted(position);
                    recyclerHistory.scrollToPosition(position);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                });
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            // Actually delete from database only if Undo wasn't clicked
                            new Thread(() -> {
                                MittiMitraDatabase.getDatabase(HistoryActivity.this)
                                    .soilDao()
                                    .deleteAnalysis(deletedItem);
                            }).start();
                        }
                    }
                });
                snackbar.show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, 
                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, 
                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                
                View itemView = viewHolder.itemView;
                
                if (dX < 0) { // Swiping left
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    background.draw(c);
                }
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHistory);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_export_csv) {
            exportToCsv();
            return true;
        } else if (item.getItemId() == R.id.action_compare) {
            startCompareMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToCsv() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, R.string.no_data_to_export, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                java.io.File exportDir = new java.io.File(getExternalFilesDir(null), "exports");
                if (!exportDir.exists()) exportDir.mkdirs();

                String fileName = "MittiMitra_History_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date()) + ".csv";
                java.io.File csvFile = new java.io.File(exportDir, fileName);

                java.io.FileWriter writer = new java.io.FileWriter(csvFile);
                writer.write("Date,Location,Nitrogen,Phosphorus,Potassium,pH,Notes\n");

                for (SoilAnalysis analysis : historyList) {
                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date(analysis.timestamp));
                    // Parse JSON for soil data
                    String location = "N/A";
                    String n = "N/A", p = "N/A", k = "N/A", ph = "N/A";
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(analysis.soilReportJson);
                        if (json.has("location")) location = json.getString("location");
                        if (json.has("nitrogen")) n = json.getString("nitrogen");
                        if (json.has("phosphorus")) p = json.getString("phosphorus");
                        if (json.has("potassium")) k = json.getString("potassium");
                        if (json.has("ph")) ph = json.getString("ph");
                    } catch (Exception ignored) {}
                    
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"\"\n", date, location, n, p, k, ph));
                }
                writer.close();

                runOnUiThread(() -> {
                    // Share the file
                    android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", csvFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/csv");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.export_csv)));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startCompareMode() {
        if (historyList.size() < 2) {
            Toast.makeText(this, R.string.need_two_records, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CompareActivity.class);
        startActivity(intent);
    }
}