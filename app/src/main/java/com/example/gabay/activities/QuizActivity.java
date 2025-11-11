package com.example.gabay.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.gabay.R;
import com.example.gabay.data.QuizRegistry; // <-- use the registry

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

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
}
