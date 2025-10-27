package com.example.gabay.fragments.pages;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gabay.R;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.viewmodels.ProgressViewModel;

import java.util.Map;

public class ProfilePage extends BaseFragment {

    private TextView usernameTextView;
    private TextView chaptersCompletedTextView;
    private TextView quizzesPassedTextView;
    private android.widget.ProgressBar overallProgressBar;
    private ImageButton settingsButton;

    private ProgressViewModel progressViewModel;
    private HomePage homePage;
    private TextView levelProgressSubtitle;

    // State tracking for inline editing
    private boolean isEditing = false;
    private String originalUsername = "";
    private boolean isUpdatingUsername = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_page, container, false);
    }

    @Override
    protected void initializeViews() {
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
        overallProgressBar = rootView.findViewById(R.id.progress_overall);
        settingsButton = rootView.findViewById(R.id.settings_button);
        View changePictureButton = rootView.findViewById(R.id.change_picture_button);
        levelProgressSubtitle = rootView.findViewById(R.id.level_progress_subtitle);

        // Setup advanced inline editing
        setupInlineEditing();

        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> openSettings());
        }

        // Stub for changing picture
        if (changePictureButton != null) {
            changePictureButton.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "Change picture coming soon", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupInlineEditing() {
        if (usernameTextView != null) {
            // Make it look interactive
            usernameTextView.setClickable(true);
            usernameTextView.setFocusable(true);
            usernameTextView.setBackgroundResource(R.drawable.username_editable_background);
            usernameTextView.setPadding(32, 16, 32, 16);

            // Add click listener
            usernameTextView.setOnClickListener(v -> {
                if (!isEditing) {
                    enableInlineEditing();
                }
            });

            // Add long click listener for additional options
            usernameTextView.setOnLongClickListener(v -> {
                showEditOptions();
                return true;
            });
        }
    }

    private void enableInlineEditing() {
        if (isEditing) return;

        isEditing = true;
        originalUsername = usernameTextView.getText().toString();

        // Create EditText with same styling
        final EditText editText = new EditText(requireContext());
        editText.setText(originalUsername);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setSelectAllOnFocus(true);
        editText.setTextSize(18); // Match your TextView text size
        editText.setTextColor(usernameTextView.getCurrentTextColor());
        editText.setBackgroundResource(R.drawable.username_editing_background);
        editText.setPadding(32, 16, 32, 16);

        // Set layout parameters to match TextView
        ViewGroup.LayoutParams params = usernameTextView.getLayoutParams();
        editText.setLayoutParams(params);

        // Replace TextView with EditText in the layout
        ViewGroup parent = (ViewGroup) usernameTextView.getParent();
        int index = parent.indexOfChild(usernameTextView);
        parent.removeView(usernameTextView);
        parent.addView(editText, index);

        // Request focus and show keyboard
        editText.requestFocus();
        showKeyboard(editText);

        // Set up action listeners
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveInlineEdit(editText, parent, index);
                return true;
            }
            return false;
        });

        // Handle focus loss (user taps elsewhere)
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && isEditing) {
                saveInlineEdit(editText, parent, index);
            }
        });

        // Add a subtle animation
        editText.setAlpha(0f);
        editText.animate().alpha(1f).setDuration(200).start();

        Log.d("ProfilePage", "Inline editing enabled");
    }

    private void saveInlineEdit(EditText editText, ViewGroup parent, int originalIndex) {
        if (!isEditing) return;

        String newUsername = editText.getText().toString().trim();
        hideKeyboard(editText);

        // Restore TextView first
        parent.removeView(editText);
        parent.addView(usernameTextView, originalIndex);

        if (!newUsername.isEmpty() && !newUsername.equals(originalUsername)) {
            // Validate username
            if (isValidUsername(newUsername)) {
                updateUsername(newUsername);
            } else {
                // Invalid username - revert to original
                usernameTextView.setText(originalUsername);
                android.widget.Toast.makeText(requireContext(), "Invalid username format", android.widget.Toast.LENGTH_SHORT).show();
            }
        } else {
            // No change or empty - restore original
            usernameTextView.setText(originalUsername);
            if (newUsername.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Username cannot be empty", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(requireContext(), "No changes made", android.widget.Toast.LENGTH_SHORT).show();
            }
        }

        isEditing = false;
        Log.d("ProfilePage", "Inline editing completed");
    }

    private boolean isValidUsername(String username) {
        // Add your username validation logic here
        return username.length() >= 3 && username.length() <= 20;
    }

    private void cancelInlineEdit(EditText editText, ViewGroup parent, int originalIndex) {
        hideKeyboard(editText);

        // Restore TextView with original text
        parent.removeView(editText);
        parent.addView(usernameTextView, originalIndex);
        usernameTextView.setText(originalUsername);

        isEditing = false;
        android.widget.Toast.makeText(requireContext(), "Edit cancelled", android.widget.Toast.LENGTH_SHORT).show();
        Log.d("ProfilePage", "Inline editing cancelled");
    }

    private void showEditOptions() {
        String[] options = {"Edit Display Name", "Cancel"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Display Name Options");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Edit Display Name
                if (!isEditing) {
                    enableInlineEditing();
                }
            }
        });
        builder.show();
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void updateUsername(String newUsername) {
        if (progressViewModel != null) {
            isUpdatingUsername = true; // Set flag

            // Show updating state
            usernameTextView.setText("Updating...");
            usernameTextView.setClickable(false);

            // Observe the update result
            progressViewModel.getUsernameUpdateResult().observe(getViewLifecycleOwner(), success -> {
                if (success != null) {
                    if (success) {
                        // Update successful - set the new username
                        usernameTextView.setText(newUsername);
                        android.widget.Toast.makeText(requireContext(), "Username updated successfully", android.widget.Toast.LENGTH_SHORT).show();

                        // Clear the flag after a short delay to allow profile to sync
                        new Handler().postDelayed(() -> {
                            isUpdatingUsername = false;
                        }, 2000);
                    } else {
                        // Update failed - revert to original
                        usernameTextView.setText(originalUsername);
                        android.widget.Toast.makeText(requireContext(), "Failed to update username", android.widget.Toast.LENGTH_SHORT).show();
                        isUpdatingUsername = false;
                    }
                    // Re-enable editing
                    usernameTextView.setClickable(true);
                }
            });

            progressViewModel.updateUsernameInSupabase(newUsername);
            Log.d("ProfilePage", "Username update initiated: " + newUsername);
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
                updateProgressDisplay(); // Also update progress when profile changes
            }
        });

        // Observe chapter progress changes - THIS IS THE KEY OBSERVER
        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (isFragmentActive()) {
                Log.d("ProfileDebug", "Chapter progress changed - updating display");
                updateProgressDisplay();
            }
        });

        // Observe total progress changes
        progressViewModel.getTotalProgress().observe(getViewLifecycleOwner(), totalProgress -> {
            if (isFragmentActive()) {
                updateProgressDisplay();
            }
        });

        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (levelProgressSubtitle == null) return;

            if (progressMap != null && !progressMap.isEmpty()) {
                int totalCompleted = 0;
                int totalLevels = 0;

                // Loop through each chapter and calculate completed + total levels
                for (Map.Entry<Integer, Integer> entry : progressMap.entrySet()) {
                    int chapter = entry.getKey();
                    int completed = entry.getValue();
                    int maxLevels = progressViewModel.getChapterProgressObject(chapter).getMaxLevels();

                    totalCompleted += completed;
                    totalLevels += maxLevels;
                }

                // Fallback if something goes wrong
                if (totalLevels == 0) totalLevels = 57;

                String progressText = totalCompleted + "/" + totalLevels + " levels completed";
                levelProgressSubtitle.setText(progressText);

            } else {
                // When no progress data available yet
                levelProgressSubtitle.setText("0/57 levels completed");
            }
        });
    }

    private void updateProfileDisplay(ProgressViewModel.UserProfile profile) {
        if (usernameTextView != null && profile != null && !isUpdatingUsername) {
            // Only update if we're not in the middle of a username update
            usernameTextView.setText(profile.getUsername());
            usernameTextView.setClickable(true);
            Log.d("ProfilePage", "Displaying username: " + profile.getUsername());
        } else if (usernameTextView != null && !isUpdatingUsername) {
            // Fallback if profile is null and not updating
            usernameTextView.setText("User");
            usernameTextView.setClickable(true);
            Log.d("ProfilePage", "Using fallback username");
        }
        // If isUpdatingUsername is true, we skip the update to avoid overwriting
    }

    private void loadUserProfile() {
        if (progressViewModel == null) return;

        // Force reload from Supabase when profile page opens
        progressViewModel.loadUserProfileFromSupabase();

        // Load current profile data
        ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
        if (profile != null) {
            updateProfileDisplay(profile);
        }

        updateProgressDisplay();
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

        if (chaptersCompletedTextView != null) {
            int completedChapters = calculateCompletedChapters();
            chaptersCompletedTextView.setText(completedChapters + "/5");
        }

        if (quizzesPassedTextView != null) {
            int quizzesPassed = calculateQuizzesPassed();
            quizzesPassedTextView.setText(String.valueOf(quizzesPassed));
        }
    }

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

    private int calculateQuizzesPassed() {
        if (progressViewModel == null) return 0;

        int quizzesPassed = 0;

        // Assume quiz is passed when chapter is 100% completed
        for (int chapter = 1; chapter <= 5; chapter++) {
            ProgressViewModel.ChapterProgress progress = progressViewModel.getChapterProgressObject(chapter);
            if (progress != null && progress.getProgressPercentage() >= 100) {
                quizzesPassed++;
                Log.d("ProfilePage", "Chapter " + chapter + " completed - quiz counted as passed");
            }
        }

        Log.d("ProfilePage", "Total quizzes passed (based on chapter completion): " + quizzesPassed);
        return quizzesPassed;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to profile page
        loadUserProfile();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Ensure editing is cancelled if user leaves the fragment
        if (isEditing) {
            // Force cancel any ongoing editing
            isEditing = false;
            hideKeyboard(requireView());
        }
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