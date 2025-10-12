package com.example.gabay.fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.gabay.R;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.data.LessonData;
import com.example.gabay.viewmodels.ProgressViewModel;

public class LessonFragment extends Fragment {

    private static final String ARG_CHAPTER_ID = "chapterId";
    private static final String ARG_LEVEL_NUMBER = "levelNumber";

    private String chapterId;
    private int levelNumber;
    private VideoView videoView;
    private Button btnNext, btnPrev;
    private ProgressViewModel progressViewModel;

    public LessonFragment() {}

    public static LessonFragment newInstance(String chapterId, int levelNumber) {
        LessonFragment fragment = new LessonFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        args.putInt(ARG_LEVEL_NUMBER, levelNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterId = getArguments().getString(ARG_CHAPTER_ID);
            levelNumber = getArguments().getInt(ARG_LEVEL_NUMBER, 1);
        }

        // FIX: Use requireActivity() instead of getActivity()
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);
        Log.d("ProgressDebug", "LessonFragment: ProgressViewModel initialized with requireActivity()");

        // ADD THIS: Verify the instance
        verifyViewModelInstance();
    }

    // ADD THIS METHOD
    private void verifyViewModelInstance() {
        if (progressViewModel != null) {
            Log.d("ProgressDebug", "LessonFragment: ViewModel instance = " + progressViewModel.toString());
            Log.d("ProgressDebug", "LessonFragment: ViewModel hashCode = " + progressViewModel.hashCode());

            // FIX: Use getChapterProgressObject() instead of getChapterProgress()
            ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(1);
            if (progress != null) {
                Log.d("ProgressDebug", "LessonFragment: Current Chapter 1 progress = " +
                        progress.getCompletedLevels() + "/" + progress.getMaxLevels());
            }
        } else {
            Log.e("ProgressDebug", "LessonFragment: ProgressViewModel is NULL in verifyViewModelInstance");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lesson, container, false);

        videoView = view.findViewById(R.id.videoView);
        btnNext = view.findViewById(R.id.btn_nextLevel);
        btnPrev = view.findViewById(R.id.btn_prevLevel);

        showLesson();

        // When Finish button is clicked: complete current level AND go to next level
        btnNext.setOnClickListener(v -> completeAndGoToNext());
        btnPrev.setOnClickListener(v -> {
            if (levelNumber > 1) goToLevel(levelNumber - 1);
        });

        return view;
    }

    private void showLesson() {
        LessonData.Lesson[] lessons = getLessonsForChapter();

        if (lessons != null && levelNumber >= 1 && levelNumber <= lessons.length) {
            LessonData.Lesson lesson = lessons[levelNumber - 1];

            if (getActivity() != null) {
                TextView actionBarTitle = getActivity().findViewById(R.id.action_bar_title);
                if (actionBarTitle != null) {
                    actionBarTitle.setText(lesson.title);
                }
            }

            // Play video
            String path = "android.resource://" + getContext().getPackageName() + "/" + lesson.videoRes;
            Uri uri = Uri.parse(path);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(getContext());
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);

            videoView.start();
        }
    }

    private LessonData.Lesson[] getLessonsForChapter() {
        if ("chapter1".equals(chapterId)) {
            return LessonData.CHAPTER1;
        }
        // add other chapters later
        return null;
    }

    private void goToLevel(int nextLevelNumber) {
        // Before going to next level, ensure it's marked as reached
        if (getActivity() != null) {
            android.content.SharedPreferences preferences = getActivity().getSharedPreferences("Chapter1Progress", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();

            // Mark that the user has reached this level
            editor.putBoolean("level_" + nextLevelNumber + "_reached", true);
            editor.apply();
        }

        LessonData.Lesson[] lessons = getLessonsForChapter();
        if (lessons != null && nextLevelNumber >= 1 && nextLevelNumber <= lessons.length) {
            Fragment fragment = LessonFragment.newInstance(chapterId, nextLevelNumber);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    private void completeAndGoToNext() {
        // Mark ALL levels up to and including current as completed/reached
        markLevelsUpToCurrent();

        // Then navigate to next level
        LessonData.Lesson[] lessons = getLessonsForChapter();
        if (lessons != null && (levelNumber + 1) <= lessons.length) {
            goToLevel(levelNumber + 1);
        } else {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "All levels completed! Great job!", Toast.LENGTH_LONG).show();
                // Set result to indicate completion
                getActivity().setResult(android.app.Activity.RESULT_OK);
                getActivity().finish();
            }
        }
    }

    private void markLevelsUpToCurrent() {
        if (getActivity() != null) {
            Log.d("ProgressDebug", "=== LESSONFRAGMENT: markLevelsUpToCurrent START ===");
            Log.d("ProgressDebug", "Chapter: " + chapterId + ", Level: " + levelNumber);

            // 1. Update SharedPreferences
            android.content.SharedPreferences preferences = getActivity().getSharedPreferences("Chapter1Progress", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean("level_" + levelNumber + "_finished", true);
            for (int i = 1; i <= levelNumber; i++) {
                editor.putBoolean("level_" + i + "_reached", true);
            }
            editor.apply();
            Log.d("ProgressDebug", "SharedPreferences updated for levels 1-" + levelNumber);

            // 2. CRITICAL: UPDATE PROGRESSVIEW MODEL
            if (progressViewModel != null) {
                int chapterNumber = extractChapterNumber(chapterId);
                Log.d("ProgressDebug", "Calling progressViewModel.updateChapterProgress(" + chapterNumber + ", " + levelNumber + ")");

                // THIS IS THE KEY LINE THAT'S PROBABLY NOT BEING CALLED
                progressViewModel.updateChapterProgress(chapterNumber, levelNumber);

                // FIX: Verify the update worked - Use getChapterProgressObject()
                ProgressViewModel.ChapterProgress updatedProgress = progressViewModel.getChapterProgressObject(chapterNumber);
                if (updatedProgress != null) {
                    Log.d("ProgressDebug", "VERIFIED: Chapter " + chapterNumber + " now has " +
                            updatedProgress.getCompletedLevels() + "/" + updatedProgress.getMaxLevels() + " levels completed");
                }

                updateUserAchievements(chapterNumber, levelNumber);

            } else {
                Log.e("ProgressDebug", "ERROR: ProgressViewModel is NULL in LessonFragment!");
                // Let's see why - check if activity exists
                if (getActivity() == null) {
                    Log.e("ProgressDebug", "Activity is also NULL!");
                } else {
                    Log.e("ProgressDebug", "Activity exists: " + getActivity().getClass().getSimpleName());
                }
            }

            Toast.makeText(getActivity(), "Level " + levelNumber + " completed!", Toast.LENGTH_SHORT).show();
            getActivity().setResult(android.app.Activity.RESULT_OK);
            Log.d("ProgressDebug", "=== LESSONFRAGMENT: markLevelsUpToCurrent END ===");
        } else {
            Log.e("ProgressDebug", "ERROR: Activity is NULL in markLevelsUpToCurrent");
        }
    }

    private int extractChapterNumber(String chapterId) {
        try {
            if (chapterId != null && chapterId.startsWith("chapter")) {
                int chapterNum = Integer.parseInt(chapterId.replace("chapter", ""));
                Log.d("ProgressDebug", "Extracted chapter number: " + chapterNum + " from " + chapterId);
                return chapterNum;
            }
        } catch (NumberFormatException e) {
            Log.e("ProgressDebug", "Error parsing chapter number from: " + chapterId);
        }
        return 1; // Default
    }

    private void updateUserAchievements(int chapterNumber, int levelNumber) {
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            // FIX: Check if chapter completed (all levels finished) - Use getChapterProgressObject()
            ProgressViewModel.ChapterProgress chapterProgress = progressViewModel.getChapterProgressObject(chapterNumber);
            if (chapterProgress != null) {
                LessonData.Lesson[] lessons = getLessonsForChapter();
                int totalLevelsInChapter = lessons != null ? lessons.length : 28; // Default to 28 for chapter 1

                if (chapterProgress.getCompletedLevels() >= totalLevelsInChapter) {
                    profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
                    Log.d("ProgressDebug", "Chapter " + chapterNumber + " completed!");
                }
            }

            // Update quizzes passed if this was a quiz level (every 5 levels)
            if (levelNumber % 5 == 0) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                // Mark quiz as completed in ViewModel
                progressViewModel.markQuizCompleted(chapterNumber, levelNumber);
                Log.d("ProgressDebug", "Quiz completed for level " + levelNumber);
            }

            progressViewModel.updateUserProfile(profile);
        }
    }
}
