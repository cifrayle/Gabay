package com.example.gabay.viewmodels;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.services.SupabaseService;

import java.util.HashMap;
import java.util.Map;

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

    // Current active chapter and level
    private final MutableLiveData<Integer> currentChapter;
    private final MutableLiveData<Integer> currentLevel;

    // Total progress
    private final MutableLiveData<Integer> totalProgress;

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
    }

    /**
     * Initialize default progress for all chapters
     */
    private void initializeDefaultProgress() {
        Map<Integer, Integer> progress = new HashMap<>();

        // Initialize progress for 5 chapters
        for (int i = 1; i <= 5; i++) {
            progress.put(i, 0);
        }

        chapterProgress.setValue(progress);
        updateTotalProgress();
    }

    /**
     * Update progress for a specific chapter and level
     */
    public void updateChapterProgress(int chapter, int level) {
        Log.d("ProgressDebug", "=== UPDATING PROGRESS: Chapter " + chapter + ", Level " + level + " ===");

        // 1. Update local state immediately for responsive UI
        updateLocalProgress(chapter, level);

        // 2. Save to Supabase in background
        new Thread(() -> {
            try {
                Log.d("ProgressDebug", "Saving to Supabase...");
                boolean success = SupabaseJavaService.updateUserProgress(chapter, level); // i am having an error here

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
        }).start();
    }

    /**
     * Update local progress state
     */
    private void updateLocalProgress(int chapter, int level) {
        Map<Integer, Integer> currentProgress = chapterProgress.getValue();
        if (currentProgress != null) {
            // Update the completed levels for this chapter
            int currentCompleted = Math.max(currentProgress.getOrDefault(chapter, 0), level);
            currentProgress.put(chapter, currentCompleted);
            chapterProgress.setValue(currentProgress);

            Log.d("ProgressDebug", "📊 Local progress updated - Chapter " + chapter + ": " + currentCompleted + " levels");

            // Update total progress
            updateTotalProgress();
        }
    }

    /**
     * Calculate total progress percentage
     */
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

    private int getMaxLevelsForChapter(int chapter) {
        switch (chapter) {
            case 1: return 27;
            case 2: return 30;
            case 3: return 30;
            case 4: return 30;
            case 5: return 30;
            default: return 30;
        }
    }

    /**
     * Refresh progress from Supabase
     */
    public void refreshProgressFromSupabase() {
        Log.d("ProgressDebug", "🔄 Refreshing progress from Supabase...");

        new Thread(() -> {
            try {
                Map<Integer, Integer> supabaseProgress = new HashMap<>();

                // Fetch progress for each chapter from Supabase
                for (int chapter = 1; chapter <= 5; chapter++) {
                    int completedLevels = SupabaseJavaService.getCompletedLevelsCount(chapter); // i am having an error here
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
        }).start();
    }

    /**
     * Get total progress as LiveData
     */
    public LiveData<Integer> getTotalProgress() {
        return totalProgress;
    }

    /**
     * Get all chapter progress as LiveData
     */
    public LiveData<Map<Integer, Integer>> getAllChapterProgress() {
        return chapterProgress;
    }

    /**
     * Get progress for a specific chapter
     */
    public int getChapterProgress(int chapter) {
        Map<Integer, Integer> progress = chapterProgress.getValue();
        return progress != null ? progress.getOrDefault(chapter, 0) : 0;
    }

    /**
     * Get ChapterProgress object (for backward compatibility)
     */
    public ChapterProgress getChapterProgressObject(int chapter) {
        int completedLevels = getChapterProgress(chapter);
        int maxLevels = getMaxLevelsForChapter(chapter);
        return new ChapterProgress(chapter, completedLevels, maxLevels);
    }

    // === KEEP ALL YOUR EXISTING METHODS FOR BACKWARD COMPATIBILITY ===

    /**
     * Mark quiz as completed for a specific chapter and level
     */
    public void markQuizCompleted(int chapter, int level) {
        String quizKey = "quiz_" + chapter + "_" + level;
        Map<String, Boolean> completion = quizCompletion.getValue();
        if (completion != null) {
            completion.put(quizKey, true);
            quizCompletion.setValue(completion);
        }
    }

    /**
     * Check if quiz is completed for a specific chapter and level
     */
    public boolean isQuizCompleted(int chapter, int level) {
        String quizKey = "quiz_" + chapter + "_" + level;
        Map<String, Boolean> completion = quizCompletion.getValue();
        return completion != null && Boolean.TRUE.equals(completion.get(quizKey));
    }

    /**
     * Get quiz completion data
     */
    public LiveData<Map<String, Boolean>> getQuizCompletion() {
        return quizCompletion;
    }

    /**
     * Set current chapter and level
     */
    public void setCurrentProgress(int chapter, int level) {
        currentChapter.setValue(chapter);
        currentLevel.setValue(level);
    }

    /**
     * Get current chapter
     */
    public LiveData<Integer> getCurrentChapter() {
        return currentChapter;
    }

    /**
     * Get current level
     */
    public LiveData<Integer> getCurrentLevel() {
        return currentLevel;
    }

    /**
     * Update user profile
     */
    public void updateUserProfile(UserProfile profile) {
        userProfile.setValue(profile);
    }

    /**
     * Get user profile
     */
    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    /**
     * Check if user should take a quiz (every 5 levels)
     */
    public boolean shouldTakeQuiz(int chapter, int level) {
        return level % 5 == 0 && !isQuizCompleted(chapter, level);
    }

    /**
     * Reset progress for a specific chapter
     */
    public void resetChapterProgress(int chapter) {
        Map<Integer, Integer> progress = chapterProgress.getValue();
        if (progress != null) {
            progress.put(chapter, 0);
            chapterProgress.setValue(progress);
            updateTotalProgress();
        }
    }

    /**
     * Reset all progress
     */
    public void resetAllProgress() {
        initializeDefaultProgress();
        quizCompletion.setValue(new HashMap<>());
        currentChapter.setValue(1);
        currentLevel.setValue(1);
    }

    /**
     * Inner class for chapter progress (for backward compatibility)
     */
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

    /**
     * Inner class for user profile
     */
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
}