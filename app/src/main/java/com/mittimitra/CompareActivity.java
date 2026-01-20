package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CompareActivity extends BaseActivity {

    private Spinner spinnerRecord1, spinnerRecord2;
    private TextView tvNitrogen1, tvNitrogen2, tvNitrogenDiff;
    private TextView tvPhosphorus1, tvPhosphorus2, tvPhosphorusDiff;
    private TextView tvPotassium1, tvPotassium2, tvPotassiumDiff;
    private TextView tvPh1, tvPh2, tvPhDiff;
    private View compareCard;

    private List<SoilAnalysis> records = new ArrayList<>();
    private SoilAnalysis selectedRecord1, selectedRecord2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        Toolbar toolbar = findViewById(R.id.compare_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.compare_title);
        }

        initViews();
        loadRecords();
    }

    private void initViews() {
        spinnerRecord1 = findViewById(R.id.spinner_record1);
        spinnerRecord2 = findViewById(R.id.spinner_record2);
        compareCard = findViewById(R.id.compare_card);

        tvNitrogen1 = findViewById(R.id.tv_nitrogen_1);
        tvNitrogen2 = findViewById(R.id.tv_nitrogen_2);
        tvNitrogenDiff = findViewById(R.id.tv_nitrogen_diff);

        tvPhosphorus1 = findViewById(R.id.tv_phosphorus_1);
        tvPhosphorus2 = findViewById(R.id.tv_phosphorus_2);
        tvPhosphorusDiff = findViewById(R.id.tv_phosphorus_diff);

        tvPotassium1 = findViewById(R.id.tv_potassium_1);
        tvPotassium2 = findViewById(R.id.tv_potassium_2);
        tvPotassiumDiff = findViewById(R.id.tv_potassium_diff);

        tvPh1 = findViewById(R.id.tv_ph_1);
        tvPh2 = findViewById(R.id.tv_ph_2);
        tvPhDiff = findViewById(R.id.tv_ph_diff);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateComparison();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerRecord1.setOnItemSelectedListener(listener);
        spinnerRecord2.setOnItemSelectedListener(listener);
    }

    private void loadRecords() {
        Executors.newSingleThreadExecutor().execute(() -> {
            records = MittiMitraDatabase.getDatabase(this).soilDao().getAllSoilAnalysis();
            runOnUiThread(() -> {
                List<String> labels = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                for (SoilAnalysis record : records) {
                    labels.add(sdf.format(new Date(record.timestamp)));
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                spinnerRecord1.setAdapter(adapter);
                spinnerRecord2.setAdapter(adapter);

                if (records.size() >= 2) {
                    spinnerRecord1.setSelection(0);
                    spinnerRecord2.setSelection(1);
                }
            });
        });
    }

    private void updateComparison() {
        int pos1 = spinnerRecord1.getSelectedItemPosition();
        int pos2 = spinnerRecord2.getSelectedItemPosition();

        if (pos1 < 0 || pos2 < 0 || pos1 >= records.size() || pos2 >= records.size()) {
            compareCard.setVisibility(View.GONE);
            return;
        }

        compareCard.setVisibility(View.VISIBLE);
        selectedRecord1 = records.get(pos1);
        selectedRecord2 = records.get(pos2);

        String[] vals1 = parseValues(selectedRecord1.soilReportJson);
        String[] vals2 = parseValues(selectedRecord2.soilReportJson);

        tvNitrogen1.setText(vals1[0]);
        tvNitrogen2.setText(vals2[0]);
        tvNitrogenDiff.setText(calculateDiff(vals1[0], vals2[0]));

        tvPhosphorus1.setText(vals1[1]);
        tvPhosphorus2.setText(vals2[1]);
        tvPhosphorusDiff.setText(calculateDiff(vals1[1], vals2[1]));

        tvPotassium1.setText(vals1[2]);
        tvPotassium2.setText(vals2[2]);
        tvPotassiumDiff.setText(calculateDiff(vals1[2], vals2[2]));

        tvPh1.setText(vals1[3]);
        tvPh2.setText(vals2[3]);
        tvPhDiff.setText(calculateDiff(vals1[3], vals2[3]));
    }

    private String[] parseValues(String json) {
        String[] values = {"N/A", "N/A", "N/A", "N/A"};
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("nitrogen")) values[0] = obj.getString("nitrogen");
            if (obj.has("phosphorus")) values[1] = obj.getString("phosphorus");
            if (obj.has("potassium")) values[2] = obj.getString("potassium");
            if (obj.has("ph")) values[3] = obj.getString("ph");
        } catch (Exception ignored) {}
        return values;
    }

    private String calculateDiff(String val1, String val2) {
        try {
            double d1 = Double.parseDouble(val1.replaceAll("[^\\d.]", ""));
            double d2 = Double.parseDouble(val2.replaceAll("[^\\d.]", ""));
            double diff = d2 - d1;
            String sign = diff >= 0 ? "+" : "";
            return sign + String.format(Locale.getDefault(), "%.1f", diff);
        } catch (Exception e) {
            return "—";
        }
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
