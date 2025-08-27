package com.example.gabay.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel for managing user progress across chapters and levels
 * Supports future features: quizzes, progress tracking, achievements
 */
public class ProgressViewModel extends ViewModel {

    // Chapter progress tracking
    private final MutableLiveData<Map<Integer, ChapterProgress>> chapterProgress;
    
    // Quiz completion tracking
    private final MutableLiveData<Map<String, Boolean>> quizCompletion;
    
    // User profile data
    private final MutableLiveData<UserProfile> userProfile;
    
    // Current active chapter and level
    private final MutableLiveData<Integer> currentChapter;
    private final MutableLiveData<Integer> currentLevel;

    public ProgressViewModel() {
        chapterProgress = new MutableLiveData<>(new HashMap<>());
        quizCompletion = new MutableLiveData<>(new HashMap<>());
        userProfile = new MutableLiveData<>(new UserProfile());
        currentChapter = new MutableLiveData<>(1);
        currentLevel = new MutableLiveData<>(1);
        
        initializeDefaultProgress();
    }

    /**
     * Initialize default progress for all chapters
     */
    private void initializeDefaultProgress() {
        Map<Integer, ChapterProgress> progress = new HashMap<>();
        
        // Initialize progress for 5 chapters
        for (int i = 1; i <= 5; i++) {
            progress.put(i, new ChapterProgress(i, 0, 0));
        }
        
        chapterProgress.setValue(progress);
    }

    /**
     * Update progress for a specific chapter and level
     */
    public void updateChapterProgress(int chapter, int level, int score) {
        Map<Integer, ChapterProgress> progress = chapterProgress.getValue();
        if (progress != null && progress.containsKey(chapter)) {
            ChapterProgress chapterProgress = progress.get(chapter);
            if (chapterProgress != null) {
                chapterProgress.updateProgress(level, score);
                this.chapterProgress.setValue(progress);
            }
        }
    }

    /**
     * Get progress for a specific chapter
     */
    public ChapterProgress getChapterProgress(int chapter) {
        Map<Integer, ChapterProgress> progress = chapterProgress.getValue();
        return progress != null ? progress.get(chapter) : null;
    }

    /**
     * Get all chapter progress
     */
    public LiveData<Map<Integer, ChapterProgress>> getAllChapterProgress() {
        return chapterProgress;
    }

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
     * Get total progress across all chapters
     */
    public int getTotalProgress() {
        Map<Integer, ChapterProgress> progress = chapterProgress.getValue();
        if (progress == null) return 0;
        
        int totalProgress = 0;
        int totalChapters = progress.size();
        
        for (ChapterProgress chapter : progress.values()) {
            totalProgress += chapter.getProgressPercentage();
        }
        
        return totalChapters > 0 ? totalProgress / totalChapters : 0;
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
        Map<Integer, ChapterProgress> progress = chapterProgress.getValue();
        if (progress != null && progress.containsKey(chapter)) {
            progress.put(chapter, new ChapterProgress(chapter, 0, 0));
            chapterProgress.setValue(progress);
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
     * Inner class for chapter progress
     */
    public static class ChapterProgress {
        private final int chapterNumber;
        private int completedLevels;
        private int totalScore;
        private final int maxLevels = 15; // Assuming 15 levels per chapter

        public ChapterProgress(int chapterNumber, int completedLevels, int totalScore) {
            this.chapterNumber = chapterNumber;
            this.completedLevels = completedLevels;
            this.totalScore = totalScore;
        }

        public void updateProgress(int level, int score) {
            if (level > completedLevels) {
                completedLevels = level;
            }
            totalScore += score;
        }

        public int getProgressPercentage() {
            return (completedLevels * 100) / maxLevels;
        }

        public int getChapterNumber() { return chapterNumber; }
        public int getCompletedLevels() { return completedLevels; }
        public int getTotalScore() { return totalScore; }
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

