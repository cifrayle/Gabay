package com.example.gabay.fragments.quiz;

import android.annotation.SuppressLint;
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


    private ImageView questionImageView, completionImageView;
    private MaterialCardView[] answerCardViews;
    private MaterialCardView timeBackground, questionImageCard;
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

    //this should handle data for chapter 1 and 4
    private void initializeQuizData() {
        // ---- Base data (defaults) ----

        // Chapter 1 (your current data)
        int[] chapter1Images = {
                R.drawable.img_letter_c,
                R.drawable.img_letter_a,
                R.drawable.img_letter_s,
                R.drawable.img_letter_h,
                R.drawable.img_letter_g,
                R.drawable.img_letter_j,
                R.drawable.img_letter_i,
                R.drawable.img_letter_u,
                R.drawable.img_letter_t,
                R.drawable.img_letter_f
        };

        String[] chapter1Choices = {
                "HELLO/KUMUSTA", "C", "NO/HINDI", "O",
                "A", "ONE/ISA", "I", "YOU/IKAW",
                "FIST/KAMAO", "S", "ME/AKO", "A",
                "H", "LEFT/KALIWA", "Y", "L",
                "X", "G", "TWO/DALAWA", "L",
                "I", "ONE/ISA", "J", "B",
                "I", "R", "J", "ONE/ISA",
                "T", "D", "F", "U",
                "D", "T", "Y", "L",
                "THREE/TATLO", "F", "OK", "K",
        };

        String[] chapter1Answers = {"C", "A", "S", "H", "G", "J", "I", "U", "T", "F"};

        // Chapter 4 (placeholder — replace with your real data)
        int[] chapter4Images = {
                // R.drawable.ch4_q1, R.drawable.ch4_q2, R.drawable.ch4_q3, R.drawable.ch4_q4, R.drawable.ch4_q5
        };

        String[] chapter4Choices = {
                // MUST be 4 × numberOfImages
                // "Choice1", "Answer1", "Choice2", "Choice3",
                // "Choice1", "Answer2", "Choice2", "Choice3",
                // "Choice1", "Choice2", "Answer3", "Choice3",
                // "Choice1", "Choice2", "Choice3", "Answer4",
                // "Answer5", "Choice1", "Choice2", "Choice3"
        };

        String[] chapter4Answers = {
                // "Answer1","Answer2","Answer3","Answer4","Answer5"
        };

        // ---- Allocate chapter tables up to chapter 4 ----
        // We keep slots for 2 & 3 empty, since those chapters use MatchingQuizFragment
        chapterImages = new int[][] {
                chapter1Images,      // index 0 -> chapter 1
                new int[] {},        // index 1 -> chapter 2 (unused here)
                new int[] {},        // index 2 -> chapter 3 (unused here)
                chapter4Images       // index 3 -> chapter 4
        };

        chapterChoices = new String[][] {
                chapter1Choices,
                new String[] {},     // ch2 placeholder
                new String[] {},     // ch3 placeholder
                chapter4Choices
        };

        chapterAnswers = new String[][] {
                chapter1Answers,
                new String[] {},     // ch2 placeholder
                new String[] {},     // ch3 placeholder
                chapter4Answers
        };

        // ---- Optional override via Bundle (from QuizRegistry) ----
        // Lets QuizRegistry inject MCQ arrays for the *current* chapter.
        Bundle args = getArguments();
        if (args != null) {
            int[] images = args.getIntArray("mcq_images");
            String[] choices = args.getStringArray("mcq_choices");
            String[] answers = args.getStringArray("mcq_answers");

            // Only override if everything lines up
            if (images != null && answers != null && choices != null
                    && images.length > 0
                    && answers.length == images.length
                    && choices.length == images.length * 4) {

                int idx = Math.max(0, currentChapter - 1);

                // Expand tables if a higher chapter index ever shows up
                if (idx >= chapterImages.length) {
                    int newLen = idx + 1;

                    int[][] newImgs = new int[newLen][];
                    String[][] newCh = new String[newLen][];
                    String[][] newAns = new String[newLen][];

                    System.arraycopy(chapterImages, 0, newImgs, 0, chapterImages.length);
                    System.arraycopy(chapterChoices, 0, newCh, 0, chapterChoices.length);
                    System.arraycopy(chapterAnswers, 0, newAns, 0, chapterAnswers.length);

                    // fill any gaps with empty arrays
                    for (int i = chapterImages.length; i < newLen; i++) {
                        newImgs[i] = new int[]{};
                        newCh[i] = new String[]{};
                        newAns[i] = new String[]{};
                    }

                    chapterImages = newImgs;
                    chapterChoices = newCh;
                    chapterAnswers = newAns;
                }

                chapterImages[idx] = images;
                chapterChoices[idx] = choices;
                chapterAnswers[idx] = answers;
            }
        }
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
        questionImageCard = rootView.findViewById(R.id.question_image_card);
        questionImageView = rootView.findViewById(R.id.question_image);
        completionImageView = rootView.findViewById(R.id.completion_image);
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
    @SuppressLint("DefaultLocale")
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
                if (isSoundEnabled() && correctAnswerSFX != 0) soundPool.play(correctAnswerSFX, 1f, 1f, 0, 0, 1f);
            } else {
                answerCardViews[selectedAnswerIndex].setStrokeColor(getResources().getColor(R.color.wrongAnswerColor));
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
        // highlight selected CardView
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
            new Handler().postDelayed(() -> {
                isSubmitting = false; // unlock after a short delay
            }, 3000);
            return;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        boolean isCorrect = checkAnswer(selectedAnswerIndex);

        if (isCorrect) {
            score += 1;
            answerCardViews[selectedAnswerIndex].setStrokeColor(
                    getResources().getColor(R.color.correctAnswerColor)
            );
            if (isSoundEnabled() && correctAnswerSFX != 0) {
                soundPool.play(correctAnswerSFX, 1f, 1f, 0, 0, 1f);
            }
        } else {
            answerCardViews[selectedAnswerIndex].setStrokeColor(
                    getResources().getColor(R.color.wrongAnswerColor)
            );
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
        // load question
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
        boolean quizPassed = score >= 3;

        Log.d("QuizDebug", "=== QUIZ COMPLETION ===");
        Log.d("QuizDebug", "Chapter: " + currentChapter);
        Log.d("QuizDebug", "Score: " + score + "/" + getCurrentImages().length);
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
        if (questionImageCard != null)
        {
            questionImageCard.setVisibility(View.GONE);
        }

        if (boldTitle_txtView != null) {
            boldTitle_txtView.setText("Your score: " + score + "/" + getCurrentImages().length);
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
        if (completionImageView != null)
        {
            completionImageView.setVisibility(View.VISIBLE);
        }
        int topMarginDp = 200;
        int topMarginPx = (int) (topMarginDp * getResources().getDisplayMetrics().density);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) completionImageView.getLayoutParams();
        params.topMargin = topMarginPx;
        completionImageView.setLayoutParams(params);

        if (score <= 4) {
            completionImageView.setImageResource(R.drawable.img_fourleafclover);
            setQuizResultText("Quiz Failed", Color.BLACK);
        } else {
            completionImageView.setImageResource(R.drawable.img_medal);
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

