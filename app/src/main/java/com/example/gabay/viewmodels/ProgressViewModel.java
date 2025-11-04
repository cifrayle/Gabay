package com.example.gabay.viewmodels;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gabay.services.SupabaseJavaService;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * ViewModel for managing user progress across chapters and levels
 * Now with Supabase integration for persistent storage
 */
public class ProgressViewModel extends ViewModel {

    // Chapter progress tracking - using simple integer counts instead of ChapterProgress objects
    private final MutableLiveData<Map<Integer, Integer>> chapterProgress;

    // Quiz completion tracking
    private final MutableLiveData<Map<String, Boolean>> quizCompletion;

    // User profile data
    private final MutableLiveData<UserProfile> userProfile;
    private MutableLiveData<Boolean> usernameUpdateResult = new MutableLiveData<>();
    private MutableLiveData<String> updatedUsername = new MutableLiveData<>();

    // Current active chapter and level
    private final MutableLiveData<Integer> currentChapter;
    private final MutableLiveData<Integer> currentLevel;

    // Total progress
    private final MutableLiveData<Integer> totalProgress;

    // Executor for background tasks
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    public ProgressViewModel() {
        chapterProgress = new MutableLiveData<>(new HashMap<>());
        quizCompletion = new MutableLiveData<>(new HashMap<>());
        userProfile = new MutableLiveData<>(new UserProfile());
        currentChapter = new MutableLiveData<>(1);
        currentLevel = new MutableLiveData<>(1);
        totalProgress = new MutableLiveData<>(0);

        initializeDefaultProgress();

        // Load progress from Supabase when ViewModel is created
        refreshProgressFromSupabase();
        loadUserProfileFromSupabase();
        loadQuizCompletionFromSupabase();// Add this line
    }

