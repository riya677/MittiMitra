package com.mittimitra;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This is the main launcher activity.
 * Its only job is to decide which screen to show first.
 * Right now, it just forwards directly to HomeActivity.
 * In the future, you can add logic here (like checking if a user is logged in)
 * before deciding where to navigate.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create an Intent to start HomeActivity
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);

        // Call finish() to remove this activity from the back stack,
        // so pressing "back" from HomeActivity won't come back here.
        finish();
    }
}