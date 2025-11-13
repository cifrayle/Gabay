package com.example.gabay.utils;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.activities.LessonActivity;
import com.example.gabay.R;
import java.util.Calendar;
import java.util.Random;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "gabay_channel";
    private static final String PREFS_NAME = "app_preferences";
    private static final String NOTIFICATION_PREF_KEY = "notifications_enabled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equals("gabay.app.DAILY_NOTIFICATION")) {

            Log.d("NotificationReceiver", "Daily notification triggered");

            // Reschedule for next day first
            scheduleNextNotification(context);

            // Then show the notification
            showDailyNotification(context);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNextNotification(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent notificationIntent = new Intent(context, NotificationReceiver.class);
            notificationIntent.setAction("gabay.app.DAILY_NOTIFICATION");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Set for same time next day (9:00 AM)
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 22);
            calendar.set(Calendar.SECOND, 0);

            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d("NotificationReceiver", "Next notification scheduled for: " + calendar.getTime());
            }
        } catch (Exception e) {
            Log.e("NotificationReceiver", "Failed to schedule next notification", e);
        }
    }

    private void showDailyNotification(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean notificationsEnabled = prefs.getBoolean(NOTIFICATION_PREF_KEY, true);

            if (!notificationsEnabled) {
                Log.d("NotificationReceiver", "Notifications disabled, skipping");
                return;
            }

            createNotificationChannel(context);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Get random daily message
            String[] notifications = getDailyNotifications();
            Random random = new Random();
            String contentText = notifications[random.nextInt(notifications.length)];

            // Create pending intent to open app
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
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
            Intent learnIntent = new Intent(context, LessonActivity.class);
            PendingIntent learnPendingIntent = PendingIntent.getActivity(context, 1, learnIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            builder.addAction(R.drawable.ic_multicolor_books, "Learn Now", learnPendingIntent);

            if (notificationManager != null) {
                int dailyId = (int) System.currentTimeMillis();
                notificationManager.notify(dailyId, builder.build());
                Log.d("NotificationReceiver", "Daily notification shown: " + contentText);
            }
        } catch (Exception e) {
            Log.e("NotificationReceiver", "Failed to show daily notification", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gabay Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for daily reminders and updates");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private String[] getDailyNotifications() {
        return new String[]{
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
}