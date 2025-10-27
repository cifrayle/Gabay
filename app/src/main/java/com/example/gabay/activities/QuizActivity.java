package com.example.gabay.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;
import com.example.gabay.fragments.quiz.QuizFragment;

public class QuizActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Get chapter and level from intent
        int chapterNumber = getIntent().getIntExtra("chapter_number", 1);
        int quizLevel = getIntent().getIntExtra("level", 0);

        // Load the QuizFragment with chapter and level
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Pass the chapter and level to the fragment
        QuizFragment quizFragment = QuizFragment.newInstance(chapterNumber, quizLevel);

        transaction.replace(R.id.quiz_fragment_container, quizFragment);
        transaction.commit();
    }
}