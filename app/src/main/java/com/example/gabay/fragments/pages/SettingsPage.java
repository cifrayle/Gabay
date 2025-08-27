package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gabay.R;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.viewmodels.ProgressViewModel;

public class SettingsPage extends BaseFragment {

    private Button logoutButton;
    private ProgressViewModel progressViewModel;

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
                    if (progressViewModel != null) {
                        progressViewModel.resetAllProgress();
                    }
                    // You can add additional logout logic here
                    android.widget.Toast.makeText(requireContext(), "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
        }
    }

    @Override
    protected void cleanupResources() {
        logoutButton = null;
        progressViewModel = null;
    }
}