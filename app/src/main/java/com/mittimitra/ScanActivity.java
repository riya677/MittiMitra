package com.mittimitra;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You would create a layout file e.g., R.layout.activity_scan
        // setContentView(R.layout.activity_scan);

        // For now, just set a title
        setTitle("Scan Soil");
    }
}