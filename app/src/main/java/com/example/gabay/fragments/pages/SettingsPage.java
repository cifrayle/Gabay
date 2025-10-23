package com.example.gabay.fragments.pages;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gabay.R;
import com.example.gabay.activities.AuthActivity;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.viewmodels.ProgressViewModel;

public class SettingsPage extends BaseFragment {

    private Button logoutButton;
    private Switch soundSwitch;
    private ProgressViewModel progressViewModel;

    private static final String PREFS_NAME = "AppSettings";
    private static final String SOUND_PREF_KEY = "sound_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_page, container, false);
    }

    @Override
    protected void initializeViews() {
        // Initialize ProgressViewModel
        progressViewModel = getActivityViewModel(ProgressViewModel.class);
        
        // Initialize logout button
        if (rootView != null) {
            logoutButton = rootView.findViewById(R.id.logout_button);
            soundSwitch = rootView.findViewById(R.id.switch_soundeffects);

            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean soundEnabled = prefs.getBoolean(SOUND_PREF_KEY, true);
            soundSwitch.setChecked(soundEnabled);

            // Save preference when toggled
            soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(SOUND_PREF_KEY, isChecked);
                editor.apply();
            });
            if (logoutButton != null) {
                logoutButton.setOnClickListener(v -> logout());
            }
        }
    }

    private void logout() {
        if (getActivity() != null) {
            new androidx.appcompat.app.AlertDialog.Builder(getActivity())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        performLogout();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    private void performLogout() {
        // 1. Clear Supabase authentication state
        SupabaseJavaService.clearAuth();

        // 2. Clear all progress data
        if (progressViewModel != null) {
            progressViewModel.resetAllProgress();
        }

        // 3. Clear SharedPreferences (credentials and tokens)
        clearAuthenticationData();

        // 4. Sign out from Google (if using Google Sign-In)
        signOutFromGoogle();

        // 5. Show confirmation message
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // 6. Navigate back to AuthActivity
        navigateToAuthActivity();
    }

    private void clearAuthenticationData() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    private void signOutFromGoogle() {
        try {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).signOutFromGoogle();
            }
        } catch (Exception e) {
            Log.e("SettingsPage", "Error signing out from Google: " + e.getMessage());
        }
    }

    private void navigateToAuthActivity() {
        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    protected void cleanupResources() {
        logoutButton = null;
        soundSwitch = null;
        progressViewModel = null;
    }
}