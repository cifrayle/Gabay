package com.example.gabay.fragments.chapters;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gabay.R;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.activities.QuizActivity;
import com.example.gabay.viewmodels.ProgressViewModel;

public class Chapter1 extends Fragment {

    Button btn_chapter_quiz;
    private static final int[] BUTTON_IDS = {
            R.id.lvl1, R.id.lvl2, R.id.lvl3, R.id.lvl4, R.id.lvl5,
            R.id.lvl6, R.id.lvl7, R.id.lvl8, R.id.lvl9, R.id.lvl10,
            R.id.lvl11, R.id.lvl12, R.id.lvl13, R.id.lvl14, R.id.lvl15,
            R.id.lvl16, R.id.lvl17, R.id.lvl18, R.id.lvl19, R.id.lvl20,
            R.id.lvl21, R.id.lvl23, R.id.lvl24, R.id.lvl25,
            R.id.lvl26, R.id.lvl27, R.id.lvl28
    };

    private static final int[] TITLE_IDS = {
            R.id.levelTitle1, R.id.levelTitle2, R.id.levelTitle3, R.id.levelTitle4, R.id.levelTitle5,
            R.id.levelTitle6, R.id.levelTitle7, R.id.levelTitle8, R.id.levelTitle9, R.id.levelTitle10,
            R.id.levelTitle11, R.id.levelTitle12, R.id.levelTitle13, R.id.levelTitle14, R.id.levelTitle15,
            R.id.levelTitle16, R.id.levelTitle17, R.id.levelTitle18, R.id.levelTitle19, R.id.levelTitle20,
            R.id.levelTitle21,  R.id.levelTitle23, R.id.levelTitle24, R.id.levelTitle25,
            R.id.levelTitle26, R.id.levelTitle27, R.id.levelTitle28
    };

    private static int currentLessonLevel = -1;
    private View view;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "Chapter1Progress";
    private ProgressViewModel progressViewModel;

    private static final int LESSON_ACTIVITY_REQUEST = 1001;

    public static Chapter1 newInstance() {
        Chapter1 fragment = new Chapter1();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        // Initialize ProgressViewModel
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);

        preferences = requireContext().getSharedPreferences(PREF_NAME, getContext().MODE_PRIVATE);

        initializeQuizButton();
        updateLevelStates();
        addResetButton();

