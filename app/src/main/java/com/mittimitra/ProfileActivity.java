package com.mittimitra;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You would create a layout file e.g., R.layout.activity_profile
        // setContentView(R.layout.activity_profile);

        // For now, just set a title
        setTitle("Profile");
    }
}