    public void loadQuizCompletionFromSupabase() {
        backgroundExecutor.execute(() -> {
            try {
                Map<String, Boolean> quizStates = new HashMap<>();

                // Load quiz completion for all chapters from Supabase
                for (int chapter = 1; chapter <= 5; chapter++) {
                    boolean completed = SupabaseJavaService.isQuizCompleted(chapter);
                    quizStates.put("chapter_" + chapter + "_quiz", completed);
                    Log.d("QuizDebug", "Loaded quiz state for chapter " + chapter + ": " + completed);
                }

                // Update LiveData on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    quizCompletion.setValue(quizStates);
                    Log.d("QuizDebug", "Quiz completion states loaded from Supabase: " + quizStates.toString());
                });

            } catch (Exception e) {
                Log.e("ProgressViewModel", "Error loading quiz completion: " + e.getMessage());
            }
        });
    }

    private void initializeDefaultProgress() {
        Map<Integer, Integer> progress = new HashMap<>();

        // Initialize progress for 5 chapters
        for (int i = 1; i <= 5; i++) {
            progress.put(i, 0);
        }

        chapterProgress.setValue(progress);
        updateTotalProgress();
    }

    public void updateChapterProgress(int chapter, int level) {
        Log.d("ProgressDebug", "=== UPDATING PROGRESS: Chapter " + chapter + ", Level " + level + " ===");

        // 1. Update local state immediately for responsive UI
        updateLocalProgress(chapter, level);

        // 2. Check if chapter is completed (all levels done) and mark quiz as passed
        if (isChapterCompleted(chapter)) {
            markChapterQuizCompleted(chapter);
            Log.d("ProgressDebug", "Chapter " + chapter + " completed - quiz auto-marked as passed");
        }

        // 3. Save to Supabase in background
        backgroundExecutor.execute(() -> {
            try {
                Log.d("ProgressDebug", "Saving to Supabase...");
                boolean success = SupabaseJavaService.updateUserProgress(chapter, level);

                if (success) {
                    Log.d("ProgressDebug", "✅ Progress saved to Supabase successfully");

                    // Refresh from Supabase to ensure consistency
                    refreshProgressFromSupabase();
                } else {
                    Log.e("ProgressDebug", "❌ Failed to save progress to Supabase");
                }
            } catch (Exception e) {
                Log.e("ProgressDebug", "❌ Error updating progress in Supabase: " + e.getMessage());
            }
        });
    }

    private void updateLocalProgress(int chapter, int level) {
        Map<Integer, Integer> currentProgress = chapterProgress.getValue();
        if (currentProgress == null) currentProgress = new HashMap<>();

        // Copy the map to ensure LiveData detects the change
        Map<Integer, Integer> updatedProgress = new HashMap<>(currentProgress);

        int currentCompleted = Math.max(updatedProgress.getOrDefault(chapter, 0), level);
        updatedProgress.put(chapter, currentCompleted);

        // ✅ Post updated map to notify observers
        chapterProgress.setValue(updatedProgress);

        Log.d("ProgressDebug", "📊 Local progress updated - Chapter " + chapter + ": " + currentCompleted + " levels");

        // Still call total progress updater (optional but safe)
        updateTotalProgress();
    }


    private void updateTotalProgress() {
        Map<Integer, Integer> progress = chapterProgress.getValue();
        if (progress == null) return;

        int totalCompleted = 0;
        int totalPossible = 0;

        for (int chapter = 1; chapter <= 5; chapter++) {
            int completed = progress.getOrDefault(chapter, 0);
            int maxLevels = getMaxLevelsForChapter(chapter);

            totalCompleted += completed;
            totalPossible += maxLevels;

            Log.d("ProgressDebug", "Chapter " + chapter + ": " + completed + "/" + maxLevels + " levels");
        }

        int percentage = totalPossible > 0 ? (totalCompleted * 100) / totalPossible : 0;
        totalProgress.setValue(percentage);

        Log.d("ProgressDebug", "🎯 Total progress: " + percentage + "% (" + totalCompleted + "/" + totalPossible + " levels)");
    }

    // Helper method to check if a chapter is fully completed
    public boolean isChapterCompleted(int chapter) {
        ProgressViewModel.ChapterProgress progress = getChapterProgressObject(chapter);
        return progress != null && progress.getProgressPercentage() >= 100;
    }

    public int getMaxLevelsForChapter(int chapter) {
        switch (chapter) {
            case 1: return 28;
            case 2: return 5;
            case 3: return 10;
            case 4: return 7;
            case 5: return 7;
            default: return 28;
        }
    }

    public void refreshProgressFromSupabase() {
        Log.d("ProgressDebug", "🔄 Refreshing progress from Supabase...");

        backgroundExecutor.execute(() -> {
            try {
                Map<Integer, Integer> supabaseProgress = new HashMap<>();

                // Fetch progress for each chapter from Supabase
                for (int chapter = 1; chapter <= 5; chapter++) {
                    int completedLevels = SupabaseJavaService.getCompletedLevelsCount(chapter);
                    supabaseProgress.put(chapter, completedLevels);
                    Log.d("ProgressDebug", "📥 Chapter " + chapter + " from Supabase: " + completedLevels + " levels");
                }

                // Update LiveData on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    chapterProgress.setValue(supabaseProgress);
                    updateTotalProgress();
                    Log.d("ProgressDebug", "✅ Progress refreshed from Supabase");
                });

            } catch (Exception e) {
                Log.e("ProgressDebug", "❌ Error refreshing progress from Supabase: " + e.getMessage());
            }
        });
    }

    // === ADD THESE NEW METHODS FOR USER PROFILE ===

    public void loadUserProfileFromSupabase() {
        if (!SupabaseJavaService.isAuthenticated()) {
            Log.e("ProgressViewModel", "User not authenticated for profile loading");
            setDefaultUserProfile();
            return;
        }

        backgroundExecutor.execute(() -> {
            // Get profile data from user_profiles table
            JSONObject profile = SupabaseJavaService.getUserProfile();

            String finalUsername = "User";
            String userEmail = SupabaseJavaService.getAuthUserEmail(); // GET EMAIL HERE

            if (userEmail == null || userEmail.isEmpty()) {
                userEmail = "user@example.com"; // fallback
                Log.w("ProgressViewModel", "Using fallback email");
            }

            if (profile != null) {
                try {
                    // Get username from user_profiles table
                    finalUsername = profile.optString("username", null);

                    // If username is not in user_profiles, fallback to auth display name
                    if (finalUsername == null || finalUsername.isEmpty()) {
                        finalUsername = SupabaseJavaService.getAuthUserDisplayName();
                    }

                    // If still null, use default
                    if (finalUsername == null || finalUsername.isEmpty()) {
                        finalUsername = "User";
                    }

                    int totalChaptersCompleted = profile.optInt("total_chapters_completed", 0);
                    int totalQuizzesPassed = profile.optInt("total_quizzes_passed", 0);

                    final UserProfile finalProfile = new UserProfile();
                    finalProfile.setUsername(finalUsername);
                    finalProfile.setEmail(userEmail); // SET THE EMAIL
                    finalProfile.setTotalChaptersCompleted(totalChaptersCompleted);
                    finalProfile.setTotalQuizzesPassed(totalQuizzesPassed);

                    // Update LiveData on main thread
                    String finalUsername1 = finalUsername;
                    String finalUserEmail = userEmail;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        userProfile.setValue(finalProfile);
                        Log.d("ProgressViewModel", "User profile loaded - username: " + finalUsername1 + ", email: " + finalUserEmail);
                    });

                } catch (Exception e) {
                    Log.e("ProgressViewModel", "Error parsing profile data: " + e.getMessage());
                    setUserProfileFromAuth(finalUsername, userEmail);
                }
            } else {
                Log.w("ProgressViewModel", "No profile data found, using auth data");
                setUserProfileFromAuth(finalUsername, userEmail);
            }
        });
    }

    // NEW METHOD: Set profile from authentication data
    private void setUserProfileFromAuth(String username, String email) {
        final UserProfile finalProfile = new UserProfile();
        finalProfile.setUsername(username);
        finalProfile.setEmail(email);
        finalProfile.setTotalChaptersCompleted(0);
        finalProfile.setTotalQuizzesPassed(0);

        new Handler(Looper.getMainLooper()).post(() -> {
            userProfile.setValue(finalProfile);
            Log.d("ProgressViewModel", "User profile set from auth - username: " + username + ", email: " + email);
        });
    }

    private void setDefaultUserProfile() {
        new Handler(Looper.getMainLooper()).post(() -> {
            UserProfile defaultProfile = new UserProfile();
            defaultProfile.setUsername("User");
            defaultProfile.setEmail("user@example.com");
            userProfile.setValue(defaultProfile);
            Log.d("ProgressViewModel", "Default user profile set");
        });
    }

    public void updateUsernameInSupabase(String newUsername) {
        if (!SupabaseJavaService.isAuthenticated()) {
            Log.e("ProgressViewModel", "User not authenticated for username update");
            usernameUpdateResult.postValue(false);
            return;
        }
        backgroundExecutor.execute(() -> {
            boolean success = SupabaseJavaService.updateUsername(newUsername);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    // Reload profile after successful update
                    loadUserProfileFromSupabase();
                    updatedUsername.postValue(newUsername);
                    usernameUpdateResult.postValue(true);
                    Log.d("ProgressViewModel", "Username updated successfully: " + newUsername);
                } else {
                    usernameUpdateResult.postValue(false);
                    Log.e("ProgressViewModel", "Failed to update username in Supabase");
                }
            });
        });
    }

