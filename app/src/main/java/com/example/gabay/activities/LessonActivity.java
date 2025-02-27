package com.example.gabay.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level1;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level2;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level3;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level4;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level5;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level6;
import com.example.gabay.fragments.chapters.chapter1.Chapter1_Level7;

public class LessonActivity extends AppCompatActivity {
    private ImageButton toMainActivity;
    private int currentLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lesson);

        // Get level parameter from intent if available
        currentLevel = getIntent().getIntExtra("level", 1);

        // Initialize back button from lesson to Journey page
        toMainActivity = findViewById(R.id.lesson_activity_back_button);
        toMainActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToJourneyPage();
            }
        });

        // Load the appropriate fragment based on the level
        loadLevelFragment(currentLevel);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadLevelFragment(int level) {
        Fragment fragment = null;

        // Select the appropriate fragment based on level
        switch (level) {
            case 1:
                fragment = new Chapter1_Level1();
                break;
            case 2:
                fragment = new Chapter1_Level2();
                break;
            case 3:
                fragment = new Chapter1_Level3();
                break;
            case 4:
                fragment = new Chapter1_Level4();
                break;
            case 5:
                fragment = new Chapter1_Level5();
                break;
            case 6:
                fragment = new Chapter1_Level6();
                break;
            case 7:
                fragment = new Chapter1_Level7();
                break;
            default:
                fragment = new Chapter1_Level1(); // Default to level 1
                break;
        }

        // Replace the current fragment with the selected one
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void returnToJourneyPage() {
        // Method 1: Finish this activity to return to previous screen
        // This is the simplest approach if you always navigate from Journey to Lesson
        finish();

        // Method 2: Explicitly navigate to MainActivity with Journey tab selected
        // Use this if you might enter from different places
        // Intent intent = new Intent(this, MainActivity.class);
        // intent.putExtra("selectedTab", R.id.nav_Journey); // Add extra to specify Journey tab
        // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear activities on top of MainActivity
        // startActivity(intent);
    }
}