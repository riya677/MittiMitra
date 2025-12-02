package com.mittimitra;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocumentsActivity extends BaseActivity {

    private static final String TAG = "DocumentsActivity";

    // UI
    private RecyclerView recyclerView;
    private TextView tvEmptyDocs;
    private DocumentAdapter adapter;

    // DB
    private MittiMitraDatabase db;
    private ExecutorService databaseExecutor;
    private Handler mainThreadHandler;
    private List<Document> documentList;

    // File Picker
    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onFilePicked);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.documents_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // --- Initialize DB & Threading ---
        db = MittiMitraDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        // --- Find Views ---
        recyclerView = findViewById(R.id.recycler_view_documents);
        tvEmptyDocs = findViewById(R.id.tv_empty_docs);
        FloatingActionButton btnAddDocument = findViewById(R.id.btn_add_document);

        // --- Set Listeners ---
        btnAddDocument.setOnClickListener(v -> {
            filePickerLauncher.launch(new String[]{"*/*"});
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }

    private void loadDocuments() {
        databaseExecutor.execute(() -> {
            // UPDATED: No user ID needed
            documentList = db.documentDao().getAllDocuments();
            mainThreadHandler.post(this::setupRecyclerView);
        });
    }

    private void setupRecyclerView() {
        if (documentList.isEmpty()) {
            tvEmptyDocs.setText("No documents found.\nTap '+' to add one.");
            tvEmptyDocs.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyDocs.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter = new DocumentAdapter(documentList, new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onViewClick(Document document) {
                viewDocument(document);
            }
            @Override
            public void onDeleteClick(Document document) {
                confirmDelete(document);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void onFilePicked(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseExecutor.execute(() -> {
            String fileName = getFileName(uri);
            String fileType = getMimeType(uri);

            File docsDir = new File(getFilesDir(), "documents");
            if (!docsDir.exists()) {
                docsDir.mkdirs();
            }

            File localFile = new File(docsDir, System.currentTimeMillis() + "_" + fileName);

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(localFile)) {

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                Document newDoc = new Document();
                // UPDATED: No user ID
                newDoc.documentName = fileName;
                newDoc.documentType = fileType;
                newDoc.internalFilePath = localFile.getAbsolutePath();

                db.documentDao().insertDocument(newDoc);

                mainThreadHandler.post(this::loadDocuments);

            } catch (Exception e) {
                Log.e(TAG, "Failed to copy file", e);
                mainThreadHandler.post(() -> Toast.makeText(DocumentsActivity.this, "Failed to save file", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void viewDocument(Document document) {
        File file = new File(document.internalFilePath);
        if (!file.exists()) {
            Toast.makeText(this, "File not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider",
                file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, document.documentType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(Document document) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Document")
                .setMessage("Are you sure you want to delete '" + document.documentName + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDocument(document))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDocument(Document document) {
        databaseExecutor.execute(() -> {
            File file = new File(document.internalFilePath);
            if (file.exists()) {
                file.delete();
            }

            db.documentDao().deleteDocument(document);

            mainThreadHandler.post(this::loadDocuments);
        });
    }

    // --- Helper Methods ---

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getMimeType(Uri uri) {
        String mimeType;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = getContentResolver().getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        if (mimeType == null) {
            mimeType = "*/*";
        }
        return mimeType;
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