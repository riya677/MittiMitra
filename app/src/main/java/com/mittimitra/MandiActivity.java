package com.mittimitra;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;



import java.util.ArrayList;
import java.util.List;

public class MandiActivity extends AppCompatActivity {

    private Spinner spinnerState, spinnerCommodity;
    private MaterialButton btnCheck;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvError;

    // Arrays now loaded from resources
    private String[] states;
    private String[] commodities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mandi);

        Toolbar toolbar = findViewById(R.id.toolbar_mandi);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.mandi_title);
        }

        initViews();
        setupSpinners();
        
        btnCheck.setOnClickListener(v -> fetchPrices());
    }

    private void initViews() {
        spinnerState = findViewById(R.id.spinner_mandi_state);
        spinnerCommodity = findViewById(R.id.spinner_mandi_commodity);
        btnCheck = findViewById(R.id.btn_check_price);
        recyclerView = findViewById(R.id.recycler_mandi);
        progressBar = findViewById(R.id.progress_mandi);
        tvError = findViewById(R.id.tv_mandi_error);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSpinners() {
        states = getResources().getStringArray(R.array.mandi_states);
        commodities = getResources().getStringArray(R.array.mandi_commodities);

        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, states);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerState.setAdapter(stateAdapter);

        ArrayAdapter<String> commAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, commodities);
        commAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCommodity.setAdapter(commAdapter);
        
        // Pre-select defaults (Maharashtra / Onion if available)
        // Find "Maharashtra" index
        for (int i=0; i<states.length; i++) {
             if (states[i].contains("Maharashtra") || states[i].contains("महाराष्ट्र")) {
                 spinnerState.setSelection(i);
                 break;
             }
        }
        // Find "Onion" index
        for (int i=0; i<commodities.length; i++) {
             if (commodities[i].contains("Onion") || commodities[i].contains("Pyaaz")) {
                 spinnerCommodity.setSelection(i);
                 break;
             }
        }
    }

    private void fetchPrices() {
        String state = spinnerState.getSelectedItem().toString();
        String commodity = spinnerCommodity.getSelectedItem().toString();
        
        // Extract English name if translated (e.g., "Maharashtra (महाराष्ट्र)" -> "Maharashtra")
        String stateClean = state.split("\\(")[0].trim();
        String commodityClean = commodity.split("\\(")[0].trim();

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
        btnCheck.setEnabled(false);

        String apiKey = BuildConfig.DATA_GOV_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            // Fallback: Check cache or show error
            handleMissingApiKey(stateClean, commodityClean);
            return;
        }

        // Real API Call to data.gov.in
        com.mittimitra.network.RetrofitClient.getMandiService()
            .getCommodityPrices(apiKey, "json", stateClean, commodityClean, 20)
            .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                @Override
                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, 
                                       retrofit2.Response<com.google.gson.JsonObject> response) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCheck.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            List<MandiPrice> prices = parseApiResponse(response.body());
                            if (!prices.isEmpty()) {
                                // Cache for offline
                                cacheResults(stateClean, commodityClean, prices);
                                recyclerView.setVisibility(View.VISIBLE);
                                recyclerView.setAdapter(new MandiAdapter(prices));
                            } else {
                                tvError.setText(R.string.mandi_no_data);
                                tvError.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // Try cached data
                            List<MandiPrice> cached = loadCachedResults(stateClean, commodityClean);
                            if (!cached.isEmpty()) {
                                recyclerView.setVisibility(View.VISIBLE);
                                recyclerView.setAdapter(new MandiAdapter(cached));
                                Toast.makeText(MandiActivity.this, 
                                    "Showing cached data", Toast.LENGTH_SHORT).show();
                            } else {
                                tvError.setText(getString(R.string.mandi_error_connect, 
                                    "API Error: " + response.code()));
                                tvError.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCheck.setEnabled(true);
                        
                        // Try cached data on network failure
                        List<MandiPrice> cached = loadCachedResults(stateClean, commodityClean);
                        if (!cached.isEmpty()) {
                            recyclerView.setVisibility(View.VISIBLE);
                            recyclerView.setAdapter(new MandiAdapter(cached));
                            Toast.makeText(MandiActivity.this, 
                                "Offline: Showing cached data", Toast.LENGTH_SHORT).show();
                        } else {
                            tvError.setText(getString(R.string.mandi_error_connect, t.getMessage()));
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
    }

    private List<MandiPrice> parseApiResponse(com.google.gson.JsonObject json) {
        List<MandiPrice> prices = new ArrayList<>();
        try {
            com.google.gson.JsonArray records = json.getAsJsonArray("records");
            if (records != null) {
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH);
                for (int i = 0; i < records.size(); i++) {
                    com.google.gson.JsonObject record = records.get(i).getAsJsonObject();
                    String market = record.has("market") ? record.get("market").getAsString() : "Unknown";
                    String min = record.has("min_price") ? record.get("min_price").getAsString() : "N/A";
                    String max = record.has("max_price") ? record.get("max_price").getAsString() : "N/A";
                    String modal = record.has("modal_price") ? record.get("modal_price").getAsString() : "N/A";
                    String date = record.has("arrival_date") ? record.get("arrival_date").getAsString() : 
                                  dateFormat.format(new java.util.Date());
                    prices.add(new MandiPrice(market, min, max, modal, date));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return prices;
    }

    private void cacheResults(String state, String commodity, List<MandiPrice> prices) {
        android.content.SharedPreferences prefs = getSharedPreferences("mandi_cache", MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String key = state + "_" + commodity;
        prefs.edit()
            .putString(key, gson.toJson(prices))
            .putLong(key + "_time", System.currentTimeMillis())
            .apply();
        
        // Also cache for notification display (global cache accessible by NotificationWorker)
        getSharedPreferences("mandi_price_cache", MODE_PRIVATE).edit()
            .putString("last_prices", gson.toJson(prices))
            .putString("last_commodity", commodity)
            .putLong("cache_time", System.currentTimeMillis())
            .apply();
    }

    private List<MandiPrice> loadCachedResults(String state, String commodity) {
        android.content.SharedPreferences prefs = getSharedPreferences("mandi_cache", MODE_PRIVATE);
        String key = state + "_" + commodity;
        String json = prefs.getString(key, null);
        if (json != null) {
            // Check if cache is less than 24 hours old
            long cacheTime = prefs.getLong(key + "_time", 0);
            if (System.currentTimeMillis() - cacheTime < 24 * 60 * 60 * 1000) {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<MandiPrice>>(){}.getType();
                return new com.google.gson.Gson().fromJson(json, listType);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Handles missing API key case.
     * Shows cached data if available, otherwise shows instructions to get a key.
     */
    private void handleMissingApiKey(String state, String commodity) {
        // Check cache first
        List<MandiPrice> cached = loadCachedResults(state, commodity);
        if (!cached.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            btnCheck.setEnabled(true);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(new MandiAdapter(cached));
            Toast.makeText(this, "Showing cached data (API key required for live prices)", Toast.LENGTH_LONG).show();
            return;
        }

        // No cache and no API key - show helpful error
        progressBar.setVisibility(View.GONE);
        btnCheck.setEnabled(true);
        tvError.setText(R.string.mandi_error_no_api_key);
        tvError.setVisibility(View.VISIBLE);
    }

    // eNAM alternative source removed - requires their own API authentication

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Data Class
    public static class MandiPrice {
        String market;
        String min;
        String max;
        String modal;
        String date;

        MandiPrice(String market, String min, String max, String modal, String date) {
            this.market = market;
            this.min = min;
            this.max = max;
            this.modal = modal;
            this.date = date;
        }
    }

    // Adapter
    private class MandiAdapter extends RecyclerView.Adapter<MandiAdapter.ViewHolder> {
        private List<MandiPrice> list;

        MandiAdapter(List<MandiPrice> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mandi_price, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            MandiPrice item = list.get(position);
            holder.tvMarket.setText(item.market);
            holder.tvModal.setText("₹" + item.modal + "/q"); // Quintal
            holder.tvRange.setText("Min: ₹" + item.min + " - Max: ₹" + item.max);
            holder.tvDate.setText(item.date);
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMarket, tvModal, tvRange, tvDate;
            ViewHolder(View itemView) {
                super(itemView);
                tvMarket = itemView.findViewById(R.id.tv_mandi_market);
                tvModal = itemView.findViewById(R.id.tv_mandi_modal);
                tvRange = itemView.findViewById(R.id.tv_mandi_range);
                tvDate = itemView.findViewById(R.id.tv_mandi_date);
            }
        }
    }
}