        // Set up click listeners for all buttons
        for (int i = 0; i < BUTTON_IDS.length; i++) {
            Button button = view.findViewById(BUTTON_IDS[i]);
            TextView titleView = view.findViewById(TITLE_IDS[i]);
            int levelNumber = i + 1;

            if (button != null && titleView != null) {
                // For locked buttons, use OnTouchListener to capture clicks
                if (!isLevelUnlocked(levelNumber)) {
                    button.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            showLockedLevelToast(levelNumber);
                        }
                        return true;
                    });
                } else {
                    // For unlocked buttons, use normal click listener
                    button.setOnClickListener(v -> {
                        String levelTitle = titleView.getText().toString();
                        Intent intent = new Intent(getActivity(), LessonActivity.class);
                        intent.putExtra("chapter", 1);
                        intent.putExtra("level", levelNumber);
                        intent.putExtra("levelTitle", levelTitle);
                        startActivityForResult(intent, LESSON_ACTIVITY_REQUEST);
                    });
                }
            }
        }
    }

    private void showLockedLevelToast(int levelNumber) {
        // Check why the level is locked to give a helpful message
        if (levelNumber == 1) {
            Toast.makeText(getContext(), "Level 1 should be unlocked! Try resetting progress.", Toast.LENGTH_SHORT).show();
        } else {
            int previousLevel = levelNumber - 1;
            Toast.makeText(getContext(), "Level " + levelNumber + " is locked! Complete Level " + previousLevel + " first.", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle the result when returning from LessonActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LESSON_ACTIVITY_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                int completedLevel = data.getIntExtra("completed_level", -1);
                int score = data.getIntExtra("score", 0);
                boolean levelCompleted = data.getBooleanExtra("level_completed", false);

                if (completedLevel != -1 && levelCompleted) {
                    // Update progress in both SharedPreferences and ViewModel
                    markLevelAsFinished(completedLevel, score);

                    // Unlock next level if exists
                    if (completedLevel < BUTTON_IDS.length) {
                        unlockLevel(completedLevel + 1);
                    }

                    // Check if user should take a quiz
                    if (progressViewModel.shouldTakeQuiz(1, completedLevel)) {
                        showQuizNotification(completedLevel);
                    }
                }
            }
            // Update the UI
            updateLevelStates();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt1, container, false);
    }

    private void updateLevelStates() {
        for (int i = 0; i < BUTTON_IDS.length; i++) {
            Button button = view.findViewById(BUTTON_IDS[i]);
            int levelNumber = i + 1;

            if (button != null) {
                LevelState state = getLevelState(levelNumber);

                switch (state) {
                    case FINISHED:
                        button.setBackgroundResource(R.drawable.circular_button);
                        button.setEnabled(true);
                        break;
                    case UNLOCKED:
                        button.setBackgroundResource(R.drawable.circular_button);
                        button.setEnabled(true);
                        break;
                    case LOCKED:
                        button.setBackgroundResource(R.drawable.circular_button_locked);
                        break;
                }
            }
        }
    }

    private LevelState getLevelState(int level) {
        // Level is unlocked if it's been reached OR if previous level is finished
        boolean levelReached = preferences.getBoolean("level_" + level + "_reached", false);
        boolean previousLevelFinished = level > 1 ? preferences.getBoolean("level_" + (level-1) + "_finished", false) : true;

        if (!levelReached && !previousLevelFinished) {
            return LevelState.LOCKED;
        }

        // Check if current level is finished
        boolean currentLevelFinished = preferences.getBoolean("level_" + level + "_finished", false);
        return currentLevelFinished ? LevelState.FINISHED : LevelState.UNLOCKED;
    }

    private boolean isLevelUnlocked(int level) {
        if (level == 1) return true;

        // Level is unlocked if previous level is finished OR if user has reached it before
        boolean previousLevelFinished = preferences.getBoolean("level_" + (level-1) + "_finished", false);
        boolean levelReached = preferences.getBoolean("level_" + level + "_reached", false);

        return previousLevelFinished || levelReached;
    }

    // Updated method to mark level as finished with score
    public void markLevelAsFinished(int level, int score) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_finished", true);
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();

        // Update ProgressViewModel
        if (progressViewModel != null) {
            progressViewModel.updateChapterProgress(1, level);

            // Update user profile achievements
            updateUserAchievements(level);
        }

        // Update the UI to reflect changes
        updateLevelStates();
    }

    private void unlockLevel(int level) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();
    }

    private void updateUserAchievements(int level) {
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            // FIX: Use getChapterProgressObject() instead of getChapterProgress()
            ProgressViewModel.ChapterProgress chapterProgress = progressViewModel.getChapterProgressObject(1);
            if (chapterProgress != null) {
                Log.d("ProgressDebug", "Chapter progress - Completed: " + chapterProgress.getCompletedLevels() +
                        "/" + chapterProgress.getMaxLevels());

                if (chapterProgress.getCompletedLevels() >= BUTTON_IDS.length) {
                    profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
                    Log.d("ProgressDebug", "Chapter completed! Total chapters: " + profile.getTotalChaptersCompleted());
                }
            }

            // Update quizzes passed if this was a quiz level
            if (level % 5 == 0) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                // Mark quiz as completed in ViewModel
                progressViewModel.markQuizCompleted(1, level);
            }

            progressViewModel.updateUserProfile(profile);
        }
    }

    private void showQuizNotification(int level) {
        Toast.makeText(requireContext(),
                "Quiz available! You've reached Level " + level + " - take the quiz to test your knowledge!",
                Toast.LENGTH_LONG).show();
    }

    // Call this method to reset progress (for testing or in settings)
    private void addResetButton() {
        Button resetBtn = new Button(requireContext());
        resetBtn.setText("Reset Progress");
        resetBtn.setBackgroundColor(getResources().getColor(R.color.transparentBackground));
        resetBtn.setTextColor(getResources().getColor(R.color.secondaryColor));

        // Add to your layout - adjust based on your layout structure
        LinearLayout layout = view.findViewById(R.id.reset_progress);
        layout.addView(resetBtn);

        resetBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Reset Progress")
                    .setMessage("Reset all progress and start from level 1?")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        resetChapterProgress();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void resetChapterProgress() {
        // Clear SharedPreferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        // Reset ProgressViewModel for this chapter
        if (progressViewModel != null) {
            progressViewModel.resetChapterProgress(1);
        }

        updateLevelStates();
        Toast.makeText(getContext(), "Progress reset!", Toast.LENGTH_SHORT).show();
    }

    private void initializeQuizButton() {
        btn_chapter_quiz = view.findViewById(R.id.btn_chapter_quiz);
        if (btn_chapter_quiz != null) {
            btn_chapter_quiz.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openQuizActivity();
                }
            });
        }
    }

    private void openQuizActivity() {
        try {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("chapter_number", 1);
            startActivity(intent);

            Log.d("Chapter1", "QuizActivity started successfully");

        } catch (Exception e) {
            Log.e("Chapter1", "Error starting QuizActivity: " + e.getMessage(), e);
            // Show a toast or error message to the user
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Error opening quiz", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update level states when returning to this fragment
        updateLevelStates();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showActionBarWithTitle("Chapter 1");
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        currentLessonLevel = -1;
    }

    private enum LevelState {
        LOCKED, UNLOCKED, FINISHED
    }
}