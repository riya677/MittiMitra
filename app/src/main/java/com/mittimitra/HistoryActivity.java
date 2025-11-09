package com.mittimitra;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You would create a layout file e.g., R.layout.activity_history
        // setContentView(R.layout.activity_history);

        // For now, just set a title
        setTitle("Scan History");
    }
}