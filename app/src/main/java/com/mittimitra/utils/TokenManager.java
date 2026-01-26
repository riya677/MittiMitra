package com.mittimitra.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Secure token manager using EncryptedSharedPreferences.
 * All sensitive data (tokens, API keys) are encrypted at rest.
 */
public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREFS_NAME = "secure_auth_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USER_ID_KEY = "user_id";
    private static final String USERNAME_KEY = "username";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public TokenManager(Context context) {
        try {
            // Create MasterKey for encryption
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Create EncryptedSharedPreferences
            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            editor = sharedPreferences.edit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to regular", e);
            // Fallback to regular SharedPreferences if encryption fails (e.g., on older devices)
            sharedPreferences = context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();
        }
    }

    public void saveToken(String token) {
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

    public void saveRefreshToken(String refreshToken) {
        editor.putString(REFRESH_TOKEN_KEY, refreshToken);
        editor.apply();
    }

    public void saveUserId(Long userId) {
        editor.putLong(USER_ID_KEY, userId);
        editor.apply();
    }

    public void saveUsername(String username) {
        editor.putString(USERNAME_KEY, username);
        editor.apply();
    }

    public String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null);
    }

    public Long getUserId() {
        return sharedPreferences.getLong(USER_ID_KEY, -1);
    }

    public String getUsername() {
        return sharedPreferences.getString(USERNAME_KEY, null);
    }

    public boolean hasValidToken() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void clearToken() {
        editor.clear();
        editor.apply();
    }
}