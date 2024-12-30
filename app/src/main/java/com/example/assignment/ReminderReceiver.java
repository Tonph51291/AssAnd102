package com.example.assignment;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the user has updated the "biết ơn" list
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            DocumentReference userDoc = db.collection("users").document(user.getUid());
            userDoc.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Date lastUpdate = task.getResult().getDate("lastBietOnUpdate");
                    Date now = new Date();

                    if (lastUpdate == null || !isSameDay(lastUpdate, now)) {
                        showNotification(context);
                    }
                } else {
                    Log.e(TAG, "Error getting document: ", task.getException());
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void showNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotifiConfig.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder")
                .setContentText("Bạn chưa nhập thông tin biết ơn hôm nay. Hãy cập nhật ngay!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }

    private boolean isSameDay(Date date1, Date date2) {
        return (date1.getYear() == date2.getYear() &&
                date1.getMonth() == date2.getMonth() &&
                date1.getDate() == date2.getDate());
    }
}
