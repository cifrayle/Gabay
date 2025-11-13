package com.example.gabay.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.gabay.R;
import com.example.gabay.fragments.pages.GabAIPage;
import com.example.gabay.fragments.pages.HomePage;
import com.example.gabay.fragments.pages.ProfilePage;
import com.example.gabay.fragments.pages.SignInPage;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.viewmodels.ProgressViewModel;
import com.example.gabay.utils.FragmentStateManager;
import com.example.gabay.utils.SessionTracker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Map<Integer, Fragment> fragmentCache;
    private Fragment currentFragment;
    private FragmentManager fragmentManager;
    private FragmentStateManager fragmentStateManager;

    private ProgressViewModel progressViewModel;

    private android.widget.TextView actionBarTitle;
    private android.view.View actionBarContainer;
    private android.widget.ImageButton backButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Gabay);
        setContentView(R.layout.activity_main);

        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        restoreSupabaseAuthentication();

        fragmentManager = getSupportFragmentManager();
        fragmentCache = new HashMap<>();
        fragmentStateManager = new FragmentStateManager(fragmentManager);

        actionBarContainer = findViewById(R.id.custom_action_bar_container);
        actionBarTitle = findViewById(R.id.action_bar_title);
        backButton = findViewById(R.id.levels_back_button);

        // Apply window insets to action bar for proper status bar handling
        if (actionBarContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(actionBarContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                return insets;
            });
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
        
        fragmentManager.addOnBackStackChangedListener(this::updateActionBarForTopFragment);

        BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
        bottomNavView.setOnItemSelectedListener(navListener);

        // default page
        bottomNavView.setSelectedItemId(R.id.nav_Home);
        
        // Start session tracking
        SessionTracker.getInstance(this).startSession();
    }

    private void restoreSupabaseAuthentication() {
        SharedPreferences prefs = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);

        if (accessToken != null && !accessToken.isEmpty()) {
            // Parse the JWT to get user ID
            try {
                String userId = parseUserIdFromToken(accessToken);
                SupabaseJavaService.setAccessToken(accessToken, userId);
                Log.d("MainActivity", "✅ SupabaseService restored with saved token");
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to restore SupabaseService: " + e.getMessage());
            }
        }
    }

    private String parseUserIdFromToken(String token) {
        try {
            // JWT format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                // Decode the payload (second part)
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
                JSONObject json = new JSONObject(payload);
                return json.getString("sub"); // "sub" claim contains user ID
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error parsing token: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        fragmentStateManager.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fragmentStateManager.restoreState(savedInstanceState);
    }


    private NavigationBarView.OnItemSelectedListener navListener = item -> {
        int itemId = item.getItemId();
        Fragment selectedFragment = getOrCreateFragment(itemId);

        if (selectedFragment != null && selectedFragment != currentFragment) {
            switchFragment(selectedFragment);
            currentFragment = selectedFragment;
            fragmentStateManager.setCurrentFragment(selectedFragment);
        }
        return true;
    };

    public void hideBottomNav() {
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
        if (bottomNavView != null) {
            bottomNavView.setVisibility(android.view.View.GONE);
        }
    }

    public void showBottomNav() {
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
        if (bottomNavView != null) {
            bottomNavView.setVisibility(android.view.View.VISIBLE);
        }
    }

    private Fragment getOrCreateFragment(int itemId) {
        // Check if fragment exists in cache
        if (fragmentCache.containsKey(itemId)) {
            return fragmentCache.get(itemId);
        }

        // Create new fragment if not cached
        Fragment newFragment = null;
        String tag = getFragmentTag(itemId);

        if (itemId == R.id.nav_Home) {
            newFragment = fragmentStateManager.getOrCreateFragment(HomePage.class, tag);
        } else if (itemId == R.id.nav_GabAI) {
            newFragment = fragmentStateManager.getOrCreateFragment(GabAIPage.class, tag);
        } else if (itemId == R.id.nav_Profile) {
            newFragment = fragmentStateManager.getOrCreateFragment(ProfilePage.class, tag);
        }

        if (newFragment != null) {
            fragmentCache.put(itemId, newFragment);
        }

        return newFragment;
    }

    private String getFragmentTag(int itemId) {
        if (itemId == R.id.nav_Home) return "home_fragment";
        if (itemId == R.id.nav_GabAI) return "gabai_fragment";
        if (itemId == R.id.nav_Profile) return "profile_fragment";
        return "unknown_fragment";
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out
        );

        // Hide current fragment if it exists
        if (currentFragment != null && currentFragment.isAdded()) {
            transaction.hide(currentFragment);
        }

        // Show the new fragment (add if not already added)
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, getFragmentTagFromFragment(fragment));
        } else {
            transaction.show(fragment);
        }

        transaction.commitNow();
        currentFragment = fragment;
        fragmentStateManager.setCurrentFragment(fragment);

        // Update action bar
        updateActionBarForFragment(fragment);
    }

    // Helper method to get tag from fragment
    private String getFragmentTagFromFragment(Fragment fragment) {
        if (fragment instanceof HomePage) return "home_fragment";
        if (fragment instanceof GabAIPage) return "gabai_fragment";
        if (fragment instanceof ProfilePage) return "profile_fragment";
        return "unknown_fragment";
    }

    private void updateActionBarForFragment(Fragment fragment) {
        if (actionBarContainer == null) return;

        if (fragment instanceof com.example.gabay.fragments.chapters.Chapter1) {
            showActionBarWithTitleAndBackButton("Chapter 1");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter2) {
            showActionBarWithTitleAndBackButton("Chapter 2");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter3) {
            showActionBarWithTitleAndBackButton("Chapter 3");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter4) {
            showActionBarWithTitleAndBackButton("Chapter 4");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter5) {
            showActionBarWithTitleAndBackButton("Chapter 5");
        } else if (fragment instanceof HomePage ||
                fragment instanceof GabAIPage ||
                fragment instanceof ProfilePage) {
            // Hide action bar for main bottom nav pages
            hideActionBar();
        } else {
            // For any other fragments, hide action bar by default
            hideActionBar();
        }
    }

    public void showActionBarWithTitleAndBackButton(String title) {
        showActionBarWithTitle(title, null);

        // Ensure back button is visible and functional
        if (backButton != null) {
            backButton.setVisibility(View.VISIBLE);
            // The click listener is already set in onCreate, so we don't need to set it again
        }
    }

    public void showActionBarWithTitle(String title) {
        showActionBarWithTitle(title, null);
    }
    public void showActionBarWithTitle(String title, String subtitle) {
        if (actionBarContainer != null) {
            actionBarContainer.setVisibility(android.view.View.VISIBLE);
        }
        if (actionBarTitle != null) {
            actionBarTitle.setText(title);
        }
    }

    public void hideActionBar() {
        if (actionBarContainer != null) {
            actionBarContainer.setVisibility(android.view.View.GONE);
        }
    }
    public void showActionBar() {
        if (actionBarContainer != null) {
            actionBarContainer.setVisibility(View.VISIBLE);
        }
    }

    private void updateActionBarForTopFragment() {
        Fragment top = fragmentManager.findFragmentById(R.id.fragment_container);
        if (top != null) {
            updateActionBarForFragment(top);
        } else {
            hideActionBar();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Resume session tracking when app comes to foreground
        SessionTracker.getInstance(this).resumeSession();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // End session when app goes to background
        SessionTracker.getInstance(this).endSession();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // End session and clear tracking
        SessionTracker.getInstance(this).endSession();
        
        // Clear fragment cache and state manager to prevent memory leaks
        if (fragmentCache != null) {
            fragmentCache.clear();
        }
        if (fragmentStateManager != null) {
            fragmentStateManager.clearFragments();
        }
    }
}