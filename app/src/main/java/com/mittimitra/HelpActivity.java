package com.mittimitra;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;

public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar toolbar = findViewById(R.id.help_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        MaterialButton btnContactPhone = findViewById(R.id.btn_contact_phone);
        MaterialButton btnContactEmail = findViewById(R.id.btn_contact_email);

        btnContactPhone.setOnClickListener(v -> {
            // Create an intent to open the phone dialer
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + getString(R.string.help_contact_phone)));
            try {
                startActivity(dialIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open dialer.", Toast.LENGTH_SHORT).show();
            }
        });

        btnContactEmail.setOnClickListener(v -> {
            // Create an intent to open an email app
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:")); // Only email apps should handle this
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.help_contact_email)});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mitti Mitra App Support");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            } catch (Exception e) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
            }
        });
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