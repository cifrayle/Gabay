package com.example.gabay.services;

import android.util.Log;

import com.example.gabay.viewmodels.ProgressViewModel;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service dedicated to handling quiz completion and ensuring proper database synchronization
 * Works with ProgressViewModel to maintain consistency between local state and Supabase
 */
public class QuizCompletionService {
    private static final String TAG = "QuizCompletionService";
    private static final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    /**
     * Complete a quiz and ensure all necessary updates are made
     * This is the main method that should be called when a user passes a quiz
     */
    public static void completeQuiz(int chapter, int score, int totalQuestions, ProgressViewModel progressViewModel) {
        if (progressViewModel == null) {
            Log.e(TAG, "ProgressViewModel is null, cannot complete quiz");
            return;
        }

        int passingScore = (int) Math.ceil(totalQuestions * 0.7);
        boolean quizPassed = score >= passingScore;

        Log.d(TAG, "=== QUIZ COMPLETION SERVICE ===");
        Log.d(TAG, "Chapter: " + chapter + " | Score: " + score + "/" + totalQuestions);
        Log.d(TAG, "Passing Score: " + passingScore + " | Quiz Passed: " + quizPassed);

        if (!quizPassed) {
            Log.d(TAG, "Quiz failed, no database updates needed");
            return;
        }

        // Step 1: Mark quiz as completed in ProgressViewModel (this handles Supabase sync)
        progressViewModel.markQuizCompleted(chapter);
        Log.d(TAG, "✅ Quiz marked as completed in ProgressViewModel");

        // Step 2: Ensure chapter progress is at maximum level
        int maxLevel = progressViewModel.getMaxLevelsForChapter(chapter);
        progressViewModel.updateChapterProgress(chapter, maxLevel);
        Log.d(TAG, "✅ Chapter progress updated to max level: " + maxLevel);

        // Step 3: Track analytics in background
        trackQuizAnalytics(chapter, score, totalQuestions, progressViewModel);

        // Step 4: Verify database synchronization
        verifyQuizCompletionSync(chapter);
    }

    /**
     * Track quiz completion analytics
     */
    private static void trackQuizAnalytics(int chapter, int score, int totalQuestions, ProgressViewModel progressViewModel) {
        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Tracking quiz analytics for chapter " + chapter);

                // Track session data (approximate 5 minutes per quiz)
                boolean sessionSuccess = UserAnalyticsService.updateSessionData(5);
                Log.d(TAG, "Session tracking: " + (sessionSuccess ? "SUCCESS" : "FAILED"));

                // Check if this quiz completion also completed the entire chapter
                boolean isChapterCompleted = progressViewModel.isChapterCompleted(chapter);
                if (isChapterCompleted) {
                    boolean chapterSuccess = UserAnalyticsService.trackChapterCompletion(chapter);
                    Log.d(TAG, "Chapter completion tracking: " + (chapterSuccess ? "SUCCESS" : "FAILED"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error tracking quiz analytics: " + e.getMessage());
            }
        });
    }

    /**
     * Verify that quiz completion is properly synced to database
     */
    private static void verifyQuizCompletionSync(int chapter) {
        backgroundExecutor.execute(() -> {
            try {
                // Wait a moment for the database operation to complete
                Thread.sleep(1000);

                // Verify the quiz is marked as completed in Supabase
                boolean isCompleted = SupabaseJavaService.isQuizCompleted(chapter);
                Log.d(TAG, "Quiz completion verification for chapter " + chapter + ": " + 
                      (isCompleted ? "✅ CONFIRMED" : "❌ NOT FOUND"));

                if (!isCompleted) {
                    Log.w(TAG, "Quiz completion not found in database, attempting retry...");
                    // Retry the database update
                    boolean retrySuccess = SupabaseJavaService.updateQuizCompletion(chapter, true);
                    Log.d(TAG, "Retry result: " + (retrySuccess ? "SUCCESS" : "FAILED"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error verifying quiz completion sync: " + e.getMessage());
            }
        });
    }

    /**
     * Force refresh quiz completion data from database
     * Useful when returning to ProfilePage or when data seems out of sync
     */
    public static void refreshQuizCompletionFromDatabase(ProgressViewModel progressViewModel) {
        if (progressViewModel == null) {
            Log.e(TAG, "ProgressViewModel is null, cannot refresh quiz data");
            return;
        }

        Log.d(TAG, "Refreshing quiz completion data from database...");
        progressViewModel.loadQuizCompletionFromSupabase();
    }

    /**
     * Check if a specific quiz is completed (with database verification)
     */
    public static void verifyQuizCompletion(int chapter, QuizCompletionCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                boolean isCompleted = SupabaseJavaService.isQuizCompleted(chapter);
                Log.d(TAG, "Quiz completion check for chapter " + chapter + ": " + isCompleted);
                
                if (callback != null) {
                    callback.onResult(chapter, isCompleted);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking quiz completion: " + e.getMessage());
                if (callback != null) {
                    callback.onError(chapter, e);
                }
            }
        });
    }

    /**
     * Get total number of completed quizzes from database
     */
    public static void getTotalCompletedQuizzes(TotalQuizzesCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                int totalCompleted = 0;
                for (int chapter = 1; chapter <= 5; chapter++) {
                    if (SupabaseJavaService.isQuizCompleted(chapter)) {
                        totalCompleted++;
                    }
                }
                
                Log.d(TAG, "Total completed quizzes from database: " + totalCompleted);
                
                if (callback != null) {
                    callback.onResult(totalCompleted);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting total completed quizzes: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Reset all quiz completion data (useful for testing or user reset)
     */
    public static void resetAllQuizzes(ProgressViewModel progressViewModel) {
        if (progressViewModel == null) {
            Log.e(TAG, "ProgressViewModel is null, cannot reset quizzes");
            return;
        }

        Log.d(TAG, "Resetting all quiz completion data...");
        
        backgroundExecutor.execute(() -> {
            try {
                // Reset each quiz in database
                for (int chapter = 1; chapter <= 5; chapter++) {
                    boolean success = SupabaseJavaService.updateQuizCompletion(chapter, false);
                    Log.d(TAG, "Reset chapter " + chapter + " quiz: " + (success ? "SUCCESS" : "FAILED"));
                }
                
                // Refresh local data
                progressViewModel.loadQuizCompletionFromSupabase();
                
            } catch (Exception e) {
                Log.e(TAG, "Error resetting quiz data: " + e.getMessage());
            }
        });
    }

    // Callback interfaces
    public interface QuizCompletionCallback {
        void onResult(int chapter, boolean isCompleted);
        void onError(int chapter, Exception error);
    }

    public interface TotalQuizzesCallback {
        void onResult(int totalCompleted);
        void onError(Exception error);
    }
}
