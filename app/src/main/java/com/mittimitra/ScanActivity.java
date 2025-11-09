package com.mittimitra;

import android.Manifest;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ScanActivity extends AppCompatActivity {

    // Views for all 3 states
    private View initialScanGroup;
    private View confirmGroup;
    private View analysisGroup;

    private ImageView imageViewPlaceholder;

    // --- ActivityResultLaunchers ---

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(),
                    (Bitmap result) -> {
                        if (result != null) {
                            imageViewPlaceholder.setImageBitmap(result);
                            // UPDATED: Show confirm/reject buttons instead of analysis
                            showConfirmUI(true);
                        } else {
                            Toast.makeText(this, "No image taken", Toast.LENGTH_SHORT).show();
                        }
                    });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    (Uri result) -> {
                        if (result != null) {
                            imageViewPlaceholder.setImageURI(result);
                            // UPDATED: Show confirm/reject buttons instead of analysis
                            showConfirmUI(true);
                        } else {
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // --- Setup Toolbar and Back Button ---
        Toolbar toolbar = findViewById(R.id.scan_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // --- Find all the views ---
        imageViewPlaceholder = findViewById(R.id.image_view_placeholder);

        // View Groups
        initialScanGroup = findViewById(R.id.initial_scan_group);
        confirmGroup = findViewById(R.id.confirm_group);
        analysisGroup = findViewById(R.id.analysis_group);

        // Initial Scan Buttons
        // Buttons
        MaterialButton btnCamera = findViewById(R.id.btn_camera);
        MaterialButton btnUpload = findViewById(R.id.btn_upload);
        FloatingActionButton btnShutter = findViewById(R.id.btn_shutter);

        // New Confirm Buttons
        MaterialButton btnAnalyze = findViewById(R.id.btn_analyze);
        MaterialButton btnReset = findViewById(R.id.btn_reset);

        // Analysis Button
        MaterialButton btnCancel = findViewById(R.id.btn_cancel);

        // --- Set Click Listeners ---

        // When Camera or Shutter is clicked, ask for permission
        View.OnClickListener cameraClickListener = v -> {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        };
        btnCamera.setOnClickListener(cameraClickListener);
        btnShutter.setOnClickListener(cameraClickListener);

        // When Upload is clicked, launch the gallery
        btnUpload.setOnClickListener(v -> {
            galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // NEW: When Analyze is clicked, show analysis UI
        btnAnalyze.setOnClickListener(v -> {
            showAnalysisUI(true);
        });

        // NEW: When Reset is clicked, go back to initial state
        btnReset.setOnClickListener(v -> {
            resetUI();
        });

        // UPDATED: When Cancel is clicked, go back to initial state
        btnCancel.setOnClickListener(v -> {
            resetUI();
        });

        // Set initial state
        resetUI();
    }

    /**
     * Shows/hides the analysis progress bar.
     */
    private void showAnalysisUI(boolean show) {
        if (show) {
            analysisGroup.setVisibility(View.VISIBLE);
            confirmGroup.setVisibility(View.GONE); // Hide confirm buttons
        } else {
            analysisGroup.setVisibility(View.GONE);
        }
    }

    /**
     * Shows/hides the confirm/reset buttons.
     */
    private void showConfirmUI(boolean show) {
        if (show) {
            confirmGroup.setVisibility(View.VISIBLE);
            initialScanGroup.setVisibility(View.GONE); // Hide initial buttons
        } else {
            confirmGroup.setVisibility(View.GONE);
        }
    }

    /**
     * NEW: Resets the entire UI to its starting state.
     */
    private void resetUI() {
        // Hide all but the initial group
        analysisGroup.setVisibility(View.GONE);
        confirmGroup.setVisibility(View.GONE);
        initialScanGroup.setVisibility(View.VISIBLE);

        // Clear the image
        imageViewPlaceholder.setImageResource(android.R.color.darker_gray);
    }

    // This method handles the click on the toolbar's back arrow
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}