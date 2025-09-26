package com.example.gabay.fragments.chapters;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gabay.R;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.fragments.LessonFragment;
import com.example.gabay.fragments.quiz.QuizFragment;
import com.example.gabay.activities.QuizActivity;

public class Chapter1 extends Fragment implements View.OnClickListener {

    Button btn_chapter_quiz;
    private static final int[] BUTTON_IDS = {
            R.id.lvl1, R.id.lvl2, R.id.lvl3, R.id.lvl4, R.id.lvl5,
            R.id.lvl6, R.id.lvl7, R.id.lvl8, R.id.lvl9, R.id.lvl10,
            R.id.lvl11, R.id.lvl12, R.id.lvl13, R.id.lvl14, R.id.lvl15,
            R.id.lvl16, R.id.lvl17, R.id.lvl18, R.id.lvl19, R.id.lvl20,
            R.id.lvl21, R.id.lvl23, R.id.lvl24, R.id.lvl25,
            R.id.lvl26
    };

    // T R.id.lvl22,
    private static final int[] TITLE_IDS = {
            R.id.levelTitle1, R.id.levelTitle2, R.id.levelTitle3, R.id.levelTitle4, R.id.levelTitle5,
            R.id.levelTitle6, R.id.levelTitle7, R.id.levelTitle8, R.id.levelTitle9, R.id.levelTitle10,
            R.id.levelTitle11, R.id.levelTitle12, R.id.levelTitle13, R.id.levelTitle14, R.id.levelTitle15,
            R.id.levelTitle16, R.id.levelTitle17, R.id.levelTitle18, R.id.levelTitle19, R.id.levelTitle20,
            R.id.levelTitle21,  R.id.levelTitle23, R.id.levelTitle24, R.id.levelTitle25,
            R.id.levelTitle26
    };

    // T R.id.levelTitle22,
    private static int currentLessonLevel = -1;
    private View view;

    public static Chapter1 newInstance() {
        Chapter1 fragment = new Chapter1();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        initializeButtons();
        initializeQuizButton();

        for (int i = 0; i < BUTTON_IDS.length; i++) {
            Button button = view.findViewById(BUTTON_IDS[i]);
            TextView titleView = view.findViewById(TITLE_IDS[i]);
            int levelNumber = i + 1;

            if (button != null && titleView != null) {
                button.setOnClickListener(v -> {
                    String levelTitle = titleView.getText().toString();

                    // Open LessonActivity with BOTH level number + title
                    Intent intent = new Intent(getActivity(), LessonActivity.class);
                    intent.putExtra("chapter", 1);
                    intent.putExtra("level", levelNumber);
                    intent.putExtra("levelTitle", levelTitle);
                    startActivity(intent);
                });
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt1, container, false);

    }
    private void initializeQuizButton() {
        btn_chapter_quiz = view.findViewById(R.id.btn_chapter_quiz);
        if (btn_chapter_quiz != null) {
            btn_chapter_quiz.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openQuizActivity();
                }
            });
        }
    }

    private void openQuizActivity() {
        try {
            Intent intent = new Intent(getActivity(), QuizActivity.class);
            intent.putExtra("chapter_number", 1);
            startActivity(intent);

            Log.d("Chapter1", "QuizActivity started successfully");

        } catch (Exception e) {
            Log.e("Chapter1", "Error starting QuizActivity: " + e.getMessage(), e);
            // Show a toast or error message to the user
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Error opening quiz", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        int level = getLevelFromId(id);

        // Find the corresponding title TextView (assuming IDs like levelTitle1, levelTitle2, etc.)
        String title = "";
        int titleId = getResources().getIdentifier("levelTitle" + level, "id", requireContext().getPackageName());
        TextView titleView = this.view.findViewById(titleId);
        if (titleView != null) {
            title = titleView.getText().toString();
        }

        Intent intent = new Intent(getActivity(), LessonActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("chapter", 1);
        intent.putExtra("levelTitle", title);

        if (currentLessonLevel != level) {
            if (currentLessonLevel != -1) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            startActivity(intent);
            currentLessonLevel = level;
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showActionBarWithTitle("Chapter 1");
        }
    }

    private int getLevelFromId(int id) {
        for (int i = 0; i < BUTTON_IDS.length; i++) {
            if (id == BUTTON_IDS[i]) {
                return i + 1; // Level numbers start from 1
            }
        }
        return -1; // Invalid ID
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        currentLessonLevel = -1;
    }

    private void initializeButtons() {
        for (int buttonId : BUTTON_IDS) {
            Button button = view.findViewById(buttonId);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
    }
}