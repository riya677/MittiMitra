package com.mittimitra;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout; // Make sure this import is here
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.entity.SoilAnalysis;

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

public class TipActivity extends AppCompatActivity {

    private static final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String TAG = "TipActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // UI
    private RecyclerView recyclerViewChat;
    private EditText etChatMessage;
    private FloatingActionButton btnSendChat, btnRecordAudio;
    private ProgressBar loadingIndicator;
    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    // API & DB
    private OkHttpClient httpClient;
    private MittiMitraDatabase db;
    private ExecutorService databaseExecutor;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private String lastSoilReportJson = null; // Local cache for the soil report

    // Audio Recording
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;

    // --- Permission Launchers ---
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (permissions) -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    Log.i(TAG, "Location permission granted.");
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Location permission is needed for local tips.", Toast.LENGTH_SHORT).show();
                    addInitialBotMessage(); // Load initial message without location
                }
            });

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), (isGranted) -> {
                if (isGranted) {
                    Log.i(TAG, "Audio permission granted.");
                    toggleRecording();
                } else {
                    Toast.makeText(this, "Audio permission is needed for speech-to-text.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip);

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.tip_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // --- Find Views ---
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        etChatMessage = findViewById(R.id.et_chat_message);
        btnSendChat = findViewById(R.id.btn_send_chat);
        btnRecordAudio = findViewById(R.id.btn_record_audio);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // --- Setup Dependencies ---
        httpClient = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();
        db = MittiMitraDatabase.getDatabase(getApplicationContext());
        databaseExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- Setup UI ---
        setupRecyclerView();
        requestLocation(); // This will chain-load the initial bot message

        // --- Set Listeners ---
        btnSendChat.setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                etChatMessage.setText("");
            }
        });

        btnRecordAudio.setOnClickListener(v -> {
            requestAudioPermission();
        });

        // Set up the audio file path
        audioFile = new File(getCacheDir(), "recording.m4a");
    }

    private void addInitialBotMessage() {
        String locationContext = (lastKnownLocation != null) ?
                " at location (" + lastKnownLocation.getLatitude() + ", " + lastKnownLocation.getLongitude() + ")" : "";

        // Get the latest soil report from the DB (on background thread)
        databaseExecutor.execute(() -> {
            SoilAnalysis lastScan = db.soilDao().getLatestReport();
            if (lastScan != null) {
                lastSoilReportJson = lastScan.soilReportJson;
            }

            String systemMessage = "You are Mitti Mitra, a friendly and expert AI farming assistant... " +
                    // (Your full prompt from before)
                    "The user is" + locationContext + ". " +
                    "**DOMAIN RESTRICTION:** Your *only* topic is agriculture. If the user asks about non-farming topics, you **must** politely decline. " +
                    "**NO THINKING TAGS:** Your final answer must *never* include `<think>` or `</think>` tags.";

            callGroqChatAPI(systemMessage, true);
        });
    }

    private void sendMessage(String userMessage) {
        addMessage(userMessage, ChatMessage.Type.USER);

        String locationContext = (lastKnownLocation != null) ?
                "My current location is latitude " + lastKnownLocation.getLatitude() + " and longitude " + lastKnownLocation.getLongitude() + ". " :
                "I have not shared my location. ";

        // UPDATED: Use the cached soil report
        String soilContext = (lastSoilReportJson != null) ?
                "My last soil scan showed: " + lastSoilReportJson + ". " :
                "I have no soil scan history. ";

        String finalPrompt = "**Here is my context (use this for farming advice):**\n" +
                "- " + locationContext + "\n" +
                "- " + soilContext + "\n\n" +
                "**Here is my question:** " + userMessage + "\n\n" +
                "**Your Task (MUST FOLLOW):**" +
                "1.  Your allowed topics are **farming** AND questions **about the context I provided**." +
                "2.  If my question is about **farming**, answer it using the context." +
                "3.  If my question is about **my location or soil**, you **ARE allowed to answer it directly**." +
                "4.  If my question is **NOT** about farming and **NOT** about my context (e.g., 'what is a movie?'), you **MUST** politely decline." +
                "5.  Do not show your `<think>` tags.";

        callGroqChatAPI(finalPrompt, false);
    }

    // ... (The rest of your TipActivity.java [callGroqChatAPI, callGroqWhisperAPI, UI helpers, etc.] remains exactly the same as before) ...
    // ... (I am omitting the rest of the file for brevity, as it does not change) ...

    // --- PASTE THE REST OF YOUR TipActivity.java CODE HERE ---
    // (Starting from the callGroqChatAPI method)

    // --- Helper Methods (Copied from previous version) ---

    private void callGroqChatAPI(String prompt, boolean isInitialMessage) {
        setLoading(true);

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            jsonBody.put("messages", messages);
            jsonBody.put("model", "qwen/qwen3-32b");
            // ... (rest of your JSON body) ...
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build JSON", e);
            setLoading(false);
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Groq API call failed", e);
                runOnUiThread(() -> {
                    addMessage("Error: Could not connect to AI. Please check your internet.", ChatMessage.Type.BOT);
                    setLoading(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Groq API Error: " + errorBody);
                    runOnUiThread(() -> {
                        addMessage("Error: Failed to get response from AI. " + errorBody, ChatMessage.Type.BOT);
                        setLoading(false);
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String aiResponse = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    final String cleanResponse = aiResponse.replaceAll("<think>(?s).*?</think>", "").trim();

                    runOnUiThread(() -> {
                        if (cleanResponse.isEmpty()) {
                            addMessage("I'm sorry, I had a thought but couldn't form a response. Could you rephrase that?", ChatMessage.Type.BOT);
                        } else {
                            addMessage(cleanResponse, ChatMessage.Type.BOT);
                        }
                        setLoading(false);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse Groq response", e);
                    runOnUiThread(() -> {
                        addMessage("Error: Failed to understand AI response.", ChatMessage.Type.BOT);
                        setLoading(false);
                    });
                }
            }
        });
    }

    private void callGroqWhisperAPI(File audioFile) {
        setLoading(true);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.get("audio/mp4")))
                .addFormDataPart("model", "whisper-large-v3")
                .addFormDataPart("temperature", "0")
                .addFormDataPart("response_format", "verbose_json")
                .build();

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Whisper API call failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(TipActivity.this, "Failed to transcribe audio.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e(TAG, "Whisper API Error: " + errorBody);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(TipActivity.this, "Transcription failed.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String transcribedText = json.getString("text");

                    runOnUiThread(() -> {
                        etChatMessage.setText(transcribedText);
                        setLoading(false);
                        sendMessage(transcribedText);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse Whisper response", e);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(TipActivity.this, "Transcription parsing failed.", Toast.LENGTH_SHORT).show();
                    });
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
    }

    private void addMessage(String message, ChatMessage.Type type) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(message, type));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerViewChat.scrollToPosition(messageList.size() - 1);
        });
    }

    private void setLoading(boolean isLoading) {
        runOnUiThread(() -> {
            if (isLoading) {
                loadingIndicator.setVisibility(View.VISIBLE);
                btnSendChat.setEnabled(false);
                btnRecordAudio.setEnabled(false);
            } else {
                loadingIndicator.setVisibility(View.GONE);
                btnSendChat.setEnabled(true);
                btnRecordAudio.setEnabled(true);
            }
        });
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            toggleRecording();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                this.lastKnownLocation = location;
                Log.i(TAG, "Location found: " + location.getLatitude() + ", " + location.getLongitude());
            } else {
                Log.w(TAG, "Location was null.");
            }
            addInitialBotMessage();
        });
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                mediaRecorder = new MediaRecorder(this);
            } else {
                mediaRecorder = new MediaRecorder();
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();

            btnRecordAudio.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            isRecording = true;
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed", e);
        }
    }

    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "stopRecording failed", e);
        }
        mediaRecorder = null;
        isRecording = false;
        btnRecordAudio.clearColorFilter();
        Toast.makeText(this, "Recording stopped. Transcribing...", Toast.LENGTH_SHORT).show();

        if (audioFile.exists() && audioFile.length() > 0) {
            callGroqWhisperAPI(audioFile);
        } else {
            Toast.makeText(this, "No audio recorded.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.messageText.setText(message.getMessage());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (message.getType() == ChatMessage.Type.USER) {
                params.gravity = android.view.Gravity.END;
                holder.messageText.setBackgroundColor(0xFF007BFF); // Blue
                holder.messageText.setTextColor(0xFFFFFFFF); // White
            } else {
                params.gravity = android.view.Gravity.START;
                holder.messageText.setBackgroundColor(0xFFE9ECEF); // Light Gray
                holder.messageText.setTextColor(0xFF000000); // Black
            }
            holder.messageText.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.chat_message_text);
            }
        }
    }
}