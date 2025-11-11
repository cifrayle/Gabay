package com.example.gabay.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                intent.getAction().equals("gabay.app.DAILY_NOTIFICATION")) {

            // Simply start your MainActivity which can handle showing the notification
            Intent mainIntent = new Intent(context, com.example.gabay.activities.MainActivity.class);
            mainIntent.putExtra("SHOW_DAILY_NOTIFICATION", true);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainIntent);
        }
    }
}