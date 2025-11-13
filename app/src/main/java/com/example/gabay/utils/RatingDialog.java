package com.example.gabay.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gabay.R;
import com.example.gabay.services.UserAnalyticsService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Custom rating dialog for the Gabay app
 * Handles both in-app rating storage and Play Store redirection
 */
public class RatingDialog {
    private static final String TAG = "RatingDialog";
    private final Context context;
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    public interface RatingCallback {
        void onRatingSubmitted(int rating, String comment);
        void onRatingCancelled();
    }

    public RatingDialog(Context context) {
        this.context = context;
    }

    public void show(RatingCallback callback) {
        // Inflate custom rating dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_rating, null);

        // Get views
        TextView titleText = dialogView.findViewById(R.id.rating_title);
        TextView messageText = dialogView.findViewById(R.id.rating_message);
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        EditText commentEdit = dialogView.findViewById(R.id.rating_comment);
        Button submitButton = dialogView.findViewById(R.id.btn_submit_rating);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel_rating);

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(context, R.style.BlackTextDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set up rating bar listener
        ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, fromUser) -> {
            if (fromUser) {
                updateMessageBasedOnRating(messageText, commentEdit, (int) rating);
            }
        });

        // Submit button click
        submitButton.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = commentEdit.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            int ratingInt = (int) rating;
            
            // Handle rating submission
            handleRatingSubmission(ratingInt, comment, callback, dialog);
        });

        // Cancel button click
        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) {
                callback.onRatingCancelled();
            }
        });

        dialog.show();
    }

    private void updateMessageBasedOnRating(TextView messageText, EditText commentEdit, int rating) {
        if (rating >= 4) {
            messageText.setText("Great! We're glad that you enjoy our app!");
            commentEdit.setHint("Tell us what you love about Gabay! (optional)");
        } else if (rating >= 2) {
            messageText.setText("Thanks for your feedback! How can we improve?");
            commentEdit.setHint("Please tell us how we can make Gabay better");
        } else {
            messageText.setText("We're sorry to hear that. Please help us improve!");
            commentEdit.setHint("What went wrong? Your feedback helps us improve");
        }
    }

    private void handleRatingSubmission(int rating, String comment, RatingCallback callback, AlertDialog dialog) {
        // Show loading state
        dialog.setCancelable(false);
        
        // Submit to Supabase in background
        backgroundExecutor.execute(() -> {
            try {
                boolean success = UserAnalyticsService.submitAppRating(rating, comment);
                
                // Return to main thread for UI updates
                ((android.app.Activity) context).runOnUiThread(() -> {
                    dialog.dismiss();
                    
                    if (success) {
                        Toast.makeText(context, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
                        
                        // If rating is 4 or 5, offer to go to Play Store
                        if (rating >= 4) {
                            showPlayStoreOption();
                        }
                        
                        if (callback != null) {
                            callback.onRatingSubmitted(rating, comment);
                        }
                    } else {
                        Toast.makeText(context, "Failed to submit rating. Please try again.", Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onRatingCancelled();
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error submitting rating: " + e.getMessage());
                ((android.app.Activity) context).runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(context, "Error submitting rating. Please try again.", Toast.LENGTH_SHORT).show();
                    if (callback != null) {
                        callback.onRatingCancelled();
                    }
                });
            }
        });
    }

    private void showPlayStoreOption() {
        new AlertDialog.Builder(context, R.style.BlackTextDialog)
                .setTitle("Rate on Play Store")
                .setMessage("Would you like to rate Gabay on the Google Play Store? This helps other users discover our app!")
                .setPositiveButton("Rate on Play Store", (dialog, which) -> {
                    openPlayStore();
                })
                .setNegativeButton("Maybe Later", null)
                .show();
    }

    private void openPlayStore() {
        try {
            // Try to open Play Store app
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=" + context.getPackageName()));
            context.startActivity(rateIntent);
        } catch (android.content.ActivityNotFoundException e) {
            // If Play Store app is not available, open in browser
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName()));
            context.startActivity(rateIntent);
        }
    }

    /**
     * Static method to show rating dialog with simple callback
     */
    public static void showRatingDialog(Context context) {
        new RatingDialog(context).show(new RatingCallback() {
            @Override
            public void onRatingSubmitted(int rating, String comment) {
                Log.d(TAG, "Rating submitted: " + rating + " stars");
            }

            @Override
            public void onRatingCancelled() {
                Log.d(TAG, "Rating cancelled");
            }
        });
    }
}
