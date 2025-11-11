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
import com.example.gabay.fragments.quiz.MatchingQuizFragment;
import com.example.gabay.viewmodels.ProgressViewModel;

public class Chapter2 extends Fragment {

    Button btn_chapter_quiz;
    private static final int[] BUTTON_IDS = {
            R.id.c2lvl1, R.id.c2lvl2, R.id.c2lvl3, R.id.c2lvl4, R.id.c2lvl5
    };

    private static final int[] TITLE_IDS = {
            R.id.levelTitle1, R.id.levelTitle2, R.id.levelTitle3,
            R.id.levelTitle4, R.id.levelTitle5
    };

    private static int currentLessonLevel = -1;
    private View view;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "Chapter2Progress";
    private ProgressViewModel progressViewModel;

    private static final int LESSON_ACTIVITY_REQUEST = 1001;

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

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNav();
        }

        this.view = view;

        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);
        preferences = requireContext().getSharedPreferences(PREF_NAME, getContext().MODE_PRIVATE);

        progressViewModel.getQuizCompletion().observe(getViewLifecycleOwner(), quizMap -> {
            updateQuizButtonState();
        });
        progressViewModel.getChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            updateLevelStates();
            updateQuizButtonState(); // this will unlock the quiz if the chapter is now completed
        });
        if (progressViewModel != null) {
            progressViewModel.refreshAllData();
        }

        initializeLevelButtons();
        initializeQuizButton();
        updateLevelStates();
        setupProgressObserver();

        Log.d("Chapter2", "Chapter2 fragment initialized");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt2, container, false);
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
        if (button.getText().toString().isEmpty()) {
            //button.setText(String.valueOf(levelNumber));
        }

        if (!isLevelUnlocked(levelNumber)) {
            button.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showLockedLevelToast(levelNumber);
                }
                return true;
            });
        } else {
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

                    markLevelAsFinished(completedLevel, score);

                    if (completedLevel < BUTTON_IDS.length) {
                        unlockLevel(completedLevel + 1);
                    }

                    if (progressViewModel.shouldTakeQuiz(2, completedLevel)) {
                        showQuizNotification(completedLevel);
                    }

                    checkChapterCompletion();
                }
            }
            updateLevelStates();
        }
    }

    private void updateLevelStates() {
        int completedLevels = progressViewModel.getChapterProgress(2);
        boolean isChapterDone = progressViewModel.isChapterCompleted(2);

        for (int i = 0; i < levelButtons.length; i++) {
            Button button = levelButtons[i];
            int levelNumber = i + 1;

            if (button != null) {
                // Reset Material effects
                button.setBackgroundTintList(null);
                button.setStateListAnimator(null);
                button.setElevation(0f);
                button.setAlpha(1.0f);

                if (isChapterDone) {
                    // entire chapter is completed → mark all levels as finished
                    button.setBackgroundResource(R.drawable.circular_button_finished);
                    button.setEnabled(true);

                } else if (levelNumber <= completedLevels) {
                    // completed levels
                    button.setBackgroundResource(R.drawable.circular_button_finished);
                    button.setEnabled(true);

                } else if (levelNumber == completedLevels + 1) {
                    // Next level unlocked (optional)
                    button.setBackgroundResource(R.drawable.circular_button);
                    button.setEnabled(true);

                } else {
                    // 🔒 Locked levels
                    button.setBackgroundResource(R.drawable.circular_button_locked);
                    button.setEnabled(false);
                }

                updateButtonClickListener(button, levelNumber);
            }
        }
        // Refresh quiz button too
        updateQuizButtonState();
    }



    private void checkChapterCompletion() {
        ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(2);
        if (progress != null && progress.getProgressPercentage() >= 100) {
            Toast.makeText(requireContext(), "Congratulations! Chapter 1 completed! Quiz unlocked!", Toast.LENGTH_LONG).show();
            // Mark chapter quiz as completed
            progressViewModel.markChapterQuizCompleted(2);

            // Update quiz button to be enabled
            updateQuizButtonState();
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
                updateQuizButtonState();
                Log.d("Chapter1", "Progress observer triggered - updating level states");
            }
        });
    }

    private void updateQuizButtonState() {
        if (btn_chapter_quiz == null) return;

        btn_chapter_quiz.setBackgroundTintList(null);
        btn_chapter_quiz.setStateListAnimator(null);
        btn_chapter_quiz.setElevation(0f);
        btn_chapter_quiz.setAlpha(1.0f);

        boolean isChapterDone = progressViewModel.isChapterCompleted(2);
        boolean isQuizDone = progressViewModel.isQuizCompleted(2);

        if (!isChapterDone) {
            btn_chapter_quiz.setEnabled(false);
            btn_chapter_quiz.setBackgroundResource(R.drawable.circular_button_locked);
        } else if (isChapterDone && !isQuizDone) {
            btn_chapter_quiz.setEnabled(true);
            btn_chapter_quiz.setBackgroundResource(R.drawable.circular_button);
        } else if (isQuizDone) {
            btn_chapter_quiz.setEnabled(true);
            btn_chapter_quiz.setBackgroundResource(R.drawable.circular_button_finished);
        }
    }

    private boolean isChapterCompleted() {
        int completedLevels = progressViewModel.getChapterProgress(2);
        return completedLevels >= BUTTON_IDS.length;
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
        int chapterNumber = 2;
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

    private void updateButtonClickListener(Button button, int levelNumber) {
        button.setOnClickListener(null);
        button.setOnTouchListener(null);

        TextView titleView = view.findViewById(TITLE_IDS[levelNumber - 1]);

        if (isLevelUnlocked(levelNumber)) {
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
            button.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showLockedLevelToast(levelNumber);
                }
                return true;
            });
        }
    }

    private boolean isLevelUnlocked(int level) {
        int completedLevels = progressViewModel.getChapterProgress(2);
        return level <= completedLevels + 1;
    }

    public void markLevelAsFinished(int level, int score) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_finished", true);
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();

        if (progressViewModel != null) {
            progressViewModel.updateChapterProgress(2, level);
            Log.d("Chapter2", "Progress updated - Chapter 2, Level " + level);
        }

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
                Log.d("ProgressDebug", "Chapter progress - Completed: " + chapterProgress.getCompletedLevels() +
                        "/" + chapterProgress.getMaxLevels());

                if (chapterProgress.getCompletedLevels() >= BUTTON_IDS.length) {
                    profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
                    Log.d("ProgressDebug", "Chapter completed! Total chapters: " + profile.getTotalChaptersCompleted());
                }
            }

            if (level % 5 == 0) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                progressViewModel.markQuizCompleted(2);
                Log.d("Chapter2", "Quiz completed at level " + level);
            }

            progressViewModel.updateUserProfile(profile);
        }
    }

    private void addResetButton() {
        Button resetBtn = new Button(requireContext());
        resetBtn.setText("Reset Progress");
        resetBtn.setBackgroundColor(getResources().getColor(R.color.transparentBackground));
        resetBtn.setTextColor(getResources().getColor(R.color.secondaryColor));

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
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        if (progressViewModel != null) {
            progressViewModel.resetChapterProgress(2);
        }

        updateLevelStates();
        Toast.makeText(getContext(), "Progress reset!", Toast.LENGTH_SHORT).show();
        Log.d("Chapter2", "Chapter progress reset");
    }


    @Override
    public void onResume() {
        super.onResume();
        // Refresh progress when returning from lessons
        if (progressViewModel != null) {
            progressViewModel.refreshAllData();
        }
        updateLevelStates();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showActionBarWithTitle("Chapter 2");
        }
        Log.d("Chapter2", "Chapter2 resumed - updating UI");
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
}