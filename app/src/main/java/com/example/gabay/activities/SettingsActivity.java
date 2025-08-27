package com.example.gabay.activities;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;
import com.example.gabay.fragments.pages.SettingsPage;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force light theme to prevent UI color changes
        setTheme(R.style.Theme_Gabay);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup toolbar
        setupToolbar();

        // Load the settings fragment
        loadSettingsFragment();
        
        // Ensure toolbar colors are properly set
        ensureToolbarColors();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Settings");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                
                // Set explicit text colors to ensure visibility
                toolbar.setTitleTextColor(getResources().getColor(R.color.buttonTextColor));
                toolbar.setSubtitleTextColor(getResources().getColor(R.color.buttonTextColor));
            }
        }
    }

    private void loadSettingsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Create and add the settings fragment
        Fragment settingsFragment = new SettingsPage();
        transaction.replace(R.id.settings_fragment_container, settingsFragment);
        transaction.commit();
    }

    private void ensureToolbarColors() {
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        if (toolbar != null) {
            // Force set colors to ensure visibility
            toolbar.setTitleTextColor(getResources().getColor(R.color.buttonTextColor));
            toolbar.setSubtitleTextColor(getResources().getColor(R.color.buttonTextColor));
            
            // Set navigation icon tint
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.buttonTextColor));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Handle back button press
        super.onBackPressed();
        finish(); // Close the activity and return to previous screen
    }
}
