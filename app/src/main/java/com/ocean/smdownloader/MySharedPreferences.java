package com.ocean.smdownloader;

import android.content.Context;
import android.content.SharedPreferences;

public class MySharedPreferences {

    private final String SHARED_PREFERENCE_NAME = "SM_DOWNLOADER_SHARED_PREFERENCE";
    private final String TOTAL_DOWNLOAD_COUNTS = "TOTAL_DOWNLOAD_COUNTS";
    private final String THREAD_STARTING_POINT = "THREAD_STARTING_POINT_OF_TASK_";
    private final String TOTAL_THREADS = "TOTAL_THREADS";
    private final String USER_LOGGED_IN_TO_INSTA = "USER_LOGGED_IN_TO_INSTA";
    private final String THEME_STATE = "THEME_STATE";

    private final String NAME_SEPARATOR = "_";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public MySharedPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setThemeState(int state) {
        editor.putInt(THEME_STATE, state);
        editor.apply();
        editor.commit();
    }

    public int getThemeState() {
        return sharedPreferences.getInt(THEME_STATE, 0);
    }

    public int incrementDownloadCounts() {
        editor.putInt(TOTAL_DOWNLOAD_COUNTS, sharedPreferences.getInt(TOTAL_DOWNLOAD_COUNTS, 0) + 1);
        editor.apply();
        editor.commit();

        return sharedPreferences.getInt(TOTAL_DOWNLOAD_COUNTS, 0);
    }

    public void setThreadStartingPoint(int taskId, int threadNo, long start) {
        editor.putLong(THREAD_STARTING_POINT + taskId + NAME_SEPARATOR + threadNo, start);
        editor.apply();
        editor.commit();
    }

    public long getThreadStartingPoint(int taskId, int threadNo, long def) {
        return sharedPreferences.getLong(THREAD_STARTING_POINT + taskId + NAME_SEPARATOR + threadNo, def);
    }

    public void deleteStartingPoints(int taskId) {
        for (int i = 0; i < getTotalThreads(); i++) {
            editor.remove(THREAD_STARTING_POINT + taskId + NAME_SEPARATOR + i);
        }
        editor.apply();
        editor.commit();
    }

    public int getTotalThreads() {
        return sharedPreferences.getInt(TOTAL_THREADS, 16);
    }


    public void setUserLoggedInToInsta(boolean userLoggedInToInsta) {
        editor.putBoolean(USER_LOGGED_IN_TO_INSTA, userLoggedInToInsta);
        editor.apply();
        editor.commit();
    }

    public boolean getUserLoggedInToInsta() {
        return sharedPreferences.getBoolean(USER_LOGGED_IN_TO_INSTA, false);
    }
}
