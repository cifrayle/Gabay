package com.example.gabay.fragments.quiz;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;

import com.example.gabay.R;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.viewmodels.ProgressViewModel;

/**
 * Template for quiz fragments that will be shown every 5 levels
 * This is a placeholder that will be fully implemented later
 */
public class QuizFragment extends BaseFragment {

    int[] img_Question_List = {
            R.drawable.letter_c,
            R.drawable.letter_i,
            R.drawable.letter_a,
            R.drawable.letter_n,
            R.drawable.letter_f
    };
    String[] choices_list = {
            "HELLO/KUMUSTA", "C", "NO/HINDI", "O",
            "A", "ONE/ISA", "I", "YOU/IKAW",
            "FIST/KAMAO", "J", "ME/AKO", "A",
            "N", "M", "Y", "L",
            "OK", "G", "THREE/TATLO", "F",
    };
    String[] correctAnswer_list = {"C", "I", "A", "N", "F"};


    private ImageView questionImageView;
    private MaterialCardView[] answerCardViews;
    private TextView[] answerTextViews;
    private Button submitButton;
    private TextView questionNumberTextView;
    private TextView quizHeaderTextView, quizHeaderTextView2;
    private TextView boldTitle_txtView;
    private TextView regTitle_txtView;
    private ProgressViewModel progressViewModel;

    private TextView timerTextView;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 11000; // 11 seconds per question
    private static final long COUNTDOWN_INTERVAL = 1000; // 1 second intervals
    
    private int currentChapter;
    private int currentLevel;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int selectedAnswerIndex = -1;
    private boolean quizInProgress = false;

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

        initializeQuizUI();

