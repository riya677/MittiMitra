package com.mittimitra;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View; // Import generic View
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        // 1. Setup Toolbar (Green Theme)
        Toolbar toolbar = findViewById(R.id.manage_data_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // Fix White Back Arrow
            Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_24);
            if (upArrow != null) {
                upArrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                getSupportActionBar().setHomeAsUpIndicator(upArrow);
            }
        }

        // 2. Bind Views (Use generic 'View' to avoid casting crashes)
        View btnClearSoil = findViewById(R.id.btn_clear_soil_history);
        View btnClearDocs = findViewById(R.id.btn_clear_documents);

        // 3. Set Listeners
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            db.soilDao().clearHistoryForUser(user.getUid());
            mainThreadHandler.post(() -> {
                Toast.makeText(this, R.string.toast_history_cleared, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void clearDocuments() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            java.util.List<com.mittimitra.database.entity.Document> docs = db.documentDao().getDocumentsForUser(user.getUid());
            if (docs != null) {
                for (com.mittimitra.database.entity.Document doc : docs) {
                    if (doc.internalFilePath == null) continue;
                    File file = new File(doc.internalFilePath);
                    if (file.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                }
            }
            db.documentDao().clearDocumentsForUser(user.getUid());
            mainThreadHandler.post(() -> {
                Toast.makeText(this, R.string.toast_documents_cleared, Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdownNow();
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
