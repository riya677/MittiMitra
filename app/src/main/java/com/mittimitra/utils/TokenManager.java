package com.mittimitra.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREFS_NAME = "auth_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USER_ID_KEY = "user_id";
    private static final String USERNAME_KEY = "username";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public TokenManager(Context context) {
        // TODO: For strict security, upgrade to EncryptedSharedPreferences (requires androidx.security library)
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveToken(String token) {
        editor.putString(TOKEN_KEY, token);
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

    public Long getUserId() {
        return sharedPreferences.getLong(USER_ID_KEY, -1);
    }

    public String getUsername() {
        return sharedPreferences.getString(USERNAME_KEY, null);
    }

    public void clearToken() {
        editor.clear();
        editor.apply();
    }
}