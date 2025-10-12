    package com.example.gabay.fragments.pages;

    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageButton;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.constraintlayout.widget.ConstraintLayout;

    import com.example.gabay.R;
    import com.example.gabay.fragments.BaseFragment;
    import com.example.gabay.viewmodels.ProgressViewModel;
    import com.example.gabay.fragments.pages.HomePage;


    public class ProfilePage extends BaseFragment {

        private TextView usernameTextView;
        private TextView chaptersCompletedTextView;
        private TextView quizzesPassedTextView;
        private android.widget.ProgressBar overallProgressBar;
        private ImageButton settingsButton;

        private ProgressViewModel progressViewModel;
        private HomePage homePage;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_profile_page, container, false);
        }

        @Override
        protected void initializeViews() {
            // Initialize ProgressViewModel
            progressViewModel = getActivityViewModel(ProgressViewModel.class);

            initializeProfileUI();
            setupProgressObservers();
            loadUserProfile();
        }

        private void initializeProfileUI() {
            if (rootView == null) return;

            usernameTextView = rootView.findViewById(R.id.edit_username);
            chaptersCompletedTextView = rootView.findViewById(R.id.chapters_completed_text);
            quizzesPassedTextView = rootView.findViewById(R.id.quizzes_passed_text);
            overallProgressBar = rootView.findViewById(R.id.progress_overall); // same as the @id/progress_bar in the homepage(should be synced)
            settingsButton = rootView.findViewById(R.id.settings_button);
            View changePictureButton = rootView.findViewById(R.id.change_picture_button);

            // Setup settings open
            if (settingsButton != null) {
                settingsButton.setOnClickListener(v -> openSettings());
            }

            // Username is a placeholder for now; editing will be enabled when DB is wired

            // Stub for changing picture
            if (changePictureButton != null) {
                changePictureButton.setOnClickListener(v -> {
                    android.widget.Toast.makeText(requireContext(), "Change picture coming soon", android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        }

        private void openSettings() {
            if (getActivity() != null) {
                android.content.Intent intent = new android.content.Intent(getActivity(), com.example.gabay.activities.SettingsActivity.class);
                startActivity(intent);
            }
        }

        private void setupProgressObservers() {
            if (progressViewModel == null) return;

            // Observe user profile changes
            progressViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
                if (isFragmentActive()) {
                    updateProfileDisplay(profile);
                }
            });

            // Observe chapter progress changes
            progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
                if (isFragmentActive()) {
                    updateProgressDisplay();
                }
            });
        }

        private void loadUserProfile() {
            if (progressViewModel == null) return;

            // Load current profile data
            ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
            if (profile != null) {
                updateProfileDisplay(profile);
            }

            updateProgressDisplay();
        }

        private void updateProfileDisplay(ProgressViewModel.UserProfile profile) {
            if (usernameTextView != null) {
                usernameTextView.setText("Cian Bernales");

                // will be implemented when database is all good
                //usernameTextView.setText(profile.getUsername());
            }
        }

        protected void updateProgressDisplay() {
            if (progressViewModel == null) return;

            // FIX: Get value from LiveData properly
            Integer totalProgressValue = progressViewModel.getTotalProgress().getValue();
            int totalProgress = (totalProgressValue != null) ? totalProgressValue : 0;

            ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();

            if (overallProgressBar != null) {
                overallProgressBar.setProgress(totalProgress);
            }

            if (chaptersCompletedTextView != null && profile != null) {
                // For now, calculate completed chapters from progress
                int completedChapters = calculateCompletedChapters();
                chaptersCompletedTextView.setText(completedChapters + "/5");

                // Uncomment when you want to use profile data:
                // chaptersCompletedTextView.setText(profile.getTotalChaptersCompleted() + "/5");
            }

            if (quizzesPassedTextView != null && profile != null) {
                quizzesPassedTextView.setText(String.valueOf(profile.getTotalQuizzesPassed()));

                // Uncomment when you want to show label:
                // quizzesPassedTextView.setText("Quizzes Passed: " + profile.getTotalQuizzesPassed());
            }
        }

        // ADD this helper method right after updateProgressDisplay()
        private int calculateCompletedChapters() {
            if (progressViewModel == null) return 0;

            int completedCount = 0;
            for (int chapter = 1; chapter <= 5; chapter++) {
                ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(chapter);
                if (progress != null && progress.getProgressPercentage() >= 100) {
                    completedCount++;
                }
            }
            return completedCount;
        }
        @Override
        protected void cleanupResources() {
            usernameTextView = null;
            chaptersCompletedTextView = null;
            quizzesPassedTextView = null;
            overallProgressBar = null;
            settingsButton = null;
            progressViewModel = null;
        }
    }
