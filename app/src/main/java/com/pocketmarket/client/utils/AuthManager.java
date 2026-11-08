package com.pocketmarket.client.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {

    private static final String PREF_NAME = "pocketmarket_auth_prefs";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NICKNAME = "user_nickname";
    private static final String KEY_IS_GUEST = "is_guest";
    private static final String KEY_FIRST_LAUNCH = "first_launch_completed";

    private static AuthManager instance;
    private final SharedPreferences prefs;

    private AuthManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
    }

    public static synchronized AuthManager getInstance() {
        return instance;
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public boolean isFirstLaunch() {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH, false);
    }

    public void setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public boolean isGuest() {
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    public void setGuestMode(boolean guest) {
        prefs.edit()
                .putBoolean(KEY_IS_GUEST, guest)
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .apply();
    }

    public void saveSession(String token, String userId, String email, String nickname) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NICKNAME, nickname)
                .putBoolean(KEY_IS_GUEST, false)
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .apply();
    }

    public void logout() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_NICKNAME)
                .putBoolean(KEY_IS_GUEST, true)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getNickname() {
        return prefs.getString(KEY_NICKNAME, "Игрок");
    }
}
