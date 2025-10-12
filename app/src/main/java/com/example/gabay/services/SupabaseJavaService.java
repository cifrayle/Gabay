package com.example.gabay.services;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseJavaService {
    private static final String SUPABASE_URL = "https://vzpjsmbpgqlanqzeiqsb.supabase.co";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ6cGpzbWJwZ3FsYW5xemVpcXNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk2NzI3MjUsImV4cCI6MjA3NTI0ODcyNX0.Iwz5iXkQxbACmYgJ6EB7bXuX76tLPYPJSZPF33N9k-s";
    private static final String REST_URL = SUPABASE_URL + "/rest/v1";

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // Store the access token after user signs in
    private static String accessToken = null;
    private static String userId = null;

    /**
     * Set the access token after successful authentication
     * Call this after user signs in/up
     */
    public static void setAccessToken(String token, String uid) {
        accessToken = token;
        userId = uid;
        Log.d("SupabaseService", "Access token set for user: " + uid);
    }

    /**
     * Clear authentication (for logout)
     */
    public static void clearAuth() {
        accessToken = null;
        userId = null;
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        return accessToken != null && userId != null;
    }

    /**
     * Update user progress for a specific chapter and level
     * This will insert or update the progress record
     */
    public static boolean updateUserProgress(int chapter, int level) {
        if (!isAuthenticated()) {
            Log.e("SupabaseService", "User not authenticated");
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        try {
            // First, check if record exists
            String checkUrl = REST_URL + "/user_progress?user_id=eq." + userId +
                    "&chapter_number=eq." + chapter +
                    "&level_number=eq." + level;

            Request checkRequest = new Request.Builder()
                    .url(checkUrl)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            client.newCall(checkRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("SupabaseService", "Failed to check existing progress: " + e.getMessage());
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONArray array = new JSONArray(responseBody);

                        if (array.length() > 0) {
                            // Record exists, update it
                            updateExistingProgress(chapter, level, latch, success);
                        } else {
                            // Record doesn't exist, insert new
                            insertNewProgress(chapter, level, latch, success);
                        }
                    } catch (JSONException e) {
                        Log.e("SupabaseService", "JSON parsing error: " + e.getMessage());
                        latch.countDown();
                    }
                }
            });

            // Wait for the operation to complete (max 10 seconds)
            latch.await(10, TimeUnit.SECONDS);
            return success.get();

        } catch (Exception e) {
            Log.e("SupabaseService", "Error updating progress: " + e.getMessage());
            return false;
        }
    }

    /**
     * Insert new progress record
     */
    private static void insertNewProgress(int chapter, int level, CountDownLatch latch, AtomicBoolean success) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("chapter_number", chapter);
            json.put("level_number", level);
            json.put("completed", true);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(REST_URL + "/user_progress")
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("SupabaseService", "Failed to insert progress: " + e.getMessage());
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("SupabaseService", "Progress inserted successfully");
                        success.set(true);
                    } else {
                        Log.e("SupabaseService", "Failed to insert progress: " + response.body().string());
                    }
                    latch.countDown();
                }
            });
        } catch (JSONException e) {
            Log.e("SupabaseService", "JSON error: " + e.getMessage());
            latch.countDown();
        }
    }

    /**
     * Update existing progress record
     */
    private static void updateExistingProgress(int chapter, int level, CountDownLatch latch, AtomicBoolean success) {
        try {
            JSONObject json = new JSONObject();
            json.put("completed", true);
            json.put("completed_at", "now()");

            RequestBody body = RequestBody.create(json.toString(), JSON);
            String updateUrl = REST_URL + "/user_progress?user_id=eq." + userId +
                    "&chapter_number=eq." + chapter +
                    "&level_number=eq." + level;

            Request request = new Request.Builder()
                    .url(updateUrl)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("SupabaseService", "Failed to update progress: " + e.getMessage());
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("SupabaseService", "Progress updated successfully");
                        success.set(true);
                    } else {
                        Log.e("SupabaseService", "Failed to update progress: " + response.body().string());
                    }
                    latch.countDown();
                }
            });
        } catch (JSONException e) {
            Log.e("SupabaseService", "JSON error: " + e.getMessage());
            latch.countDown();
        }
    }

    /**
     * Get count of completed levels for a specific chapter
     */
    public static int getCompletedLevelsCount(int chapter) {
        if (!isAuthenticated()) {
            Log.e("SupabaseService", "User not authenticated");
            return 0;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);

        try {
            String url = REST_URL + "/user_progress?user_id=eq." + userId +
                    "&chapter_number=eq." + chapter +
                    "&completed=eq.true&select=level_number";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("SupabaseService", "Failed to get completed levels: " + e.getMessage());
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            JSONArray array = new JSONArray(responseBody);

                            // Find the highest level number completed
                            int maxLevel = 0;
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                int levelNum = obj.getInt("level_number");
                                if (levelNum > maxLevel) {
                                    maxLevel = levelNum;
                                }
                            }

                            count.set(maxLevel);
                            Log.d("SupabaseService", "Chapter " + chapter + " completed levels: " + maxLevel);
                        } else {
                            Log.e("SupabaseService", "Failed to get levels: " + response.body().string());
                        }
                    } catch (JSONException e) {
                        Log.e("SupabaseService", "JSON parsing error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });

            // Wait for the operation to complete (max 10 seconds)
            latch.await(10, TimeUnit.SECONDS);
            return count.get();

        } catch (Exception e) {
            Log.e("SupabaseService", "Error getting completed levels: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get user's access token
     */
    public static String getAccessToken() {
        return accessToken;
    }

    /**
     * Get user's ID
     */
    public static String getUserId() {
        return userId;
    }

    /**
     * Create initial user profile after signup
     */
    public static boolean createUserProfile(String username) {
        if (!isAuthenticated()) {
            Log.e("SupabaseService", "User not authenticated");
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        try {
            JSONObject json = new JSONObject();
            json.put("id", userId);
            json.put("username", username);
            json.put("total_chapters_completed", 0);
            json.put("total_quizzes_passed", 0);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(REST_URL + "/user_profiles")
                    .addHeader("apikey", SUPABASE_API_KEY)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("SupabaseService", "Failed to create profile: " + e.getMessage());
                    latch.countDown();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("SupabaseService", "User profile created successfully");
                        success.set(true);
                    } else {
                        Log.e("SupabaseService", "Failed to create profile: " + response.body().string());
                    }
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            return success.get();

        } catch (Exception e) {
            Log.e("SupabaseService", "Error creating profile: " + e.getMessage());
            return false;
        }
    }
}