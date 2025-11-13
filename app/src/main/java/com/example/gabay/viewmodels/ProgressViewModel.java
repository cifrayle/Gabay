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

    private final MutableLiveData<Boolean> _progressUpdateEvent = new MutableLiveData<>();
    public LiveData<Boolean> getProgressUpdateEvent() { return _progressUpdateEvent; }

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
        loadQuizCompletionFromSupabase(); // Load quiz data once at startup
        
        // Don't run aggressive fix methods that overwrite database quiz completion states
        // The database is the source of truth for quiz completion
    }
    public void triggerProgressUpdate() {
        _progressUpdateEvent.setValue(true);
    }

    public void markProgressUpdateHandled() {
        _progressUpdateEvent.setValue(false);
    }

    // Add this convenience method
    public void refreshAllData() {
        refreshProgressFromSupabase();
        loadUserProfileFromSupabase();
        // Quiz completion is preserved and not refreshed to prevent flickering
    }

    // Method to refresh quiz completion data when needed (e.g., returning to ProfilePage)
    public void refreshQuizCompletionIfNeeded() {
        Map<String, Boolean> currentQuizData = quizCompletion.getValue();
        if (currentQuizData == null || currentQuizData.isEmpty()) {
            Log.d("QuizDebug", "No quiz data found, loading from database");
            loadQuizCompletionFromSupabase();
        } else {
            Log.d("QuizDebug", "Quiz data exists, checking for updates from database");
            loadQuizCompletionFromSupabasePreservingLocal();
        }
    }

    public void loadQuizCompletionFromSupabase() {
        backgroundExecutor.execute(() -> {
            try {
                // First, ensure the quiz completion fields exist in the database
                boolean fieldsInitialized = SupabaseJavaService.ensureQuizCompletionFields();
                if (!fieldsInitialized) {
                    Log.w("QuizDebug", "Warning: Could not initialize quiz completion fields");
                }

                Map<String, Boolean> quizStates = new HashMap<>();

                for (int chapter = 1; chapter <= 5; chapter++) {
                    boolean completed = SupabaseJavaService.isQuizCompleted(chapter);
                    quizStates.put("chapter_" + chapter + "_quiz", completed);
                    Log.d("QuizDebug", "Loaded quiz state for chapter " + chapter + ": " + completed);
                }
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
        updateLocalProgress(chapter, level);

        // Don't auto-mark quiz as completed when all levels are done
        // Quiz completion should only happen when user actually takes the quiz
        if (isChapterCompleted(chapter)) {
            Log.d("ProgressDebug", "🎯 Chapter " + chapter + " fully completed - Quiz now available");
        }
        
        triggerProgressUpdate();
        // save to supabase
        backgroundExecutor.execute(() -> {
            try {
                Log.d("ProgressDebug", "Saving to Supabase...");
                boolean success = SupabaseJavaService.updateUserProgress(chapter, level);

                if (success) {
                    Log.d("ProgressDebug", "✅ Progress saved to Supabase successfully");
                    // Don't refresh immediately - it overwrites quiz completion states
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
        int completedQuizzes = 0;
        int totalQuizzes = 5; // One quiz per chapter

        for (int chapter = 1; chapter <= 5; chapter++) {
            int completed = progress.getOrDefault(chapter, 0);
            int maxLevels = getMaxLevelsForChapter(chapter);

            totalCompleted += completed;
            totalPossible += maxLevels;

            // Check if quiz is completed for this chapter
            if (isQuizCompleted(chapter)) {
                completedQuizzes++;
            }

            Log.d("ProgressDebug", "Chapter " + chapter + ": " + completed + "/" + maxLevels + " levels, quiz: " + isQuizCompleted(chapter));
        }
        
        // Calculate level progress percentage (this is the main progress indicator)
        int levelPercentage = totalPossible > 0 ? (totalCompleted * 100) / totalPossible : 0;
        
        // Calculate quiz progress percentage  
        int quizPercentage = (completedQuizzes * 100) / totalQuizzes;
        
        // NEW LOGIC: Overall progress is based on level completion primarily
        // If all levels are completed (100%), show 100% progress
        // Otherwise, show the level progress percentage
        int overallPercentage = levelPercentage;
        
        totalProgress.setValue(overallPercentage);

        Log.d("ProgressDebug", "🎯 Progress breakdown:");
        Log.d("ProgressDebug", "   Levels: " + levelPercentage + "% (" + totalCompleted + "/" + totalPossible + ")");
        Log.d("ProgressDebug", "   Quizzes: " + quizPercentage + "% (" + completedQuizzes + "/" + totalQuizzes + ")");
        Log.d("ProgressDebug", "   Overall: " + overallPercentage + "% (based on level completion)");
        
        // Check if we've reached 100% completion (all levels done)
        if (overallPercentage >= 100) {
            Log.d("ProgressDebug", "🏆 MILESTONE REACHED: 100% level completion achieved!");
            
            // Additional check: if quizzes are also completed, note that too
            if (quizPercentage >= 100) {
                Log.d("ProgressDebug", "🎯 PERFECT COMPLETION: Both levels AND quizzes are 100%!");
            } else {
                Log.d("ProgressDebug", "📝 Note: Some quizzes may still be pending (" + quizPercentage + "%)");
            }
        }
    }
    /**
     * Checks if the user's total progress has reached 100%.
     * This is a synchronous check on the current value of the LiveData.
     * @return true if total progress is 100 or more, false otherwise.
     */
    public boolean isTotalProgressComplete() {
        Integer currentProgress = totalProgress.getValue();
        if (currentProgress == null) {
            // If the value hasn't been loaded yet, assume it's not complete.
            return false;
        }
        // Check if the progress is 100% or more (to be safe).
        return currentProgress >= 100;
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
                // Store current quiz completion to preserve it
                Map<String, Boolean> currentQuizCompletion = quizCompletion.getValue();
                if (currentQuizCompletion == null) currentQuizCompletion = new HashMap<>();
                
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
                    
                    // IMPORTANT: Don't overwrite quiz completion data during progress refresh
                    // Quiz completion is managed separately to prevent flickering
                    
                    updateTotalProgress(); // This will now include quiz completion in calculation
                    Log.d("ProgressDebug", "✅ Progress refreshed from Supabase (quiz data preserved)");
                });

            } catch (Exception e) {
                Log.e("ProgressDebug", "❌ Error refreshing progress from Supabase: " + e.getMessage());
            }
        });
    }

    /**
     * Load quiz completion from Supabase while preserving local data that hasn't been saved yet.
     * This prevents the refresh from overwriting recent quiz completions.
     */
    private void loadQuizCompletionFromSupabasePreservingLocal() {
        backgroundExecutor.execute(() -> {
            try {
                // First, ensure the quiz completion fields exist in the database
                boolean fieldsInitialized = SupabaseJavaService.ensureQuizCompletionFields();
                if (!fieldsInitialized) {
                    Log.w("QuizDebug", "Warning: Could not initialize quiz completion fields");
                }

                // Get current local data to preserve
                Map<String, Boolean> localQuizStates = quizCompletion.getValue();
                if (localQuizStates == null) localQuizStates = new HashMap<>();

                Map<String, Boolean> mergedQuizStates = new HashMap<>();

                for (int chapter = 1; chapter <= 5; chapter++) {
                    String quizKey = "chapter_" + chapter + "_quiz";
                    
                    // Check if we have recent local data that should be preserved
                    Boolean localCompleted = localQuizStates.get(quizKey);
                    
                    // Get database state
                    boolean dbCompleted = SupabaseJavaService.isQuizCompleted(chapter);
                    
                    // Preserve local state if it's true (recently completed) or use database state
                    boolean finalState = (localCompleted != null && localCompleted) ? localCompleted : dbCompleted;
                    
                    mergedQuizStates.put(quizKey, finalState);
                    
                    Log.d("QuizDebug", "Chapter " + chapter + " quiz - Local: " + localCompleted + 
                          ", DB: " + dbCompleted + ", Final: " + finalState);
                }
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    quizCompletion.setValue(mergedQuizStates);
                    Log.d("QuizDebug", "Quiz completion states merged and updated: " + mergedQuizStates.toString());
                });

            } catch (Exception e) {
                Log.e("ProgressViewModel", "Error loading quiz completion with preservation: " + e.getMessage());
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
        
        // Save to database
        saveQuizCompletionToSupabase(chapter);
        
        // Recalculate total progress to potentially trigger congratulatory dialog
        updateTotalProgress();
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

        // Save to Supabase
        saveQuizCompletionToSupabase(chapter);
        
        // IMPORTANT: Recalculate total progress after quiz completion
        // This ensures the congratulatory dialog can trigger if this was the final quiz
        updateTotalProgress();
        
        // Trigger progress update event to notify observers
        triggerProgressUpdate();
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

    /**
     * Fix existing progress by marking quizzes as completed for chapters that are fully finished.
     * Call this method if you have completed all levels but progress shows less than 100%.
     * This method can be called manually or automatically when data loads.
     */
    public void fixExistingProgress() {
        Log.d("ProgressDebug", "🔧 Fixing existing progress - Checking for completed chapters...");
        
        Map<Integer, Integer> progress = chapterProgress.getValue();
        if (progress == null) return;
        
        boolean anyFixes = false;
        
        for (int chapter = 1; chapter <= 5; chapter++) {
            int completedLevels = progress.getOrDefault(chapter, 0);
            int maxLevels = getMaxLevelsForChapter(chapter);
            
            // If all levels in this chapter are completed but quiz isn't marked, fix it
            if (completedLevels >= maxLevels && !isQuizCompleted(chapter)) {
                Log.d("ProgressDebug", "🔧 Chapter " + chapter + " is fully completed but quiz not marked - Fixing...");
                markChapterQuizCompleted(chapter);
                anyFixes = true;
            }
        }
        
        if (anyFixes) {
            // Recalculate progress after fixes
            updateTotalProgress();
            Log.d("ProgressDebug", "✅ Progress fixes applied - Check if progress is now 100%");
        } else {
            Log.d("ProgressDebug", "✅ No fixes needed - Progress is already correct");
        }
    }

    /**
     * Force mark all completed chapters' quizzes as completed.
     * This method should be called if you have completed all levels but quizzes show 0%.
     */
    public void forceMarkCompletedChapterQuizzes() {
        Log.d("ProgressDebug", "🔧 Force marking completed chapter quizzes...");
        
        Map<Integer, Integer> progress = chapterProgress.getValue();
        if (progress == null) {
            Log.w("ProgressDebug", "❌ Chapter progress is null - cannot force mark quizzes");
            return;
        }
        
        Log.d("ProgressDebug", "Current chapter progress: " + progress.toString());
        
        for (int chapter = 1; chapter <= 5; chapter++) {
            int completedLevels = progress.getOrDefault(chapter, 0);
            int maxLevels = getMaxLevelsForChapter(chapter);
            boolean currentlyCompleted = isQuizCompleted(chapter);
            
            Log.d("ProgressDebug", "Chapter " + chapter + ": " + completedLevels + "/" + maxLevels + 
                  " levels, quiz currently completed: " + currentlyCompleted);
            
            // If all levels in this chapter are completed but quiz is not marked, mark it
            if (completedLevels >= maxLevels && !currentlyCompleted) {
                Log.d("ProgressDebug", "🔧 Chapter " + chapter + " is fully completed but quiz not marked - Force marking quiz as completed");
                markQuizCompleted(chapter);
            } else if (completedLevels >= maxLevels && currentlyCompleted) {
                Log.d("ProgressDebug", "✅ Chapter " + chapter + " is fully completed and quiz already marked");
            } else {
                Log.d("ProgressDebug", "⏳ Chapter " + chapter + " is not fully completed yet");
            }
        }
        
        // Recalculate progress after fixes
        updateTotalProgress();
        Log.d("ProgressDebug", "✅ Force quiz completion applied - Check if progress is now 100%");
    }

    /**
     * Manual fix for quiz completion - call this if quizzes still show 0% after completing all levels.
     * This method will immediately mark all completed chapters' quizzes as completed and save to database.
     */
    public void manualFixQuizCompletion() {
        Log.d("ProgressDebug", "🔧 MANUAL FIX: Forcing quiz completion for all completed chapters...");
        
        // Force mark all completed chapters as having completed quizzes
        for (int chapter = 1; chapter <= 5; chapter++) {
            int completedLevels = getChapterProgress(chapter);
            int maxLevels = getMaxLevelsForChapter(chapter);
            boolean currentlyCompleted = isQuizCompleted(chapter);
            
            Log.d("ProgressDebug", "🔧 MANUAL FIX: Chapter " + chapter + " - Levels: " + completedLevels + "/" + maxLevels + ", Quiz completed: " + currentlyCompleted);
            
            if (completedLevels >= maxLevels) {
                Log.d("ProgressDebug", "🔧 MANUAL FIX: Chapter " + chapter + " is fully completed - FORCING quiz completion");
                markQuizCompleted(chapter);
            } else {
                Log.d("ProgressDebug", "🔧 MANUAL FIX: Chapter " + chapter + " not fully completed yet (" + completedLevels + "/" + maxLevels + ")");
            }
        }
        
        // Force refresh all data after a short delay to ensure database operations complete
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d("ProgressDebug", "🔧 MANUAL FIX: Refreshing all data from database...");
            refreshAllData();
        }, 2000);
        
        Log.d("ProgressDebug", "✅ MANUAL FIX: Quiz completion fix applied!");
    }

    /**
     * Manually sync all local quiz completion data to Supabase database.
     * Call this method to ensure quiz completion is properly saved.
     */
    public void syncQuizCompletionToDatabase() {
        Log.d("QuizDebug", "🔄 Manually syncing quiz completion to database...");
        
        backgroundExecutor.execute(() -> {
            Map<String, Boolean> localQuizStates = quizCompletion.getValue();
            if (localQuizStates == null) {
                Log.d("QuizDebug", "No local quiz data to sync");
                return;
            }
            
            int syncedCount = 0;
            for (int chapter = 1; chapter <= 5; chapter++) {
                String quizKey = "chapter_" + chapter + "_quiz";
                Boolean localCompleted = localQuizStates.get(quizKey);
                
                if (localCompleted != null && localCompleted) {
                    // Save to database
                    boolean success = SupabaseJavaService.updateQuizCompletion(chapter, true);
                    if (success) {
                        syncedCount++;
                        Log.d("QuizDebug", "✅ Synced chapter " + chapter + " quiz completion to database");
                    } else {
                        Log.e("QuizDebug", "❌ Failed to sync chapter " + chapter + " quiz completion");
                    }
                }
            }
            
            Log.d("QuizDebug", "Sync completed: " + syncedCount + " quizzes synced to database");
        });
    }

    /**
     * Test method to simulate 100% completion by setting all chapters and quizzes as completed.
     * This can be called for testing the congratulatory dialog.
     */
    public void simulateFullCompletion() {
        Log.d("ProgressDebug", "🧪 Simulating full completion for testing...");
        
        // Set all chapters to maximum levels
        Map<Integer, Integer> fullProgress = new HashMap<>();
        fullProgress.put(1, 28); // Chapter 1: 28 levels
        fullProgress.put(2, 5);  // Chapter 2: 5 levels
        fullProgress.put(3, 10); // Chapter 3: 10 levels
        fullProgress.put(4, 7);  // Chapter 4: 7 levels
        fullProgress.put(5, 7);  // Chapter 5: 7 levels
        
        chapterProgress.setValue(fullProgress);
        
        // Set all quizzes as completed
        Map<String, Boolean> fullQuizCompletion = new HashMap<>();
        fullQuizCompletion.put("chapter_1_quiz", true);
        fullQuizCompletion.put("chapter_2_quiz", true);
        fullQuizCompletion.put("chapter_3_quiz", true);
        fullQuizCompletion.put("chapter_4_quiz", true);
        fullQuizCompletion.put("chapter_5_quiz", true);
        
        quizCompletion.setValue(fullQuizCompletion);
        
        // Recalculate progress
        updateTotalProgress();
        
        Log.d("ProgressDebug", "🏆 Full completion simulated - Check if congratulatory dialog appears!");
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