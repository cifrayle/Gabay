package com.example.gabay.fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.gabay.R;
import com.example.gabay.data.LessonData;
import com.example.gabay.viewmodels.ProgressViewModel;

public class LessonFragment extends Fragment {

    private static final String ARG_CHAPTER_ID = "chapterId";
    private static final String ARG_LEVEL_NUMBER = "levelNumber";

    private String chapterId;
    private int levelNumber;
    private VideoView videoView;
    private Button btnNext, btnPrev;
    private ImageButton btnReplay, btnPausePlay;

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

        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);
        Log.d("ProgressDebug", "LessonFragment: Chapter " + chapterId + ", Level " + levelNumber);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lesson, container, false);

        videoView = view.findViewById(R.id.videoView);
        btnNext = view.findViewById(R.id.btn_nextLevel);
        btnPrev = view.findViewById(R.id.btn_prevLevel);
        btnReplay = view.findViewById(R.id.btn_replay);
        btnPausePlay = view.findViewById(R.id.btn_pause_play);

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

            // Set title
            if (getActivity() != null) {
                TextView actionBarTitle = getActivity().findViewById(R.id.action_bar_title);
                if (actionBarTitle != null) actionBarTitle.setText(lesson.title);
            }

            // Prepare video
            String path = "android.resource://" + getContext().getPackageName() + "/" + lesson.videoRes;
            Uri uri = Uri.parse(path);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(getContext());
            //videoView.setMediaController(mediaController);
            //mediaController.setAnchorView(videoView);

            btnReplay.setVisibility(View.GONE);
            btnPausePlay.setVisibility(View.GONE);

            // Start video
            videoView.start();

            // Show pause/play when ready
            videoView.setOnPreparedListener(mp -> {
                btnPausePlay.setVisibility(View.VISIBLE);
                btnPausePlay.setImageResource(R.drawable.ic_pause);

                // Auto-hide animation
                btnPausePlay.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setStartDelay(2500)
                        .withEndAction(() -> btnPausePlay.setVisibility(View.GONE))
                        .start();

                // Tap to toggle play/pause
                videoView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {

                        // 🔹 Cancel any ongoing fade animations
                        btnPausePlay.animate().cancel();

                        // 🔹 Reset alpha & make sure it's visible
                        btnPausePlay.setAlpha(1f);
                        btnPausePlay.setVisibility(View.VISIBLE);

                        // 🔹 Toggle play/pause
                        if (videoView.isPlaying()) {
                            videoView.pause();
                            btnPausePlay.setImageResource(R.drawable.ic_play);
                        } else {
                            videoView.start();
                            btnPausePlay.setImageResource(R.drawable.ic_pause);
                        }

                        // 🔹 Reapply fade-out animation after 2.5s
                        btnPausePlay.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .setStartDelay(500)
                                .withEndAction(() -> btnPausePlay.setVisibility(View.GONE))
                                .start();
                    }
                    return true; // consume the touch
                });
            });

            // Replay button (for when video ends)
            videoView.setOnCompletionListener(mp -> {
                btnPausePlay.setVisibility(View.GONE);
                btnReplay.setVisibility(View.VISIBLE);
                btnReplay.setAlpha(0f);
                btnReplay.animate().alpha(1f).setDuration(300).start();
            });

            // Replay click handler
            btnReplay.setOnClickListener(v -> {
                btnReplay.setVisibility(View.GONE);
                videoView.seekTo(0);
                videoView.start();
                btnPausePlay.setImageResource(R.drawable.ic_pause);
                btnPausePlay.setVisibility(View.VISIBLE);
            });

            Log.d("LessonDebug", "Playing video: " + lesson.title + " (Resource: " + lesson.videoRes + ")");
        } else {
            Log.e("LessonDebug", "Invalid lesson: Chapter " + chapterId + ", Level " + levelNumber);
        }
    }

    private LessonData.Lesson[] getLessonsForChapter() {
        switch (chapterId) {
            case "chapter1":
                return LessonData.CHAPTER1;
            case "chapter2":
                return LessonData.CHAPTER2;
            case "chapter3":
                return LessonData.CHAPTER3;
            case "chapter4":
                return LessonData.CHAPTER4;
            case "chapter5":
                return LessonData.CHAPTER5;
            default:
                Log.e("LessonDebug", "Unknown chapter: " + chapterId);
                return null;
        }
    }

    private int getTotalLevelsInChapter() {
        LessonData.Lesson[] lessons = getLessonsForChapter();
        return lessons != null ? lessons.length : 0;
    }

    private void goToLevel(int nextLevelNumber) {
        int chapterNumber = extractChapterNumber(chapterId);
        String prefName = "Chapter" + chapterNumber + "Progress";

        // Before going to next level, ensure it's marked as reached
        if (getActivity() != null) {
            android.content.SharedPreferences preferences = getActivity().getSharedPreferences(prefName, android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();

            // Mark that the user has reached this level
            editor.putBoolean("level_" + nextLevelNumber + "_reached", true);
            editor.apply();

            Log.d("ProgressDebug", "Marked level " + nextLevelNumber + " as reached in " + prefName);
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

            int chapterNumber = extractChapterNumber(chapterId);
            String prefName = "Chapter" + chapterNumber + "Progress";

            // 1. Update SharedPreferences
            android.content.SharedPreferences preferences = getActivity().getSharedPreferences(prefName, android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();

            editor.putBoolean("level_" + levelNumber + "_finished", true);
            for (int i = 1; i <= levelNumber; i++) {
                editor.putBoolean("level_" + i + "_reached", true);
            }
            editor.apply();
            Log.d("ProgressDebug", "SharedPreferences updated for " + prefName + " levels 1-" + levelNumber);

            // 2. UPDATE PROGRESSVIEW MODEL - This is what triggers profile updates
            if (progressViewModel != null) {
                Log.d("ProgressDebug", "Calling progressViewModel.updateChapterProgress(" + chapterNumber + ", " + levelNumber + ")");

                // THIS IS THE KEY CALL THAT UPDATES THE PROFILE
                progressViewModel.updateChapterProgress(chapterNumber, levelNumber);

                // Verify the update worked
                ProgressViewModel.ChapterProgress updatedProgress = progressViewModel.getChapterProgressObject(chapterNumber);
                if (updatedProgress != null) {
                    Log.d("ProgressDebug", "VERIFIED: Chapter " + chapterNumber + " now has " +
                            updatedProgress.getCompletedLevels() + "/" + updatedProgress.getMaxLevels() + " levels completed");
                }

                updateUserAchievements(chapterNumber, levelNumber);

            } else {
                Log.e("ProgressDebug", "ERROR: ProgressViewModel is NULL in LessonFragment!");
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
                String numberStr = chapterId.replace("chapter", "");
                int chapterNum = Integer.parseInt(numberStr);
                Log.d("ProgressDebug", "Extracted chapter number: " + chapterNum + " from " + chapterId);
                return chapterNum;
            }
        } catch (NumberFormatException e) {
            Log.e("ProgressDebug", "Error parsing chapter number from: " + chapterId);
        }
        return 1; // Default fallback
    }

    private void updateUserAchievements(int chapterNumber, int levelNumber) {
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            // Check if chapter completed (all levels finished)
            ProgressViewModel.ChapterProgress chapterProgress = progressViewModel.getChapterProgressObject(chapterNumber);
            if (chapterProgress != null) {
                int totalLevelsInChapter = getTotalLevelsInChapter();

                if (chapterProgress.getCompletedLevels() >= totalLevelsInChapter) {
                    profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
                    Log.d("ProgressDebug", "Chapter " + chapterNumber + " completed! Total chapters: " + profile.getTotalChaptersCompleted());

                    // Mark chapter quiz as completed
                    progressViewModel.markChapterQuizCompleted(chapterNumber);
                }
            }

            // Update quizzes passed if this was a quiz level (every 5 levels)
            if (levelNumber % 5 == 0) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                // Mark quiz as completed in ViewModel
                progressViewModel.markQuizCompleted(chapterNumber, levelNumber);
                Log.d("ProgressDebug", "Quiz completed for chapter " + chapterNumber + " level " + levelNumber);
            }

            progressViewModel.updateUserProfile(profile);
        }
    }
}