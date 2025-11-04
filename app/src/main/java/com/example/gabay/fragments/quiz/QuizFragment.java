package com.example.gabay.fragments.quiz;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
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

public class QuizFragment extends BaseFragment {

    private int[][] chapterImages;
    private String[][] chapterChoices;
    private String[][] chapterAnswers;


    private ImageView questionImageView;
    private MaterialCardView[] answerCardViews;
    private MaterialCardView timeBackground;
    private TextView[] answerTextViews;
    private Button submitButton;
    private TextView questionNumberTextView;
    private TextView quizHeaderTextView, quizHeaderTextView2;
    private TextView boldTitle_txtView, regTitle_txtView;
    private ProgressViewModel progressViewModel;

    private TextView timerTextView;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 11000; // 11 seconds per question
    private static final long COUNTDOWN_INTERVAL = 1000; // 1 second intervals
    private int lastSecond = -1;

    private SoundPool soundPool;
    private int tickSoundSFX, timeoutSoundSFX;
    private int correctAnswerSFX, wrongAnswerSFX, completeSFX;
    
    private int currentChapter;
    private int currentLevel;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int selectedAnswerIndex = -1;
    private boolean quizInProgress = false;
    private boolean isSubmitting = false;
    private boolean isSoundEnabled() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getBoolean("sound_enabled", true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_quiz, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        }

        // load sounds
        tickSoundSFX = soundPool.load(requireContext(),R.raw.tick, 1);
        timeoutSoundSFX = soundPool.load(requireContext(),R.raw.timeout, 1);
        correctAnswerSFX = soundPool.load(requireContext(),R.raw.correctchoice, 1);
        wrongAnswerSFX = soundPool.load(requireContext(),R.raw.wrongchoice, 1);
        completeSFX = soundPool.load(requireContext(),R.raw.complete, 1);

