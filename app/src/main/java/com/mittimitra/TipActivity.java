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
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    // --- WARNING: Store your API Key securely. DO NOT hardcode it like this in a real app. ---
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

    // API
    private OkHttpClient httpClient;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;

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
                    // Still load the initial message, just without location.
                    addInitialBotMessage();
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
        httpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // --- Setup UI ---
        setupRecyclerView();
        requestLocation(); // Get location on start, which will then call addInitialBotMessage()

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

        // This prompt now sets the "rules" for the AI
        String systemMessage = "You are Mitti Mitra, a friendly and expert AI farming assistant for farmers in India. " +
                "You are communicating with a user" + locationContext + ". " +
                "You understand all major Indian languages and farming terms. " +

                "**Your Role:**" +
                "1.  **Greet the user** and ask how you can help with their farming questions." +
                "2.  **Be Interactive:** If a user's question is vague (e.g., 'is my plant sick?'), ask for more details, like 'Can you describe the leaves?' or 'What color are the spots?'. " +
                "3.  **BE INFORMATIVE:** Use your knowledge of farming, combined with the user's location and soil data (if provided), to give detailed, actionable advice." +
                "4.  **DOMAIN RESTRICTION:** Your *only* topic is agriculture (farming, crops, soil, pests, weather for farming). If the user asks about non-farming topics (like sports, movies, politics, personal opinions), you **must** politely decline and steer the conversation back to farming. For example: 'I am Mitti Mitra, your farming assistant. I can only help with questions about agriculture. Do you have a question about your crops or soil?'" +
                "5.  **NO THINKING TAGS:** Your final answer must *never* include `<think>` or `</think>` tags. Provide only the direct, clean response to the user.";

        callGroqChatAPI(systemMessage, true);
    }

    /**
     * THIS METHOD IS UPDATED
     */
    private void sendMessage(String userMessage) {
        // Add user's message to UI
        addMessage(userMessage, ChatMessage.Type.USER);

        String locationContext = (lastKnownLocation != null) ?
                "My current location is latitude " + lastKnownLocation.getLatitude() + " and longitude " + lastKnownLocation.getLongitude() + ". " :
                "I have not shared my location. ";

        // TODO: Get this from your database
        String soilContext = "My last soil scan showed low Nitrogen. ";

        // --- NEW, IMPROVED PROMPT ---
        // This new prompt explicitly allows the AI to answer questions about location/soil context.
        String finalPrompt = "**Here is my context (use this for farming advice):**\n" +
                "- " + locationContext + "\n" +
                "- " + soilContext + "\n\n" +
                "**Here is my question:** " + userMessage + "\n\n" +
                "**Your Task (MUST FOLLOW):**" +
                "1.  Your allowed topics are **farming** AND questions **about the context I provided** (my location, my soil data)." +
                "2.  If my question is about **farming**, answer it using the context." +
                "3.  If my question is about **my location or soil** (e.g., 'what is my location?' or 'what was my soil result?'), you **ARE allowed to answer it directly**. After answering, ask if I have a farming question." +
                "4.  If my question is **NOT** about farming and **NOT** about my context (e.g., 'what is a movie?'), you **MUST** politely decline and remind me you are a farming assistant. **DO NOT** mention my soil or location in this case." +
                "5.  Do not show your `<think>` tags.";


        callGroqChatAPI(finalPrompt, false);
    }

    private void callGroqChatAPI(String prompt, boolean isInitialMessage) {
        setLoading(true);

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", prompt));

            jsonBody.put("messages", messages);
            jsonBody.put("model", "qwen/qwen3-32b");
            jsonBody.put("temperature", 0.6);
            jsonBody.put("max_completion_tokens", 4096);
            jsonBody.put("top_p", 0.95);
            jsonBody.put("stream", false); // Using stream=false for a simple, single response
            jsonBody.put("stop", null);

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

                    // --- FIX: Remove <think>...</think> blocks ---
                    final String cleanResponse = aiResponse.replaceAll("<think>(?s).*?</think>", "").trim();

                    runOnUiThread(() -> {
                        if (cleanResponse.isEmpty()) {
                            // Handle case where AI *only* returned a think block
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

        // Build a Multipart request for file upload
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
                    assert response.body() != null;
                    String errorBody = response.body().string();
                    Log.e(TAG, "Whisper API Error: " + errorBody);
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(TipActivity.this, "Transcription failed.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    assert response.body() != null;
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String transcribedText = json.getString("text");

                    runOnUiThread(() -> {
                        etChatMessage.setText(transcribedText); // Put text in chat box
                        setLoading(false);
                        sendMessage(transcribedText); // Automatically send the message
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


    // --- UI & Permissions Logic ---

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // New messages appear from bottom
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
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            // Request permission
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

    @SuppressWarnings("MissingPermission") // We check for permission before calling this
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                this.lastKnownLocation = location;
                Log.i(TAG, "Location found: " + location.getLatitude() + ", " + location.getLongitude());
            } else {
                Log.w(TAG, "Location was null. Maybe location is turned off on device?");
            }
            // Now that we have location (or null), get the tip of the day
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
            // Check for API 31+ constructor
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

        // Send the file to Whisper API
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


    // --- Inner Class for Chat Adapter ---
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

            // Align user messages to the right, bot messages to the left
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