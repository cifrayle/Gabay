package com.example.gabay.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

/**
 * Enhanced base fragment class that provides common functionality for all fragments
 * Includes lifecycle management, view binding optimization, and common utilities
 * Designed to support future features: profile pages, quizzes, progress tracking
 */
public abstract class BaseFragment extends Fragment {

    protected Context fragmentContext;
    protected View rootView;
    protected boolean isViewCreated = false;
    protected boolean isFragmentActive = false;

    // Progress tracking support for future quiz features
    protected int currentChapter = 1;
    protected int currentLevel = 1;
    protected int totalProgress = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        fragmentContext = context;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        isViewCreated = true;
        isFragmentActive = true;
        
        initializeViews();
        setupObservers();
        setupProgressTracking();
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        updateProgressDisplay();
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
        isFragmentActive = false;
        cleanupResources();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragmentContext = null;
    }

    /**
     * Initialize views and set up click listeners
     * Override this method in child fragments
     */
    protected abstract void initializeViews();

    /**
     * Set up observers for LiveData or other reactive components
     * Override this method in child fragments
     */
    protected void setupObservers() {
        // Default implementation - override if needed
    }

    /**
     * Set up progress tracking for chapters and levels
     * Override this method in child fragments that need progress tracking
     */
    protected void setupProgressTracking() {
        // Default implementation - override if needed
    }

    /**
     * Update progress display (for circular progress bars, etc.)
     * Override this method in child fragments that show progress
     */
    protected void updateProgressDisplay() {
        // Default implementation - override if needed
    }

    /**
     * Clean up resources when fragment is destroyed
     * Override this method in child fragments
     */
    protected void cleanupResources() {
        // Default implementation - override if needed
    }

    /**
     * Check if fragment view is created and safe to use
     */
    protected boolean isFragmentActive() {
        return isViewCreated && isAdded() && !isDetached() && isFragmentActive;
    }

    /**
     * Safe way to run code on UI thread
     */
    protected void runOnUiThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }

    /**
     * Get ViewModel with proper lifecycle scope
     */
    protected <T extends androidx.lifecycle.ViewModel> T getViewModel(Class<T> modelClass) {
        return new ViewModelProvider(this).get(modelClass);
    }

    /**
     * Get ViewModel with activity scope for shared data
     */
    protected <T extends androidx.lifecycle.ViewModel> T getActivityViewModel(Class<T> modelClass) {
        return new ViewModelProvider(requireActivity()).get(modelClass);
    }

    /**
     * Set current chapter and level for progress tracking
     */
    protected void setCurrentProgress(int chapter, int level) {
        this.currentChapter = chapter;
        this.currentLevel = level;
        updateProgressDisplay();
    }

    /**
     * Get current progress as percentage
     */
    protected int getProgressPercentage() {
        return totalProgress;
    }

    /**
     * Update total progress
     */
    protected void updateTotalProgress(int progress) {
        this.totalProgress = Math.min(100, Math.max(0, progress));
        updateProgressDisplay();
    }

    /**
     * Check if quiz should be shown (every 5 levels)
     */
    protected boolean shouldShowQuiz() {
        return currentLevel % 5 == 0;
    }

    /**
     * Navigate to quiz fragment
     */
    protected void navigateToQuiz(int chapter, int level) {
        // This will be implemented when quiz feature is added
        // For now, just log the navigation
        android.util.Log.d("BaseFragment", "Navigate to quiz for Chapter " + chapter + " Level " + level);
    }

    /**
     * Navigate to profile page
     */
    protected void navigateToProfile() {
        // This will be implemented when profile feature is added
        android.util.Log.d("BaseFragment", "Navigate to profile page");
    }
}
