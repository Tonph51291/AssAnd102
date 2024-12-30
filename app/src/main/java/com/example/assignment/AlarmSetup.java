package com.example.assignment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class AlarmSetup {
    public static void setDailyReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long triggerAtMillis = SystemClock.elapsedRealtime() + calculateTimeUntil9AM();
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private static long calculateTimeUntil9AM() {
        // Calculate the time in milliseconds until the next 9:00 AM
        long now = System.currentTimeMillis();
        long millisInDay = (8 * 60 * 60 * 1000) + (58 * 60 * 1000);
        long targetTime = getToday9AM();
        if (now > targetTime) {
            targetTime += millisInDay; // Schedule for the next day
        }
        return targetTime - now;
    }

    private static long getToday9AM() {
        // Get 9:00 AM of today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 9);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
