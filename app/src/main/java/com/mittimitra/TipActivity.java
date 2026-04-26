package com.mittimitra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mittimitra.backend.ApiEnvelope;
import com.mittimitra.backend.BackendCallback;
import com.mittimitra.backend.model.AiModels;
import com.mittimitra.data.repository.FirebasePredictionRepository;
import com.mittimitra.database.MittiMitraDatabase;
import com.mittimitra.database.dao.ChatDao;
import com.mittimitra.database.entity.SoilAnalysis;
import com.mittimitra.domain.repository.PredictionRepository;
import com.mittimitra.ui.adapter.ChatAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TipActivity extends BaseActivity {

    private RecyclerView recyclerViewChat;
    private EditText etChatMessage;
    private FloatingActionButton btnSendChat;
    private ImageView btnRecordAudio;
    private ProgressBar loadingIndicator;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private MittiMitraDatabase db;
    private ChatDao chatDao;
    private ExecutorService databaseExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private volatile String lastSoilReportJson;

    private SpeechRecognizer speechRecognizer;
    private final PredictionRepository predictionRepository = new FirebasePredictionRepository();

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    getCurrentLocation();
                } else {
                    addInitialBotMessage();
                }
            });

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startListening();
                } else {
                    Toast.makeText(this, R.string.mic_permission_required, Toast.LENGTH_SHORT).show();
                }
            });

    private static final Map<String, String> LANGUAGE_NAMES;

    static {
        LANGUAGE_NAMES = new HashMap<>();
        LANGUAGE_NAMES.put("hi", "Hindi");
        LANGUAGE_NAMES.put("ta", "Tamil");
        LANGUAGE_NAMES.put("ml", "Malayalam");
        LANGUAGE_NAMES.put("te", "Telugu");
        LANGUAGE_NAMES.put("kn", "Kannada");
        LANGUAGE_NAMES.put("mr", "Marathi");
        LANGUAGE_NAMES.put("bn", "Bengali");
        LANGUAGE_NAMES.put("gu", "Gujarati");
        LANGUAGE_NAMES.put("pa", "Punjabi");
        LANGUAGE_NAMES.put("as", "Assamese");
        LANGUAGE_NAMES.put("mni", "Manipuri");
        LANGUAGE_NAMES.put("brx", "Bodo");
        LANGUAGE_NAMES.put("lus", "Mizo");
        LANGUAGE_NAMES.put("grt", "Garo");
        LANGUAGE_NAMES.put("en", "English");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip);

        Toolbar toolbar = findViewById(R.id.tip_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.kisan_sahayak_title);
        }

        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        etChatMessage = findViewById(R.id.et_chat_message);
        loadingIndicator = findViewById(R.id.loading_indicator);
        btnSendChat = findViewById(R.id.btn_send_chat);
        btnRecordAudio = findViewById(R.id.btn_record_audio);

        db = MittiMitraDatabase.getDatabase(getApplicationContext());
        chatDao = db.chatDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupRecyclerView();
        requestLocation();

        btnSendChat.setOnClickListener(v -> {
            String message = etChatMessage.getText().toString().trim();
            if (message.isEmpty()) return;
            sendMessage(message);
            etChatMessage.setText("");
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechListener);
        btnRecordAudio.setOnClickListener(v -> requestAudioPermission());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (databaseExecutor != null) databaseExecutor.shutdownNow();
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.voice_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        speechRecognizer.startListening(intent);
    }

    private final RecognitionListener speechListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            btnRecordAudio.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
        }

        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            btnRecordAudio.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#757575")));
        }

        @Override
        public void onError(int error) {
            Toast.makeText(TipActivity.this, getString(R.string.speech_no_match), Toast.LENGTH_SHORT).show();
            btnRecordAudio.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#757575")));
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches == null || matches.isEmpty()) return;

            String spokenText = matches.get(0);
            etChatMessage.setText(spokenText);
            sendMessage(spokenText);
            etChatMessage.setText("");
        }

        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    };

    private String getLanguageInstruction() {
        AppPreferences prefs = new AppPreferences(this);
        String code = prefs.getLanguage();
        if (code == null || code.isEmpty() || "en".equals(code)) return "";
        String name = LANGUAGE_NAMES.get(code);
        if (name == null) return "";
        return "IMPORTANT: Always respond strictly in " + name + ". ";
    }

    private void addInitialBotMessage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            SoilAnalysis lastScan = db.soilDao().getLatestReportForUser(user.getUid());
            if (lastScan != null) {
                lastSoilReportJson = lastScan.soilReportJson;
            }

            String prompt = getLanguageInstruction()
                    + "You are Kisan Sahayak, an expert farming assistant for Indian farmers. "
                    + "Respond clearly in 3-4 short sentences unless detailed answer is requested.";
            callBackendChat(prompt, true, "");
        });
    }

    private void sendMessage(String userMessage) {
        addMessage(userMessage, ChatMessage.Type.USER);

        String locContext = (lastKnownLocation != null)
                ? (lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude())
                : "";

        String prompt = "Context:\nLocation=" + locContext + "\n"
                + "LastSoil=" + (lastSoilReportJson != null ? lastSoilReportJson : "") + "\n"
                + "User=" + userMessage;

        callBackendChat(prompt, false, userMessage);
    }

    private void callBackendChat(String prompt, boolean isInitial, String userQuery) {
        setLoading(true);
        if (!isNetworkAvailable()) {
            loadOfflineTips(userQuery);
            setLoading(false);
            return;
        }

        AppPreferences prefs = new AppPreferences(this);
        AiModels.ChatRequest request = new AiModels.ChatRequest();
        request.prompt = prompt;
        request.userQuery = userQuery;
        request.languageCode = prefs.getLanguage();
        request.location = (lastKnownLocation != null)
                ? (lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude())
                : "";
        request.soilContext = lastSoilReportJson;

        predictionRepository.fetchChatResponse(request, new BackendCallback<AiModels.ChatResponseData>() {
            @Override
            public void onSuccess(@NonNull ApiEnvelope<AiModels.ChatResponseData> envelope) {
                runOnUiThread(() -> {
                    AiModels.ChatResponseData data = envelope.data;
                    if (data == null || data.replyMarkdown == null || data.replyMarkdown.trim().isEmpty()) {
                        loadOfflineTips(userQuery);
                    } else {
                        addMessage(data.replyMarkdown, ChatMessage.Type.BOT);
                        if (data.confidence != null && data.confidence < 45 && data.uncertaintyMessage != null) {
                            addMessage(data.uncertaintyMessage, ChatMessage.Type.BOT);
                        }
                    }
                    setLoading(false);
                });
            }

            @Override
            public void onFailure(@NonNull ApiEnvelope<AiModels.ChatResponseData> envelope, Throwable throwable) {
                runOnUiThread(() -> {
                    loadOfflineTips(userQuery);
                    setLoading(false);
                });
            }
        });
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(network);
            return nc != null && nc.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            android.net.NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }
    }

    private void loadOfflineTips(String userQuery) {
        try {
            String jsonString;
            try (InputStream is = getAssets().open("offline_tips.json")) {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                jsonString = baos.toString(java.nio.charset.StandardCharsets.UTF_8.name());
            }
            JSONObject json = new JSONObject(jsonString);

            if (userQuery != null && !userQuery.isEmpty()) {
                JSONArray qaArray = json.optJSONArray("qa");
                if (qaArray != null) {
                    String lowerQuery = userQuery.toLowerCase(Locale.ROOT);
                    for (int i = 0; i < qaArray.length(); i++) {
                        JSONObject entry = qaArray.getJSONObject(i);
                        JSONArray keywords = entry.getJSONArray("keywords");
                        for (int k = 0; k < keywords.length(); k++) {
                            if (lowerQuery.contains(keywords.getString(k).toLowerCase(Locale.ROOT))) {
                                addMessage("[OFFLINE MODE]\n" + entry.getString("answer"), ChatMessage.Type.BOT);
                                return;
                            }
                        }
                    }
                }
            }

            JSONObject tips = json.getJSONObject("tips");
            String advice = tips.optString("default", "General farming tip unavailable offline.");
            if (lastSoilReportJson != null) {
                String lowerSoil = lastSoilReportJson.toLowerCase(Locale.ROOT);
                if (lowerSoil.contains("clay")) advice = tips.optString("clay", advice);
                else if (lowerSoil.contains("red")) advice = tips.optString("red", advice);
                else if (lowerSoil.contains("black")) advice = tips.optString("black", advice);
                else if (lowerSoil.contains("sandy")) advice = tips.optString("sandy", advice);
                else if (lowerSoil.contains("loam")) advice = tips.optString("loam", advice);
            }
            addMessage("[OFFLINE MODE]\n" + advice, ChatMessage.Type.BOT);
        } catch (Exception e) {
            android.util.Log.e("TipActivity", "Failed to load offline tips", e);
            addMessage("Offline tips unavailable.", ChatMessage.Type.BOT);
        }
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
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

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);
        loadChatHistory();
    }

    private void loadChatHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            List<com.mittimitra.database.entity.ChatMessage> savedMessages = chatDao.getMessagesForUser(user.getUid());
            if (savedMessages == null || savedMessages.isEmpty()) return;

            runOnUiThread(() -> {
                for (com.mittimitra.database.entity.ChatMessage msg : savedMessages) {
                    ChatMessage.Type type = msg.isUser ? ChatMessage.Type.USER : ChatMessage.Type.BOT;
                    messageList.add(new ChatMessage(msg.content, type));
                }
                chatAdapter.notifyDataSetChanged();
                recyclerViewChat.scrollToPosition(messageList.size() - 1);
            });
        });
    }

    private void addMessage(String message, ChatMessage.Type type) {
        runOnUiThread(() -> {
            messageList.add(new ChatMessage(message, type));
            chatAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerViewChat.scrollToPosition(messageList.size() - 1);
        });

        if (type == ChatMessage.Type.LOADING) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            com.mittimitra.database.entity.ChatMessage dbMessage =
                    new com.mittimitra.database.entity.ChatMessage(message, type == ChatMessage.Type.USER);
            dbMessage.userId = user.getUid();
            chatDao.insertMessage(dbMessage);
        });
    }

    private void setLoading(boolean isLoading) {
        runOnUiThread(() -> {
            loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSendChat.setEnabled(!isLoading);
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
