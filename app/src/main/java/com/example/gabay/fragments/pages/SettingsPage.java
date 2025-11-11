package com.example.gabay.fragments.pages;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;

import com.example.gabay.R;
import com.example.gabay.activities.AuthActivity;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.utils.NotificationReceiver;
import com.example.gabay.viewmodels.ProgressViewModel;

import java.util.Calendar;
import java.util.Random;

public class SettingsPage extends BaseFragment {

    private Button logoutButton;
    private Switch soundSwitch;
    private Switch notificationSwitch;
    private TextView languageValue;
    private CardView languageCard;
    private CardView contactUsCard;
    private CardView rateAppCard;
    private ProgressViewModel progressViewModel;

    private static final String PREFS_NAME = "AppSettings";
    private static final String SOUND_PREF_KEY = "sound_enabled";
    private static final String NOTIFICATION_PREF_KEY = "notifications_enabled";
    private static final String LANGUAGE_PREF_KEY = "app_language";
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_FILIPINO = "fil";

    private static final String CHANNEL_ID = "gabay_daily_notifications";
    private static final int NOTIFICATION_ID = 1;

    private String[] getDailyNotifications() {
        return new String[]{
                // Motivational & Engagement
                "📚 Daily FSL Practice! Learn a new sign today and expand your communication skills!",
                "🌟 Consistency is key! Spend 5 minutes practicing Filipino Sign Language now.",
                "💫 Your FSL journey continues! Open Gabay to learn something new today.",

                // Educational & Practical
                "👋 Did you know? Practice the Filipino Sign Language alphabet today!",
                "🗣️ Learn how to sign basic Filipino greetings in today's session!",
                "🔄 Quick FSL Tip: Master 3 new signs every day and see your progress soar!",

                // Cultural & Community Focused
                "🤟 Connect with the Filipino Deaf community through sign language! Practice now.",
                "🇵🇭 Embrace Filipino culture through sign language. Learn a new phrase today!",
                "❤️ Communication bridges gaps. Practice FSL to connect with more people!",

                // Progress & Achievement
                "📈 Your FSL skills are growing! Continue your daily learning streak today!",
                "🎯 Daily Challenge: Can you sign 'Kumusta' perfectly? Let's practice!",
                "🏆 Consistency builds fluency! Don't break your learning streak - practice now!",

                // Practical Usage
                "🛍️ Learn FSL signs for shopping and dining - useful for everyday conversations!",

                // Interactive & Engaging
                "🔍 Quiz Time! Test your FSL knowledge with today's quick sign recognition!",
                "🎭 Learn emotional expressions in FSL - convey feelings without words!",
                "📝 New Lesson Available: Emergency signs that could be life-saving!",

                // Weekend Specials
                "🎉 Weekend Learning! Explore fun FSL phrases for social situations!",
                "📖 Story Time: Learn how to sign a short Filipino story in FSL!",
                "👥 Social Signs Saturday: Master introductions and conversations in FSL!"
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_page, container, false);
    }

    @Override
    protected void initializeViews() {
        // Initialize ProgressViewModel
        progressViewModel = getActivityViewModel(ProgressViewModel.class);

        // Initialize views
        if (rootView != null) {
            logoutButton = rootView.findViewById(R.id.logout_button);
            soundSwitch = rootView.findViewById(R.id.switch_soundeffects);
            notificationSwitch = rootView.findViewById(R.id.switch_notif);
            languageValue = rootView.findViewById(R.id.language_value);
            languageCard = rootView.findViewById(R.id.language_setting_card);
            contactUsCard = rootView.findViewById(R.id.ContactUs_setting);
            rateAppCard = rootView.findViewById(R.id.rateApp_setting);

            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Initialize sound switch
            boolean soundEnabled = prefs.getBoolean(SOUND_PREF_KEY, true);
            soundSwitch.setChecked(soundEnabled);
            soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(SOUND_PREF_KEY, isChecked);
                editor.apply();
            });

            // Initialize notification switch - UPDATED
            boolean notificationsEnabled = prefs.getBoolean(NOTIFICATION_PREF_KEY, true);
            notificationSwitch.setChecked(notificationsEnabled);
            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(NOTIFICATION_PREF_KEY, isChecked);
                editor.apply();

