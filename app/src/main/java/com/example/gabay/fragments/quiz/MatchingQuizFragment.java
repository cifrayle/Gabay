package com.example.gabay.fragments.quiz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.gabay.R;
import com.example.gabay.utils.SharedPreferenceHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingQuizFragment extends BaseQuizFragment {

    // Quiz data
    private int[] imageResources;
    private String[] answerLetters;
    private Map<Integer, Integer> correctMatches; // imageResource -> containerIndex
    private Map<Integer, Integer> userMatches; // containerIndex -> imageResource

    // UI Components
    private LinearLayout leftColumn;
    private LinearLayout rightColumn;
    private Button finishButton;
    private TextView quizHeader;
    private TextView quizHeader2;
    private TextView instructionsText;
    private TextView boldTitleTextView, boldTitleTextView2;
    private TextView regTitleTextView, regTitleTextView2;
    private ImageView completion_image_pass;
    private ImageView completion_image_fail;
    private View scrollContainer;
    private LinearLayout completionTextHolder, completionTextHolder2;

    // Draggable images and drop containers
    private List<MaterialCardView> draggableImageCards;
    private List<MaterialCardView> dropContainers;
    private List<ImageView> containerImages; // Images currently in containers
    private List<TextView> containerLabels; // Text labels in containers
    private Map<Integer, MaterialCardView> imageResourceToCard; // Map image resource to its card view

    // Quiz state
    private int score = 0;
    private int totalQuestions = 0;
    private boolean quizCompleted = false;
    private boolean quizInProgress = false;

    // Sound effects
    private SoundPool soundPool;
    private int correctAnswerSFX;
    private int wrongAnswerSFX;
    private int completeSFX;
    private int dropSoundSFX;

    // Constants
    private static final int CONTAINER_MIN_HEIGHT_DP = 120;
    private static final int IMAGE_SIZE_DP = 100;
    private static final int CONTAINER_PADDING_DP = 16;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_matching_quiz, container, false);

        // Initialize sound pool
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

        // Load sounds
        correctAnswerSFX = soundPool.load(requireContext(), R.raw.correctchoice, 1);
        wrongAnswerSFX = soundPool.load(requireContext(), R.raw.wrongchoice, 1);
        completeSFX = soundPool.load(requireContext(), R.raw.complete, 1);
        dropSoundSFX = soundPool.load(requireContext(), R.raw.tick, 1);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeQuizData();
        initializeUI();
        showQuizIntroduction();
    }

    private void initializeQuizData() {
        // defaults (only used if no args provided)
        int[] defaultImages = new int[]{
                R.drawable.img_letter_c,
                R.drawable.img_letter_a,
                R.drawable.img_letter_s,
                R.drawable.img_letter_h,
                R.drawable.img_letter_g,
        };
        String[] defaultLabels = new String[]{"C", "A", "S", "H", "G"};

        // try to get data from QuizRegistry
        Bundle args = getArguments();
        int[] imagesFromArgs = args != null ? args.getIntArray("match_images") : null;
        String[] labelsFromArgs = args != null ? args.getStringArray("match_labels") : null;

        boolean validArgs = imagesFromArgs != null
                && labelsFromArgs != null
                && imagesFromArgs.length > 0
                && imagesFromArgs.length == labelsFromArgs.length;

        imageResources = validArgs ? imagesFromArgs : defaultImages;
        answerLetters = validArgs ? labelsFromArgs : defaultLabels;

        // Build correct map (index-based mapping: image[i] ↔ label[i])
        correctMatches = new HashMap<>();
        for (int i = 0; i < imageResources.length; i++) {
            correctMatches.put(imageResources[i], i);
        }

        totalQuestions = imageResources.length;
        userMatches = new HashMap<>();
        draggableImageCards = new ArrayList<>();
        dropContainers = new ArrayList<>();
        containerImages = new ArrayList<>();
        containerLabels = new ArrayList<>();
        imageResourceToCard = new HashMap<>();
    }

    private void initializeUI() {
        View rootView = getView();
        if (rootView == null) return;

        leftColumn = rootView.findViewById(R.id.leftColumn);
        rightColumn = rootView.findViewById(R.id.rightColumn);
        finishButton = rootView.findViewById(R.id.finish_button);
        quizHeader = rootView.findViewById(R.id.quiz_header);
        quizHeader2 = rootView.findViewById(R.id.quiz_header2);
        instructionsText = rootView.findViewById(R.id.instructions_text);
        boldTitleTextView = rootView.findViewById(R.id.boldTitle_txtView);
        regTitleTextView = rootView.findViewById(R.id.regTitle_txtView);
        completionTextHolder2 = rootView.findViewById(R.id.completion_text_holder2);
        boldTitleTextView2 = rootView.findViewById(R.id.boldTitle_txtView2);
        regTitleTextView2 = rootView.findViewById(R.id.regTitle_txtView2);
        completion_image_pass = rootView.findViewById(R.id.completion_image_pass);
        completion_image_fail = rootView.findViewById(R.id.completion_image_fail);
        scrollContainer = rootView.findViewById(R.id.scroll_container);
        completionTextHolder = rootView.findViewById(R.id.completion_text_holder);

        finishButton.setOnClickListener(v -> finishQuiz());

        setupDragAndDropUI();
    }

    private void setupDragAndDropUI() {
        if (leftColumn == null || rightColumn == null) return;

        leftColumn.removeAllViews();
        rightColumn.removeAllViews();
        draggableImageCards.clear();
        dropContainers.clear();
        containerImages.clear();
        containerLabels.clear();
        imageResourceToCard.clear();

        float density = getResources().getDisplayMetrics().density;
        int imageSizePx = (int) (IMAGE_SIZE_DP * density);
        int containerMinHeightPx = (int) (CONTAINER_MIN_HEIGHT_DP * density);
        int containerPaddingPx = (int) (CONTAINER_PADDING_DP * density);

        // Create shuffled list of images for display
        List<Integer> shuffledImages = new ArrayList<>();
        for (int imageRes : imageResources) {
            shuffledImages.add(imageRes);
        }
        Collections.shuffle(shuffledImages);

        // Create draggable images on the left
        for (int imageRes : shuffledImages) {
            MaterialCardView cardView = createDraggableImage(imageRes, imageSizePx);
            leftColumn.addView(cardView);
            draggableImageCards.add(cardView);
            imageResourceToCard.put(imageRes, cardView);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardView.getLayoutParams();
            params.setMargins(0, 0, 0, (int) (16 * density));
            cardView.setLayoutParams(params);
        }

        // Create drop containers on the right with letters
        for (int i = 0; i < answerLetters.length; i++) {
            MaterialCardView container = createDropContainer(
                    answerLetters[i],
                    i,
                    containerMinHeightPx,
                    containerPaddingPx
            );
            rightColumn.addView(container);
            dropContainers.add(container);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) container.getLayoutParams();
            params.setMargins(0, 0, 0, (int) (16 * density));
            container.setLayoutParams(params);
        }
    }

    private MaterialCardView createDraggableImage(int imageResource, int sizePx) {
        // Create card view
        MaterialCardView cardView = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        cardView.setCardElevation(8f);
        cardView.setRadius(12f);
        cardView.setPadding(12, 12, 12, 12);
        cardView.setTag(imageResource);

        // Create image view
        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(sizePx, sizePx);
        imageView.setLayoutParams(imageParams);
        imageView.setImageResource(imageResource);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        cardView.addView(imageView);

        // Make draggable
        cardView.setOnLongClickListener(v -> {
            if (quizCompleted) return false;
            if (cardView.getVisibility() != View.VISIBLE) return false; // Can't drag hidden images

            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(cardView);
            DragData dragData = new DragData(imageResource, cardView);
            cardView.startDragAndDrop(
                    null,
                    shadowBuilder,
                    dragData,
                    0
            );
            cardView.setAlpha(0.5f);

            // Set up drag end listener to reset alpha if drag is cancelled
            cardView.setOnDragListener((view, event) -> {
                if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    if (!event.getResult()) {
                        // Drag was cancelled or dropped outside valid drop zone
                        cardView.setAlpha(1.0f);
                    }
                    cardView.setOnDragListener(null); // Remove listener after drag ends
                }
                return false;
            });

            return true;
        });

        return cardView;
    }

    private MaterialCardView createDropContainer(String letter, int index, int minHeight, int padding) {
        MaterialCardView container = new MaterialCardView(requireContext());
        Typeface tf = ResourcesCompat.getFont(requireContext(), R.font.nunito_bold);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setMinimumHeight(minHeight);
        container.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        container.setCardElevation(4f);
        container.setRadius(12f);
        container.setPadding(padding, padding, padding, padding);
        container.setStrokeWidth(3);
        container.setStrokeColor(getResources().getColor(R.color.secondaryColor));

        // Create inner layout
        LinearLayout innerLayout = new LinearLayout(requireContext());
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setGravity(android.view.Gravity.CENTER);
        innerLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Letter label
        TextView letterLabel = new TextView(requireContext());
        letterLabel.setText(letter);
        letterLabel.setTextSize(24f);
        letterLabel.setTypeface(tf);
        letterLabel.setTextColor(getResources().getColor(R.color.buttonTextColor));
        letterLabel.setGravity(android.view.Gravity.CENTER);
        letterLabel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Placeholder for dropped image
        ImageView placeholderImage = new ImageView(requireContext());
        placeholderImage.setVisibility(View.GONE);
        placeholderImage.setLayoutParams(new LinearLayout.LayoutParams(
                (int) (IMAGE_SIZE_DP * getResources().getDisplayMetrics().density),
                (int) (IMAGE_SIZE_DP * getResources().getDisplayMetrics().density)
        ));
        placeholderImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        innerLayout.addView(letterLabel);
        innerLayout.addView(placeholderImage);

        container.addView(innerLayout);
        container.setTag(index);

        containerLabels.add(letterLabel);
        containerImages.add(placeholderImage);

        // Set up drag listener
        container.setOnDragListener(new DropTargetListener(index));

        return container;
    }

    private class DropTargetListener implements View.OnDragListener {
        private final int containerIndex;

        DropTargetListener(int containerIndex) {
            this.containerIndex = containerIndex;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            MaterialCardView container = (MaterialCardView) v;
            int action = event.getAction();

            switch (action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    container.setCardBackgroundColor(getResources().getColor(R.color.primaryColor));
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    container.setCardBackgroundColor(getResources().getColor(R.color.secondaryColor));
                    container.setAlpha(0.8f);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    container.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                    container.setAlpha(1.0f);
                    return true;

                case DragEvent.ACTION_DROP:
                    Object localState = event.getLocalState();
                    if (localState instanceof DragData) {
                        DragData dragData = (DragData) localState;
                        handleDrop(dragData.imageResource, containerIndex, container);
                        return true;
                    }
                    return false;

                case DragEvent.ACTION_DRAG_ENDED:
                    container.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                    container.setAlpha(1.0f);

                    // Reset draggable image alpha if drag was cancelled (not dropped)
                    if (!event.getResult()) {
                        if (event.getLocalState() instanceof DragData) {
                            DragData dragData = (DragData) event.getLocalState();
                            if (dragData.originalView != null) {
                                dragData.originalView.setAlpha(1.0f);
                            }
                        }
                    }
                    return true;

                default:
                    return false;
            }
        }
    }

    private void handleDrop(int imageResource, int containerIndex, MaterialCardView container) {
        if (quizCompleted) return;

        // Remove image from previous container if it was already placed
        Integer previousContainer = null;
        for (Map.Entry<Integer, Integer> entry : userMatches.entrySet()) {
            if (entry.getValue() == imageResource) {
                previousContainer = entry.getKey();
                break;
            }
        }
        if (previousContainer != null && previousContainer != containerIndex) {
            clearContainer(previousContainer);
            userMatches.remove(previousContainer);
        }

        // Check if container already has an image - return it to left column
        if (userMatches.containsKey(containerIndex)) {
            int oldImageResource = userMatches.get(containerIndex);
            clearContainer(containerIndex);
            userMatches.remove(containerIndex);
            // Show the old image again in left column
            showImageInLeftColumn(oldImageResource);
        }

        // Place image in container
        ImageView containerImage = containerImages.get(containerIndex);
        containerImage.setImageResource(imageResource);
        containerImage.setVisibility(View.VISIBLE);
        userMatches.put(containerIndex, imageResource);

        // Validate match
        boolean isCorrect = validateMatch(imageResource, containerIndex);

        // Visual feedback
        if (isCorrect) {
            container.setStrokeColor(getResources().getColor(R.color.correctAnswerColor));
            container.setStrokeWidth(5);
            if (isSoundEnabled() && correctAnswerSFX != 0) {
                soundPool.play(correctAnswerSFX, 1f, 1f, 0, 0, 1f);
            }
        } else {
            container.setStrokeColor(getResources().getColor(R.color.wrongAnswerColor));
            container.setStrokeWidth(5);
            if (isSoundEnabled() && wrongAnswerSFX != 0) {
                soundPool.play(wrongAnswerSFX, 0.5f, 0.5f, 0, 0, 1f);
            }
        }

        // Hide the dragged image from left column
        hideDraggedImage(imageResource);

        // Reset card alpha if drag ended
        MaterialCardView draggedCard = imageResourceToCard.get(imageResource);
        if (draggedCard != null) {
            draggedCard.setAlpha(1.0f);
        }
    }

    private void hideDraggedImage(int imageResource) {
        // Find and hide the card view in the left column
        MaterialCardView cardView = imageResourceToCard.get(imageResource);
        if (cardView != null) {
            cardView.setVisibility(View.GONE);
        }
    }

    private void showImageInLeftColumn(int imageResource) {
        // Find and show the card view in the left column
        MaterialCardView cardView = imageResourceToCard.get(imageResource);
        if (cardView != null) {
            cardView.setVisibility(View.VISIBLE);
            cardView.setAlpha(1.0f);
        }
    }

    private void clearContainer(int containerIndex) {
        if (containerIndex >= 0 && containerIndex < containerImages.size()) {
            ImageView containerImage = containerImages.get(containerIndex);
            containerImage.setVisibility(View.GONE);
            containerImage.setImageResource(0);

            MaterialCardView container = dropContainers.get(containerIndex);
            container.setStrokeColor(getResources().getColor(R.color.secondaryColor));
            container.setStrokeWidth(3);
        }
    }

    private boolean validateMatch(int imageResource, int containerIndex) {
        Integer correctContainer = correctMatches.get(imageResource);
        return correctContainer != null && correctContainer == containerIndex;
    }

    private void showQuizIntroduction() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext(), R.style.BlackTextDialog);
        builder.setTitle("Matching Quiz!");
        builder.setMessage("Quiz instructions:\n" +
                "• Drag images from the left to match with letters on the right\n" +
                "• Each image should be matched with its corresponding letter\n" +
                "• You can change your answers by dragging a new image\n" +
                "• Click Finish when you're done\n\n" +
                "Good luck!");
        builder.setPositiveButton("Start Quiz", (dialog, which) -> {
            startQuiz();
        });
        builder.setNegativeButton("Back", (dialog, which) -> {
            navigateBack();
        });
        builder.setCancelable(false);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.secondaryColor));
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor));
        }
    }

    private void startQuiz() {
        quizInProgress = true;
        quizCompleted = false;
        finishButton.setEnabled(true);
        finishButton.setText(R.string.finish);
    }

    private void finishQuiz() {
        if (quizCompleted) {
            navigateBack();
            return;
        }

        // Check if all items are matched
        int matchedCount = userMatches.size();
        if (matchedCount < totalQuestions) {
            // Show confirmation dialog
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext(), R.style.BlackTextDialog);
            builder.setTitle("Finish Quiz?");
            builder.setMessage("You have matched " + matchedCount + " out of " + totalQuestions + " items.\n\n" +
                    "Are you sure you want to finish?");
            builder.setPositiveButton("Finish", (dialog, which) -> {
                calculateAndShowResults();
            });
            builder.setNegativeButton("Continue", (dialog, which) -> {
                // Do nothing, just close the dialog
            });
            builder.setCancelable(true);

            android.app.AlertDialog dialog = builder.create();
            dialog.show();

            Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(getResources().getColor(R.color.secondaryColor));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor));
            }
        } else {
            calculateAndShowResults();
        }
    }

    private void calculateAndShowResults() {
        // Calculate score
        score = 0;
        for (Map.Entry<Integer, Integer> entry : userMatches.entrySet()) {
            int containerIndex = entry.getKey();
            int imageResource = entry.getValue();
            if (validateMatch(imageResource, containerIndex)) {
                score++;
            }
        }

        // Disable all dragging
        quizCompleted = true;
        quizInProgress = false;

        // Lock all containers to prevent further changes
        for (MaterialCardView container : dropContainers) {
            container.setOnDragListener(null);
        }
        for (MaterialCardView card : draggableImageCards) {
            card.setOnLongClickListener(null);
        }

        Log.d("MatchingQuizDebug", "=== QUIZ COMPLETION ===");
        Log.d("MatchingQuizDebug", "Chapter: " + currentChapter);
        Log.d("MatchingQuizDebug", "Score: " + score + "/" + totalQuestions);

        // Call the base class method to handle completion logic
        onQuizCompleted(score, totalQuestions);
    }

    // ============ REQUIRED BASE CLASS METHODS ============

    @Override
    protected void updateUIForCompletion() {
        // Hide instructions/header
        if (instructionsText != null) instructionsText.setVisibility(View.GONE);
        if (quizHeader != null)       quizHeader.setVisibility(View.GONE);

        // Hide both completion text holders initially
        if (completionTextHolder != null) completionTextHolder.setVisibility(View.GONE);
        if (completionTextHolder2 != null) completionTextHolder2.setVisibility(View.GONE);

        // Determine pass/fail using your existing criteria
        int passingScore = (int) Math.ceil(totalQuestions * 0.7);
        boolean isPass = score >= passingScore;

        // Get the localized score string
        String scoreText = getString(R.string.score) +" "+ score + "/" + totalQuestions;

        // Show the appropriate completion text holder based on pass/fail
        if (isPass) {
            if (completionTextHolder != null) completionTextHolder.setVisibility(View.VISIBLE);

            if (boldTitleTextView != null) {
                boldTitleTextView.setVisibility(View.VISIBLE);
                boldTitleTextView.setText(scoreText);
                boldTitleTextView.setTextSize(20);
            }
            if (regTitleTextView != null) {
                regTitleTextView.setText("");
                regTitleTextView.setTextSize(20);
            }
        } else {
            if (completionTextHolder2 != null) completionTextHolder2.setVisibility(View.VISIBLE);

            if (boldTitleTextView2 != null) {
                boldTitleTextView2.setVisibility(View.VISIBLE);
                boldTitleTextView2.setText(scoreText);
                boldTitleTextView2.setTextSize(20);
            }
            if (regTitleTextView2 != null) {
                regTitleTextView2.setText("");
                regTitleTextView2.setTextSize(20);
            }
        }

        // This method handles showing the right image (pass/fail)
        updateQuizResult();

        // Hide the matching UI so results are front-and-center.
        if (scrollContainer != null) scrollContainer.setVisibility(View.GONE);
        if (leftColumn != null) { leftColumn.removeAllViews(); leftColumn.setVisibility(View.GONE); }
        if (rightColumn != null){ rightColumn.removeAllViews(); rightColumn.setVisibility(View.GONE); }

        // Make sure result widgets are on top.
        if (completionTextHolder != null) completionTextHolder.bringToFront();
        if (completionTextHolder2 != null) completionTextHolder2.bringToFront();
        // Also bring the new images to the front (only one will be visible).
        if (completion_image_pass != null) completion_image_pass.bringToFront();
        if (completion_image_fail != null) completion_image_fail.bringToFront();
    }

    @Override
    protected void cleanupQuizResources() {
        // Clean up any quiz-specific resources
        if (leftColumn != null) leftColumn.removeAllViews();
        if (rightColumn != null) rightColumn.removeAllViews();
        draggableImageCards.clear();
        dropContainers.clear();
        containerImages.clear();
        containerLabels.clear();
        imageResourceToCard.clear();
    }

    @Override
    protected void setupNavigation() {
        if (finishButton != null) {
            finishButton.setText(R.string.back_to_lesson);
            finishButton.setOnClickListener(v -> navigateBack());
        }
    }

    @Override
    protected void showCompletionFeedback() {
        if (getContext() == null) return;
        if (isSoundEnabled() && completeSFX != 0) {
            soundPool.play(completeSFX, 1f, 1f, 0, 0, 1f);
        }
    }

    // ============ HELPER METHODS ============

    private void updateQuizResult() {
        // Ensure the views were found before proceeding
        if (completion_image_pass == null || completion_image_fail == null) {
            Log.e("MatchingQuiz", "Completion ImageViews not found.");
            return;
        }

        // Define which view will be shown and which will be hidden
        ImageView viewToShow;
        ImageView viewToHide;

        // Use a clear passing score threshold
        int passingScore = (int) Math.ceil(totalQuestions * 0.6);
        if (score >= passingScore) {
            viewToShow = completion_image_pass;
            viewToHide = completion_image_fail;
            setQuizResultText(getString(R.string.quiz_completed), Color.BLACK);
        } else {
            viewToShow = completion_image_fail;
            viewToHide = completion_image_pass;
            setQuizResultText(getString(R.string.quiz_failed), Color.BLACK);
        }

        // Apply the standard layout logic to the image that will be SHOWN
        int topMarginDp = 200;
        int topMarginPx = (int) (topMarginDp * getResources().getDisplayMetrics().density);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) viewToShow.getLayoutParams();
        params.topMargin = topMarginPx;
        viewToShow.setLayoutParams(params);

        // Finally, set the visibility
        viewToHide.setVisibility(View.GONE);
        viewToShow.setVisibility(View.VISIBLE);
    }

    private void setQuizResultText(String text, int color) {
        if (quizHeader2 != null) {
            quizHeader2.setText(text);
            quizHeader2.setTextColor(color);
        }
    }

    private void resetAllContainerStrokes() {
        // Reset all containers to show final validation state
        for (Map.Entry<Integer, Integer> entry : userMatches.entrySet()) {
            int containerIndex = entry.getKey();
            int imageResource = entry.getValue();
            MaterialCardView container = dropContainers.get(containerIndex);

            boolean isCorrect = validateMatch(imageResource, containerIndex);
            if (isCorrect) {
                container.setStrokeColor(getResources().getColor(R.color.correctAnswerColor));
                container.setStrokeWidth(5);
            } else {
                container.setStrokeColor(getResources().getColor(R.color.wrongAnswerColor));
                container.setStrokeWidth(5);
            }
        }

        // Show containers that weren't matched in a neutral state
        for (int i = 0; i < dropContainers.size(); i++) {
            if (!userMatches.containsKey(i)) {
                MaterialCardView container = dropContainers.get(i);
                container.setStrokeColor(getResources().getColor(R.color.secondaryColor));
                container.setStrokeWidth(3);
            }
        }
    }

    private boolean isSoundEnabled() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getBoolean("sound_enabled", true);
    }

    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (quizInProgress && !quizCompleted) {
                    Toast.makeText(requireContext(), "Please finish the quiz first!", Toast.LENGTH_SHORT).show();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public static MatchingQuizFragment newInstance(int chapter, int level) {
        MatchingQuizFragment fragment = new MatchingQuizFragment();
        Bundle args = new Bundle();
        args.putInt("chapter", chapter);
        args.putInt("level", level);
        fragment.setArguments(args);
        return fragment;
    }

    // Helper class to store drag data
    private static class DragData {
        final int imageResource;
        final View originalView;

        DragData(int imageResource, View originalView) {
            this.imageResource = imageResource;
            this.originalView = originalView;
        }
    }
}