        return rootView;
    }

    @Override
    protected void initializeViews() {
        progressViewModel = getActivityViewModel(ProgressViewModel.class);

        // Get chapter and level from arguments
        Bundle args = getArguments();
        if (args != null) {
            currentChapter = args.getInt("chapter", 1);
            currentLevel = args.getInt("level", 1);
        }

        initializeQuizData();

        initializeQuizUI();
        showQuizIntroduction();
    }

    private void initializeQuizData() {
        // Chapter 1 data (your current data)
        int[] chapter1Images = {
                R.drawable.letter_c,
                R.drawable.letter_i,
                R.drawable.letter_a,
                R.drawable.letter_n,
                R.drawable.letter_f
        };

        String[] chapter1Choices = {
                "HELLO/KUMUSTA", "C", "NO/HINDI", "O",
                "A", "ONE/ISA", "I", "YOU/IKAW",
                "FIST/KAMAO", "J", "ME/AKO", "A",
                "N", "M", "Y", "L",
                "OK", "G", "THREE/TATLO", "F"
        };

        String[] chapter1Answers = {"C", "I", "A", "N", "F"};

        // Chapter 2 data (add your questions here)
        int[] chapter2Images = {
//                R.drawable.chapter2_q1,
//                R.drawable.chapter2_q2,
//                R.drawable.chapter2_q3,
//                R.drawable.chapter2_q4,
//                R.drawable.chapter2_q5
        };

        String[] chapter2Choices = {
                "Choice1", "Answer1", "Choice2", "Choice3",
                "Choice1", "Answer2", "Choice2", "Choice3",
                "Choice1", "Choice2", "Answer3", "Choice3",
                "Choice1", "Choice2", "Choice3", "Answer4",
                "Answer5", "Choice1", "Choice2", "Choice3"
        };

        String[] chapter2Answers = {"Answer1", "Answer2", "Answer3", "Answer4", "Answer5"};

        // Chapter 3, 4, 5 data (similar structure)
        // ... add more chapters

        // Organize all chapters
        chapterImages = new int[][] {
                chapter1Images,
                //chapter2Images,
                // chapter3Images,
                // chapter4Images,
                // chapter5Images
        };

        chapterChoices = new String[][] {
                chapter1Choices,
                //chapter2Choices,
                // chapter3Choices,
                // chapter4Choices,
                // chapter5Choices
        };

        chapterAnswers = new String[][] {
                chapter1Answers,
                //chapter2Answers,
                // chapter3Answers,
                // chapter4Answers,
                // chapter5Answers
        };
    }

    private int[] getCurrentImages() {
        return chapterImages[currentChapter - 1];
    }

    private String[] getCurrentChoices() {
        return chapterChoices[currentChapter - 1];
    }

    private String[] getCurrentAnswers() {
        return chapterAnswers[currentChapter - 1];
    }

    private void showQuizIntroduction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.BlackTextDialog);
        builder.setTitle("Quiz Time!");
        builder.setMessage("Quiz instructions:\n" +
                "• The quiz is multiple choice\n" +
                "• You have 10 seconds per question\n" +
                "• Once you submit your answer, you cannot go back\n" +
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

        timeBackground = rootView.findViewById(R.id.time_background);
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
                onTimeUp();
            }
        }.start();
    }
    private void updateTimer() {
        if (timerTextView != null) {
            int seconds = (int) (timeLeftInMillis / 1000);
            timerTextView.setText(String.format(":%02d", seconds));

            if (seconds > 3) {
                if (isSoundEnabled() && tickSoundSFX != 0) {
                    soundPool.play(tickSoundSFX, 10f, 10f, 0, 0, 1f);
                }
            } else {
                if (isSoundEnabled() && timeoutSoundSFX != 0) {
                    soundPool.play(timeoutSoundSFX, 1f, 1f, 0, 0, 1f);
                }
            }

            if (seconds <= 3) {
                timeBackground.setCardBackgroundColor(getResources().getColor(R.color.wrongAnswerColor));
                timeBackground.setStrokeWidth(0);
            } else {
                timeBackground.setCardBackgroundColor(getResources().getColor(R.color.secondaryColor));
                timeBackground.setStrokeWidth(0);
            }
        }
    }

    private void onTimeUp() {
        if (selectedAnswerIndex == -1) {
            Toast.makeText(getContext(), "Time's up! No answer selected.", Toast.LENGTH_SHORT).show();
        } else {
            boolean isCorrect = checkAnswer(selectedAnswerIndex);
            if (isCorrect) {
                score += 1;
                answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.correctAnswerColor));
                Toast.makeText(getContext(), "Time's up! Answer was correct!", Toast.LENGTH_SHORT).show();
                if (isSoundEnabled() && correctAnswerSFX != 0) soundPool.play(correctAnswerSFX, 1f, 1f, 0, 0, 1f);
            } else {
                answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.wrongAnswerColor));
                Toast.makeText(getContext(), "Time's up! Answer was: " + getCurrentAnswers()[currentQuestionIndex], Toast.LENGTH_SHORT).show();
                if (isSoundEnabled() && wrongAnswerSFX != 0) soundPool.play(wrongAnswerSFX, 1f, 1f, 0, 0, 1f);
            }
        }
        if (isSoundEnabled() && timeoutSoundSFX != 0) {
            soundPool.play(timeoutSoundSFX, 1f, 1f, 0, 0, 1f);
        }

        new Handler().postDelayed(() -> {
            moveToNextQuestion();
        }, 1200);
    }

    private void moveToNextQuestion() {
        currentQuestionIndex++;
        selectedAnswerIndex = -1;

        Log.d("QuizDebug", "Moving to question: " + currentQuestionIndex + " of " + getCurrentImages().length);

        if (currentQuestionIndex < getCurrentImages().length) {
            resetTimer();
            loadQuestion();
        } else {
            Log.d("QuizDebug", "🎯 ALL QUESTIONS COMPLETED - Calling completeQuiz()");
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
        if (isSubmitting) return; // block spam taps
        isSubmitting = true;      // lock the button

        if (selectedAnswerIndex == -1) {
            Toast.makeText(getContext(), "Please select an answer", Toast.LENGTH_SHORT).show();
            if (isSoundEnabled() && wrongAnswerSFX != 0) {
                soundPool.play(wrongAnswerSFX, 1f, 1f, 0, 0, 1f);
            }
            // FIX: Don't cancel timer here, just unlock after delay
            new Handler().postDelayed(() -> {
                isSubmitting = false; // unlock after a short delay
            }, 3000);
            return;
        }

        // Only cancel the timer when we have a valid answer submission
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        boolean isCorrect = checkAnswer(selectedAnswerIndex);

        if (isCorrect) {
            score += 1;
            answerCardViews[selectedAnswerIndex].setStrokeColor(
                    getResources().getColor(R.color.correctAnswerColor)
            );
            Toast.makeText(getContext(), "Correct!", Toast.LENGTH_SHORT).show();
            if (isSoundEnabled() && correctAnswerSFX != 0) {
                soundPool.play(correctAnswerSFX, 1f, 1f, 0, 0, 1f);
            }
        } else {
            answerCardViews[selectedAnswerIndex].setStrokeColor(
                    getResources().getColor(R.color.wrongAnswerColor)
            );
            Toast.makeText(
                    getContext(),
                    "Wrong! Correct answer: " + getCurrentAnswers()[currentQuestionIndex],
                    Toast.LENGTH_SHORT
            ).show();
            if (isSoundEnabled() && wrongAnswerSFX != 0) {
                soundPool.play(wrongAnswerSFX, 1f, 1f, 0, 0, 1f);
            }
        }

        // delay before going to next question
        new Handler().postDelayed(() -> {
            moveToNextQuestion();
            isSubmitting = false; // unlock again for the next question
        }, 1200);
    }

    private boolean checkAnswer(int selectedIndex) {
        if (answerTextViews[selectedIndex] == null) return false;
        String selectedAnswer = answerTextViews[selectedIndex].getText().toString();
        return selectedAnswer.equals(getCurrentAnswers()[currentQuestionIndex]);
    }

    private void loadQuestion() {

        updateTimer();
        if (questionNumberTextView != null) {
            questionNumberTextView.setText("Question " + (currentQuestionIndex + 1) + " of " + getCurrentImages().length);
        }

        // Load question image
        if (questionImageView != null && currentQuestionIndex < getCurrentImages().length) {
            questionImageView.setImageResource(getCurrentImages()[currentQuestionIndex]);
        }

        if (answerTextViews != null) {
            int choicesStartIndex = currentQuestionIndex * 4;
            String[] currentChoices = getCurrentChoices();
            for (int i = 0; i < answerTextViews.length; i++) {
                if (answerTextViews[i] != null && (choicesStartIndex + i) < currentChoices.length) {
                    answerTextViews[i].setText(currentChoices[choicesStartIndex + i]);

                    // Reset CardView appearance including stroke
                    if (answerCardViews[i] != null) {
                        answerCardViews[i].setCardBackgroundColor(getResources().getColor(android.R.color.white));
                        answerCardViews[i].setCardElevation(2f);
                        answerCardViews[i].setStrokeWidth(0); // Remove stroke
                    }
                }
            }
        }

        selectedAnswerIndex = -1;
    }

    private void completeQuiz() {
        quizInProgress = false;

        // Only mark as completed if score is passing (3/5 or more)
        boolean quizPassed = score >= 3;

        Log.d("QuizDebug", "=== QUIZ COMPLETION ===");
        Log.d("QuizDebug", "Chapter: " + currentChapter);
        Log.d("QuizDebug", "Score: " + score + "/5");
        Log.d("QuizDebug", "Quiz Passed: " + quizPassed);

        if (progressViewModel != null && quizPassed) {
            Log.d("QuizDebug", "✅ Quiz passed - marking as completed");
            progressViewModel.markQuizCompleted(currentChapter);

            // Also update chapter progress
            progressViewModel.updateChapterProgress(currentChapter, currentLevel);

            // Verify after delay
            new Handler().postDelayed(() -> {
                boolean isComplete = progressViewModel.isQuizCompleted(currentChapter);
                Log.d("QuizDebug", "Verification - Quiz completed: " + isComplete);
                progressViewModel.loadQuizCompletionFromSupabase();
            }, 1500);

        } else if (!quizPassed) {
            Log.d("QuizDebug", "❌ Quiz failed - not marking as completed");
            Toast.makeText(getContext(), "Quiz failed. Score: " + score + "/5. Need 3/5 to pass.", Toast.LENGTH_LONG).show();
        } else {
            Log.e("QuizDebug", "❌ ProgressViewModel is null");
        }

        showCompletionFeedback();
        updateUIForCompletion();
        cleanupQuizResources();
        setupNavigation();
    }

    private void showCompletionFeedback() {
        if (getContext() == null) return;

        if (isSoundEnabled() && completeSFX != 0) {
            soundPool.play(completeSFX, 1f, 1f, 0, 0, 1f);
        }
    }

    private void updateUIForCompletion() {
        setVisibility(View.GONE, questionNumberTextView, quizHeaderTextView);

        if (timeBackground != null) {
            timeBackground.setVisibility(View.GONE);
        }

        if (boldTitle_txtView != null) {
            boldTitle_txtView.setText("Your score: " + score + "/5");
            boldTitle_txtView.setTextSize(24);
        }
        if (regTitle_txtView != null) {
            regTitle_txtView.setText("");
            regTitle_txtView.setTextSize(24);
        }

        updateQuizResult();

        for (MaterialCardView cardView : answerCardViews) {
            if (cardView != null) {
                cardView.setVisibility(View.GONE);
            }
        }
    }

    private void updateQuizResult() {
        if (questionImageView == null) return;

        // setting up the layout programmatically for good ui
        int topMarginDp = 200;
        int topMarginPx = (int) (topMarginDp * getResources().getDisplayMetrics().density);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) questionImageView.getLayoutParams();
        params.topMargin = topMarginPx;
        questionImageView.setLayoutParams(params);

        if (score <= 2) {
            questionImageView.setImageResource(R.drawable.img_fourleafclover);
            Toast.makeText(getContext(), "Quiz Failed. Nice try!", Toast.LENGTH_SHORT).show();
            setQuizResultText("Quiz Failed", Color.BLACK);
        } else {
            questionImageView.setImageResource(R.drawable.img_medal);
            Toast.makeText(getContext(), "Quiz Completed. Good job!", Toast.LENGTH_SHORT).show();
            setQuizResultText("Quiz Completed", Color.BLACK);
        }
    }

    private void setQuizResultText(String text, int color) {
        if (quizHeaderTextView2 != null) {
            quizHeaderTextView2.setText(text);
            quizHeaderTextView2.setTextColor(color);
        }
    }

    private void setVisibility(int visibility, TextView... views) {
        for (TextView view : views) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }

    private void cleanupQuizResources() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void setupNavigation() {
        if (submitButton != null) {
            submitButton.setText("Back to lesson");
            submitButton.setOnClickListener(v -> navigateBack());
        }
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public static QuizFragment newInstance(int chapter, int level) {
        QuizFragment fragment = new QuizFragment();
        Bundle args = new Bundle();
        args.putInt("chapter", chapter);
        args.putInt("level", level);
        fragment.setArguments(args);
        return fragment;
    }
}

