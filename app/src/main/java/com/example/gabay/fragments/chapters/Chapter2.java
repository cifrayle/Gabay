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

public class Chapter2 extends Fragment {

    Button btn_chapter_quiz;
    private static final int[] BUTTON_IDS = {
            R.id.c2lvl1, R.id.lvl2, R.id.lvl3, R.id.lvl4, R.id.lvl5
    };

    private static final int[] TITLE_IDS = {
            R.id.levelTitle1, R.id.levelTitle2, R.id.levelTitle3, R.id.levelTitle4, R.id.levelTitle5
    };

    private static int currentLessonLevel = -1;
    private View view;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "Chapter2Progress";
    private ProgressViewModel progressViewModel;

    private static final int LESSON_ACTIVITY_REQUEST = 1002;

    // Add level button array for easier management
    private Button[] levelButtons;

    public static Chapter2 newInstance() {
        Chapter2 fragment = new Chapter2();
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

        initializeLevelButtons();
        initializeQuizButton();
        updateLevelStates();
        addResetButton();
        setupProgressObserver();

        Log.d("Chapter2", "Chapter2 fragment initialized");
    }

    private void initializeLevelButtons() {
        levelButtons = new Button[BUTTON_IDS.length];

        for (int i = 0; i < BUTTON_IDS.length; i++) {
            Button button = view.findViewById(BUTTON_IDS[i]);
            TextView titleView = view.findViewById(TITLE_IDS[i]);
            int levelNumber = i + 1;

            levelButtons[i] = button;

            if (button != null && titleView != null) {
                setupLevelButton(button, titleView, levelNumber);
            }
        }
    }