                if (isChecked) {
                    createNotificationChannel();
                    //scheduleDailyNotification(); // Start scheduling when enabled
                    showTestNotification(); // Optional: show test notification
                } else {
                    //cancelDailyNotification(); // Cancel when disabled
                }
            });

            // Initialize language
            String currentLanguage = prefs.getString(LANGUAGE_PREF_KEY, LANGUAGE_ENGLISH);
            updateLanguageDisplay(currentLanguage);
            languageCard.setOnClickListener(v -> showLanguageSelectionDialog());

            // Initialize contact us
            contactUsCard.setOnClickListener(v -> openContactUs());

            // Initialize rate app
            rateAppCard.setOnClickListener(v -> openRateApp());

            // Initialize logout button
            if (logoutButton != null) {
                logoutButton.setOnClickListener(v -> logout());
            }

            // Create notification channel on initialization
            createNotificationChannel();

            // Schedule notifications if they're enabled
            if (notificationsEnabled) {
                //scheduleDailyNotification();
            }
        }
    }

    // ADD THESE NOTIFICATION METHODS:

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gabay Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for daily reminders and updates");

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showDailyNotification() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(NOTIFICATION_PREF_KEY, true);

        if (!notificationsEnabled) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Get random daily message
        String[] notifications = getDailyNotifications();
        Random random = new Random();
        String contentText = notifications[random.nextInt(notifications.length)];

        // Create pending intent to open app
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Gabay - Filipino Sign Language")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Add action button for quick learning
        Intent learnIntent = new Intent(requireContext(), LessonActivity.class);
        PendingIntent learnPendingIntent = PendingIntent.getActivity(requireContext(), 1, learnIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.ic_multicolor_books, "Learn Now", learnPendingIntent);

        if (notificationManager != null) {
            // Use different ID each day to allow multiple notifications
            int dailyId = (int) System.currentTimeMillis();
            notificationManager.notify(dailyId, builder.build());
        }
    }

    @SuppressLint({"ScheduleExactAlarm", "ObsoleteSdkInt"})
    private void scheduleDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(requireContext(), NotificationReceiver.class);
        notificationIntent.setAction("gabay.app.DAILY_NOTIFICATION");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set notification time (e.g., 9:00 AM daily)
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // If it's already past 9 AM, schedule for next day
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            // Use setExactAndAllowWhileIdle for better reliability on Android 6.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);
            }
            Log.d("SettingsPage", "Daily notification scheduled for: " + calendar.getTime());
        }
    }

    private void cancelDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent notificationIntent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("SettingsPage", "Daily notifications cancelled");
        }
    }

    private void showTestNotification() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(NOTIFICATION_PREF_KEY, true);

        if (!notificationsEnabled) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Gabay")
                .setContentText("Daily FSL reminders are now enabled!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(999, builder.build()); // Use fixed ID for test
        }
    }

    // REST OF YOUR EXISTING METHODS (keep all your existing methods below)

    private void updateLanguageDisplay(String language) {
        if (languageValue != null) {
            if (LANGUAGE_FILIPINO.equals(language)) {
                languageValue.setText("Filipino");
            } else {
                languageValue.setText("English");
            }
        }
    }

    private void showLanguageSelectionDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentLanguage = prefs.getString(LANGUAGE_PREF_KEY, LANGUAGE_ENGLISH);

        String[] languages = {"English", "Filipino"};
        int selectedIndex = LANGUAGE_FILIPINO.equals(currentLanguage) ? 1 : 0;

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.BlackTextDialog)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, selectedIndex, (dialogInterface, which) -> {
                    String selectedLanguage = (which == 0) ? LANGUAGE_ENGLISH : LANGUAGE_FILIPINO;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(LANGUAGE_PREF_KEY, selectedLanguage);
                    editor.apply();

                    updateLanguageDisplay(selectedLanguage);

                    // Show message (in selected language)
                    String message = (which == 0)
                            ? "Language changed to English"
                            : "Nabago ang wika sa Filipino";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

                    dialogInterface.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Optionally set button colors if needed
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor));
        }
    }

    private void openContactUs() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"gabayfsl@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Gabay App - Contact Us");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello Gabay Team,\n\n");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No email app found. Please contact us at gabayfsl@gmail.com", Toast.LENGTH_LONG).show();
        }
    }

    private void openRateApp() {
        try {
            // Try to open Play Store
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + requireContext().getPackageName()));
            startActivity(rateIntent);
        } catch (android.content.ActivityNotFoundException e) {
            // If Play Store app is not available, open in browser
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().getPackageName()));
            startActivity(rateIntent);
        }
    }

    private void logout() {
        if (getActivity() != null) {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getActivity(), R.style.BlackTextDialog)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialogInterface, which) -> {
                        performLogout();
                    })
                    .setNegativeButton("No", null)
                    .create();

            dialog.show();

            // Set button colors after showing the dialog
            Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(getResources().getColor(R.color.secondaryColor));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor));
            }
        }
    }

    private void performLogout() {
        // Cancel notifications on logout
       // cancelDailyNotification();

        // 1. Clear Supabase authentication state
        SupabaseJavaService.clearAuth();

        // 2. Clear all progress data
        if (progressViewModel != null) {
            progressViewModel.resetAllProgress();
        }

        // 3. Clear SharedPreferences (credentials and tokens)
        clearAuthenticationData();

        // 4. Sign out from Google (if using Google Sign-In)
        signOutFromGoogle();

        // 5. Show confirmation message
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // 6. Navigate back to AuthActivity
        navigateToAuthActivity();
    }

    private void clearAuthenticationData() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    private void signOutFromGoogle() {
        try {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).signOutFromGoogle();
            }
        } catch (Exception e) {
            Log.e("SettingsPage", "Error signing out from Google: " + e.getMessage());
        }
    }

    private void navigateToAuthActivity() {
        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    @Override
    protected void cleanupResources() {
        logoutButton = null;
        soundSwitch = null;
        notificationSwitch = null;
        languageValue = null;
        languageCard = null;
        contactUsCard = null;
        rateAppCard = null;
        progressViewModel = null;
    }
}