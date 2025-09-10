package com.example.gabay.fragments.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gabay.R;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.viewmodels.ProgressViewModel;

/**
 * Template for quiz fragments that will be shown every 5 levels
 * This is a placeholder that will be fully implemented later
 */
public class QuizFragment extends BaseFragment {

    private ImageView questionImageView;
    private Button[] answerButtons;
    private Button submitButton;
    private ProgressViewModel progressViewModel;
    
    private int currentChapter;
    private int currentLevel;
    private int currentQuestionIndex = 0;
    private int score = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    protected void initializeViews() {
        // Initialize ProgressViewModel
        progressViewModel = getActivityViewModel(ProgressViewModel.class);
        
        // Get chapter and level from arguments
        Bundle args = getArguments();
        if (args != null) {
            currentChapter = args.getInt("chapter", 1);
            currentLevel = args.getInt("level", 1);
        }
        
        // Initialize quiz UI
        initializeQuizUI();
        loadQuestion();
    }

    private void initializeQuizUI() {
        if (rootView == null) return;
        
        questionImageView = rootView.findViewById(R.id.question_image);
        
        // Initialize answer buttons
        int[] buttonIds = {R.id.Answer_A, R.id.Answer_B, R.id.Answer_C, R.id.Answer_D};
        answerButtons = new Button[buttonIds.length];
        
        for (int i = 0; i < buttonIds.length; i++) {
            answerButtons[i] = rootView.findViewById(buttonIds[i]);
            if (answerButtons[i] != null) {
                final int answerIndex = i;
                answerButtons[i].setOnClickListener(v -> selectAnswer(answerIndex));
            }
        }
        
        submitButton = rootView.findViewById(R.id.submit_button);
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> submitAnswer());
        }
    }

    private void loadQuestion() {
        // This will be implemented when quiz content is added
        // Set placeholder answers
        if (answerButtons != null) {
            for (int i = 0; i < answerButtons.length; i++) {
                if (answerButtons[i] != null) {
                    answerButtons[i].setText("Answer " + (i + 1));
                }
            }
        }
    }

    private void selectAnswer(int answerIndex) {
        // Reset all button states
        for (int i = 0; i < answerButtons.length; i++) {
            if (answerButtons[i] != null) {
                answerButtons[i].setSelected(i == answerIndex);
            }
        }
    }

    private void submitAnswer() {
        // This will be implemented when quiz logic is added
        // For now, just simulate a correct answer
        score += 10;
        currentQuestionIndex++;
        
        if (currentQuestionIndex < 5) { // Assuming 5 questions per quiz
            loadQuestion();
        } else {
            completeQuiz();
        }
    }

    private void completeQuiz() {
        // Mark quiz as completed
        if (progressViewModel != null) {
            progressViewModel.markQuizCompleted(currentChapter, currentLevel);
            progressViewModel.updateChapterProgress(currentChapter, currentLevel, score);
        }
        
        // Hide answer buttons and submit button
        for (Button button : answerButtons) {
            if (button != null) button.setVisibility(View.GONE);
        }
        if (submitButton != null) submitButton.setVisibility(View.GONE);

    }

    private void navigateBack() {
        // Navigate back to the lesson
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    protected void cleanupResources() {
        questionImageView = null;
        answerButtons = null;
        submitButton = null;
        progressViewModel = null;
    }

    /**
     * Create a new instance of QuizFragment with chapter and level
     */
    public static QuizFragment newInstance(int chapter, int level) {
        QuizFragment fragment = new QuizFragment();
        Bundle args = new Bundle();
        args.putInt("chapter", chapter);
        args.putInt("level", level);
        fragment.setArguments(args);
        return fragment;
    }
}

