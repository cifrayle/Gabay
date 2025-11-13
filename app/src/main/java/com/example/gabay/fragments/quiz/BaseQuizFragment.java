package com.example.gabay.fragments.quiz;


import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import android.widget.Button;

import com.example.gabay.R;
import com.example.gabay.services.QuizCompletionService;
import com.example.gabay.services.UserAnalyticsService;
import com.example.gabay.utils.SharedPreferenceHelper;
import com.example.gabay.viewmodels.ProgressViewModel;


public abstract class BaseQuizFragment extends Fragment {

    protected ProgressViewModel progressViewModel;
    protected int currentChapter;
    protected int currentLevel;
    private boolean quizInProgress;
    private boolean congratulationDialogShown = false;

    // Abstract methods that child fragments MUST implement with their specific UI logic
    protected abstract void updateUIForCompletion();
    protected abstract void cleanupQuizResources();
    protected abstract void setupNavigation();
    protected abstract void showCompletionFeedback();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);
        if (getArguments() != null) {
            currentChapter = getArguments().getInt("chapter", 0);
            currentLevel = getArguments().getInt("level", 0);
        }
        // Reset the congratulation flag when creating a new quiz
        congratulationDialogShown = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up any remaining observers when the view is destroyed
        if (progressViewModel != null) {
            progressViewModel.getTotalProgress().removeObservers(getViewLifecycleOwner());
        }
    }

    /**
     * This is the core shared logic. Any quiz fragment calls this.
     */
    protected void onQuizCompleted(int score, int totalQuestions) {
        if (getContext() == null) return;

        int passingScore = (int) Math.ceil(totalQuestions * 0.7);
        boolean quizPassed = score >= passingScore;
        quizInProgress = false;

        Log.d("BaseQuizDebug", "=== QUIZ COMPLETION (from Base) ===");
        Log.d("BaseQuizDebug", "Chapter: " + currentChapter + " | Score: " + score + "/" + totalQuestions);
        Log.d("BaseQuizDebug", "Quiz Passed: " + quizPassed);

        if (progressViewModel != null && quizPassed) {
            // --- Enhanced approach with better timing handling ---
            Log.d("BaseQuizDebug", "Setting up progress observer for 100% completion check");
            
            // Check current progress immediately in case it's already 100%
            Integer currentProgress = progressViewModel.getTotalProgress().getValue();
            Log.d("BaseQuizDebug", "Current progress before observer: " + currentProgress);
            
            if (currentProgress != null && currentProgress >= 100 && !congratulationDialogShown) {
                Log.d("BaseQuizDebug", "🏆 PROGRESS IS ALREADY 100%! Showing final dialog immediately.");
                showFinalCompletionDialog();
                congratulationDialogShown = true;
            } else {
                // Create an observer that will react to the progress change.
                progressViewModel.getTotalProgress().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<Integer>() {
                    @Override
                    public void onChanged(Integer progress) {
                        // This code will execute EVERY time the totalProgress changes.
                        Log.d("BaseQuizDebug", "Progress observer triggered: " + progress);
                        
                        if (progress != null && progress >= 100 && !congratulationDialogShown) {
                            // If progress hits 100, show the final dialog.
                            Log.d("BaseQuizDebug", "🏆 TOTAL PROGRESS IS NOW " + progress + "! Showing final dialog.");
                            showFinalCompletionDialog();
                            congratulationDialogShown = true;
                            // Important: Remove the observer to prevent it from firing again.
                            progressViewModel.getTotalProgress().removeObserver(this);
                        }
                    }
                });
            }

            // Use the new QuizCompletionService for comprehensive quiz completion handling
            QuizCompletionService.completeQuiz(currentChapter, score, totalQuestions, progressViewModel);

            // Show the regular completion UI immediately. If the observer triggers,
            // the final dialog will appear over it.
            updateUIForCompletion();

        } else {
            Log.d("BaseQuizDebug", "❌ Quiz failed or ViewModel is null. Showing regular completion UI.");
            updateUIForCompletion();
        }

        // These are always called, regardless of pass/fail
        showCompletionFeedback();
        cleanupQuizResources();
        setupNavigation();
    }



    private void showFinalCompletionDialog() {
        if (getContext() == null) {
            Log.e("BaseQuizDebug", "❌ Cannot show dialog - Context is null");
            return;
        }

        if (congratulationDialogShown) {
            Log.w("BaseQuizDebug", "⚠️ Dialog already shown, skipping duplicate");
            return;
        }

        try {
            Log.d("BaseQuizDebug", "🎉 Creating congratulatory dialog");
            
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.BlackTextDialog);
            builder.setTitle(R.string.final_completion_title);
            builder.setMessage(R.string.final_completion_message);
            builder.setPositiveButton(R.string.final_completion_button, (dialog, which) -> {
                Log.d("BaseQuizDebug", "✅ Congratulatory dialog dismissed by user");
                // Simple back button behavior
                requireActivity().onBackPressed();
            });
            builder.setCancelable(false);
            
            AlertDialog dialog = builder.create();
            dialog.show();

            // Style the positive button
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(getResources().getColor(R.color.secondaryColor));
                Log.d("BaseQuizDebug", " Dialog button styled successfully");
            }

            Log.d("BaseQuizDebug", " Congratulatory dialog displayed successfully!");
            
        } catch (Exception e) {
            Log.e("BaseQuizDebug", " Error showing congratulatory dialog: " + e.getMessage(), e);
        }
    }

}