//    private void setDefaultUserProfile() {
//        new Handler(Looper.getMainLooper()).post(() -> {
//            userProfile.setValue(new UserProfile());
//        });
//    }

    // === KEEP ALL YOUR EXISTING METHODS ===

    public LiveData<Integer> getTotalProgress() {
        return totalProgress;
    }

    public LiveData<Map<Integer, Integer>> getAllChapterProgress() {
        return chapterProgress;
    }

    public int getChapterProgress(int chapter) {
        Map<Integer, Integer> progress = chapterProgress.getValue();
        return progress != null ? progress.getOrDefault(chapter, 0) : 0;
    }

    public ChapterProgress getChapterProgressObject(int chapter) {
        int completedLevels = getChapterProgress(chapter);
        int maxLevels = getMaxLevelsForChapter(chapter);
        return new ChapterProgress(chapter, completedLevels, maxLevels);
    }

    public void markChapterQuizCompleted(int chapter) {
        String quizKey = "chapter_" + chapter + "_quiz";

        Map<String, Boolean> currentMap = quizCompletion.getValue();
        if (currentMap == null) currentMap = new HashMap<>();

        // Copy and modify (important for LiveData to trigger observers)
        Map<String, Boolean> updatedMap = new HashMap<>(currentMap);
        updatedMap.put(quizKey, true);

        quizCompletion.setValue(updatedMap); // notify observers
        Log.d("ProgressDebug", "Chapter " + chapter + " quiz marked as completed");
    }


    public void markQuizCompleted(int chapter) {
        String quizKey = "chapter_" + chapter + "_quiz";
        Map<String, Boolean> currentMap = quizCompletion.getValue();
        if (currentMap == null) currentMap = new HashMap<>();

        // Create a new map to ensure LiveData triggers observers
        Map<String, Boolean> updatedMap = new HashMap<>(currentMap);
        updatedMap.put(quizKey, true);

        quizCompletion.setValue(updatedMap);
        Log.d("QuizDebug", "✅ Quiz marked completed for chapter " + chapter + " - Key: " + quizKey);
        Log.d("QuizDebug", "Current quiz states: " + updatedMap.toString());

        // Save to Supabase if needed
        saveQuizCompletionToSupabase(chapter);
    }
    private void saveQuizCompletionToSupabase(int chapter) {
        backgroundExecutor.execute(() -> {
            try {
                // Update user profile in Supabase
                boolean success = SupabaseJavaService.updateQuizCompletion(chapter, true);
                if (success) {
                    Log.d("QuizDebug", "Quiz completion saved to Supabase for chapter " + chapter);

                    // Also update total quizzes passed
                    updateTotalQuizzesPassed();
                } else {
                    Log.e("QuizDebug", "Failed to save quiz completion to Supabase");
                }
            } catch (Exception e) {
                Log.e("QuizDebug", "Error saving quiz completion: " + e.getMessage());
            }
        });
    }

    private void updateTotalQuizzesPassed() {
        backgroundExecutor.execute(() -> {
            try {
                int totalQuizzes = calculateTotalQuizzesPassed();
                boolean success = SupabaseJavaService.updateTotalQuizzesPassed(totalQuizzes);
                if (success) {
                    Log.d("QuizDebug", "Total quizzes passed updated: " + totalQuizzes);
                }
            } catch (Exception e) {
                Log.e("QuizDebug", "Error updating total quizzes passed: " + e.getMessage());
            }
        });
    }

    private int calculateTotalQuizzesPassed() {
        int total = 0;
        Map<String, Boolean> quizMap = quizCompletion.getValue();
        if (quizMap != null) {
            for (boolean completed : quizMap.values()) {
                if (completed) total++;
            }
        }
        return total;
    }


    public boolean isQuizCompleted(int chapter) {
        String quizKey = "chapter_" + chapter + "_quiz";
        Map<String, Boolean> completion = quizCompletion.getValue();
        boolean completed = completion != null && Boolean.TRUE.equals(completion.get(quizKey));
        Log.d("QuizDebug", "Chapter " + chapter + " quiz completed: " + completed);
        return completed;
    }

    public LiveData<Map<String, Boolean>> getQuizCompletion() {
        return quizCompletion;
    }

    public void setCurrentProgress(int chapter, int level) {
        currentChapter.setValue(chapter);
        currentLevel.setValue(level);
    }
    public LiveData<Map<Integer, Integer>> getChapterProgress() {
        return chapterProgress;
    }
    public LiveData<Integer> getCurrentChapter() {
        return currentChapter;
    }

    public LiveData<Integer> getCurrentLevel() {
        return currentLevel;
    }

    public void updateUserProfile(UserProfile profile) {
        userProfile.setValue(profile);
    }

    public LiveData<Boolean> getUsernameUpdateResult() {
        return usernameUpdateResult;
    }

    public LiveData<String> getUpdatedUsername() {
        return updatedUsername;
    }

    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public boolean shouldTakeQuiz(int chapter, int level) {
        return level % 5 == 0 && !isQuizCompleted(chapter);
    }

    public void resetChapterProgress(int chapter) {
        Map<Integer, Integer> progress = chapterProgress.getValue();
        if (progress != null) {
            progress.put(chapter, 0);
            chapterProgress.setValue(progress);
            updateTotalProgress();
        }
    }

    public void resetAllProgress() {
        initializeDefaultProgress();
        quizCompletion.setValue(new HashMap<>());
        currentChapter.setValue(1);
        currentLevel.setValue(1);
    }

    public static class ChapterProgress {
        private final int chapterNumber;
        private int completedLevels;
        private final int maxLevels;

        public ChapterProgress(int chapterNumber, int completedLevels, int maxLevels) {
            this.chapterNumber = chapterNumber;
            this.completedLevels = completedLevels;
            this.maxLevels = maxLevels;
        }

        public void updateProgress(int level) {
            if (level > completedLevels) {
                completedLevels = level;
            }
        }

        public int getProgressPercentage() {
            return maxLevels > 0 ? (completedLevels * 100) / maxLevels : 0;
        }

        public int getChapterNumber() { return chapterNumber; }
        public int getCompletedLevels() { return completedLevels; }
        public int getMaxLevels() { return maxLevels; }
    }

    public static class UserProfile {
        private String username = "User";
        private String email = "";
        private int totalChaptersCompleted = 0;
        private int totalQuizzesPassed = 0;
        private long joinDate = System.currentTimeMillis();

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public int getTotalChaptersCompleted() { return totalChaptersCompleted; }
        public void setTotalChaptersCompleted(int totalChaptersCompleted) { this.totalChaptersCompleted = totalChaptersCompleted; }

        public int getTotalQuizzesPassed() { return totalQuizzesPassed; }
        public void setTotalQuizzesPassed(int totalQuizzesPassed) { this.totalQuizzesPassed = totalQuizzesPassed; }

        public long getJoinDate() { return joinDate; }
        public void setJoinDate(long joinDate) { this.joinDate = joinDate; }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up if needed
    }
}