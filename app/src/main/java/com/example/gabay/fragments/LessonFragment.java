package com.example.gabay.fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.gabay.R;
import com.example.gabay.data.LessonData;

public class LessonFragment extends Fragment {

    private static final String ARG_CHAPTER_ID = "chapterId";
    private static final String ARG_LEVEL_NUMBER = "levelNumber";

    private String chapterId;
    private int levelNumber;
    private VideoView videoView;
    private Button btnNext, btnPrev;

    public LessonFragment() {}

    public static LessonFragment newInstance(String chapterId, int levelNumber) {
        LessonFragment fragment = new LessonFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAPTER_ID, chapterId);
        args.putInt(ARG_LEVEL_NUMBER, levelNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chapterId = getArguments().getString(ARG_CHAPTER_ID);
            levelNumber = getArguments().getInt(ARG_LEVEL_NUMBER, 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lesson, container, false);

        videoView = view.findViewById(R.id.videoView);
        btnNext = view.findViewById(R.id.btn_nextLevel);
        btnPrev = view.findViewById(R.id.btn_prevLevel);

        showLesson();

        btnNext.setOnClickListener(v -> goToLevel(levelNumber + 1));
        btnPrev.setOnClickListener(v -> {
            if (levelNumber > 1) goToLevel(levelNumber - 1);
        });

        return view;
    }

    private void showLesson() {
        LessonData.Lesson[] lessons = getLessonsForChapter();

        if (lessons != null && levelNumber >= 1 && levelNumber <= lessons.length) {
            LessonData.Lesson lesson = lessons[levelNumber - 1];

            if (getActivity() != null) {
                TextView actionBarTitle = getActivity().findViewById(R.id.action_bar_title);
                if (actionBarTitle != null) {
                    actionBarTitle.setText(lesson.title);
                }
            }

            // Play video
            String path = "android.resource://" + getContext().getPackageName() + "/" + lesson.videoRes;
            Uri uri = Uri.parse(path);
            videoView.setVideoURI(uri);

            MediaController mediaController = new MediaController(getContext());
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);

            videoView.start();
        }
    }

    private LessonData.Lesson[] getLessonsForChapter() {
        if ("chapter1".equals(chapterId)) {
            return LessonData.CHAPTER1;
        }
        // add other chapters later
        return null;
    }

    private void goToLevel(int nextLevelNumber) {
        LessonData.Lesson[] lessons = getLessonsForChapter();
        if (lessons != null && nextLevelNumber >= 1 && nextLevelNumber <= lessons.length) {
            Fragment fragment = LessonFragment.newInstance(chapterId, nextLevelNumber);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}

