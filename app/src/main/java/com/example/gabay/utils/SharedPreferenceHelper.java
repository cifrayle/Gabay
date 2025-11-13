package com.example.gabay.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferenceHelper {

    // Base key to create unique keys like "chapter_completed_1", "chapter_completed_2"
    private static final String CHAPTER_COMPLETED_KEY_PREFIX = "chapter_completed_";

    /**
     * Marks a specific chapter as completed.
     * @param context The application context.
     * @param chapterNumber The chapter number (e.g., 1, 2, 5).
     */
    public static void setChapterCompleted(Context context, int chapterNumber) {
        if (context == null) return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = CHAPTER_COMPLETED_KEY_PREFIX + chapterNumber;
        prefs.edit().putBoolean(key, true).apply();
    }

    /**
     * Checks if a specific chapter has been completed.
     * @param context The application context.
     * @param chapterNumber The chapter number to check.
     * @return true if the chapter is completed, false otherwise.
     */
    public static boolean isChapterCompleted(Context context, int chapterNumber) {
        if (context == null) return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = CHAPTER_COMPLETED_KEY_PREFIX + chapterNumber;
        return prefs.getBoolean(key, false);
    }

    /**
     * Checks if ALL required chapters have been completed.
     * This is where the core logic resides.
     * @param context The application context.
     * @param totalChapters The total number of chapters in the app.
     * @return true if all chapters from 1 to totalChapters are complete, false otherwise.
     */
    public static boolean areAllChaptersCompleted(Context context, int totalChapters) {
        if (context == null) return false;
        for (int i = 1; i <= totalChapters; i++) {
            // If we find even one chapter that is NOT completed, we can immediately return false.
            if (!isChapterCompleted(context, i)) {
                return false;
            }
        }
        // If the loop finishes without returning false, it means all chapters were completed.
        return true;
    }
}
