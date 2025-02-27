package com.example.gabay.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.example.gabay.fragments.pages.GabAIPage;
import com.example.gabay.fragments.pages.HomePage;
import com.example.gabay.fragments.pages.JourneyPage;
import com.example.gabay.fragments.pages.SettingsPage;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity{
//implements View.OnClickListener

    private TextView actionBarTitle;
    private View actionBarContainer; // For toggling visibility

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBarTitle = findViewById(R.id.action_bar_title); // Initialize the title
        actionBarContainer = findViewById(R.id.custom_action_bar_container); // Initialize the container

        // Handle navigation item clicks
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
        bottomNavView.setOnItemSelectedListener(navListener);

        // Set HomePage as default page
        bottomNavView.setSelectedItemId(R.id.nav_Home); // Set the default item
        Fragment selectedFragment = new HomePage();
        updateActionBarVisibility(false); // Hide action bar on Home page


        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit(); // Set the default fragment

        // Check if we should navigate to a specific tab on startup
        if (getIntent().hasExtra("selectedTab")) {
            int tabId = getIntent().getIntExtra("selectedTab", R.id.nav_Home);
            bottomNavView.setSelectedItemId(tabId);
        }
    }


    private NavigationBarView.OnItemSelectedListener navListener = item -> {

        int itemId = item.getItemId();
        Fragment selectedFragment = null;
        String title = ""; // Placeholder for the action bar title
        boolean showActionBar = true; // Default to showing the action bar

        if (itemId == R.id.nav_Home) {
            selectedFragment = new HomePage();
            showActionBar = false;
            updateActionBarVisibility(false); // Hide action bar for HomePage
        } else if (itemId == R.id.nav_GabAI) {
            selectedFragment = new GabAIPage();
            title = "GabAI";
            updateActionBarVisibility(true); // Show action bar for GabAIPage
        } else if (itemId == R.id.nav_Settings) {
            title = "Settings";
            selectedFragment = new SettingsPage();
            updateActionBarVisibility(true); // Show action bar for SettingsPage
        } else if (itemId == R.id.nav_Journey) {
            title = "Journey";
            selectedFragment = new JourneyPage();
            updateActionBarVisibility(true); // Show action bar for SettingsPage
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            updateActionBarVisibility(showActionBar); // Update action bar visibility
            updateActionBarTitle(title); // Update the title
        }
        return true;
    };

    // Method to update the action bar title
    private void updateActionBarTitle(String title) {
        if (actionBarTitle != null) {
            actionBarTitle.setText(title);
        }
    }

    // Method to toggle the visibility of the action bar
    private void updateActionBarVisibility(boolean show) {
        RelativeLayout actionBar = findViewById(R.id.custom_action_bar_container);
        if (actionBar != null) {
            actionBar.setVisibility(show ? View.VISIBLE : View.GONE);
            // Add the log here
            Log.d("MainActivity", "Action bar visibility: " + (show ? "VISIBLE" : "GONE"));
        } else {
            Log.d("MainActivity", "Action bar container is NULL");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Check if we need to select a specific tab
        if (intent.hasExtra("selectedTab")) {
            int tabId = intent.getIntExtra("selectedTab", R.id.nav_Home);
            BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
            bottomNavView.setSelectedItemId(tabId);
        }
    }
}