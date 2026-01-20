package com.mittimitra;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class UnitConverterActivity extends BaseActivity {

    private TextInputEditText etArea;
    private Spinner spFrom, spTo;
    private TextView tvResultValue, tvResultUnit;
    private CardView cardResult;

    // Conversion factors relative to 1 Acre
    // 1 Acre = 1.0
    // 1 Hectare = 2.47105 Acres
    // 1 Bigha (Standard) = 0.4 Acres (approx, varies by state)
    // 1 Guntha = 0.025 Acres
    // 1 Cent = 0.01 Acres
    private final String[] units = {"Acre", "Hectare", "Bigha", "Guntha", "Cent"};
    private final double[] factorsToAcre = {1.0, 2.47105, 0.4, 0.025, 0.01};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_converter);

        Toolbar toolbar = findViewById(R.id.converter_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etArea = findViewById(R.id.et_area_input);
        spFrom = findViewById(R.id.spinner_from);
        spTo = findViewById(R.id.spinner_to);
        tvResultValue = findViewById(R.id.tv_result_value);
        tvResultUnit = findViewById(R.id.tv_result_unit);
        cardResult = findViewById(R.id.card_result);
        MaterialButton btnConvert = findViewById(R.id.btn_convert_action);

        // Setup Spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spFrom.setAdapter(adapter);
        spTo.setAdapter(adapter);
        spTo.setSelection(1); // Default to Hectare

        // Restore saved values
        android.content.SharedPreferences prefs = getSharedPreferences("MittiMitraPrefs", MODE_PRIVATE);
        String savedArea = prefs.getString("last_area", "");
        int savedFrom = prefs.getInt("last_unit_from", 0);
        int savedTo = prefs.getInt("last_unit_to", 1);
        
        etArea.setText(savedArea);
        spFrom.setSelection(savedFrom);
        spTo.setSelection(savedTo);

        btnConvert.setOnClickListener(v -> calculate());
    }

    private void calculate() {
        String input = etArea.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter value", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double val = Double.parseDouble(input);
            int fromIndex = spFrom.getSelectedItemPosition();
            int toIndex = spTo.getSelectedItemPosition();

            // Step 1: Convert 'From' unit to Acres
            double valInAcres = val * factorsToAcre[fromIndex];

            // Step 2: Convert Acres to 'To' unit
            double result = valInAcres / factorsToAcre[toIndex];

            tvResultValue.setText(String.format("%.4f", result));
            tvResultUnit.setText(units[toIndex]);
            cardResult.setVisibility(View.VISIBLE);

            // Save for next time
            android.content.SharedPreferences.Editor editor = getSharedPreferences("MittiMitraPrefs", MODE_PRIVATE).edit();
            editor.putString("last_area", input);
            editor.putInt("last_unit_from", fromIndex);
            editor.putInt("last_unit_to", toIndex);
            editor.apply();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
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