package com.example.gabay.fragments.pages;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gabay.R;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.fragments.chapters.Chapter1;
import com.example.gabay.fragments.chapters.Chapter2;
import com.example.gabay.fragments.chapters.Chapter3;
import com.example.gabay.fragments.chapters.Chapter4;
import com.example.gabay.fragments.chapters.Chapter5;
import com.example.gabay.utils.LayoutPerformanceMonitor;
import com.example.gabay.viewmodels.ProgressViewModel;

public class HomePage extends BaseFragment {

    private ConstraintLayout mainContentContainer;
    private View progressCardView;
    private TextView userTitleTextView;
    private TextView lessonsTitleTextView;
    private TextView todoDescTextView;
    private android.widget.ProgressBar progressBar;

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressViewModel progressViewModel;
    private CardView[] chapterCards;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_page, container, false);
        mainContentContainer = view.findViewById(R.id.chapters_container);

        progressCardView = view.findViewById(R.id.todo_card);
        userTitleTextView = view.findViewById(R.id.user_greet);
        lessonsTitleTextView = view.findViewById(R.id.lessons_title);
        todoDescTextView = view.findViewById(R.id.todo_desc);
        progressBar = view.findViewById(R.id.progress_bar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setColorSchemeResources(R.color.secondaryColor);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.primaryColor);


        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("HomePage", "User swiped to refresh");

            if (progressViewModel != null) {
                progressViewModel.refreshProgressFromSupabase();
                progressViewModel.loadUserProfileFromSupabase();
            }
            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1500);
        });

        return view;
    }

    @Override
    protected void initializeViews() {
        // Make sure this uses activity scope
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);

        Log.d("ProgressDebug", "HomePage: ProgressViewModel initialized with requireActivity()");
        initializeChapterCards();
        setupProgressObservers();

        verifyViewModelInstance();

        loadUserProfile();

        // layout performanmce
        if (rootView != null) {
            LayoutPerformanceMonitor.analyzeLayout(rootView);
        }
    }

    private void verifyViewModelInstance() {
        if (progressViewModel != null) {
            Log.d("ProgressDebug", "HomePage: ViewModel instance = " + progressViewModel.toString());
            Log.d("ProgressDebug", "HomePage: ViewModel hashCode = " + progressViewModel.hashCode());

            // FIX: Use getChapterProgressObject() instead of getChapterProgress()
            ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(1);
            if (progress != null) {
                Log.d("ProgressDebug", "HomePage: Current Chapter 1 progress = " +
                        progress.getCompletedLevels() + "/" + progress.getMaxLevels());
            }
        } else {
            Log.e("ProgressDebug", "HomePage: ProgressViewModel is NULL in verifyViewModelInstance");
        }
    }

    @Override
    protected void setupProgressTracking() {
        updateProgressDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ProgressDebug", "HomePage: onResume() called");
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideActionBar();
        }

        updateProgressDisplay();
        loadUserProfile(); //reload profile data
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("ProgressDebug", "HomePage: onPause() called");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("ProgressDebug", "HomePage: onStart() called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("ProgressDebug", "HomePage: onStop() called");
    }
    private void loadUserProfile() {
        if (progressViewModel != null) {
            progressViewModel.loadUserProfileFromSupabase();
            Log.d("ProgressDebug", "HomePage: Loading user profile from Supabase");
        } else {
            Log.e("ProgressDebug", "HomePage: ProgressViewModel is null, cannot load profile");
        }
    }
    @Override
    protected void updateProgressDisplay() {
        if (!isFragmentActive() || progressViewModel == null) return;

        // Update user title with profile data
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null && userTitleTextView != null) {
            userTitleTextView.setText("Hello, " + profile.getUsername() + "!");
            Log.d("ProgressDebug", "HomePage: Updated greeting with username: " + profile.getUsername());
        } else {
            // Fallback if profile not loaded yet
            userTitleTextView.setText("Hello!");
            Log.d("ProgressDebug", "HomePage: Using fallback greeting");
        }

        updateProgressCard();
        updateChapterCardsProgress();
    }

    private void setupProgressObservers() {
        if (progressViewModel == null) {
            Log.d("ProgressDebug", "HomePage: ProgressViewModel is NULL in setupProgressObservers");
            progressViewModel = getActivityViewModel(ProgressViewModel.class);
        }

        Log.d("ProgressDebug", "HomePage: Setting up progress observers");

        progressViewModel.getTotalProgress().observe(getViewLifecycleOwner(), totalProgress -> {
            Log.d("ProgressDebug", "HomePage: Total progress changed to " + totalProgress + "%");
            if (isFragmentActive() && progressBar != null && todoDescTextView != null) {
                progressBar.setProgress(totalProgress != null ? totalProgress : 0);
                todoDescTextView.setText("Overall Progress: " + (totalProgress != null ? totalProgress : 0) + "%");
            }
        });

        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            Log.d("ProgressDebug", "HomePage: Chapter progress map updated");
            if (progressMap != null) {
                for (int chapter : progressMap.keySet()) {
                    Log.d("ProgressDebug", "Chapter " + chapter + ": " + progressMap.get(chapter) + " levels");
                }
            }

            if (isFragmentActive()) {
                Log.d("ProgressDebug", "HomePage: Updating chapter cards from progress change");
                updateChapterCardsProgress();
            }
        });

        progressViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            Log.d("ProgressDebug", "HomePage: UserProfile LiveData triggered");
            if (isFragmentActive() && userTitleTextView != null) {
                if (profile != null && profile.getUsername() != null && !profile.getUsername().isEmpty()) {
                    userTitleTextView.setText("Hello, " + profile.getUsername() + "!");
                } else {
                    userTitleTextView.setText("Hello!");
                }
            }
        });

        Log.d("ProgressDebug", "HomePage: All observers set up successfully");
    }

    private void updateProgressCard() {
        Log.d("ProgressDebug", "HomePage: updateProgressCard called");

        if (progressViewModel == null) {
            Log.d("ProgressDebug", "ProgressViewModel is NULL in updateProgressCard");
            return;
        }
        if (todoDescTextView == null) {
            Log.d("ProgressDebug", "todoDescTextView is NULL");
            return;
        }
        if (progressBar == null) {
            Log.d("ProgressDebug", "progressBar is NULL");
            return;
        }

        Integer totalProgressValue = progressViewModel.getTotalProgress().getValue();
        int totalProgress = (totalProgressValue != null) ? totalProgressValue : 0;

        Log.d("ProgressDebug", "HomePage: Total progress = " + totalProgress + "%");

        todoDescTextView.setText("Overall Progress: " + totalProgress + "%");

        // Update progress bar
        progressBar.setProgress(totalProgress);
        Log.d("ProgressDebug", "HomePage: Progress bar set to " + totalProgress + "%");

        // Force UI refresh
        progressBar.invalidate();
    }

    private void updateChapterCardsProgress() {
        if (progressViewModel == null) return;

        // percentage TextView IDs
        int[] percentageViewIds = {
                R.id.chapter1Percentage, R.id.chapter2Percentage,
                R.id.chapter3Percentage, R.id.chapter4Percentage,
                R.id.chapter5Percentage
        };

        for (int i = 0; i < percentageViewIds.length; i++) {
            int chapterNumber = i + 1;
            ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(chapterNumber);

            if (progress != null) {
                int progressPercent = progress.getProgressPercentage();
                TextView percentageTextView = getView().findViewById(percentageViewIds[i]);

                if (percentageTextView != null) {
                    // Only show percentage if progress is greater than 0%
                    if (progressPercent > 0) {
                        percentageTextView.setText(" (" + progressPercent + "%)");
                        // Set color based on progress
                        setProgressColor(percentageTextView, progressPercent);
                    } else {
                        percentageTextView.setText(""); // Empty string
                    }
                }
            }
        }
    }

    private void setProgressColor(TextView textView, int progressPercent) {
        int colorRes;

        if (progressPercent == 0) {
            colorRes = R.color.wrongAnswerColor; // not started
        } else if (progressPercent <= 33) {
            colorRes = R.color.wrongAnswerColor; // just started (1-33%)
        } else if (progressPercent <= 66) {
            colorRes = R.color.secondaryColor; // halfway (34-66%)
        } else if (progressPercent < 100) {
            colorRes = R.color.secondaryColor; // almost done (67-99%)
        } else {
            colorRes = R.color.correctAnswerColor; // completed (100%)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextColor(getResources().getColor(colorRes, getContext().getTheme()));
        } else {
            textView.setTextColor(getResources().getColor(colorRes));
        }
    }

    private int getChapterStringResource(int chapterNumber) {
        switch (chapterNumber) {
            case 1: return R.string.chapter_1;
            case 2: return R.string.chapter_2;
            case 3: return R.string.chapter_3;
            case 4: return R.string.chapter_4;
            case 5: return R.string.chapter_5;
            default: return R.string.chapter_1;
        }
    }
    private void initializeChapterCards() {
        if (!isFragmentActive()) return;
        int[] cardIds = {R.id.chpt1, R.id.chpt2, R.id.chpt3, R.id.chpt4, R.id.chpt5};
        chapterCards = new CardView[cardIds.length];

        for (int i = 0; i < cardIds.length; i++) {
            chapterCards[i] = rootView.findViewById(cardIds[i]);
            if (chapterCards[i] != null) {
                final int chapterNumber = i + 1;
                chapterCards[i].setOnClickListener(v -> loadChapter(chapterNumber));
            }
        }
    }

    private void loadChapter(int chapterNumber) {
        if (!isFragmentActive()) return;

        if (progressViewModel != null) {
            progressViewModel.setCurrentProgress(chapterNumber, 1);
        }

        // Hide views before loading chapter
        if (progressCardView != null) progressCardView.setVisibility(View.GONE);
        if (userTitleTextView != null) userTitleTextView.setVisibility(View.GONE);
        if (lessonsTitleTextView != null) lessonsTitleTextView.setVisibility(View.GONE);

        // Load chapter content
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null || mainContentContainer == null) return;

        mainContentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View chapterView;
        Fragment selectedChapter = null;

        // Load the appropriate chapter fragment based on the chapter number
        switch (chapterNumber) {
            case 1:
                chapterView = inflater.inflate(R.layout.fragment_lessons_chpt1, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter1();
                break;
            case 2:
                chapterView = inflater.inflate(R.layout.fragment_lessons_chpt2, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter2();
                break;
            case 3:
                chapterView = inflater.inflate(R.layout.fragment_lessons_chpt3, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter3();
                break;
            case 4:
                chapterView = inflater.inflate(R.layout.fragment_lessons_chpt4, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter4();
                break;
            case 5:
                chapterView = inflater.inflate(R.layout.fragment_lessons_chpt5, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter5();
                break;
            default:
                return;
        }

        // Load the selected chapter fragment
        if (selectedChapter != null) {
            // Show action bar with chapter title in MainActivity
            if (activity instanceof MainActivity) {
                String title = getString(getChapterStringResource(chapterNumber));
                ((MainActivity) activity).showActionBarWithTitle(title);
            }
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedChapter)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    protected void cleanupResources() {
        // for cleaning up any resources specific to HomePage
        mainContentContainer = null;
        progressCardView = null;
        userTitleTextView = null;
        lessonsTitleTextView = null;
        todoDescTextView = null;
        progressBar = null;
        chapterCards = null; // Changed from chapterButtons
        progressViewModel = null;
    }
}
