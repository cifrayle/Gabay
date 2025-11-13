package com.example.gabay.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.gabay.services.UserAnalyticsService;

/**
 * Utility class to track user session data
 * Automatically tracks session start/end and duration
 */
public class SessionTracker {
    private static final String TAG = "SessionTracker";
    private static final String PREFS_NAME = "SessionTracking";
    private static final String KEY_SESSION_START = "session_start_time";
    private static final String KEY_LAST_SESSION_DURATION = "last_session_duration";
    
    private static SessionTracker instance;
    private Context context;
    private long sessionStartTime = 0;
    private boolean sessionActive = false;

    private SessionTracker(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized SessionTracker getInstance(Context context) {
        if (instance == null) {
            instance = new SessionTracker(context);
        }
        return instance;
    }

    /**
     * Start tracking a new session
     */
    public void startSession() {
        if (!sessionActive) {
            sessionStartTime = System.currentTimeMillis();
            sessionActive = true;
            
            // Save session start time
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putLong(KEY_SESSION_START, sessionStartTime).apply();
            
            Log.d(TAG, "Session started at: " + sessionStartTime);
        }
    }

    /**
     * End the current session and track duration
     */
    public void endSession() {
        if (sessionActive && sessionStartTime > 0) {
            long sessionEndTime = System.currentTimeMillis();
            long sessionDurationMs = sessionEndTime - sessionStartTime;
            int sessionDurationMinutes = (int) (sessionDurationMs / (1000 * 60));
            
            // Minimum session duration of 1 minute to avoid noise
            if (sessionDurationMinutes < 1) {
                sessionDurationMinutes = 1;
            }
            
            sessionActive = false;
            
            // Save last session duration locally
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_LAST_SESSION_DURATION, sessionDurationMinutes).apply();
            
            Log.d(TAG, "Session ended. Duration: " + sessionDurationMinutes + " minutes");
            
            // Track session in analytics (background thread)
            trackSessionInAnalytics(sessionDurationMinutes);
        }
    }

    /**
     * Get the current session duration in minutes
     */
    public int getCurrentSessionDuration() {
        if (sessionActive && sessionStartTime > 0) {
            long currentTime = System.currentTimeMillis();
            long durationMs = currentTime - sessionStartTime;
            return (int) (durationMs / (1000 * 60));
        }
        return 0;
    }

    /**
     * Check if a session is currently active
     */
    public boolean isSessionActive() {
        return sessionActive;
    }

    /**
     * Get the last session duration from preferences
     */
    public int getLastSessionDuration() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_LAST_SESSION_DURATION, 0);
    }

    /**
     * Resume session tracking (useful for app resume)
     */
    public void resumeSession() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long savedStartTime = prefs.getLong(KEY_SESSION_START, 0);
        
        if (savedStartTime > 0) {
            // Check if the saved session is recent (within last 30 minutes)
            long timeSinceStart = System.currentTimeMillis() - savedStartTime;
            if (timeSinceStart < 30 * 60 * 1000) { // 30 minutes
                sessionStartTime = savedStartTime;
                sessionActive = true;
                Log.d(TAG, "Session resumed from: " + savedStartTime);
            } else {
                // Start new session if too much time has passed
                startSession();
            }
        } else {
            startSession();
        }
    }

    /**
     * Track session data in analytics (background operation)
     */
    private void trackSessionInAnalytics(int durationMinutes) {
        new Thread(() -> {
            try {
                boolean success = UserAnalyticsService.updateSessionData(durationMinutes);
                if (success) {
                    Log.d(TAG, "Session data tracked in analytics: " + durationMinutes + " minutes");
                } else {
                    Log.w(TAG, "Failed to track session data in analytics");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error tracking session data: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Reset session tracking (useful for logout)
     */
    public void resetSession() {
        sessionActive = false;
        sessionStartTime = 0;
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        Log.d(TAG, "Session tracking reset");
    }
}
