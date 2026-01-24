package com.mittimitra;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.mittimitra.databinding.ActivitySchemeBinding;
import com.mittimitra.models.Scheme;
import com.mittimitra.utils.SchemeRepository;

import java.util.ArrayList;
import java.util.List;

public class SchemeActivity extends AppCompatActivity {

    private ActivitySchemeBinding binding;
    private SchemeRepository repository;
    private SchemeAdapter adapter;
    private List<Scheme> allSchemes = new ArrayList<>();

    // Filter States
    private double selectedLandSize = 0.0; // 0 = All/Ignore
    private String selectedState = "All India";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySchemeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Government Schemes");
        }

        repository = new SchemeRepository(this);
        setupRecyclerView();
        setupFilters();
        loadSchemes();
    }

    private void setupRecyclerView() {
        adapter = new SchemeAdapter(new ArrayList<>(), scheme -> {
            // Open URL on click
            if (scheme.getUrl() != null && !scheme.getUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme.getUrl()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "No link available", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvSchemes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSchemes.setAdapter(adapter);
    }

    private void setupFilters() {
        // State Spinner - Added more states
        String[] states = {
            "All India", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Karnataka", "Kerala", 
            "Madhya Pradesh", "Maharashtra", "Meghalaya", "Odisha", "Punjab", "Rajasthan", 
            "Sikkim", "Tamil Nadu", "Telangana", "Uttar Pradesh", "West Bengal"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, states);
        binding.spinnerState.setAdapter(adapter);
        
        binding.spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedState = states[position];
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Simulating simple filter log (In a real app, logic would be more complex)
        binding.chipGroupLand.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedLandSize = 0.0;
            } else {
                int id = checkedIds.get(0);
                if (id == binding.chipSmall.getId()) selectedLandSize = 1.0; // < 2ha
                else if (id == binding.chipMedium.getId()) selectedLandSize = 3.0; // 2-4ha
                else if (id == binding.chipLarge.getId()) selectedLandSize = 10.0; // > 4ha
            }
            applyFilters();
        });
    }

    private void loadSchemes() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.getSchemes(schemes -> {
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                if (schemes != null) {
                    allSchemes.clear();
                    allSchemes.addAll(schemes);
                    applyFilters();
                } else {
                    Toast.makeText(this, "Failed to load schemes", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void applyFilters() {
        List<Scheme> filtered = new ArrayList<>();
        for (Scheme s : allSchemes) {
            // 1. Filter by State
            // Show if Scheme is "Central" OR matches selected state OR User selected "All India"
            boolean stateMatch = s.getState() == null || 
                                 s.getState().equalsIgnoreCase("Central") || 
                                 s.getState().equalsIgnoreCase(selectedState) || 
                                 selectedState.equals("All India");
            
            if (!stateMatch) continue;

            // 2. Filter by Land Size (if scheme has a max limit)
            // If user selects "Small (1ha)" (val=1.0) and scheme max is 2ha -> MATCH
            // If user selects "Large (10ha)" (val=10.0) and scheme max is 2ha -> NO MATCH
            if (s.getMaxLandHectares() > 0 && selectedLandSize > s.getMaxLandHectares() && selectedLandSize > 0) {
                continue; 
            }
            filtered.add(s);
        }
        
        adapter.updateList(filtered);
        
        if (filtered.isEmpty()) {
            binding.tvNoData.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoData.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // --- Inner Adapter Class ---
    private static class SchemeAdapter extends RecyclerView.Adapter<SchemeAdapter.ViewHolder> {
        
        private List<Scheme> items;
        private final OnSchemeClickListener listener;

        interface OnSchemeClickListener {
            void onClick(Scheme scheme);
        }

        public SchemeAdapter(List<Scheme> items, OnSchemeClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        public void updateList(List<Scheme> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_scheme, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Scheme item = items.get(position);
            holder.tvName.setText(item.getName());
            holder.tvBenefits.setText(item.getBenefits());
            holder.tvEligibility.setText(item.getEligibilityText());
            
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvBenefits, tvEligibility;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_scheme_name);
                tvBenefits = itemView.findViewById(R.id.tv_scheme_benefits);
                tvEligibility = itemView.findViewById(R.id.tv_scheme_eligibility);
            }
        }
    }
}
