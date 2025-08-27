package com.example.gabay.fragments.chapters;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.activities.MainActivity;

public class Chapter5 extends Fragment implements View.OnClickListener {
    private static final int[] BUTTON_IDS = {
            R.id.lvl1, R.id.lvl2, R.id.lvl3, R.id.lvl4, R.id.lvl5, R.id.lvl6, R.id.lvl7
    };

    private static int currentLessonLevel = -1;
    private View view;

    public static Chapter5 newInstance() {
        Chapter5 fragment = new Chapter5();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        initializeButtons();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lessons_chpt5, container, false);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        int level = getLevelFromId(id);

        Intent intent = new Intent(getActivity(), LessonActivity.class);
        intent.putExtra("level", level);
        intent.putExtra("fragment_to_load", "level_" + level);
        intent.putExtra("chapter", 5);

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
            ((MainActivity) getActivity()).showActionBarWithTitle("Chapter 5");
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