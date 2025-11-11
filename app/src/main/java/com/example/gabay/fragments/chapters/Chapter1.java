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
            R.id.lvl21, R.id.lvl22, R.id.lvl23, R.id.lvl24, R.id.lvl25,
            R.id.lvl26, R.id.lvl27, R.id.lvl28
    };

    private static final int[] TITLE_IDS = {
            R.id.levelTitle1, R.id.levelTitle2, R.id.levelTitle3, R.id.levelTitle4, R.id.levelTitle5,
            R.id.levelTitle6, R.id.levelTitle7, R.id.levelTitle8, R.id.levelTitle9, R.id.levelTitle10,
            R.id.levelTitle11, R.id.levelTitle12, R.id.levelTitle13, R.id.levelTitle14, R.id.levelTitle15,
            R.id.levelTitle16, R.id.levelTitle17, R.id.levelTitle18, R.id.levelTitle19, R.id.levelTitle20,
            R.id.levelTitle21, R.id.levelTitle22, R.id.levelTitle23, R.id.levelTitle24, R.id.levelTitle25,
            R.id.levelTitle26, R.id.levelTitle27, R.id.levelTitle28
    };

    private static int currentLessonLevel = -1;
    //private int currentChapter = 1;
    private View view;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "Chapter1Progress";
    private ProgressViewModel progressViewModel;

    private static final int LESSON_ACTIVITY_REQUEST = 1001;

    // Add level button array for easier management
    private Button[] levelButtons;

    public static Chapter1 newInstance() {
        Chapter1 fragment = new Chapter1();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }
        this.view = view;

        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);
        preferences = requireContext().getSharedPreferences(PREF_NAME, getContext().MODE_PRIVATE);
        progressViewModel.getChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            updateLevelStates();
        });

        if (progressViewModel != null) {
            progressViewModel.refreshAllData();
        }

        setupQuizObservers();
        initializeLevelButtons();
        initializeQuizButton();
        //addResetButton();
        setupProgressObserver();

        Log.d("Chapter1", "Chapter1 fragment initialized");
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

    private void showLockedLevelToast(int levelNumber) {
        if (levelNumber == 1) {
            Toast.makeText(getContext(), "Level 1 should be unlocked! Try resetting progress.", Toast.LENGTH_SHORT).show();
        } else {
            int previousLevel = levelNumber - 1;
            Toast.makeText(getContext(), "Level " + levelNumber + " is locked! Complete Level " + previousLevel + " first.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LESSON_ACTIVITY_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                int completedLevel = data.getIntExtra("completed_level", -1);
                int score = data.getIntExtra("score", 0);
                boolean levelCompleted = data.getBooleanExtra("level_completed", false);

                if (completedLevel != -1 && levelCompleted) {
                    Log.d("Chapter1", "Level " + completedLevel + " completed with score: " + score);

                    markLevelAsFinished(completedLevel, score);

                    if (completedLevel < BUTTON_IDS.length) {
                        unlockLevel(completedLevel + 1);
                    }

                    // Check if user should take a quiz
                    if (progressViewModel.shouldTakeQuiz(1, completedLevel)) {
                        showQuizNotification(completedLevel);
                    }

                    checkChapterCompletion();
                }
            }
            updateLevelStates();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt1, container, false);
    }

    private void updateLevelStates() {
        int completedLevels = progressViewModel.getChapterProgress(1);
        boolean isChapterDone = progressViewModel.isChapterCompleted(1);

        for (int i = 0; i < levelButtons.length; i++) {
            Button button = levelButtons[i];
            int levelNumber = i + 1;

            if (button != null) {
                button.setBackgroundTintList(null);
                button.setStateListAnimator(null);
                button.setElevation(0f);
                button.setAlpha(1.0f);

                if (isChapterDone) {
                    button.setBackgroundResource(R.drawable.circular_button_finished);
                    button.setEnabled(true);

                } else if (levelNumber <= completedLevels) {
                    button.setBackgroundResource(R.drawable.circular_button_finished);
                    button.setEnabled(true);

                } else if (levelNumber == completedLevels + 1) {
                    button.setBackgroundResource(R.drawable.circular_button);
                    button.setEnabled(true);

                } else {
                    button.setBackgroundResource(R.drawable.circular_button_locked);
                    button.setEnabled(false);
                }

                updateButtonClickListener(button, levelNumber);
            }
        }
        updateQuizButtonState();
    }

    private void checkChapterCompletion() {
        ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(1);
        if (progress != null && progress.getProgressPercentage() >= 100) {
            Toast.makeText(requireContext(), "Congratulations! Chapter 1 completed! Quiz unlocked!", Toast.LENGTH_LONG).show();

            progressViewModel.markChapterQuizCompleted(1);
            updateQuizButtonState();
        }
    }

    private void setupProgressObserver() {
        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (isAdded()) {
                updateLevelStates();
                updateQuizButtonState();
                Log.d("Chapter1", "Progress observer triggered - updating level states");
            }
        });
    }

    private void updateQuizButtonState() {
        if (btn_chapter_quiz == null) return;

        // Reset button styling
        btn_chapter_quiz.setBackgroundTintList(null);
        btn_chapter_quiz.setStateListAnimator(null);
        btn_chapter_quiz.setElevation(0f);
        btn_chapter_quiz.setAlpha(1.0f);

        boolean isChapterDone = progressViewModel.isChapterCompleted(1);
        boolean isQuizDone = progressViewModel.isQuizCompleted(1);

        Log.d("QuizDebug", "Chapter 1 - Completed: " + isChapterDone + ", Quiz Done: " + isQuizDone);
        Log.d("QuizDebug", "Chapter 1 Progress: " + progressViewModel.getChapterProgress(1) + "/" + progressViewModel.getMaxLevelsForChapter(1));

        if (!isChapterDone) {
            btn_chapter_quiz.setEnabled(false);
            btn_chapter_quiz.setBackgroundResource(R.drawable.circular_button_locked);
            Log.d("QuizDebug", "Button state: LOCKED - Chapter not completed");
        } else if (isChapterDone && !isQuizDone) {
            btn_chapter_quiz.setEnabled(true);
            btn_chapter_quiz.setBackgroundResource(R.drawable.circular_button);
            Log.d("QuizDebug", "Button state: READY - Chapter completed, quiz not taken");
        } else if (isQuizDone) {
            btn_chapter_quiz.setEnabled(true);
            btn_chapter_quiz.setBackgroundResource(R.drawable.circular_button_finished);
            Log.d("QuizDebug", "Button state: FINISHED - Quiz completed");
        }
    }
    private void setupQuizObservers() {
        if (progressViewModel == null) return;

        // Observe chapter progress changes
        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            Log.d("QuizDebug", "Chapter progress updated, refreshing quiz button");
            updateQuizButtonState();
        });

        // Observe quiz completion changes
        progressViewModel.getQuizCompletion().observe(getViewLifecycleOwner(), quizMap -> {
            Log.d("QuizDebug", "Quiz completion updated, refreshing quiz button");
            updateQuizButtonState();
        });

        // Also observe user profile changes in case they affect progress
        progressViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            Log.d("QuizDebug", "User profile updated, refreshing quiz button");
            updateQuizButtonState();
        });
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
                    intent.putExtra("chapter", 1);
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
    private void showQuizNotification(int level) {
        Toast.makeText(requireContext(),
                "Quiz available! You've reached Level " + level + " - take the quiz to test your knowledge!",
                Toast.LENGTH_LONG).show();
    }

    private LevelState getLevelState(int level) {
        int completedLevels = progressViewModel.getChapterProgress(1);

        if (level <= completedLevels) {
            return LevelState.FINISHED;
        } else if (level == completedLevels + 1) {
            return LevelState.UNLOCKED;
        } else {
            return LevelState.LOCKED;
        }
    }

    private boolean isLevelUnlocked(int level) {
        int completedLevels = progressViewModel.getChapterProgress(1);
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
            progressViewModel.updateChapterProgress(1, level);
            Log.d("Chapter1", "Progress updated - Chapter 1, Level " + level);
        }

        // Update user profile achievements
        updateUserAchievements(level);
    }

    private void unlockLevel(int level) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();
        Log.d("Chapter1", "Level " + level + " unlocked");
    }

    private void updateUserAchievements(int level) {
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
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
                progressViewModel.markQuizCompleted(1);
                Log.d("Chapter1", "Quiz completed at level " + level);
            }

            progressViewModel.updateUserProfile(profile);
        }
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
            progressViewModel.resetChapterProgress(1);
        }

        updateLevelStates();
        Toast.makeText(getContext(), "Progress reset!", Toast.LENGTH_SHORT).show();
        Log.d("Chapter1", "Chapter progress reset");
    }

    private void initializeQuizButton() {
        btn_chapter_quiz = view.findViewById(R.id.btn_chapter_quiz);
        if (btn_chapter_quiz != null) {
            // Check if all levels are completed and update button state
            updateQuizButtonState();

            btn_chapter_quiz.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isChapterCompleted()) {
                        openQuizActivity();
                    } else {
                        Toast.makeText(getContext(), "Complete all levels to unlock the quiz!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void openQuizActivity() {
        int chapterNumber = 1;
        try {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("chapter_number", chapterNumber);
            intent.putExtra("level", 1); // compute your real level if needed
            startActivity(intent);
            Log.d("Chapter" + chapterNumber, "QuizActivity started successfully");
        } catch (Exception e) {
            Log.e("Chapter" + chapterNumber, "Error starting QuizActivity: " + e.getMessage(), e);
            Toast.makeText(getActivity(), "Error opening quiz", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isChapterCompleted() {
        int completedLevels = progressViewModel.getChapterProgress(1);
        return completedLevels >= BUTTON_IDS.length;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("Chapter1", "Chapter1 resumed - updating UI");

        // Refresh progress when returning from lessons
        if (progressViewModel != null) {
            progressViewModel.refreshAllData();
            progressViewModel.loadQuizCompletionFromSupabase();
            updateLevelStates();
            updateQuizButtonState();

            // Debug current state
            boolean isQuizDone = progressViewModel.isQuizCompleted(1);
            Log.d("QuizDebug", "OnResume - Chapter 1 quiz completed: " + isQuizDone);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNav();
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