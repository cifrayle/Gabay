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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set onClickListener for bottomNavigationView
        BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
        bottomNavView.setOnItemSelectedListener(navListener);

        // Set HomePage as default page
        bottomNavView.setSelectedItemId(R.id.nav_Home);

        // Check if we should navigate to a specific tab on startup
//        if (getIntent().hasExtra("selectedTab")) {
//            int tabId = getIntent().getIntExtra("selectedTab", R.id.nav_Home);
//            bottomNavView.setSelectedItemId(tabId);
//        }
    }


    //Manages bottomNavigation pages
    private NavigationBarView.OnItemSelectedListener navListener = item -> {
        int itemId = item.getItemId();
        Fragment selectedFragment = null;

        if (itemId == R.id.nav_Home) {
            selectedFragment = new HomePage();
        } else if (itemId == R.id.nav_GabAI) {
            selectedFragment = new GabAIPage();
        } else if (itemId == R.id.nav_Settings) {
            selectedFragment = new SettingsPage();
        } else if (itemId == R.id.nav_Journey) {
            selectedFragment = new JourneyPage();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
        }
        return true;
    };


//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        setIntent(intent);
//
//        // Check if we need to select a specific tab
//        if (intent.hasExtra("selectedTab")) {
//            int tabId = intent.getIntExtra("selectedTab", R.id.nav_Home);
//            BottomNavigationView bottomNavView = findViewById(R.id.bottom_navigation);
//            bottomNavView.setSelectedItemId(tabId);
//        }
//    }
}