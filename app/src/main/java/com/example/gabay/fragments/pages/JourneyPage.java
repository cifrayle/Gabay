package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.example.gabay.fragments.chapters.Chapter1;
import com.example.gabay.fragments.chapters.Chapter2;
import com.example.gabay.fragments.chapters.Chapter3;
import com.example.gabay.fragments.chapters.Chapter4;
import com.example.gabay.fragments.chapters.Chapter5;

public class JourneyPage extends Fragment{
    private ConstraintLayout mainContentContainer;
    View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.fragment_journey_page, container, false);
        mainContentContainer = view.findViewById(R.id.chapters_container);
        initializeChapterButtons();

        return view;
    }

    private void initializeChapterButtons() {
        int[] buttonIds = {R.id.chpt1, R.id.chpt2, R.id.chpt3, R.id.chpt4, R.id.chpt5};

        // Set the onClickListener for each chapter button
        for (int i = 0; i < buttonIds.length; i++) {
            Button chapterButton = view.findViewById(buttonIds[i]);
            final int chapterNumber = i + 1;
            chapterButton.setOnClickListener(v -> loadChapter(chapterNumber));
        }
    }

    private void loadChapter(int chapterNumber) {
        // Clear current container & inflate layout based on selectedChapter number
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        mainContentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View chapterView;

        Fragment selectedChapter = null;

        switch(chapterNumber) {
            case 1:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt1, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter1();
                break;
            case 2:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt2, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter2();
                break;
            case 3:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt3, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter3();
                break;
            case 4:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt4, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter4();
                break;
            case 5:
                chapterView = inflater.inflate(R.layout.fragment_journey_chpt5, mainContentContainer, false);
                mainContentContainer.addView(chapterView);
                selectedChapter = new Chapter5();
                break;
        }
        if (selectedChapter != null){
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.chapters_container, selectedChapter).commit();

        }
    }

}