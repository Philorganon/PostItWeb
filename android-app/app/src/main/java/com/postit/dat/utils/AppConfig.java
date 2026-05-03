package com.postit.dat.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    private static final String PREF_NAME = "postit_config";
    private static final String KEY_GITHUB_TOKEN = "github_token";
    private static final String KEY_GITHUB_USER = "github_user";
    private static final String KEY_GITHUB_REPO = "github_repo";
    private static final String KEY_POLLING_INTERVAL = "polling_interval";

    private SharedPreferences prefs;
    private static AppConfig instance;

    private AppConfig(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppConfig getInstance(Context context) {
        if (instance == null) {
            instance = new AppConfig(context);
        }
        return instance;
    }

    public String getGithubToken() {
        return prefs.getString(KEY_GITHUB_TOKEN, "");
    }

    public void setGithubToken(String token) {
        prefs.edit().putString(KEY_GITHUB_TOKEN, token).apply();
    }

    public String getGithubUser() {
        return prefs.getString(KEY_GITHUB_USER, "");
    }

    public void setGithubUser(String user) {
        prefs.edit().putString(KEY_GITHUB_USER, user).apply();
    }

    public String getGithubRepo() {
        return prefs.getString(KEY_GITHUB_REPO, "");
    }

    public void setGithubRepo(String repo) {
        prefs.edit().putString(KEY_GITHUB_REPO, repo).apply();
    }

    public int getPollingInterval() {
        return prefs.getInt(KEY_POLLING_INTERVAL, 4000); // default 4 detik
    }

    public void setPollingInterval(int ms) {
        prefs.edit().putInt(KEY_POLLING_INTERVAL, ms).apply();
    }

    public boolean isConfigured() {
        return !getGithubToken().isEmpty()
            && !getGithubUser().isEmpty()
            && !getGithubRepo().isEmpty();
    }

    public String getApiBase() {
        return "https://api.github.com/repos/" + getGithubUser() + "/" + getGithubRepo() + "/contents/";
    }
}
