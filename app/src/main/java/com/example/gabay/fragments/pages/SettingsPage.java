package com.example.gabay.fragments.pages;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.os.LocaleListCompat;

import com.example.gabay.R;
import com.example.gabay.activities.AuthActivity;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.fragments.BaseFragment;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.services.UserAnalyticsService;
import com.example.gabay.utils.NotificationReceiver;
import com.example.gabay.utils.RatingDialog;
import com.example.gabay.utils.SessionTracker;
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

            // Initialize notification switch - FIXED
            boolean notificationsEnabled = prefs.getBoolean(NOTIFICATION_PREF_KEY, true);
            notificationSwitch.setChecked(notificationsEnabled);
            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(NOTIFICATION_PREF_KEY, isChecked);
                editor.apply();

                if (isChecked) {
                    createNotificationChannel();
                    // Schedule on UI thread to avoid context issues
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        scheduleDailyNotification();
                        showTestNotification(); // Optional: show confirmation
                    }, 100);
                } else {
                    cancelDailyNotification();
                }
            });

            // Schedule initial notifications with safety check
            if (notificationsEnabled) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded() && !isDetached()) {
                        scheduleDailyNotification();
                    }
                }, 500);
            }

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

    @SuppressLint({"ScheduleExactAlarm", "ObsoleteSdkInt"})
    private void scheduleDailyNotification() {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e("SettingsPage", "AlarmManager is null");
                return;
            }

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

            // Use setExactAndAllowWhileIdle for better reliability on Android 6.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);
            }
            Log.d("SettingsPage", "Daily notification scheduled for: " + calendar.getTime());

        } catch (SecurityException e) {
            Log.e("SettingsPage", "Schedule exact alarm permission required", e);

            // ========== PUT THE FALLBACK CODE RIGHT HERE ==========
            // Handle Android 12+ SCHEDULE_EXACT_ALARM permission requirement
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                    Intent notificationIntent = new Intent(requireContext(), NotificationReceiver.class);
                    notificationIntent.setAction("gabay.app.DAILY_NOTIFICATION");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    // FIX: Create calendar instance for fallback
                    Calendar fallbackCalendar = Calendar.getInstance();
                    fallbackCalendar.setTimeInMillis(System.currentTimeMillis());
                    fallbackCalendar.set(Calendar.HOUR_OF_DAY, 9);
                    fallbackCalendar.set(Calendar.MINUTE, 0);
                    fallbackCalendar.set(Calendar.SECOND, 0);
                    if (fallbackCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                        fallbackCalendar.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    // Fallback: use setAlarmClock which doesn't require exact alarm permission
                    alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(fallbackCalendar.getTimeInMillis(), pendingIntent), pendingIntent);
                    Log.d("SettingsPage", "Used setAlarmClock fallback");
                } catch (Exception ex) {
                    Log.e("SettingsPage", "Fallback scheduling also failed", ex);
                }
            }
        } catch (Exception e) {
            Log.e("SettingsPage", "Failed to schedule notification", e);
        }
    }

    private void cancelDailyNotification() {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent notificationIntent = new Intent(requireContext(), NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("SettingsPage", "Daily notifications cancelled");
            }
        } catch (Exception e) {
            Log.e("SettingsPage", "Failed to cancel notification", e);
        }
    }

    private void showTestNotification() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean(NOTIFICATION_PREF_KEY, true);

            if (!notificationsEnabled) {
                return;
            }

            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                Log.e("SettingsPage", "NotificationManager is null");
                return;
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentTitle("Gabay")
                    .setContentText("Daily FSL reminders are now enabled!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            notificationManager.notify(999, builder.build()); // Use fixed ID for test
            Log.d("SettingsPage", "Test notification shown");
        } catch (Exception e) {
            Log.e("SettingsPage", "Failed to show test notification", e);
        }
    }

    // REST OF YOUR EXISTING METHODS (keep all your existing methods below)

    // Use string resources instead of hardcoded text
    private void updateLanguageDisplay(String language) {
        if (languageValue != null) {
            if (LANGUAGE_FILIPINO.equals(language)) {
                languageValue.setText(R.string.language_filipino);
            } else {
                languageValue.setText(R.string.language_english);
            }
        }
    }

    private void showLanguageSelectionDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentLanguage = prefs.getString(LANGUAGE_PREF_KEY, LANGUAGE_ENGLISH);

        // Use string resources for the dialog items
        final String[] languages = {
                getString(R.string.language_english),
                getString(R.string.language_filipino)
        };
        final String[] languageCodes = {LANGUAGE_ENGLISH, LANGUAGE_FILIPINO};

        int selectedIndex = LANGUAGE_FILIPINO.equals(currentLanguage) ? 1 : 0;

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.BlackTextDialog)
                .setTitle(R.string.select_language) // Use string resource
                .setSingleChoiceItems(languages, selectedIndex, (dialogInterface, which) -> {
                    String selectedLanguageCode = languageCodes[which];

                    // Only proceed if the language was actually changed
                    if (!currentLanguage.equals(selectedLanguageCode)) {
                        // 1. Save the new preference
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(LANGUAGE_PREF_KEY, selectedLanguageCode);
                        editor.apply();

                        // 2. Set the application locale
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLanguageCode));
                    }

                    // 3. Dismiss the dialog. The activity will be recreated by the system.
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.cancel, null) // Use string resource
                .create();

        dialog.show();

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor, null));
        }
    }

    private void logout() {
        if (getActivity() != null) {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getActivity(), R.style.BlackTextDialog)
                    .setTitle(R.string.logout_title) // Use string resource
                    .setMessage(R.string.logout_message) // Use string resource
                    .setPositiveButton(R.string.logout_yes, (dialogInterface, which) -> { // Use string resource
                        performLogout();
                    })
                    .setNegativeButton(R.string.logout_no, null) // Use string resource
                    .create();

            dialog.show();

            // Set button colors after showing the dialog
            Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
            if (positiveButton != null) {
                positiveButton.setTextColor(getResources().getColor(R.color.secondaryColor, null));
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(getResources().getColor(R.color.buttonTextColor, null));
            }
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
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(requireContext(), "No email app found. Please contact us at gabayfsl@gmail.com", Toast.LENGTH_LONG).show();
        }
    }

    private void openRateApp() {
        // Show custom rating dialog instead of directly opening Play Store
        RatingDialog ratingDialog = new RatingDialog(requireContext());
        ratingDialog.show(new RatingDialog.RatingCallback() {
            @Override
            public void onRatingSubmitted(int rating, String comment) {
                Log.d("SettingsPage", "User submitted rating: " + rating + " stars");
                // Track this rating event in analytics
                trackUserRatingEvent(rating);
            }

            @Override
            public void onRatingCancelled() {
                Log.d("SettingsPage", "User cancelled rating dialog");
            }
        });
    }

    /**
     * Track rating event for analytics (optional)
     */
    private void trackUserRatingEvent(int rating) {
        // This could be used for internal analytics
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("last_app_rating", rating);
        editor.putLong("last_rating_time", System.currentTimeMillis());
        editor.apply();
        
        Log.d("SettingsPage", "Rating event tracked locally: " + rating + " stars");
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

        // 4. Reset session tracking
        SessionTracker.getInstance(requireContext()).resetSession();

        // 5. Sign out from Google (if using Google Sign-In)
        signOutFromGoogle();

        // 6. Show confirmation message
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // 7. Navigate back to AuthActivity
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