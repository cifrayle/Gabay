package com.example.gabay.fragments;

import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.gabay.R;
import com.example.gabay.data.LessonData;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.viewmodels.ProgressViewModel;

public class LessonFragment extends Fragment {

    private static final String ARG_CHAPTER_ID = "chapterId";
    private static final String ARG_LEVEL_NUMBER = "levelNumber";

    private String chapterId;
    private int levelNumber;
    private VideoView videoView;
    private Button btnNext, btnPrev;
    private android.widget.ProgressBar video_progressBar;
    private ImageButton btnReplay, btnPausePlay;

    private ProgressViewModel progressViewModel;
    private Handler progressHandler = new Handler();

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
        video_progressBar = view.findViewById(R.id.progressBar);

        showLesson();

        btnNext.setOnClickListener(v -> completeAndGoToNext());
        btnPrev.setOnClickListener(v -> {
            if (levelNumber > 1) goToLevel(levelNumber - 1);
        });

        return view;
    }

    private void showLesson() {
        LessonData.Lesson[] lessons = getLessonsForChapter();

        if (lessons == null || levelNumber < 1 || levelNumber > lessons.length) {
            Log.e("LessonDebug", "Invalid lesson: Chapter " + chapterId + ", Level " + levelNumber);
            return;
        }

        LessonData.Lesson lesson = lessons[levelNumber - 1];

        // Set title
        if (getActivity() != null) {
            TextView actionBarTitle = getActivity().findViewById(R.id.action_bar_title);
            if (actionBarTitle != null) actionBarTitle.setText(lesson.title);
        }

        // ✅ Prepare video correctly
        String path = "android.resource://" + requireContext().getPackageName() + "/" + lesson.videoRes;
        Uri uri = Uri.parse(path);
        videoView.setVideoURI(uri);

        btnReplay.setVisibility(View.GONE);
        btnPausePlay.setVisibility(View.GONE);
        video_progressBar.setProgress(0);
        btnNext.setEnabled(false); // lock initially

        int chapterNumber = extractChapterNumber(chapterId);

        // ✅ Wait for the video to be ready before checking Supabase
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            videoView.start();

            // Start progress handler (paused initially)
            Log.d("LessonDebug", "Video prepared successfully: " + lesson.title);

            // 🔹 Now check Supabase asynchronously (video is already playing)
            new Thread(() -> {
                int completedCount = SupabaseJavaService.getCompletedLevelsCount(chapterNumber);
                boolean alreadyCompleted = completedCount >= levelNumber;

                requireActivity().runOnUiThread(() -> {
                    setupVideoPlayback(lesson, alreadyCompleted, chapterNumber);
                    btnNext.setEnabled(alreadyCompleted);

                    if (alreadyCompleted) {
                        Log.d("LessonDebug", "Level " + levelNumber + " already completed — Next enabled.");
                    }
                });
            }).start();
        });

        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(getContext(), "Error playing video", Toast.LENGTH_SHORT).show();
            Log.e("LessonDebug", "Video playback error: what=" + what + ", extra=" + extra);
            return true;
        });
    }


    private void setupVideoPlayback(LessonData.Lesson lesson, boolean alreadyCompleted, int chapterNumber) {
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    int position = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    if (duration > 0) {
                        int progress = (int) (((float) position / duration) * 100);
                        video_progressBar.setProgress(progress);

                        if (progress >= 90 && !btnNext.isEnabled() && !alreadyCompleted) {
                            btnNext.setEnabled(true);
                            Toast.makeText(getContext(), "You can now proceed to the next level!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                progressHandler.postDelayed(this, 10);
            }
        };

        // ✅ Ensure handler starts after video begins
        progressHandler.postDelayed(progressRunnable, 10);

        btnPausePlay.setVisibility(View.VISIBLE);
        btnPausePlay.setImageResource(R.drawable.ic_pause);

        // Auto-hide play/pause button
        btnPausePlay.animate()
                .alpha(0f)
                .setDuration(300)
                .setStartDelay(2500)
                .withEndAction(() -> btnPausePlay.setVisibility(View.GONE))
                .start();

        videoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                btnPausePlay.animate().cancel();
                btnPausePlay.setAlpha(1f);
                btnPausePlay.setVisibility(View.VISIBLE);

                if (videoView.isPlaying()) {
                    videoView.pause();
                    btnPausePlay.setImageResource(R.drawable.ic_play);
                } else {
                    videoView.start();
                    btnPausePlay.setImageResource(R.drawable.ic_pause);
                }

                btnPausePlay.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setStartDelay(500)
                        .withEndAction(() -> btnPausePlay.setVisibility(View.GONE))
                        .start();
            }
            return true;
        });

        videoView.setOnCompletionListener(mp -> {
            btnPausePlay.setVisibility(View.GONE);
            btnReplay.setVisibility(View.VISIBLE);
            video_progressBar.setProgress(100);
            progressHandler.removeCallbacks(progressRunnable);
            btnNext.setEnabled(true);

            btnReplay.setAlpha(0f);
            btnReplay.animate().alpha(1f).setDuration(300).start();
        });

        btnReplay.setOnClickListener(v -> {
            btnReplay.setVisibility(View.GONE);
            video_progressBar.setProgress(0);
            videoView.seekTo(0);
            videoView.start();
            btnPausePlay.setImageResource(R.drawable.ic_pause);
            btnPausePlay.setVisibility(View.VISIBLE);
            progressHandler.post(progressRunnable);
        });
    }


    private void completeAndGoToNext() {
        markLevelsUpToCurrent();

        new Handler().postDelayed(() -> {
            LessonData.Lesson[] lessons = getLessonsForChapter();
            if (lessons != null && (levelNumber + 1) <= lessons.length) {
                goToLevel(levelNumber + 1);
            } else if (getActivity() != null)  {
                Toast.makeText(getActivity(), "All levels completed! Great job!", Toast.LENGTH_LONG).show();
                getActivity().setResult(android.app.Activity.RESULT_OK);
                getActivity().finish();
            }
        }, 800); // 0.8-second delay prevents overlap crash
    }

    private void markLevelsUpToCurrent() {
        if (getActivity() == null) return;

        int chapterNumber = extractChapterNumber(chapterId);

        new Thread(() -> {
            try {
                int completedCount = SupabaseJavaService.getCompletedLevelsCount(chapterNumber);
                if (completedCount >= levelNumber) {
                    Log.d("ProgressDebug", "Level " + levelNumber + " already completed — skipping update.");
                    return;
                }

                boolean success = SupabaseJavaService.updateUserProgress(chapterNumber, levelNumber);

                // ✅ Check again before updating UI
                if (!isAdded()) {
                    Log.w("ProgressDebug", "Fragment detached — skipping UI update for level " + levelNumber);
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    // ✅ Double-check in case activity was destroyed mid-call
                    if (!isAdded() || getActivity() == null) {
                        Log.w("ProgressDebug", "Fragment not attached during UI update — safely skipped.");
                        return;
                    }

                    if (success) {
                        Toast.makeText(getActivity(), "Level " + levelNumber + " completed!", Toast.LENGTH_SHORT).show();

                        if (progressViewModel != null) {
                            progressViewModel.updateChapterProgress(chapterNumber, levelNumber);
                            updateUserAchievements(chapterNumber, levelNumber);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to update progress online.", Toast.LENGTH_SHORT).show();
                    }

                    // Safe result set
                    if (getActivity() != null) {
                        getActivity().setResult(android.app.Activity.RESULT_OK);
                    }
                });

            } catch (Exception e) {
                Log.e("ProgressDebug", "Error in markLevelsUpToCurrent: " + e.getMessage());
            }
        }).start();
    }


    private void goToLevel(int nextLevelNumber) {
        Fragment fragment = LessonFragment.newInstance(chapterId, nextLevelNumber);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private int extractChapterNumber(String chapterId) {
        try {
            if (chapterId != null && chapterId.startsWith("chapter")) {
                return Integer.parseInt(chapterId.replace("chapter", ""));
            }
        } catch (NumberFormatException e) {
            Log.e("LessonDebug", "Error parsing chapter number from " + chapterId);
        }
        return 1;
    }

    private LessonData.Lesson[] getLessonsForChapter() {
        switch (chapterId) {
            case "chapter1": return LessonData.CHAPTER1;
            case "chapter2": return LessonData.CHAPTER2;
            case "chapter3": return LessonData.CHAPTER3;
            case "chapter4": return LessonData.CHAPTER4;
            case "chapter5": return LessonData.CHAPTER5;
            default:
                Log.e("LessonDebug", "Unknown chapter: " + chapterId);
                return null;
        }
    }

    private void updateUserAchievements(int chapterNumber, int levelNumber) {
        if (progressViewModel == null) return;

        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            ProgressViewModel.ChapterProgress chapterProgress = progressViewModel.getChapterProgressObject(chapterNumber);
            if (chapterProgress != null && chapterProgress.getCompletedLevels() >= getTotalLevelsInChapter()) {
                profile.setTotalChaptersCompleted(profile.getTotalChaptersCompleted() + 1);
                progressViewModel.markChapterQuizCompleted(chapterNumber);
            }

            if (levelNumber % 5 == 0) {
                profile.setTotalQuizzesPassed(profile.getTotalQuizzesPassed() + 1);
                progressViewModel.markQuizCompleted(chapterNumber);
            }

            progressViewModel.updateUserProfile(profile);
        }
    }

    private int getTotalLevelsInChapter() {
        LessonData.Lesson[] lessons = getLessonsForChapter();
        return lessons != null ? lessons.length : 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressHandler.removeCallbacksAndMessages(null);
    }
}
