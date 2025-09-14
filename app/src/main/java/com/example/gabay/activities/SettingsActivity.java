package com.example.gabay.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
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

        setupToolbar();
        loadSettingsFragment();
    }

    private void setupToolbar() {
        ImageButton backButton = findViewById(R.id.levels_back_button);
        TextView titleTextView = findViewById(R.id.action_bar_title);

        if (titleTextView != null) {
            titleTextView.setText("Settings");
            titleTextView.setTextColor(getResources().getColor(R.color.buttonTextColor));
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
    }

    private void setToolbarTitleAppearance(Toolbar toolbar) {
        // Find the title TextView
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView) {
                TextView titleTextView = (TextView) child;
                if (titleTextView.getText().equals(toolbar.getTitle())) {
                    // Set font size
                    titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); // 20sp

                    // Set font family (if you have Nunito font added)
                    try {
                        Typeface typeface = ResourcesCompat.getFont(this, R.font.nunito_bold);
                        titleTextView.setTypeface(typeface);
                    } catch (Exception e) {
                        // Fallback to default bold typeface
                        titleTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    }
                    break;
                }
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
