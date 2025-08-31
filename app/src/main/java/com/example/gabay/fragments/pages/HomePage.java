package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

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
    private View progressBar;
    private ImageButton profileButton;

    // Progress tracking
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
        userTitleTextView = view.findViewById(R.id.user_title);
        lessonsTitleTextView = view.findViewById(R.id.lessons_title);
        todoDescTextView = view.findViewById(R.id.todo_desc);
        progressBar = view.findViewById(R.id.progress_bar);
        profileButton = null;

        return view;
    }

    @Override
    protected void initializeViews() {
        // Initialize ProgressViewModel
        progressViewModel = getActivityViewModel(ProgressViewModel.class);

        initializeChapterCards();
        setupProgressObservers();

        // layout perf
        if (rootView != null) {
            LayoutPerformanceMonitor.analyzeLayout(rootView);
        }
    }

    private void initializeProfileButton() {}

    protected void navigateToProfile() {
        if (getActivity() != null) {
            // Create and show ProfilePage fragment
            ProfilePage profilePage = new ProfilePage();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, profilePage)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    protected void setupProgressTracking() {
        // Update progress display when fragment becomes active
        updateProgressDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideActionBar();
        }
    }

    @Override
    protected void updateProgressDisplay() {
        if (!isFragmentActive() || progressViewModel == null) return;

        // Update user title with profile data
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null && userTitleTextView != null) {
            userTitleTextView.setText("Hello, Cian!");
            // will be implemented when database is all good
            //userTitleTextView.setText("Hello, " + profile.getUsername() + "!");
        }

        // Update progress card with current progress
        updateProgressCard();

        // Update chapter cards with progress indicators
        updateChapterCardsProgress(); // Changed method name
    }

    private void setupProgressObservers() {
        if (progressViewModel == null) return;

        // Observe chapter progress changes
        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (isFragmentActive()) {
                updateProgressCard();
                updateChapterCardsProgress(); // Changed method name
            }
        });

        // Observe user profile changes
        progressViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (isFragmentActive() && userTitleTextView != null) {
                userTitleTextView.setText("Hello, Cian!");
                // will be implemented when database is all good
                //userTitleTextView.setText("Hello, " + profile.getUsername() + "!");
            }
        });
    }

    private void updateProgressCard() {
        if (progressViewModel == null || todoDescTextView == null) return;

        // Get current progress
        int totalProgress = progressViewModel.getTotalProgress();

        // Update progress description
        if (totalProgress > 0) {
            todoDescTextView.setText("Overall Progress: " + totalProgress + "%");
        } else {
            todoDescTextView.setText("Start your learning journey!");
        }

        // Update progress bar (if it's a ProgressBar)
        if (progressBar instanceof android.widget.ProgressBar) {
            ((android.widget.ProgressBar) progressBar).setProgress(totalProgress);
        }
    }

    private void updateChapterCardsProgress() { // Updated method for CardView
        if (progressViewModel == null || chapterCards == null) return;

        for (int i = 0; i < chapterCards.length; i++) {
            int chapterNumber = i + 1;
            ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgress(chapterNumber);

            if (progress != null && chapterCards[i] != null) {
                int progressPercent = progress.getProgressPercentage();

                // Find the title TextView inside the CardView and update it with progress
                TextView titleTextView = findTitleTextViewInCard(chapterCards[i]);
                if (titleTextView != null) {
                    String buttonText = getString(getChapterStringResource(chapterNumber));
                    if (progressPercent > 0) {
                        buttonText += " (" + progressPercent + "%)";
                    }
                    titleTextView.setText(buttonText);
                }

                // Change card appearance based on progress
                updateCardAppearance(chapterCards[i], progressPercent);
            }
        }
    }

    private TextView findTitleTextViewInCard(CardView cardView) {
        // Find the first TextView in the CardView (which should be the title)
        if (cardView.getChildCount() > 0 && cardView.getChildAt(0) instanceof ViewGroup) {
            ViewGroup linearLayout = (ViewGroup) cardView.getChildAt(0);
            if (linearLayout.getChildCount() > 0 && linearLayout.getChildAt(0) instanceof TextView) {
                return (TextView) linearLayout.getChildAt(0);
            }
        }
        return null;
    }

    private void updateCardAppearance(CardView card, int progressPercent) {
        // You can update card appearance here if you have different styles
        // For now, we'll keep the same appearance but you could change:
        // - Card background color
        // - Card elevation
        // - Text color
        // etc.

        if (progressPercent >= 100) {
            // Chapter completed - could change card color
            card.setCardBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else if (progressPercent > 0) {
            // Chapter in progress - could change card color
            card.setCardBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            // Chapter not started - default white
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
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
        profileButton = null;
        chapterCards = null; // Changed from chapterButtons
        progressViewModel = null;
    }
}