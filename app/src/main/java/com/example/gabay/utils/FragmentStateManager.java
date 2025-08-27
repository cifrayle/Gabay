package com.example.gabay.utils;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for managing fragment states and preventing recreation
 * Helps maintain fragment instances and their states across configuration changes
 */
public class FragmentStateManager {
    
    private static final String FRAGMENT_STATE_KEY = "fragment_state";
    private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
    
    private Map<String, Fragment> fragmentInstances;
    private Fragment currentFragment;
    private FragmentManager fragmentManager;
    
    public FragmentStateManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        this.fragmentInstances = new HashMap<>();
    }
    
    /**
     * Get or create a fragment instance
     * @param fragmentClass The fragment class
     * @param tag The fragment tag
     * @return Fragment instance
     */
    @SuppressWarnings("unchecked")
    public <T extends Fragment> T getOrCreateFragment(Class<T> fragmentClass, String tag) {
        Fragment existingFragment = fragmentInstances.get(tag);
        
        if (existingFragment != null && existingFragment.getClass() == fragmentClass) {
            return (T) existingFragment;
        }
        
        try {
            T newFragment = fragmentClass.newInstance();
            fragmentInstances.put(tag, newFragment);
            return newFragment;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Set the current active fragment
     */
    public void setCurrentFragment(Fragment fragment) {
        this.currentFragment = fragment;
    }
    
    /**
     * Get the current active fragment
     */
    public Fragment getCurrentFragment() {
        return currentFragment;
    }
    
    /**
     * Save fragment states to bundle
     */
    public void saveState(Bundle outState) {
        if (outState != null) {
            outState.putString(CURRENT_FRAGMENT_KEY, 
                currentFragment != null ? currentFragment.getClass().getSimpleName() : "");
        }
    }
    
    /**
     * Restore fragment states from bundle
     */
    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String currentFragmentName = savedInstanceState.getString(CURRENT_FRAGMENT_KEY, "");
            if (!currentFragmentName.isEmpty()) {
                // Restore current fragment if needed
                // This can be extended based on your app's needs
            }
        }
    }
    
    /**
     * Clear all fragment instances
     */
    public void clearFragments() {
        fragmentInstances.clear();
        currentFragment = null;
    }
    
    /**
     * Check if a fragment exists in the manager
     */
    public boolean hasFragment(String tag) {
        return fragmentInstances.containsKey(tag);
    }
    
    /**
     * Remove a specific fragment instance
     */
    public void removeFragment(String tag) {
        fragmentInstances.remove(tag);
    }
    
    /**
     * Get fragment count
     */
    public int getFragmentCount() {
        return fragmentInstances.size();
    }
}

