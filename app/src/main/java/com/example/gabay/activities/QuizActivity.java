package com.example.gabay.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.example.gabay.R;
import com.example.gabay.data.QuizRegistry;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Apply window insets to prevent overlapping with system bars
        applyWindowInsets();

        // Only add/replace once (prevents duplicate fragments after rotation)
        if (savedInstanceState == null) {
            int chapterNumber = getIntent().getIntExtra("chapter_number", 1);
            int quizLevel     = getIntent().getIntExtra("level", 1); // default to 1

            // Build the fully-configured fragment (with bundles) from the registry
            Fragment target = QuizRegistry.buildFragmentForChapter(chapterNumber, quizLevel);

            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.quiz_fragment_container, target)
                    .commit();
        }
    }

    private void applyWindowInsets() {
        View rootView = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Apply padding to avoid system bars
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });
    }
}