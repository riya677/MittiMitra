package com.mittimitra;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This is the "contract" for the app.
 * Your teammate's Login/Signup code MUST call saveUser() on success.
 * All our feature (Documents, Tip, History) will read from this.
 */
public class SessionManager {

    private static final String PREF_NAME = "MittiMitraSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences sharedPreferences;

    public SessionManager(Context context) {
        // Use application context to avoid memory leaks
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * To be called from your teammate's Login/Signup screen.
     */
    public void saveUser(String userId, String userName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.apply();
    }

    /**
     * To be called from our Logout button.
     */
    public void clearSession() {
        sharedPreferences.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getString(KEY_USER_ID, null) != null;
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "User");
    }
}