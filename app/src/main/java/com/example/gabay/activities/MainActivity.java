package com.example.gabay.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;
import com.example.gabay.fragments.pages.GabAIPage;
import com.example.gabay.fragments.pages.HomePage;
import com.example.gabay.fragments.pages.SettingsPage;
import com.example.gabay.fragments.pages.ProfilePage;
import com.example.gabay.fragments.pages.DictionaryPage;
import com.example.gabay.utils.FragmentStateManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Map<Integer, Fragment> fragmentCache;
    private Fragment currentFragment;
    private FragmentManager fragmentManager;
    private FragmentStateManager fragmentStateManager;
    private android.widget.TextView actionBarTitle;
    private android.view.View actionBarContainer;
    private android.widget.ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize fragment manager and cache
        fragmentManager = getSupportFragmentManager();
        fragmentCache = new HashMap<>();
        fragmentStateManager = new FragmentStateManager(fragmentManager);

        // Initialize custom action bar views
        actionBarContainer = findViewById(R.id.custom_action_bar_container);
        actionBarTitle = findViewById(R.id.action_bar_title);
        backButton = findViewById(R.id.levels_back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }

        // Listen for back stack changes to update action bar visibility/title
        fragmentManager.addOnBackStackChangedListener(this::updateActionBarForTopFragment);

        // Set onClickListener for bottomNavigationView
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
        bottomNavView.setOnItemSelectedListener(navListener);

        // Set HomePage as default page
        bottomNavView.setSelectedItemId(R.id.nav_Home);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save fragment states
        fragmentStateManager.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore fragment states
        fragmentStateManager.restoreState(savedInstanceState);
    }

    // Manages bottomNavigation pages with caching
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
        } else if (itemId == R.id.nav_Dictionary) {
            newFragment = fragmentStateManager.getOrCreateFragment(DictionaryPage.class, tag);
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
        if (itemId == R.id.nav_Dictionary) return "dictionary_fragment";
        if (itemId == R.id.nav_Profile) return "profile_fragment";
        return "unknown_fragment";
    }

    private void switchFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Use setCustomAnimations for smooth transitions
        transaction.setCustomAnimations(
            R.anim.fade_in,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.fade_out
        );

        // Check if fragment is already added
        if (fragment.isAdded()) {
            transaction.show(fragment);
            // Hide other fragments
            for (Fragment cachedFragment : fragmentCache.values()) {
                if (cachedFragment != fragment && cachedFragment.isAdded()) {
                    transaction.hide(cachedFragment);
                }
            }
        } else {
            // Add new fragment
            transaction.add(R.id.fragment_container, fragment);
            // Hide current fragment if exists
            if (currentFragment != null && currentFragment.isAdded()) {
                transaction.hide(currentFragment);
            }
        }

        // Use commitNow for immediate execution
        transaction.commitNow();

        // After switching, update action bar visibility/title for top-level tabs
        updateActionBarForFragment(fragment);
    }

    private void updateActionBarForFragment(Fragment fragment) {
        if (actionBarContainer == null) return;
        // Hide action bar by default for bottom tabs
        actionBarContainer.setVisibility(android.view.View.GONE);

        if (fragment instanceof com.example.gabay.fragments.chapters.Chapter1) {
            showActionBarWithTitle("Chapter 1");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter2) {
            showActionBarWithTitle("Chapter 2");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter3) {
            showActionBarWithTitle("Chapter 3");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter4) {
            showActionBarWithTitle("Chapter 4");
        } else if (fragment instanceof com.example.gabay.fragments.chapters.Chapter5) {
            showActionBarWithTitle("Chapter 5");
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

    private void updateActionBarForTopFragment() {
        Fragment top = fragmentManager.findFragmentById(R.id.fragment_container);
        if (top != null) {
            updateActionBarForFragment(top);
        } else {
            hideActionBar();
        }
    }

    /**
     * Navigate to quiz fragment (for future implementation)
     */
    public void navigateToQuiz(int chapter, int level) {
        // This will be implemented when quiz navigation is needed
        android.util.Log.d("MainActivity", "Navigate to quiz for Chapter " + chapter + " Level " + level);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear fragment cache and state manager to prevent memory leaks
        if (fragmentCache != null) {
            fragmentCache.clear();
        }
        if (fragmentStateManager != null) {
            fragmentStateManager.clearFragments();
        }
    }
}

