package com.mittimitra;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.mittimitra.database.MittiMitraDatabase;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManageDataActivity extends BaseActivity {

    private MittiMitraDatabase db;
    private ExecutorService databaseExecutor;
    private Handler mainThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_data);

        db = MittiMitraDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        Toolbar toolbar = findViewById(R.id.manage_data_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout btnClearSoil = findViewById(R.id.btn_clear_soil_history);
        LinearLayout btnClearDocs = findViewById(R.id.btn_clear_documents);

        btnClearSoil.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.manage_data_soil_title)
                    .setMessage(R.string.dialog_are_you_sure)
                    .setPositiveButton(R.string.dialog_delete, (dialog, which) -> clearSoilHistory())
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        });

        btnClearDocs.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.manage_data_docs_title)
                    .setMessage(R.string.dialog_are_you_sure)
                    .setPositiveButton(R.string.dialog_delete, (dialog, which) -> clearDocuments())
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
        });
    }

    private void clearSoilHistory() {
        databaseExecutor.execute(() -> {
            db.soilDao().clearAllHistory();
            mainThreadHandler.post(() -> {
                Toast.makeText(this, R.string.toast_history_cleared, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void clearDocuments() {
        databaseExecutor.execute(() -> {
            // 1. Clear the database records
            db.documentDao().clearAllDocuments();

            // 2. Delete the actual files
            File docsDir = new File(getFilesDir(), "documents");
            if (docsDir.exists() && docsDir.isDirectory()) {
                for (File file : docsDir.listFiles()) {
                    file.delete();
                }
            }

            mainThreadHandler.post(() -> {
                Toast.makeText(this, R.string.toast_documents_cleared, Toast.LENGTH_SHORT).show();
            });
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