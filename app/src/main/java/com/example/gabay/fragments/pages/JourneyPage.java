package com.example.gabay.fragments.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.example.gabay.activities.LessonActivity;

public class JourneyPage extends Fragment implements View.OnClickListener {
    private Button ch1btn1, ch1btn2, ch1btn3, ch1btn4, ch1btn5, ch1btn6, ch1btn7;
    private Button chpt1Btn, chpt2Btn, chpt3Btn; // Add chapter buttons
    private static int currentLessonLevel = -1;
    private RelativeLayout fragmentContainer;
    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_journey_page, container, false);

        // Initialize the container that will hold our chapter fragments
        fragmentContainer = view.findViewById(R.id.custom_fragment_journey_container);

        // Initialize chapter buttons
        initializeChapterButtons();

        // Load Chapter 1 by default
        loadChapter(1);

        return view;
    }

    private void initializeChapterButtons() {
        chpt1Btn = view.findViewById(R.id.chpt1);
        chpt2Btn = view.findViewById(R.id.chpt2);
        chpt3Btn = view.findViewById(R.id.chpt3);

        chpt1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadChapter(1);
            }
        });

        chpt2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadChapter(2);
            }
        });

        chpt3Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadChapter(3);
            }
        });
    }

    private void loadChapter(int chapterNumber) {
        // Clear current container
        fragmentContainer.removeAllViews();

        // Inflate the appropriate layout based on chapter number
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View chapterView;

        switch(chapterNumber) {
            case 1:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt1, fragmentContainer, false);
                fragmentContainer.addView(chapterView);
                initializeChapter1Buttons();
                break;
            case 2:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt2, fragmentContainer, false);
                fragmentContainer.addView(chapterView);
                // Initialize Chapter 2 buttons if needed
                // initializeChapter2Buttons();
                break;
            case 3:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt3, fragmentContainer, false);
                fragmentContainer.addView(chapterView);
                // Initialize Chapter 3 buttons if needed
                // initializeChapter3Buttons();
                break;
        }
    }

    private void initializeChapter1Buttons() {
        // Need to find buttons within the container now, not directly in view
        View chapterView = fragmentContainer.getChildAt(0);
        if (chapterView == null) return;

        ch1btn1 = chapterView.findViewById(R.id.lvl1);
        ch1btn2 = chapterView.findViewById(R.id.lvl2);
        ch1btn3 = chapterView.findViewById(R.id.lvl3);
        ch1btn4 = chapterView.findViewById(R.id.lvl4);
        ch1btn5 = chapterView.findViewById(R.id.lvl5);
        ch1btn6 = chapterView.findViewById(R.id.lvl6);
        ch1btn7 = chapterView.findViewById(R.id.lvl7);

        ch1btn1.setOnClickListener(this);
        ch1btn2.setOnClickListener(this);
        ch1btn3.setOnClickListener(this);
        ch1btn4.setOnClickListener(this);
        ch1btn5.setOnClickListener(this);
        ch1btn6.setOnClickListener(this);
        ch1btn7.setOnClickListener(this);
    }

    // Your existing onClick method for level buttons remains the same
    @Override
    public void onClick(View view) {
        int id = view.getId();
        int level = getLevelFromId(id);

        Intent intent = new Intent(getActivity(), LessonActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("fragment_to_load", "level_" + level);

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

    // Your existing helper method
    private int getLevelFromId(int id) {
        if (id == R.id.lvl1) return 1;
        if (id == R.id.lvl2) return 2;
        if (id == R.id.lvl3) return 3;
        if (id == R.id.lvl4) return 4;
        if (id == R.id.lvl5) return 5;
        if (id == R.id.lvl6) return 6;
        if (id == R.id.lvl7) return 7;
        return -1; // Invalid ID
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        currentLessonLevel = -1;
    }
}