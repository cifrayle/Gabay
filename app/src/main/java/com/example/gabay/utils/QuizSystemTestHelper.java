package com.example.gabay.utils;

import android.content.Context;
import android.util.Log;

import com.example.gabay.services.QuizCompletionService;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.viewmodels.ProgressViewModel;

/**
 * Test helper for verifying the quiz system is working correctly
 * Use this to test quiz completion, database synchronization, and ProfilePage updates
 */
public class QuizSystemTestHelper {
    private static final String TAG = "QuizSystemTestHelper";

    /**
     * Test quiz completion for a specific chapter
     */
    public static void testQuizCompletion(int chapter, ProgressViewModel progressViewModel) {
        Log.d(TAG, "=== TESTING QUIZ COMPLETION FOR CHAPTER " + chapter + " ===");
        
        if (progressViewModel == null) {
            Log.e(TAG, "ProgressViewModel is null!");
            return;
        }

        // Simulate a passing quiz (80% score)
        int totalQuestions = 10;
        int passingScore = 8;
        
        Log.d(TAG, "Simulating quiz completion with score: " + passingScore + "/" + totalQuestions);
        
        // Use QuizCompletionService to complete the quiz
        QuizCompletionService.completeQuiz(chapter, passingScore, totalQuestions, progressViewModel);
        
        // Wait a moment for processing
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                
                // Verify the quiz is marked as completed
                verifyQuizCompletion(chapter);
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Test interrupted: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Verify quiz completion in database
     */
    public static void verifyQuizCompletion(int chapter) {
        QuizCompletionService.verifyQuizCompletion(chapter, new QuizCompletionService.QuizCompletionCallback() {
            @Override
            public void onResult(int chapter, boolean isCompleted) {
                Log.d(TAG, "Chapter " + chapter + " quiz verification: " + 
                      (isCompleted ? "✅ COMPLETED" : "❌ NOT COMPLETED"));
            }

            @Override
            public void onError(int chapter, Exception error) {
                Log.e(TAG, "Error verifying chapter " + chapter + ": " + error.getMessage());
            }
        });
    }

    /**
     * Test all quiz completions
     */
    public static void testAllQuizzes(ProgressViewModel progressViewModel) {
        Log.d(TAG, "=== TESTING ALL QUIZ COMPLETIONS ===");
        
        for (int chapter = 1; chapter <= 5; chapter++) {
            final int currentChapter = chapter;
            
            // Stagger the tests to avoid overwhelming the system
            new Thread(() -> {
                try {
                    Thread.sleep(currentChapter * 1000); // 1 second delay between each
                    testQuizCompletion(currentChapter, progressViewModel);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Test interrupted for chapter " + currentChapter);
                }
            }).start();
        }
    }

    /**
     * Get comprehensive quiz status report
     */
    public static void getQuizStatusReport(ProgressViewModel progressViewModel) {
        Log.d(TAG, "=== QUIZ STATUS REPORT ===");
        
        if (progressViewModel == null) {
            Log.e(TAG, "ProgressViewModel is null!");
            return;
        }

        // Check local ViewModel state
        for (int chapter = 1; chapter <= 5; chapter++) {
            boolean localCompleted = progressViewModel.isQuizCompleted(chapter);
            Log.d(TAG, "Chapter " + chapter + " (Local): " + (localCompleted ? "✅" : "❌"));
        }

        // Check database state
        QuizCompletionService.getTotalCompletedQuizzes(new QuizCompletionService.TotalQuizzesCallback() {
            @Override
            public void onResult(int totalCompleted) {
                Log.d(TAG, "Total quizzes completed (Database): " + totalCompleted + "/5");
                
                // Individual chapter verification
                for (int chapter = 1; chapter <= 5; chapter++) {
                    verifyQuizCompletion(chapter);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error getting total quiz count: " + error.getMessage());
            }
        });
    }

    /**
     * Reset all quizzes for testing
     */
    public static void resetAllQuizzesForTesting(ProgressViewModel progressViewModel) {
        Log.d(TAG, "=== RESETTING ALL QUIZZES FOR TESTING ===");
        QuizCompletionService.resetAllQuizzes(progressViewModel);
    }

    /**
     * Test ProfilePage quiz count synchronization
     */
    public static void testProfilePageSync(ProgressViewModel progressViewModel) {
        Log.d(TAG, "=== TESTING PROFILE PAGE SYNC ===");
        
        // Refresh quiz data
        QuizCompletionService.refreshQuizCompletionFromDatabase(progressViewModel);
        
        // Wait for data to load
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                
                // Get status report
                getQuizStatusReport(progressViewModel);
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Profile sync test interrupted: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Test database connectivity and authentication
     */
    public static void testDatabaseConnection() {
        Log.d(TAG, "=== TESTING DATABASE CONNECTION ===");
        
        if (!SupabaseJavaService.isAuthenticated()) {
            Log.e(TAG, "❌ User not authenticated with Supabase!");
            return;
        }
        
        Log.d(TAG, "✅ User is authenticated with Supabase");
        
        // Test database read
        new Thread(() -> {
            try {
                boolean testResult = SupabaseJavaService.isQuizCompleted(1);
                Log.d(TAG, "✅ Database read test successful");
                
                // Test database write (toggle and toggle back)
                boolean originalState = testResult;
                boolean writeTest1 = SupabaseJavaService.updateQuizCompletion(1, !originalState);
                Thread.sleep(1000);
                boolean writeTest2 = SupabaseJavaService.updateQuizCompletion(1, originalState);
                
                if (writeTest1 && writeTest2) {
                    Log.d(TAG, "✅ Database write test successful");
                } else {
                    Log.e(TAG, "❌ Database write test failed");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Database test error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Run comprehensive quiz system tests
     */
    public static void runComprehensiveTests(ProgressViewModel progressViewModel) {
        Log.d(TAG, "=== RUNNING COMPREHENSIVE QUIZ SYSTEM TESTS ===");
        
        // Test 1: Database connection
        testDatabaseConnection();
        
        // Test 2: Current status
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                getQuizStatusReport(progressViewModel);
                
                Thread.sleep(2000);
                // Test 3: ProfilePage sync
                testProfilePageSync(progressViewModel);
                
                Log.d(TAG, "=== COMPREHENSIVE TESTS COMPLETED ===");
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Comprehensive test interrupted: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Simple method to mark a specific quiz as completed for testing
     */
    public static void markQuizCompleted(int chapter, ProgressViewModel progressViewModel) {
        Log.d(TAG, "Marking chapter " + chapter + " quiz as completed for testing...");
        QuizCompletionService.completeQuiz(chapter, 8, 10, progressViewModel);
    }
}