    private void setupLevelButton(Button button, TextView titleView, int levelNumber) {
        // Set level number as text if not already set
        if (button.getText().toString().isEmpty()) {
            button.setText(String.valueOf(levelNumber));
        }

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
                intent.putExtra("chapter", 2);
                intent.putExtra("level", levelNumber);
                intent.putExtra("levelTitle", levelTitle);
                startActivityForResult(intent, LESSON_ACTIVITY_REQUEST);
            });
        }
    }

    private void showLockedLevelToast(int levelNumber) {
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
                    Log.d("Chapter2", "Level " + completedLevel + " completed with score: " + score);

                    // Update progress in both SharedPreferences and ViewModel
                    markLevelAsFinished(completedLevel, score);

                    // Unlock next level if exists
                    if (completedLevel < BUTTON_IDS.length) {
                        unlockLevel(completedLevel + 1);
                    }

                    // Check if user should take a quiz
                    if (progressViewModel.shouldTakeQuiz(2, completedLevel)) {
                        showQuizNotification(completedLevel);
                    }

                    // Check if chapter is completed
                    checkChapterCompletion();
                }
            }
            // Update the UI
            updateLevelStates();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt2, container, false);
    }

    private void updateLevelStates() {
        int completedLevels = progressViewModel.getChapterProgress(2);

        for (int i = 0; i < levelButtons.length; i++) {
            Button button = levelButtons[i];
            int levelNumber = i + 1;

            if (button != null) {
                // Remove any Material theme effects
                button.setBackgroundTintList(null);
                button.setStateListAnimator(null);
                button.setElevation(0f);

                if (levelNumber <= completedLevels) {
                    // Level completed
                    button.setBackgroundResource(R.drawable.circular_button);
                    button.setEnabled(true);
                } else if (levelNumber == completedLevels + 1) {
                    // Current level
                    button.setBackgroundResource(R.drawable.circular_button);
                    button.setEnabled(true);
                } else {
                    // Level locked
                    button.setBackgroundResource(R.drawable.circular_button_locked);
                    button.setEnabled(false);
                }

                // Ensure full opacity and remove any theme transparency
                button.setAlpha(1.0f);

                // Update button click behavior
                updateButtonClickListener(button, levelNumber);

                Log.d("Chapter2", "Level " + levelNumber + " state updated");
            }
        }
    }

    private void updateButtonClickListener(Button button, int levelNumber) {
        // Remove existing listeners
        button.setOnClickListener(null);
        button.setOnTouchListener(null);

        TextView titleView = view.findViewById(TITLE_IDS[levelNumber - 1]);

        if (isLevelUnlocked(levelNumber)) {
            // For unlocked buttons, use normal click listener
            button.setOnClickListener(v -> {
                if (titleView != null) {
                    String levelTitle = titleView.getText().toString();
                    Intent intent = new Intent(getActivity(), LessonActivity.class);
                    intent.putExtra("chapter", 2);
                    intent.putExtra("level", levelNumber);
                    intent.putExtra("levelTitle", levelTitle);
                    startActivityForResult(intent, LESSON_ACTIVITY_REQUEST);
                }
            });
        } else {
            // For locked buttons, show message
            button.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showLockedLevelToast(levelNumber);
                }
                return true;
            });
        }
    }

    private LevelState getLevelState(int level) {
        int completedLevels = progressViewModel.getChapterProgress(2);

        if (level <= completedLevels) {
            return LevelState.FINISHED;
        } else if (level == completedLevels + 1) {
            return LevelState.UNLOCKED;
        } else {
            return LevelState.LOCKED;
        }
    }

    private boolean isLevelUnlocked(int level) {
        int completedLevels = progressViewModel.getChapterProgress(2);
        return level <= completedLevels + 1; // Current level and all completed levels are unlocked
    }

    // Updated method to mark level as finished with score
    public void markLevelAsFinished(int level, int score) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_finished", true);
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();

        // Update ProgressViewModel - this is the key change
        if (progressViewModel != null) {
            progressViewModel.updateChapterProgress(2, level);
            Log.d("Chapter2", "Progress updated - Chapter 2, Level " + level);
        }

        // Update user profile achievements
        updateUserAchievements(level);
    }

    private void unlockLevel(int level) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();
        Log.d("Chapter2", "Level " + level + " unlocked");
    }

    private void updateUserAchievements(int level) {
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            ProgressViewModel.ChapterProgress chapterProgress = progressViewModel.getChapterProgressObject(2);
            if (chapterProgress != null) {
                Log.d("ProgressDebug", "Chapter 2 progress - Completed: " + chapterProgress.getCompletedLevels() +
                        "/" + chapterProgress.getMaxLevels());

                if (chapterProgress.getCompletedLevels() >= BUTTON_IDS.length) {
                    profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
                    Log.d("ProgressDebug", "Chapter 2 completed! Total chapters: " + profile.getTotalChaptersCompleted());
                }
            }

            // Update quizzes passed if this was a quiz level
            // For Chapter 2 with 5 levels, we can have a quiz at level 5
            if (level == 5) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                // Mark quiz as completed in ViewModel
                progressViewModel.markQuizCompleted(2, level);
                Log.d("Chapter2", "Quiz completed at level " + level);
            }

            progressViewModel.updateUserProfile(profile);
        }
    }

    private void checkChapterCompletion() {
        ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(2);
        if (progress != null && progress.getProgressPercentage() >= 100) {
            Toast.makeText(requireContext(), "Congratulations! Chapter 2 completed!", Toast.LENGTH_LONG).show();
            // Mark chapter quiz as completed
            progressViewModel.markChapterQuizCompleted(2); // Use the method that takes only chapter
        }
    }

    private void showQuizNotification(int level) {
        Toast.makeText(requireContext(),
                "Quiz available! You've reached Level " + level + " - take the quiz to test your knowledge!",
                Toast.LENGTH_LONG).show();
    }

    private void setupProgressObserver() {
        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (isAdded()) {
                updateLevelStates();
                Log.d("Chapter2", "Progress observer triggered - updating level states");
            }
        });
    }

    // Call this method to reset progress (for testing or in settings)
    private void addResetButton() {
        Button resetBtn = new Button(requireContext());
        resetBtn.setText("Reset Progress");
        resetBtn.setBackgroundColor(getResources().getColor(R.color.transparentBackground));
        resetBtn.setTextColor(getResources().getColor(R.color.secondaryColor));

        // Add to your layout - adjust based on your layout structure
        LinearLayout layout = view.findViewById(R.id.reset_progress);
        if (layout != null) {
            layout.addView(resetBtn);
        }

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
            progressViewModel.resetChapterProgress(2);
        }

        updateLevelStates();
        Toast.makeText(getContext(), "Progress reset!", Toast.LENGTH_SHORT).show();
        Log.d("Chapter2", "Chapter progress reset");
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
            intent.putExtra("chapter_number", 2);
            startActivity(intent);

            Log.d("Chapter2", "QuizActivity started successfully");

        } catch (Exception e) {
            Log.e("Chapter2", "Error starting QuizActivity: " + e.getMessage(), e);
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
            ((MainActivity) getActivity()).showActionBarWithTitle("Chapter 2");
        }
        Log.d("Chapter2", "Chapter2 resumed - updating UI");
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