package com.example.assignment.HdBuocChan;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.assignment.MainActivity;
import com.example.assignment.Model.HoatDOng;
import com.example.assignment.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomNayFragment extends Fragment implements SensorEventListener {

    private TextView txt_sobuochn, txt_khoangcachhn, txt_calohn, txt_timehn;
    private Button btn_start, btn_stop;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isRunning = false;
    private int stepCount = 0;
    private long startTime, elapsedTime;

    private Handler handler = new Handler();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private static final String CHANNEL_ID = "FPTPOLYTECHNICH";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hom_nay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        txt_sobuochn = view.findViewById(R.id.txt_sobuoc);
        txt_khoangcachhn = view.findViewById(R.id.txt_khoangcach);
        txt_calohn = view.findViewById(R.id.txt_calo);
        txt_timehn = view.findViewById(R.id.txt_time);
        btn_start = view.findViewById(R.id.btn_start);
        btn_stop = view.findViewById(R.id.btn_stop);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        createNotificationChannel();

        btn_start.setOnClickListener(v -> startCounting());
        btn_stop.setOnClickListener(v -> stopCounting());

        // Receive data from notification
        if (getArguments() != null) {
            stepCount = getArguments().getInt("stepCount", 0);
            double distance = getArguments().getDouble("distance", 0);
            int calories = getArguments().getInt("calories", 0);
            elapsedTime = getArguments().getLong("elapsedTime", 0);

            txt_sobuochn.setText(String.format(Locale.getDefault(), "%d", stepCount));
            txt_khoangcachhn.setText(String.format(Locale.getDefault(), "%.2f km", distance));
            txt_calohn.setText(String.format(Locale.getDefault(), "%d cal", calories));
            txt_timehn.setText(String.format(Locale.getDefault(), "%02d:%02d", elapsedTime / 60000, (elapsedTime % 60000) / 1000));
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "ten_channel";
            String description = "mo_ta";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void startCounting() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
            isRunning = true;
            stepCount = 0;
            startTime = System.currentTimeMillis();
            handler.post(updateRunnable);
            showNotification();
        }
    }

    private void stopCounting() {
        if (isRunning) {
            sensorManager.unregisterListener(this, stepSensor);
            isRunning = false;
            handler.removeCallbacks(updateRunnable);
            showStopDialog();
            cancelNotification();
        }
    }

    private void showNotification() {
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.setAction("SHOW_HOMNAY_FRAGMENT");
        intent.putExtra("stepCount", stepCount);
        intent.putExtra("distance", stepCount * 0.0008);
        intent.putExtra("calories", calculateCalories(stepCount));
        intent.putExtra("elapsedTime", elapsedTime);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("FPT POLYTECHNICH")
                .setContentText("Steps: " + stepCount + ", Distance: " + String.format(Locale.getDefault(), "%.2f km", stepCount * 0.0008) + ", Calories: " + calculateCalories(stepCount) + " cal")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getContext());

        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 9999);
        } else {
            notificationManagerCompat.notify(NOTIFICATION_ID, notification);
        }
    }

    private void cancelNotification() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void showStopDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Dừng Đếm Bước Chân")
                .setMessage("Bạn có muốn lưu tiến trình không?")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    saveToFirebase();
                    Toast.makeText(getContext(), "Lưu thành công!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void saveToFirebase() {
        long duration = elapsedTime / 1000;
        int calories = calculateCalories(stepCount);
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        HoatDOng hoatDOng = new HoatDOng(date, stepCount, calories, duration);
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).collection("hoatdong")
                    .add(hoatDOng)
                    .addOnSuccessListener(documentReference -> resetAll())
                    .addOnFailureListener(e -> {
                        // Xử lý khi lưu thất bại
                    });
        }
    }

    private void resetAll() {
        stepCount = 0;
        elapsedTime = 0;
        txt_sobuochn.setText(String.format(Locale.getDefault(), "%d", stepCount));
        txt_khoangcachhn.setText(String.format(Locale.getDefault(), "%.2f km", stepCount * 0.0008));
        txt_calohn.setText(String.format(Locale.getDefault(), "%d cal", calculateCalories(stepCount)));
        txt_timehn.setText(String.format(Locale.getDefault(), "%02d:%02d", elapsedTime / 60000, (elapsedTime % 60000) / 1000));
    }

    private int calculateCalories(int steps) {
        return steps / 20;
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
                int calories = calculateCalories(stepCount);

                txt_timehn.setText(String.format(Locale.getDefault(), "%02d:%02d", elapsedTime / 60000, (elapsedTime % 60000) / 1000));
                txt_sobuochn.setText(String.format(Locale.getDefault(), "%d", stepCount));
                txt_khoangcachhn.setText(String.format(Locale.getDefault(), "%.2f km", stepCount * 0.0008));
                txt_calohn.setText(String.format(Locale.getDefault(), "%d cal", calories));

                handler.postDelayed(this, 1000); // Cập nhật mỗi giây
                showNotification(); // Update the notification
            }
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRunning && event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không làm gì
    }
}
