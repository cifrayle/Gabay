package com.example.gabay.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;
import com.example.gabay.fragments.LessonFragment;

public class LessonActivity extends AppCompatActivity {

    private int currentLevel = 1;
    private int currentChapter = 1;
    private TextView actionBarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Gabay);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson);

        // Apply window insets to handle system bars properly
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Apply padding to the AppBarLayout
                View appBarLayout = findViewById(R.id.app_bar_layout);
                if (appBarLayout != null) {
                    appBarLayout.setPadding(0, systemBars.top, 0, 0);
                }
                return insets;
            });
        }

        ImageButton backButton = findViewById(R.id.levels_back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        currentLevel = getIntent().getIntExtra("level", 1);
        currentChapter = getIntent().getIntExtra("chapter", 1);
        String levelTitle = getIntent().getStringExtra("levelTitle");

        actionBarTitle = findViewById(R.id.action_bar_title);
        if (levelTitle != null) {
            actionBarTitle.setText(levelTitle);
        } else {
            updateActionBarTitle();
        }

        loadLevelFragment(currentLevel, levelTitle);
    }

    private void updateActionBarTitle() {
        if (actionBarTitle != null) {
            String title = "Chapter " + currentChapter + " - Level " + currentLevel;
            actionBarTitle.setText(title);
        }
    }
    public void updateLessonTitle(String newTitle) {
        if (actionBarTitle != null) {
            actionBarTitle.setText(newTitle);
        }
    }

    private void loadLevelFragment(int chapter, String levelTitle) {
        String chapterId = "chapter" + currentChapter;

        Fragment fragment = LessonFragment.newInstance(chapterId, currentLevel);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }


    @Override
    public void onBackPressed() {
        // When going back to Chapter1, make sure we set the result
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}