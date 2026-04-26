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

import java.io.IOException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
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
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            documentList = db.documentDao().getDocumentsForUser(user.getUid());
            mainThreadHandler.post(this::setupRecyclerView);
        });
    }

    private void setupRecyclerView() {
        if (documentList == null || documentList.isEmpty()) {
            tvEmptyDocs.setText(R.string.msg_no_documents);
            tvEmptyDocs.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyDocs.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        adapter = new DocumentAdapter(documentList != null ? documentList : new java.util.ArrayList<>(),
                new DocumentAdapter.OnDocumentClickListener() {
            @Override
            public void onViewClick(Document document) {
                viewDocument(document);
            }
            @Override
            public void onDeleteClick(Document document) {
                confirmDelete(document);
            }
        });
        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdownNow();
    }

    private void onFilePicked(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, R.string.doc_no_file_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show();
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

                if (in == null) throw new IOException("Cannot open input stream for URI");
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                Document newDoc = new Document();
                newDoc.documentName = fileName;
                newDoc.documentType = fileType;
                newDoc.internalFilePath = localFile.getAbsolutePath();
                newDoc.userId = user.getUid();

                // ASK FOR EXPIRY DATE (Optional)
                mainThreadHandler.post(() -> showExpiryDialog(newDoc));

            } catch (Exception e) {
                Log.e(TAG, "Failed to copy file", e);
                mainThreadHandler.post(() -> Toast.makeText(DocumentsActivity.this, R.string.doc_save_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showExpiryDialog(Document newDoc) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.doc_expiry_dialog_title)
                .setMessage(R.string.doc_expiry_dialog_msg)
                .setPositiveButton(R.string.doc_expiry_yes, (dialog, which) -> {
                    java.util.Calendar c = java.util.Calendar.getInstance();
                    new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                        c.set(year, month, dayOfMonth);
                        newDoc.expiryDate = c.getTimeInMillis();
                        saveDocumentToDb(newDoc);
                        Toast.makeText(this, R.string.doc_expiry_set, Toast.LENGTH_SHORT).show();
                    }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show();
                })
                .setNegativeButton(R.string.doc_expiry_no, (dialog, which) -> {
                    newDoc.expiryDate = null;
                    saveDocumentToDb(newDoc);
                })
                .setCancelable(false)
                .show();
    }

    private void saveDocumentToDb(Document doc) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            doc.userId = user.getUid();
        }

        databaseExecutor.execute(() -> {
            db.documentDao().insertDocument(doc);
            mainThreadHandler.post(this::loadDocuments);
        });
    }

    private void viewDocument(Document document) {
        File file = new File(document.internalFilePath);
        if (!file.exists()) {
            Toast.makeText(this, R.string.doc_file_not_found, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, R.string.doc_no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete(Document document) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.doc_delete_title)
                .setMessage(getString(R.string.doc_delete_message, document.documentName))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteDocument(document))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void deleteDocument(Document document) {
        databaseExecutor.execute(() -> {
            File file = new File(document.internalFilePath);
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Failed to delete file: " + document.internalFilePath);
            }

            db.documentDao().deleteDocument(document);

            mainThreadHandler.post(this::loadDocuments);
        });
    }

    // --- Helper Methods ---

    private String getFileName(Uri uri) {
        String result = null;
        String scheme = uri.getScheme();
        if ("content".equals(scheme)) {
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
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        if (result == null || result.trim().isEmpty()) {
            result = "document_" + System.currentTimeMillis();
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
