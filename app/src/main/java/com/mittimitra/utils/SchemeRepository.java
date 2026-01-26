package com.mittimitra.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mittimitra.models.Scheme;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SchemeRepository {

    private static final String SCHEME_FILE_NAME = "cached_schemes.json";
    
    // Remote URL for dynamic scheme updates (optional - local data is comprehensive)
    // Set to empty string to disable remote updates, or use a real GitHub Gist URL with actual schemes
    private static final String REMOTE_URL = ""; // Disabled - using local schemes_data.json which has 35+ real schemes 
    
    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;

    public SchemeRepository(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public interface SchemeCallback {
        void onschemesLoaded(List<Scheme> schemes);
    }

    public void  getSchemes(SchemeCallback callback) {
        // 1. Try to load from internal cache first (Dynamic update)
        List<Scheme> cachedSchemes = loadFromInternalStorage();
        if (cachedSchemes != null && !cachedSchemes.isEmpty()) {
            callback.onschemesLoaded(cachedSchemes);
            checkForUpdates(); // Check for updates in background
            return;
        }

        // 2. Fallback to Assets (Ship-time data)
        List<Scheme> assetSchemes = loadFromAssets();
        callback.onschemesLoaded(assetSchemes);
        
        // 3. Trigger update check (only if remote URL is configured)
        if (REMOTE_URL != null && !REMOTE_URL.isEmpty()) {
            checkForUpdates();
        }
    }

    private List<Scheme> loadFromAssets() {
        try {
            InputStream is = context.getAssets().open("schemes_data.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            Type listType = new TypeToken<ArrayList<Scheme>>(){}.getType();
            return gson.fromJson(json, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Return empty list on failure
        }
    }

    private List<Scheme> loadFromInternalStorage() {
        File file = new File(context.getFilesDir(), SCHEME_FILE_NAME);
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Scheme>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkForUpdates() {
        Request request = new Request.Builder().url(REMOTE_URL).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Ignore failure, we just keep using local/cached data
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    try {
                        // Validate JSON
                        Type listType = new TypeToken<ArrayList<Scheme>>(){}.getType();
                        List<Scheme> schemes = gson.fromJson(json, listType);
                        
                        if (schemes != null && !schemes.isEmpty()) {
                            // Save to internal storage
                            saveToInternalStorage(json);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void saveToInternalStorage(String json) {
        File file = new File(context.getFilesDir(), SCHEME_FILE_NAME);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
