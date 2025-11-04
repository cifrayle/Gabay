package com.example.gabay.fragments.pages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.gabay.R;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.viewmodels.ProgressViewModel;

import java.util.Map;

public class ProfilePage extends BaseFragment {

    private TextView usernameTextView, userEmail;
    private TextView chaptersCompletedTextView;
    private TextView quizzesPassedTextView;
    private TextView overallProgressTextView;
    private android.widget.ProgressBar overallProgressBar;
    private ImageButton settingsButton;

    private ProgressViewModel progressViewModel;

    // Level progress TextViews for each chapter - FIXED VARIABLE NAMES
    private TextView chapter1LevelProgress, chapter2LevelProgress, chapter3LevelProgress,
            chapter4LevelProgress, chapter5LevelProgress;

    // State tracking for inline editing
    private boolean isEditing = false;
    private String originalUsername = "";
    private boolean isUpdatingUsername = false;

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int CAMERA_REQUEST = 101;
    private static final int PERMISSION_CAMERA_REQUEST = 200;
    private static final int PERMISSION_GALLERY_REQUEST = 201;
    private ImageView profilePicture;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        setProfilePictureFromBitmap(imageBitmap);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) setProfilePicture(imageUri);
                }
            });

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
        loadProfilePicture();
    }

    private void initializeProfileUI() {
        if (rootView == null) return;

        usernameTextView = rootView.findViewById(R.id.edit_username);
        chaptersCompletedTextView = rootView.findViewById(R.id.chapters_completed_text);
        quizzesPassedTextView = rootView.findViewById(R.id.quizzes_passed_text);

        overallProgressBar = rootView.findViewById(R.id.progress_overall);
        overallProgressTextView = rootView.findViewById(R.id.progress_percent_text); // must be synced with progress bar

        settingsButton = rootView.findViewById(R.id.settings_button);

        // email
        userEmail = rootView.findViewById(R.id.user_email);

        // level progress - FIXED: Using consistent variable names
        chapter1LevelProgress = rootView.findViewById(R.id.chapter1_lvl_progress);
        chapter2LevelProgress = rootView.findViewById(R.id.chapter2_lvl_progress);
        chapter3LevelProgress = rootView.findViewById(R.id.chapter3_lvl_progress);
        chapter4LevelProgress = rootView.findViewById(R.id.chapter4_lvl_progress);
        chapter5LevelProgress = rootView.findViewById(R.id.chapter5_lvl_progress);

        profilePicture = rootView.findViewById(R.id.profile_picture);
        View changePictureButton = rootView.findViewById(R.id.change_picture_button);

        // Setup advanced inline editing
        setupInlineEditing();

        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> openSettings());
        }

        if (changePictureButton != null) {
            changePictureButton.setOnClickListener(v -> {
                showImagePickerOptions();
            });
        }
    }
    // Add these methods to your ProfilePage class
    private void showImagePickerOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Take Photo
                    if (checkCameraPermission()) {
                        openCamera();
                    }
                    break;
                case 1: // Choose from Gallery
                    if (checkStoragePermission()) {
                        openGallery();
                    }
                    break;
                case 2: // Cancel
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }
    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission granted, retry the action
            switch (requestCode) {
                case PERMISSION_CAMERA_REQUEST:
                    openCamera();
                    break;
                case PERMISSION_GALLERY_REQUEST:
                    openGallery();
                    break;
            }
        } else {
            // Permission denied
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    // Check camera permission
    private boolean checkCameraPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST);
                return false;
            }
        }
        return true;
    }

    // Check storage permission (handle differently for different Android versions)
    private boolean checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ only needs READ_MEDIA_IMAGES
            if (requireContext().checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_GALLERY_REQUEST);
                return false;
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android 6-12 needs READ_EXTERNAL_STORAGE
            if (requireContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_GALLERY_REQUEST);
                return false;
            }
        }
        return true;
    }

    private void openCamera() {
        if (getActivity() != null) {
            Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(requireContext(), "No camera app available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        galleryLauncher.launch(Intent.createChooser(intent, "Select Picture"));

    }

    private void setProfilePicture(Uri imageUri) {
        try {
            // Load and set the image
            Glide.with(requireContext())
                    .load(imageUri)
                    .circleCrop()
                    .into(profilePicture);

            // Save the image URI to SharedPreferences or your database
            saveProfilePictureUri(imageUri.toString());

            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            Log.e("ProfilePage", "Error setting profile picture: " + e.getMessage());
        }
    }

    private void setProfilePictureFromBitmap(Bitmap bitmap) {
        try {
            // Set the bitmap to ImageView
            profilePicture.setImageBitmap(bitmap);

            // Save the bitmap to storage and get URI
            Uri imageUri = saveBitmapToStorage(bitmap);
            if (imageUri != null) {
                saveProfilePictureUri(imageUri.toString());
            }

            Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            Log.e("ProfilePage", "Error setting profile picture from bitmap: " + e.getMessage());
        }
    }

    // Save the profile picture URI to SharedPreferences
    private void saveProfilePictureUri(String uriString) {
        String key = getProfilePictureKey();
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString(key, uriString).apply();
        Log.d("ProfilePage", "Profile picture saved with key: " + key);
    }

    // Load the saved profile picture
    private void loadProfilePicture() {
        String key = getProfilePictureKey();
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE);
        String uriString = prefs.getString(key, null);

        Log.d("ProfilePage", "Loading profile picture with key: " + key);

        if (uriString != null && profilePicture != null) {
            try {
                Uri imageUri = Uri.parse(uriString);
                Glide.with(requireContext())
                        .load(imageUri)
                        .circleCrop()
                        .into(profilePicture);
                Log.d("ProfilePage", "Profile picture loaded successfully");
            } catch (Exception e) {
                Log.e("ProfilePage", "Error loading profile picture: " + e.getMessage());
                loadDefaultProfilePicture();
            }
        } else {
            loadDefaultProfilePicture();
        }
    }
    private void loadDefaultProfilePicture() {
        if (profilePicture != null) {
            // Use your app's default profile picture
            profilePicture.setImageResource(R.drawable.avatar); // Replace with your actual drawable
        }
    }

    // Helper method to save bitmap to internal storage
    private Uri saveBitmapToStorage(Bitmap bitmap) {
        try {
            String userId = getCurrentUserId();
            java.io.File file = new java.io.File(requireContext().getFilesDir(), "profile_picture_" + userId + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Log.d("ProfilePage", "Bitmap saved to: " + file.getAbsolutePath());
            return Uri.fromFile(file);
        } catch (Exception e) {
            Log.e("ProfilePage", "Error saving bitmap: " + e.getMessage());
            return null;
        }
    }

    private String getCurrentUserId() {
        if (progressViewModel != null) {
            ProgressViewModel.UserProfile profile = progressViewModel.getUserProfile().getValue();
            if (profile != null && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                String email = profile.getEmail();
                // Use email as identifier (hash for consistency)
                return "user_" + Math.abs(email.hashCode());
            }
        }
        return "default_user";
    }
    private String getProfilePictureKey() {
        return "profile_picture_uri_" + getCurrentUserId();
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

        // Observe user profile changes - RELOAD PROFILE PICTURE WHEN USER CHANGES
        progressViewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (isFragmentActive()) {
                updateProfileDisplay(profile);
                updateProgressDisplay();

                // RELOAD PROFILE PICTURE FOR THE CURRENT USER
                new Handler().postDelayed(this::loadProfilePicture, 100);
            }
        });

        // Observe chapter progress changes
        progressViewModel.getAllChapterProgress().observe(getViewLifecycleOwner(), progressMap -> {
            if (isFragmentActive()) {
                Log.d("ProfileDebug", "Chapter progress changed - updating display");
                updateIndividualChapterProgress(progressMap);
                updateProgressDisplay();
            }
        });

        // Observe total progress changes
        progressViewModel.getTotalProgress().observe(getViewLifecycleOwner(), totalProgress -> {
            if (isFragmentActive()) {
                updateProgressDisplay();
            }
        });
    }
    private void updateIndividualChapterProgress(Map<Integer, Integer> progressMap) {
        if (progressMap == null || progressMap.isEmpty()) {
            Log.d("ProfilePage", "No chapter progress data available");
            setDefaultChapterProgress();
            return;
        }

        Log.d("ProfilePage", "Updating individual chapter progress: " + progressMap.toString());

        // Update each chapter's progress display
        for (int chapter = 1; chapter <= 5; chapter++) {
            int completedLevels = progressMap.getOrDefault(chapter, 0);
            int maxLevels = getMaxLevelsForChapter(chapter);

            String progressText;
            if (completedLevels >= maxLevels) {
                // All levels completed - show completion message
                progressText = "All levels completed! 🎉";
            } else {
                // Show normal progress
                progressText = completedLevels + "/" + maxLevels + " levels completed";
            }

            updateChapterProgressView(chapter, progressText);
        }
    }

    private void setDefaultChapterProgress() {
        for (int chapter = 1; chapter <= 5; chapter++) {
            int maxLevels = getMaxLevelsForChapter(chapter);
            String progressText = "0/" + maxLevels + " levels completed";
            updateChapterProgressView(chapter, progressText);
        }
    }

    private int getMaxLevelsForChapter(int chapter) {
        switch (chapter) {
            case 1: return 28;
            case 2: return 5;
            case 3: return 10;
            case 4: return 7;
            case 5: return 7;
            default: return 28;
        }
    }

    // Update specific chapter progress view
    private void updateChapterProgressView(int chapter, String progressText) {
        TextView progressView = null;

        switch (chapter) {
            case 1:
                progressView = chapter1LevelProgress;
                break;
            case 2:
                progressView = chapter2LevelProgress;
                break;
            case 3:
                progressView = chapter3LevelProgress;
                break;
            case 4:
                progressView = chapter4LevelProgress;
                break;
            case 5:
                progressView = chapter5LevelProgress;
                break;
        }

        if (progressView != null) {
            progressView.setText(progressText);
            Log.d("ProfilePage", "Updated chapter " + chapter + " progress: " + progressText);
        } else {
            Log.d("ProfilePage", "Progress view not found for chapter " + chapter);
        }
    }

    private void updateProfileDisplay(ProgressViewModel.UserProfile profile) {
        if (profile != null) {
            // Update username (only if not currently updating)
            if (usernameTextView != null && !isUpdatingUsername) {
                usernameTextView.setText(profile.getUsername());
                usernameTextView.setClickable(true);
                Log.d("ProfilePage", "Displaying username: " + profile.getUsername());
            }

            // UPDATE: Set the user's email from profile
            if (userEmail != null && profile.getEmail() != null) {
                userEmail.setText(profile.getEmail());
                Log.d("ProfilePage", "Displaying email: " + profile.getEmail());
            } else if (userEmail != null) {
                userEmail.setText("user@example.com");
            }

        } else if (usernameTextView != null && !isUpdatingUsername) {
            // Fallback if profile is null and not updating
            usernameTextView.setText("User");
            usernameTextView.setClickable(true);
            if (userEmail != null) {
                userEmail.setText("user@example.com");
            }
            Log.d("ProfilePage", "Using fallback user data");
        }
    }

    private void loadUserProfile() {
        if (progressViewModel == null) return;

        // Force reload from Supabase when profile page opens
        progressViewModel.loadUserProfileFromSupabase();
        progressViewModel.refreshProgressFromSupabase(); // Also refresh progress data

        updateProgressDisplay();
    }

    protected void updateProgressDisplay() {
        if (progressViewModel == null) return;

        // Get value from LiveData properly
        Integer totalProgressValue = progressViewModel.getTotalProgress().getValue();
        int totalProgress = (totalProgressValue != null) ? totalProgressValue : 0;

        // UPDATE: Sync progress bar with text view
        if (overallProgressBar != null) {
            overallProgressBar.setProgress(totalProgress);
        }

        // NEW: Update the progress text view to match the progress bar
        if (overallProgressTextView != null) {
            overallProgressTextView.setText(totalProgress + "%");
            Log.d("ProfilePage", "Overall progress updated: " + totalProgress + "%");
        }

        if (chaptersCompletedTextView != null) {
            int completedChapters = calculateCompletedChapters();
            chaptersCompletedTextView.setText(completedChapters + "/5");
        }

        if (quizzesPassedTextView != null) {
            int quizzesPassed = calculateQuizzesPassed();
            quizzesPassedTextView.setText(quizzesPassed + "/5");
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

        // Ensure profile picture is loaded for current user
        new Handler().postDelayed(this::loadProfilePicture, 200);
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
        overallProgressTextView = null;
        settingsButton = null;
        progressViewModel = null;
    }
}