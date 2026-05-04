package com.mittimitra;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mittimitra.backend.ApiEnvelope;
import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AiModels;
import com.mittimitra.data.repository.FirebasePredictionRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.domain.repository.PredictionRepository;
import com.mittimitra.tasks.TaskSuggestionEngine;
import com.mittimitra.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
public class RecommendationActivity extends BaseActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = "RecommendationActivity";
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private TextView tvName, tvPhone, tvEmail, tvLocation, tvDate;
    private TextView valN, ratN, valP, ratP, valK, ratK, valPh, ratPh;
    private TextView tvAiContext, tvAiAdvice;
    
    // New Gauge Views
    private android.widget.ProgressBar gaugeSoilHealth;
    private TextView tvHealthStatus;
    
    private View reportContainer;
    private ExtendedFloatingActionButton btnDownload;
    private Uri savedPdfUri = null; // Store saved PDF for sharing
    private int analysisId = -1;
    private TextToSpeech tts;
    private FirebaseFirestore db;
    private String passedSoilType = null;
    private final PredictionRepository predictionRepository = new FirebasePredictionRepository();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);
        Toolbar toolbar = findViewById(R.id.toolbar_recommendation);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        db = FirebaseFirestore.getInstance();
        tts = new TextToSpeech(this, this);
        // Get data from intent or database
        analysisId = getIntent().getIntExtra("analysis_id", -1);
        passedSoilType = getIntent().getStringExtra("DETECTED_SOIL_TYPE");
        reportContainer = findViewById(R.id.report_container);
        btnDownload = findViewById(R.id.btn_download_pdf);
        btnDownload.setVisibility(View.GONE);
        tvName = findViewById(R.id.tv_rep_name);
        tvPhone = findViewById(R.id.tv_rep_phone);
        tvEmail = findViewById(R.id.tv_rep_email);
        tvLocation = findViewById(R.id.tv_rep_location);
        tvDate = findViewById(R.id.tv_report_date);
        valN = findViewById(R.id.tv_val_n); ratN = findViewById(R.id.tv_rat_n);
        valP = findViewById(R.id.tv_val_p); ratP = findViewById(R.id.tv_rat_p);
        valK = findViewById(R.id.tv_val_k); ratK = findViewById(R.id.tv_rat_k);
        valPh = findViewById(R.id.tv_val_ph); ratPh = findViewById(R.id.tv_rat_ph);
        tvAiContext = findViewById(R.id.tv_ai_context);
        tvAiAdvice = findViewById(R.id.tv_ai_advice);
        // Init Gauge
        gaugeSoilHealth = findViewById(R.id.gauge_soil_health);
        tvHealthStatus = findViewById(R.id.tv_health_status);
        btnDownload.setOnClickListener(v -> savePdf());
        loadFarmerProfile();
        loadAnalysisData();
    }
    private void loadFarmerProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = UserIdentityResolver.getActiveUserId(this);
        if (uid == null || uid.trim().isEmpty()) return;
        String email = user != null ? user.getEmail() : null;
        tvEmail.setText((email != null && !email.isEmpty()) ? email : getString(R.string.no_email_linked));
        db.collection("farmers").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("firstName");
                        String phone = doc.getString("phone");
                        tvName.setText((name != null && !name.isEmpty()) ? name : "Mitti Mitra Farmer");
                        tvPhone.setText((phone != null && !phone.isEmpty()) ? phone : "Not Registered");
                    } else {
                        tvName.setText("Mitti Mitra User");
                    }
                })
                .addOnFailureListener(e -> tvName.setText("Offline User"));
    }
    private void loadAnalysisData() {
        String uid = UserIdentityResolver.getActiveUserIdOrCreateGuest(this);
        dbExecutor.execute(() -> {
            SoilAnalysis analysis;
            if (analysisId != -1) {
                analysis = MittiMitraDatabase.getDatabase(this).soilDao().getAnalysisById(analysisId);
            } else {
                analysis = MittiMitraDatabase.getDatabase(this).soilDao().getLatestReportForUser(uid);
            }
            if (analysis != null && uid.equals(analysis.userId)) {
                SoilAnalysis finalAnalysis = analysis;
                runOnUiThread(() -> populateForm(finalAnalysis));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.msg_no_report_found), Toast.LENGTH_SHORT).show();
                    tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()));
                    tvAiAdvice.setText(R.string.rec_error_loading);
                });
            }
        });
    }
    private void populateForm(SoilAnalysis analysis) {
        try {
            JSONObject json = new JSONObject(analysis.soilReportJson);
            // UPDATED DATE LOGIC: Use timestamp if valid (>0), else use current time
            long reportTime = analysis.timestamp > 0 ? analysis.timestamp : System.currentTimeMillis();
            tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(reportTime)));
            tvLocation.setText(json.optString("location", "Unknown Location"));
            int n = json.optInt("N");
            int p = json.optInt("P");
            int k = json.optInt("K");
            double ph = json.optDouble("pH", 6.5);
            setRow(valN, ratN, n, 280, 560, "kg/ha");
            setRow(valP, ratP, p, 10, 25, "kg/ha");
            setRow(valK, ratK, k, 108, 280, "kg/ha");
            valPh.setText(String.format("%.1f", ph));
            if(ph < 6.0) { ratPh.setText(R.string.soil_ph_acidic); ratPh.setTextColor(Color.RED); }
            else if(ph > 7.5) { ratPh.setText(R.string.soil_ph_alkaline); ratPh.setTextColor(Color.BLUE); }
            else { ratPh.setText(R.string.soil_ph_neutral); ratPh.setTextColor(Color.parseColor("#2E7D32")); }
            // --- CALCULATE SOIL HEALTH SCORE ---
            // Score Logic: 100 is perfect (pH 6.5-7.5). Deduct 15 points for every 1.0 deviation from 7.0
            float deviation = (float) Math.abs(ph - 7.0);
            int healthScore = Math.max(0, 100 - (int)(deviation * 15));
            gaugeSoilHealth.setProgress(healthScore);
            
            String statusText;
            int statusColor;
            
            if (healthScore >= 80) {
                statusText = getString(R.string.health_excellent);
                statusColor = Color.parseColor("#2E7D32"); // Green
            } else if (healthScore >= 60) {
                statusText = getString(R.string.health_good);
                statusColor = Color.DKGRAY;
            } else if (healthScore >= 40) {
                statusText = getString(R.string.health_attention);
                statusColor = Color.parseColor("#FF9800"); // Orange
            } else {
                statusText = getString(R.string.health_poor);
                statusColor = Color.RED;
            }
            
            tvHealthStatus.setText(statusText);
            tvHealthStatus.setTextColor(statusColor);
            // -----------------------------------
            String userNotes = json.optString("user_notes", "None");
            String weather = json.optString("weather", "N/A");
            String moisture = json.optString("soil_dynamic", "N/A");
            // Prefer the JSON data, but fallback to intent data if needed
            String detectedSoil = json.optString("detected_soil", passedSoilType != null ? passedSoilType : "Not Scanned");
            String contextText = String.format("Input: %s | Weather: %s | Type: %s", userNotes, weather, detectedSoil);
            tvAiContext.setText(contextText);
            fetchProfessionalAdvice(n, p, k, ph, userNotes, weather, moisture, detectedSoil);
        } catch (Exception e) {
            android.util.Log.e("RecommendationActivity", "Error parsing soil report", e);
            tvAiAdvice.setText(R.string.rec_error_loading);
        }
    }
    private void setRow(TextView vVal, TextView vRat, int val, int low, int high, String unit) {
        vVal.setText(val + " " + unit);
        if (val < low) { vRat.setText(getString(R.string.soil_status_low)); vRat.setTextColor(Color.RED); }
        else if (val > high) { vRat.setText(getString(R.string.soil_status_high)); vRat.setTextColor(Color.RED); }
        else { vRat.setText(getString(R.string.soil_status_ok)); vRat.setTextColor(Color.parseColor("#2E7D32")); }
    }
    private void fetchProfessionalAdvice(int n, int p, int k, double ph, String userNotes, String weather, String moisture, String detectedSoil) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            tvAiAdvice.setText(buildFallbackAdvice(n, p, k, ph));
            tvAiContext.setText(getString(R.string.rec_fallback_context_format, weather, detectedSoil));
            btnDownload.setVisibility(View.VISIBLE);
            Toast.makeText(this, getString(R.string.rec_error_network_offline), Toast.LENGTH_LONG).show();
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        AiModels.SoilAdvisoryRequest request = new AiModels.SoilAdvisoryRequest();
        request.nitrogen = n;
        request.phosphorus = p;
        request.potassium = k;
        request.ph = ph;
        request.userNotes = userNotes;
        request.weather = weather;
        request.moisture = moisture;
        request.detectedSoil = detectedSoil;
        request.languageCode = prefs.getLanguage();
        request.location = tvLocation.getText() != null ? tvLocation.getText().toString() : "";
        predictionRepository.fetchSoilAdvisory(request, new BackendCallback<AiModels.SoilAdvisoryData>() {
            @Override
            public void onSuccess(@NonNull ApiEnvelope<AiModels.SoilAdvisoryData> envelope) {
                runOnUiThread(() -> {
                    AiModels.SoilAdvisoryData data = envelope.data;
                    if (data == null || data.advisoryMarkdown == null || data.advisoryMarkdown.trim().isEmpty()) {
                        tvAiAdvice.setText(R.string.rec_error_formatting);
                        return;
                    }
                    String finalAdvice = data.advisoryMarkdown;
                    if (data.confidence != null && data.confidence < 45) {
                        String uncertain = data.uncertaintyMessage != null ? data.uncertaintyMessage : getString(R.string.prediction_low_confidence_hint);
                        finalAdvice = finalAdvice + "\n\n" + uncertain;
                    }
                    if (data.contextSummary != null && !data.contextSummary.trim().isEmpty()) {
                        tvAiContext.setText(data.contextSummary);
                    }
                    Spanned styledText = parseMarkdown(finalAdvice);
                    tvAiAdvice.setText(styledText);
                    btnDownload.setVisibility(View.VISIBLE);
                    String uid = UserIdentityResolver.getActiveUserId(RecommendationActivity.this);
                    if (uid != null && !uid.trim().isEmpty()) {
                        String crop = new AppPreferences(RecommendationActivity.this).getLastCrop();
                        dbExecutor.execute(() -> TaskSuggestionEngine.suggestFromSoilAdvisory(
                                RecommendationActivity.this, uid, crop, n, p, k, ph, data.confidence));
                    }
                });
            }
            @Override
            public void onFailure(@NonNull ApiEnvelope<AiModels.SoilAdvisoryData> envelope, Throwable throwable) {
                if (throwable != null) {
                    Log.e(TAG, "Soil advisory fetch failed. code=" + envelope.code
                            + ", traceId=" + envelope.traceId + ", message=" + envelope.message, throwable);
                } else {
                    Log.e(TAG, "Soil advisory fetch failed. code=" + envelope.code
                            + ", traceId=" + envelope.traceId + ", message=" + envelope.message);
                }
                runOnUiThread(() -> {
                    tvAiAdvice.setText(buildFallbackAdvice(n, p, k, ph));
                    tvAiContext.setText(getString(R.string.rec_fallback_context_format, weather, detectedSoil));
                    btnDownload.setVisibility(View.VISIBLE);
                    Toast.makeText(RecommendationActivity.this, resolveBackendError(envelope), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private String resolveBackendError(ApiEnvelope<?> envelope) {
        String code = envelope != null && envelope.code != null
                ? envelope.code.toUpperCase(Locale.ROOT)
                : "UNKNOWN";
        String traceId = envelope != null ? envelope.traceId : null;
        int baseResId;
        if ("UNAUTHENTICATED".equals(code) || "PERMISSION_DENIED".equals(code)) {
            baseResId = R.string.rec_error_backend_auth;
        } else if ("NOT_FOUND".equals(code)) {
            baseResId = R.string.rec_error_backend_not_deployed;
        } else if ("UNAVAILABLE".equals(code) || "DEADLINE_EXCEEDED".equals(code)) {
            baseResId = R.string.rec_error_backend_unavailable;
        } else if ("RESOURCE_EXHAUSTED".equals(code)) {
            baseResId = R.string.rec_error_backend_rate_limit;
        } else {
            baseResId = R.string.rec_error_connection;
        }
        if (traceId != null && !traceId.trim().isEmpty()) {
            return getString(R.string.rec_error_with_trace_format, getString(baseResId), traceId);
        }
        return getString(baseResId);
    }
    private String buildFallbackAdvice(int n, int p, int k, double ph) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.rec_fallback_title)).append("\n\n");
        if (n < 280) sb.append("- ").append(getString(R.string.rec_fallback_n_low)).append("\n");
        else if (n > 560) sb.append("- ").append(getString(R.string.rec_fallback_n_high)).append("\n");
        if (p < 10) sb.append("- ").append(getString(R.string.rec_fallback_p_low)).append("\n");
        else if (p > 25) sb.append("- ").append(getString(R.string.rec_fallback_p_high)).append("\n");
        if (k < 108) sb.append("- ").append(getString(R.string.rec_fallback_k_low)).append("\n");
        else if (k > 280) sb.append("- ").append(getString(R.string.rec_fallback_k_high)).append("\n");
        if (ph < 6.0) sb.append("- ").append(getString(R.string.rec_fallback_ph_low)).append("\n");
        else if (ph > 7.5) sb.append("- ").append(getString(R.string.rec_fallback_ph_high)).append("\n");
        else sb.append("- ").append(getString(R.string.rec_fallback_ph_ok)).append("\n");
        sb.append("\n").append(getString(R.string.rec_fallback_footer));
        return sb.toString().trim();
    }
    private static Spanned parseMarkdown(String markdown) {
        String html = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("####\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("###\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("##\\s*(.*)", "<br><b>$1</b><br>");
        html = html.replaceAll("(?m)^-\\s+(.*)$", "&#8226; $1<br>");
        html = html.replace("\n", "<br>");
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }
    private void savePdf() {
        PdfDocument document = null;
        try {
            document = new PdfDocument();
            // A4 Size in points (595 x 842)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            
            drawAgriPass(canvas); // Custom drawing
            document.finishPage(page);
            String fileName = "MittiMitra_Report_" + System.currentTimeMillis() + ".pdf";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);
            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    document.writeTo(out);
                }
                savedPdfUri = uri; // Store for sharing
                
                // Offer to share
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(R.string.share_report)
                    .setMessage(R.string.msg_pdf_saved_share)
                    .setPositiveButton(R.string.share_via, (dialog, which) -> shareReport())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }
        } catch (Exception e) {
            android.util.Log.e("RecommendationActivity", "PDF save error", e);
            Toast.makeText(this, getString(R.string.error_saving_pdf), Toast.LENGTH_SHORT).show();
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
    private void shareReport() {
        if (savedPdfUri == null) {
            Toast.makeText(this, getString(R.string.no_report_to_share), Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, savedPdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mitti Mitra Soil Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is my soil health report from Mitti Mitra app.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
    }
    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("en", "IN"));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Read Aloud").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, 2, 1, R.string.share_report).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, 3, 2, R.string.share_as_image).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == 1 && tts != null) {
            tts.speak(tvAiAdvice.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
            return true;
        }
        if (item.getItemId() == 2) {
            if (savedPdfUri != null) {
                shareReport();
            } else {
                Toast.makeText(this, R.string.msg_download_first, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (item.getItemId() == 3) {
            shareAsImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void shareAsImage() {
        try {
            // Draw the soil health card onto a Bitmap (same logic as PDF drawAgriPass)
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(595, 842,
                    android.graphics.Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            drawAgriPass(canvas);
            // Save bitmap to cache dir
            java.io.File exportsDir = new java.io.File(getCacheDir(), "exports");
            if (!exportsDir.exists()) exportsDir.mkdirs();
            java.io.File imageFile = new java.io.File(exportsDir, "soil_card.jpg");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
            }
            bmp.recycle();
            // Get FileProvider URI
            android.net.Uri imageUri = androidx.core.content.FileProvider.getUriForFile(
                    this, getApplicationContext().getPackageName() + ".provider", imageFile);
            // Share via ACTION_SEND
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_image_title));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image_title)));
        } catch (Exception e) {
            android.util.Log.e("RecommendationActivity", "Failed to share as image", e);
            Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show();
        }
    }
    private void drawAgriPass(Canvas canvas) {
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.Paint titlePaint = new android.graphics.Paint();
        
        // Background
        canvas.drawColor(Color.WHITE);
        
        // Header
        titlePaint.setColor(getColor(R.color.brand_green));
        titlePaint.setTextSize(24);
        titlePaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        titlePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("MITTI MITRA - SOIL HEALTH CARD", 595/2, 50, titlePaint);
        
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText(getString(R.string.report_non_affiliation_note), 595 / 2, 70, paint);
        
        // Border
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(getColor(R.color.brand_green));
        canvas.drawRect(20, 20, 575, 822, paint);
        
        // Reset Paint
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setTextAlign(android.graphics.Paint.Align.LEFT);
        paint.setTextSize(14);
        
        int y = 120;
        int x = 40;
        
        // Farmer Details
        paint.setFakeBoldText(true);
        canvas.drawText("FARMER DETAILS", x, y, paint);
        paint.setFakeBoldText(false);
        y += 20;
        canvas.drawText("Name: " + tvName.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Location: " + tvLocation.getText().toString(), x, y, paint);
        y += 20;
        canvas.drawText("Date: " + tvDate.getText().toString(), x, y, paint);
        
        // Divider
        y += 30;
        paint.setColor(Color.LTGRAY);
        canvas.drawLine(40, y, 555, y, paint);
        paint.setColor(Color.BLACK);
        
        // Soil Metrics
        y += 30;
        paint.setFakeBoldText(true);
        canvas.drawText("SOIL ANALYSIS RESULTS", x, y, paint);
        paint.setFakeBoldText(false);
        y += 25;
        
        // Header Row
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(40, y-15, 555, y+10, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText("PARAMETER", x+10, y, paint);
        canvas.drawText("VALUE", x+200, y, paint);
        canvas.drawText("STATUS", x+350, y, paint);
        paint.setColor(Color.BLACK);
        
        y += 30;
        drawRow(canvas, "Nitrogen (N)", valN.getText().toString(), ratN.getText().toString(), ratN.getCurrentTextColor(), x, y);
        y += 25;
        drawRow(canvas, "Phosphorus (P)", valP.getText().toString(), ratP.getText().toString(), ratP.getCurrentTextColor(), x, y);
        y += 25;
        drawRow(canvas, "Potassium (K)", valK.getText().toString(), ratK.getText().toString(), ratK.getCurrentTextColor(), x, y);
        y += 25;
        drawRow(canvas, "pH Level", valPh.getText().toString(), ratPh.getText().toString(), ratPh.getCurrentTextColor(), x, y);
        
        // Advisory
        y += 50;
        paint.setFakeBoldText(true);
        canvas.drawText("SCIENTIFIC ADVISORY", x, y, paint);
        paint.setFakeBoldText(false);
        y += 25;
        
        String advisory = tvAiAdvice.getText().toString();
        // Simple text wrap (very basic)
        android.text.TextPaint textPaint = new android.text.TextPaint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12);
        
        android.text.StaticLayout staticLayout = android.text.StaticLayout.Builder.obtain(
                advisory, 0, advisory.length(), textPaint, 515)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .build();
        
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
        
        // QR Code Placeholder
        y += staticLayout.getHeight() + 50;
        paint.setColor(Color.LTGRAY);
        canvas.drawRect(595/2 - 40, y, 595/2 + 40, y + 80, paint);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("Scan to Verify", 595/2, y + 95, paint);
    }
    
    private void drawRow(Canvas canvas, String param, String val, String status, int statusColor, int x, int y) {
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setTextSize(12);
        paint.setColor(Color.BLACK);
        canvas.drawText(param, x+10, y, paint);
        canvas.drawText(val, x+200, y, paint);
        paint.setColor(statusColor);
        canvas.drawText(status, x+350, y, paint);
    }
    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        dbExecutor.shutdownNow();
        super.onDestroy();
    }
}
