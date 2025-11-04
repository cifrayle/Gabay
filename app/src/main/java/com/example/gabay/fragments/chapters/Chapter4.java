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

public class Chapter4 extends Fragment {

    Button btn_chapter_quiz;
    // Update BUTTON_IDS and TITLE_IDS based on your layout
    private static final int[] BUTTON_IDS = {
            R.id.lvl1, R.id.lvl2, R.id.lvl3, R.id.lvl4, R.id.lvl5,
            R.id.lvl6, R.id.lvl7,
    };

    private static final int[] TITLE_IDS = {
            R.id.levelTitle1, R.id.levelTitle2, R.id.levelTitle3, R.id.levelTitle4, R.id.levelTitle5,
            R.id.levelTitle6, R.id.levelTitle7,
    };

    private View view;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "Chapter4Progress";
    private ProgressViewModel progressViewModel;
    private static final int LESSON_ACTIVITY_REQUEST = 1001;
    private Button[] levelButtons;

    public static Chapter4 newInstance() {
        Chapter4 fragment = new Chapter4();
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
            updateQuizButtonState();
        });

        initializeLevelButtons();
        initializeQuizButton();
        addResetButton();
        setupProgressObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt4, container, false);
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
                intent.putExtra("chapter", 4);
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
            Toast.makeText(getContext(), "Level " + levelNumber + " is locked! Complete Level " + (levelNumber - 1) + " first.", Toast.LENGTH_SHORT).show();
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
                    markLevelAsFinished(completedLevel, score);
                    if (completedLevel < BUTTON_IDS.length) {
                        unlockLevel(completedLevel + 1);
                    }
                    if (progressViewModel.shouldTakeQuiz(4, completedLevel)) {
                        showQuizNotification(completedLevel);
                    }
                    checkChapterCompletion();
                }
            }
            updateLevelStates();
        }
    }

    private void updateLevelStates() {
        int completedLevels = progressViewModel.getChapterProgress(4);
        boolean isChapterDone = progressViewModel.isChapterCompleted(4);

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
                    // 🟩 Entire chapter is completed → mark all levels as finished
                    button.setBackgroundResource(R.drawable.circular_button_finished);
                    button.setEnabled(true);

                } else if (levelNumber <= completedLevels) {
                    // 🟩 Previously completed levels
                    button.setBackgroundResource(R.drawable.circular_button_finished);
                    button.setEnabled(true);

                } else if (levelNumber == completedLevels + 1) {
                    // 🟢 Current level being played
                    button.setBackgroundResource(R.drawable.circular_button);
                    button.setEnabled(true);

                }  else {
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
        ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(4);
        if (progress != null && progress.getProgressPercentage() >= 100) {
            Toast.makeText(requireContext(), "Congratulations! Chapter 1 completed! Quiz unlocked!", Toast.LENGTH_LONG).show();
            // Mark chapter quiz as completed
            progressViewModel.markChapterQuizCompleted(4);

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

        boolean isChapterDone = progressViewModel.isChapterCompleted(4);
        boolean isQuizDone = progressViewModel.isQuizCompleted(4);

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
        int completedLevels = progressViewModel.getChapterProgress(4);
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

    private void updateButtonClickListener(Button button, int levelNumber) {
        button.setOnClickListener(null);
        button.setOnTouchListener(null);
        TextView titleView = view.findViewById(TITLE_IDS[levelNumber - 1]);
        if (isLevelUnlocked(levelNumber)) {
            button.setOnClickListener(v -> {
                if (titleView != null) {
                    String levelTitle = titleView.getText().toString();
                    Intent intent = new Intent(getActivity(), LessonActivity.class);
                    intent.putExtra("chapter", 4);
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
        int completedLevels = progressViewModel.getChapterProgress(4);
        return level <= completedLevels + 1;
    }

    public void markLevelAsFinished(int level, int score) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_finished", true);
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();
        if (progressViewModel != null) {
            progressViewModel.updateChapterProgress(4, level);
        }
        updateUserAchievements(level);
    }

    private void unlockLevel(int level) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("level_" + level + "_reached", true);
        editor.apply();
    }

    private void updateUserAchievements(int level) {
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            ProgressViewModel.ChapterProgress chapterProgress = progressViewModel.getChapterProgressObject(4);
            if (chapterProgress != null && chapterProgress.getCompletedLevels() >= BUTTON_IDS.length) {
                profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
            }
            if (level % 5 == 0) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                progressViewModel.markQuizCompleted(4);
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
                    .setPositiveButton("Reset", (dialog, which) -> resetChapterProgress())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void resetChapterProgress() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        if (progressViewModel != null) {
            progressViewModel.resetChapterProgress(4);
        }
        updateLevelStates();
        Toast.makeText(getContext(), "Progress reset!", Toast.LENGTH_SHORT).show();
    }

    private void openQuizActivity() {
        try {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("chapter_number", 4);
            startActivity(intent);
        } catch (Exception e) {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Error opening quiz", Toast.LENGTH_SHORT).show();
            }
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
    public void onResume() {
        super.onResume();
        updateLevelStates();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showActionBarWithTitle("Chapter 4");
        }
    }
}