        showQuizIntroduction();
    }

    private void showQuizIntroduction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.BlackTextDialog);
        builder.setTitle("Quiz Time!");
        builder.setMessage("Quiz instructions:\n" +
                "• The quiz is multiple choice\n" +
                "• You have 10 seconds per question\n" +
                "• You cannot cancel/pause the quiz once started\n\n" +
                "Good luck!");
        builder.setPositiveButton("Start Quiz", (dialog, which) -> {
            startQuiz();
        });
        builder.setNegativeButton("Back", (dialog, which) -> {
            navigateBack();
        });
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.secondaryColor));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor));
        }
    }

    private void startQuiz() {
        quizInProgress = true;
        startTimer();
        loadQuestion();
    }

    private void initializeQuizUI() {
        if (rootView == null) return;

        questionImageView = rootView.findViewById(R.id.question_image);
        quizHeaderTextView = rootView.findViewById(R.id.quiz_header);
        quizHeaderTextView2 = rootView.findViewById(R.id.quiz_header2);
        boldTitle_txtView = rootView.findViewById(R.id.boldTitle_txtView);
        regTitle_txtView = rootView.findViewById(R.id.regTitle_txtView);
        questionNumberTextView = rootView.findViewById(R.id.question_number);
        timerTextView = rootView.findViewById(R.id.timerTextView);

        // Initialize answer CardViews and their TextViews
        int[] cardViewIds = {R.id.Answer_A, R.id.Answer_B, R.id.Answer_C, R.id.Answer_D};
        int[] textViewIds = {R.id.answerTextView_A, R.id.answerTextView_B, R.id.answerTextView_C, R.id.answerTextView_D};

        answerCardViews = new MaterialCardView[cardViewIds.length];
        answerTextViews = new TextView[cardViewIds.length];

        for (int i = 0; i < cardViewIds.length; i++) {
            answerCardViews[i] = rootView.findViewById(cardViewIds[i]);
            if (answerCardViews[i] != null) {
                // Use the correct TextView ID for each CardView
                answerTextViews[i] = answerCardViews[i].findViewById(textViewIds[i]);

                final int answerIndex = i;
                answerCardViews[i].setOnClickListener(v -> selectAnswer(answerIndex));
            }
        }

        submitButton = rootView.findViewById(R.id.submit_button);
        if (submitButton != null) {
            submitButton.setOnClickListener(v -> submitAnswer());
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimer();
            }
            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateTimer();
                // Time's up! Auto-submit or mark as wrong
                onTimeUp();
            }
        }.start();
    }
    private void updateTimer() {
        if (timerTextView != null) {
            int seconds = (int) (timeLeftInMillis / 1000);
            timerTextView.setText(String.format("Time left: %02d", seconds));

            // Change color when time is running out
            if (seconds <= 5) {
                timerTextView.setBackground(getResources().getDrawable(R.drawable.timer_bg_color_red));
            } else {
                timerTextView.setBackground(getResources().getDrawable(R.drawable.timer_bg_color_blue));
            }
        }
    }

    private void onTimeUp() {
        // Time's up! Handle accordingly
        if (selectedAnswerIndex == -1) {
            // No answer selected, mark as wrong
            Toast.makeText(getContext(), "Time's up! No answer selected.", Toast.LENGTH_SHORT).show();
        } else {
            // Answer was selected but time ran out, check it
            boolean isCorrect = checkAnswer(selectedAnswerIndex);
            if (isCorrect) {
                score += 1;
                answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.correctAnswerColor));
                Toast.makeText(getContext(), "Time's up! Answer was correct!", Toast.LENGTH_SHORT).show();
            } else {
                answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.wrongAnswerColor));
                Toast.makeText(getContext(), "Time's up! Answer was: " + correctAnswer_list[currentQuestionIndex], Toast.LENGTH_SHORT).show();
            }
        }

        // Move to next question after a brief delay
        new Handler().postDelayed(() -> {
            moveToNextQuestion();
        }, 1200);
    }

    private void moveToNextQuestion() {
        currentQuestionIndex++;
        selectedAnswerIndex = -1;

        if (currentQuestionIndex < img_Question_List.length) {
            resetTimer();
            loadQuestion();
        } else {
            completeQuiz();
        }
    }
    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = 11000; // reset to 11s since there's a 1s delay
        startTimer();
    }

    private void selectAnswer(int answerIndex) {
        // reset all CardView states
        for (int i = 0; i < answerCardViews.length; i++) {
            if (answerCardViews[i] != null) {
                answerCardViews[i].setCardBackgroundColor(getResources().getColor(android.R.color.white));
                answerCardViews[i].setCardElevation(2f);
                answerCardViews[i].setStrokeWidth(0);
            }
        }
        // Highlight selected CardView
        if (answerCardViews[answerIndex] != null) {
            answerCardViews[answerIndex].setCardBackgroundColor(getResources().getColor(R.color.primaryColor));
            answerCardViews[answerIndex].setCardElevation(12f);
            answerCardViews[answerIndex].setStrokeWidth(5);
            answerCardViews[answerIndex].setStrokeColor(getResources().getColor(R.color.secondaryColor));
        }

        selectedAnswerIndex = answerIndex;
    }

    private void submitAnswer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Stop the timer when answer is submitted
        }

        if (selectedAnswerIndex == -1) {
            Toast.makeText(getContext(), "Please select an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isCorrect = checkAnswer(selectedAnswerIndex);

        if (isCorrect) {
            score += 1;
            answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.correctAnswerColor));
            Toast.makeText(getContext(), "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.wrongAnswerColor));
            Toast.makeText(getContext(), "Wrong! Correct answer: " + correctAnswer_list[currentQuestionIndex], Toast.LENGTH_SHORT).show();
        }

        // Move to next question after a brief delay
        new Handler().postDelayed(() -> {
            moveToNextQuestion();
        }, 1200);
    }
    private boolean checkAnswer(int selectedIndex) {
        if (answerTextViews[selectedIndex] == null) return false;

        String selectedAnswer = answerTextViews[selectedIndex].getText().toString();
        return selectedAnswer.equals(correctAnswer_list[currentQuestionIndex]);
    }

    private void loadQuestion() {

        updateTimer();
        // Update question number
        if (questionNumberTextView != null) {
            questionNumberTextView.setText("Question " + (currentQuestionIndex + 1) + " of " + img_Question_List.length);
        }

        // Load question image
        if (questionImageView != null && currentQuestionIndex < img_Question_List.length) {
            questionImageView.setImageResource(img_Question_List[currentQuestionIndex]);
        }

        // Load choices for current question
        if (answerTextViews != null) {
            int choicesStartIndex = currentQuestionIndex * 4;
            for (int i = 0; i < answerTextViews.length; i++) {
                if (answerTextViews[i] != null && (choicesStartIndex + i) < choices_list.length) {
                    answerTextViews[i].setText(choices_list[choicesStartIndex + i]);

                    // Reset CardView appearance including stroke
                    if (answerCardViews[i] != null) {
                        answerCardViews[i].setCardBackgroundColor(getResources().getColor(android.R.color.white));
                        answerCardViews[i].setCardElevation(2f);
                        answerCardViews[i].setStrokeWidth(0); // Remove stroke
                    }
                }
            }
        }

        // Reset selection
        selectedAnswerIndex = -1;
    }

    private void completeQuiz() {
        quizInProgress = false;

        if (progressViewModel != null) {
            progressViewModel.markQuizCompleted(currentChapter, currentLevel);
            progressViewModel.updateChapterProgress(currentChapter, currentLevel, score);
        }

        // show final score
        if (getContext() != null) {
            Toast.makeText(getContext(), "Quiz completed!", Toast.LENGTH_SHORT).show();
        }

        if (questionNumberTextView != null) {
            questionNumberTextView.setVisibility(View.GONE);
        }
        if (boldTitle_txtView != null) {
            boldTitle_txtView.setText("Your score: ");
            boldTitle_txtView.setTextSize(24);
        }
        if (regTitle_txtView != null) {
            regTitle_txtView.setText(score + "/5");
            regTitle_txtView.setTextSize(24);
        }
        if (timerTextView != null) {
            timerTextView.setVisibility(View.GONE);
        }
        if (quizHeaderTextView != null) {
            quizHeaderTextView.setVisibility(View.GONE);
        }
        if (quizHeaderTextView2 != null) {
            quizHeaderTextView2.setText("Quiz Completed!");
            quizHeaderTextView2.setTextColor(Color.BLACK);
        }
        if (questionImageView != null) {
            questionImageView.setImageResource(R.drawable.img_medal);

        }
        for (MaterialCardView cardView : answerCardViews) {
            if (cardView != null) {
                cardView.setVisibility(View.GONE);
            }
        }
        // cancel timer to prevent memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (submitButton != null) {
            submitButton.setText("Back to lesson");
            submitButton.setOnClickListener(v -> navigateBack());
        }
    }

    // Handle back button press
    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (quizInProgress) {
                    Toast.makeText(requireContext(), "You cannot cancel the quiz once started!", Toast.LENGTH_SHORT).show();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });
    }

    private void navigateBack() {
        // navigate back to the lesson
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    protected void cleanupResources() {
        questionImageView = null;
        answerCardViews = null;
        answerTextViews = null;
        timerTextView = null;
        countDownTimer = null;
        submitButton = null;
        questionNumberTextView = null;
        progressViewModel = null;
    }

    /**
     * Create a new instance of QuizFragment with chapter and level
     */
    public static QuizFragment newInstance() {
        QuizFragment fragment = new QuizFragment();
        Bundle args = new Bundle();
        args.putInt("chapter", 1);
        args.putInt("level", 0);
        fragment.setArguments(args);
        return fragment;
    }
}

