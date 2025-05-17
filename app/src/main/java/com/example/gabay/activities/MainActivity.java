package com.example.gabay.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.example.gabay.fragments.pages.GabAIPage;
import com.example.gabay.fragments.pages.HomePage;
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
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
        }
        return true;
    };

}

