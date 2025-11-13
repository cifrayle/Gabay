package com.example.gabay.utils;

import android.content.Context;
import android.util.Log;

import com.example.gabay.services.UserAnalyticsService;

import org.json.JSONObject;

/**
 * Helper class for testing analytics integration
 * Use this to verify that the analytics system is working correctly
 */
public class AnalyticsTestHelper {
    private static final String TAG = "AnalyticsTestHelper";

    /**
     * Test app rating functionality
     */
    public static void testAppRating(Context context) {
        Log.d(TAG, "Testing app rating functionality...");
        
        new Thread(() -> {
            try {
                // Test rating submission
                boolean success = UserAnalyticsService.submitAppRating(5, "Great app for learning FSL!");
                Log.d(TAG, "Rating submission test: " + (success ? "SUCCESS" : "FAILED"));
                
                // Test analytics retrieval
                JSONObject analytics = UserAnalyticsService.getUserAnalytics();
                if (analytics != null) {
                    Log.d(TAG, "Analytics retrieval test: SUCCESS");
                    Log.d(TAG, "Analytics data: " + analytics.toString());
                } else {
                    Log.d(TAG, "Analytics retrieval test: FAILED");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error testing app rating: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Test chapter completion tracking
     */
    public static void testChapterCompletion(Context context, int chapterNumber) {
        Log.d(TAG, "Testing chapter completion tracking for chapter " + chapterNumber + "...");
        
        new Thread(() -> {
            try {
                boolean success = UserAnalyticsService.trackChapterCompletion(chapterNumber);
                Log.d(TAG, "Chapter completion tracking test: " + (success ? "SUCCESS" : "FAILED"));
                
            } catch (Exception e) {
                Log.e(TAG, "Error testing chapter completion: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Test session tracking
     */
    public static void testSessionTracking(Context context) {
        Log.d(TAG, "Testing session tracking...");
        
        SessionTracker tracker = SessionTracker.getInstance(context);
        
        // Start a test session
        tracker.startSession();
        Log.d(TAG, "Session started. Active: " + tracker.isSessionActive());
        
        // Simulate some activity time
        try {
            Thread.sleep(2000); // 2 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        Log.d(TAG, "Current session duration: " + tracker.getCurrentSessionDuration() + " minutes");
        
        // End session
        tracker.endSession();
        Log.d(TAG, "Session ended. Active: " + tracker.isSessionActive());
        Log.d(TAG, "Last session duration: " + tracker.getLastSessionDuration() + " minutes");
    }

    /**
     * Test session data update in analytics
     */
    public static void testSessionDataUpdate(Context context) {
        Log.d(TAG, "Testing session data update in analytics...");
        
        new Thread(() -> {
            try {
                // Test updating session data with 15 minutes
                boolean success = UserAnalyticsService.updateSessionData(15);
                Log.d(TAG, "Session data update test: " + (success ? "SUCCESS" : "FAILED"));
                
            } catch (Exception e) {
                Log.e(TAG, "Error testing session data update: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Run all tests
     */
    public static void runAllTests(Context context) {
        Log.d(TAG, "=== Running All Analytics Tests ===");
        
        testSessionTracking(context);
        
        // Wait a bit between tests
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                testAppRating(context);
                
                Thread.sleep(1000);
                testChapterCompletion(context, 1);
                
                Thread.sleep(1000);
                testSessionDataUpdate(context);
                
                Log.d(TAG, "=== All Tests Completed ===");
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Test execution interrupted: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Print current analytics data
     */
    public static void printAnalyticsData(Context context) {
        Log.d(TAG, "Fetching current analytics data...");
        
        new Thread(() -> {
            try {
                JSONObject analytics = UserAnalyticsService.getUserAnalytics();
                if (analytics != null) {
                    Log.d(TAG, "=== Current Analytics Data ===");
                    Log.d(TAG, analytics.toString(2)); // Pretty print with indentation
                } else {
                    Log.d(TAG, "No analytics data found or user not authenticated");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching analytics data: " + e.getMessage());
            }
        }).start();
    }
}
