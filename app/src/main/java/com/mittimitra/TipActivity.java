package com.mittimitra;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.LinearLayout; // Needed for LayoutParams
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.ImageView;
import com.mittimitra.config.ApiConfig;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.dao.ChatDao;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.ui.adapter.ChatAdapter;
import com.mittimitra.utils.ErrorHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TipActivity extends BaseActivity {

    private static final String TAG = "TipActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // UI Components
    private RecyclerView recyclerViewChat;
    private EditText etChatMessage;

    // Matched to XML types to prevent Crash
    private FloatingActionButton btnSendChat;
    private ImageView btnRecordAudio;

    private ProgressBar loadingIndicator;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    // Dependencies
    private OkHttpClient httpClient;
    private MittiMitraDatabase db;
    private ChatDao chatDao;
    private ExecutorService databaseExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private String lastSoilReportJson = null;

    // Audio
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;

    // Permissions
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (permissions) -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    getCurrentLocation();
                } else {
                    addInitialBotMessage();
                }
            });

    private SpeechRecognizer speechRecognizer;

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), (isGranted) -> {
                if (isGranted) startListening();
                else Toast.makeText(this, "Mic permission required.", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip);

        Toolbar toolbar = findViewById(R.id.tip_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Find Views
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        etChatMessage = findViewById(R.id.et_chat_message);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // THESE CASTS MUST MATCH THE XML ELEMENTS
        btnSendChat = findViewById(R.id.btn_send_chat);
        btnRecordAudio = findViewById(R.id.btn_record_audio);

        httpClient = new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build();
        db = MittiMitraDatabase.getDatabase(getApplicationContext());
        chatDao = db.chatDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupRecyclerView();
        requestLocation();

        btnSendChat.setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                etChatMessage.setText("");
            }
        });

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechListener);
        
        // Set up audio button click
        btnRecordAudio.setOnClickListener(v -> requestAudioPermission());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }



    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask MittiMitra...");
        
        speechRecognizer.startListening(intent);
    }

    // --- SpeechListener Implementation ---
    private final RecognitionListener speechListener = new RecognitionListener() {
        @Override public void onReadyForSpeech(Bundle params) { 
            btnRecordAudio.setImageTintList(ColorStateList.valueOf(Color.RED)); // Red when listening
        }
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {
            btnRecordAudio.setImageTintList(ColorStateList.valueOf(Color.parseColor("#757575"))); // Reset to grey
        }
        @Override public void onError(int error) {
            String msg = "Error: " + error;
            if (error == SpeechRecognizer.ERROR_NO_MATCH) msg = "Did not understand, please try again.";
            Toast.makeText(TipActivity.this, msg, Toast.LENGTH_SHORT).show();
            btnRecordAudio.setImageTintList(ColorStateList.valueOf(Color.parseColor("#757575"))); // Reset to grey
        }
        @Override public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                etChatMessage.setText(spokenText);
                sendMessage(spokenText); // Auto-send
                etChatMessage.setText("");
            }
        }
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    };

    private void addInitialBotMessage() {
        databaseExecutor.execute(() -> {
            SoilAnalysis lastScan = db.soilDao().getLatestReport();
            if (lastScan != null) lastSoilReportJson = lastScan.soilReportJson;
            String sysMsg = "You are 'Kisan Sahayak', a friendly and expert AI farming assistant for Indian farmers. " +
                    "Speak in simple language. If the user speaks in Hindi/local language, reply in that language. " +
                    "Keep answers concise (max 3-4 sentences) unless asked for details.";
            callGroqChatAPI(sysMsg, true);
        });
    }

    private void sendMessage(String userMessage) {
        addMessage(userMessage, ChatMessage.Type.USER);
        String locContext = (lastKnownLocation != null) ? "Loc: " + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude() : "";
        String soilContext = (lastSoilReportJson != null) ? "Last Soil Data: " + lastSoilReportJson : "";

        String prompt = "Context: " + locContext + "\n" + soilContext + "\nUser: " + userMessage + "\n\nAct as a friendly Agri-Expert. Format with Markdown (bold, lists) where helpful.";
        callGroqChatAPI(prompt, false);
    }

    private void callGroqChatAPI(String prompt, boolean isInitial) {
        setLoading(true);
        String rawKey = BuildConfig.GROQ_API_KEY;
        if(rawKey == null || rawKey.isEmpty()){
            addMessage("API Key Missing", ChatMessage.Type.BOT);
            setLoading(false);
            return;
        }
        
        // Check for network before making call
        if (!isNetworkAvailable()) {
            loadOfflineTips();
            setLoading(false);
            return;
        }

        String cleanKey = rawKey.replace("\"", "").trim();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", ApiConfig.GROQ_MODEL_CHAT);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            jsonBody.put("messages", messages);
        } catch (JSONException e) {
            setLoading(false);
            return;
        }

        Request request = new Request.Builder()
                .url(ApiConfig.GROQ_CHAT_ENDPOINT)
                .header("Authorization", "Bearer " + cleanKey)
                .post(RequestBody.create(jsonBody.toString(), JSON))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    loadOfflineTips(); // FALLBACK TO OFFLINE
                    setLoading(false);
                });
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    String resBody = response.body().string();
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> {
                            loadOfflineTips(); // FALLBACK ON ERROR
                            setLoading(false);
                        });
                        return;
                    }
                    JSONObject json = new JSONObject(resBody);
                    String content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

                    runOnUiThread(() -> {
                        addMessage(content, ChatMessage.Type.BOT);
                        setLoading(false);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> loadOfflineTips());
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void loadOfflineTips() {
        try {
            java.io.InputStream is = getAssets().open("offline_tips.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, "UTF-8");
            JSONObject json = new JSONObject(jsonString);
            JSONObject tips = json.getJSONObject("tips");
            
            // Basic logic: if last soil known, show specific advice, else default
            String advice = tips.getString("default");
            
            if (lastSoilReportJson != null) {
                if (lastSoilReportJson.toLowerCase().contains("clay")) advice = tips.getString("clay");
                else if (lastSoilReportJson.toLowerCase().contains("red")) advice = tips.getString("red");
                else if (lastSoilReportJson.toLowerCase().contains("black")) advice = tips.getString("black");
                else if (lastSoilReportJson.toLowerCase().contains("sandy")) advice = tips.getString("sandy");
                else if (lastSoilReportJson.toLowerCase().contains("loam")) advice = tips.getString("loam");
            }

            addMessage("<b>[OFFLINE MODE]</b><br>" + advice, ChatMessage.Type.BOT);
        } catch (Exception e) {
            addMessage("Offline tips unavailable.", ChatMessage.Type.BOT);
        }
    }

    // --- Helper Methods ---
    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
        
        // Update Title to "Kisan Sahayak"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.kisan_sahayak_title);
        }
    }

    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            lastKnownLocation = location;
            addInitialBotMessage();
        });
    }

    private void toggleRecording() {
        if (isRecording) stopRecording();
        else startRecording();
    }

    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            btnRecordAudio.setImageTintList(ColorStateList.valueOf(Color.RED)); // Red when recording
            isRecording = true;
            Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            ErrorHandler.logError(TAG, "Failed to start recording", e);
            ErrorHandler.showToast(this, "Could not start recording");
        }
    }

    private void stopRecording() {
        try {
            if (mediaRecorder != null) { mediaRecorder.stop(); mediaRecorder.release(); }
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "Error stopping recording", e);
        }
        mediaRecorder = null;
        isRecording = false;
        btnRecordAudio.setImageTintList(ColorStateList.valueOf(Color.parseColor("#757575"))); // Grey when idle
        if (audioFile.exists()) callGroqWhisperAPI(audioFile);
    }

    private void callGroqWhisperAPI(File audioFile) {
        setLoading(true);
        String rawKey = BuildConfig.GROQ_API_KEY.replace("\"", "").trim();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(), RequestBody.create(audioFile, MediaType.get("audio/mp4")))
                .addFormDataPart("model", "whisper-large-v3").build();

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + rawKey)
                .post(requestBody).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> setLoading(false));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String text = json.getString("text");
                    runOnUiThread(() -> {
                        etChatMessage.setText(text);
                        setLoading(false);
                        sendMessage(text);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> setLoading(false));
                }
            }
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);
        loadChatHistory();
    }

    private void loadChatHistory() {
        databaseExecutor.execute(() -> {
            List<com.mittimitra.database.entity.ChatMessage> savedMessages = chatDao.getAllMessages();
            if (savedMessages != null && !savedMessages.isEmpty()) {
                runOnUiThread(() -> {
                    for (com.mittimitra.database.entity.ChatMessage msg : savedMessages) {
                        ChatMessage.Type type = msg.isUser ? ChatMessage.Type.USER : ChatMessage.Type.BOT;
                        messageList.add(new ChatMessage(msg.content, type));
                    }
                    chatAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerViewChat.scrollToPosition(messageList.size() - 1);
                    }
                });
            }
        });
    }

    private void addMessage(String message, ChatMessage.Type type) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(message, type));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerViewChat.scrollToPosition(messageList.size() - 1);
        });
        
        // Persist to database (don't persist LOADING type)
        if (type != ChatMessage.Type.LOADING) {
            databaseExecutor.execute(() -> {
                com.mittimitra.database.entity.ChatMessage dbMessage = 
                    new com.mittimitra.database.entity.ChatMessage(message, type == ChatMessage.Type.USER);
                chatDao.insertMessage(dbMessage);
            });
        }
    }

    private void setLoading(boolean isLoading) {
        runOnUiThread(() -> {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSendChat.setEnabled(!isLoading);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // --- MARKDOWN PARSER ---
    private static Spanned parseMarkdown(String markdown) {
        if (markdown == null) return Html.fromHtml("", Html.FROM_HTML_MODE_COMPACT);

        // Convert common Markdown to HTML tags
        String html = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        html = html.replaceAll("####\\s*(.*)", "<br><b>$1</b><br>"); // H4
        html = html.replaceAll("###\\s*(.*)", "<br><b>$1</b><br>"); // H3
        html = html.replaceAll("##\\s*(.*)", "<br><b>$1</b><br>"); // H2
        html = html.replaceAll("^-\\s+(.*)", "• $1<br>"); // List start
        html = html.replaceAll("\\n-\\s+(.*)", "<br>• $1"); // List item
        html = html.replace("\n", "<br>"); // Newlines

        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }

}