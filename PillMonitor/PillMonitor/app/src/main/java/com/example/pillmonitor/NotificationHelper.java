package com.example.pillmonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class NotificationHelper {

    private static final String CHANNEL_URGENT = "channel_urgent";
    private static final String CHANNEL_NORMAL = "channel_normal";

    // Call this ONCE in MainActivity onCreate()
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // 1. THE URGENT CHANNEL (High pitch, loud, pops up on screen)
            NotificationChannel urgentChannel = new NotificationChannel(
                    CHANNEL_URGENT, "Urgent Medical Alerts", NotificationManager.IMPORTANCE_HIGH);

            Uri urgentSound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.urgent_beep);
            AudioAttributes urgentAudio = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            urgentChannel.setSound(urgentSound, urgentAudio);
            urgentChannel.enableVibration(true);

            // 2. THE NORMAL CHANNEL (Soft chime, gentle reminder)
            NotificationChannel normalChannel = new NotificationChannel(
                    CHANNEL_NORMAL, "Standard Updates", NotificationManager.IMPORTANCE_DEFAULT);

            Uri normalSound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.soft_chime);
            AudioAttributes normalAudio = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            normalChannel.setSound(normalSound, normalAudio);

            if (manager != null) {
                manager.createNotificationChannel(urgentChannel);
                manager.createNotificationChannel(normalChannel);
            }
        }
    }

    // Show Method (Now takes 'isUrgent' true/false)
    public static void show(Context c, String title, String text, int id, boolean isUrgent) {

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(c, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; 
            }
        }

        String channelId = isUrgent ? CHANNEL_URGENT : CHANNEL_NORMAL;

        // Opens the app when the notification is tapped
        Intent intent = new Intent(c, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, channelId)
                .setSmallIcon(R.mipmap.ic_launcher) 
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (isUrgent) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        NotificationManagerCompat.from(c).notify(id, builder.build());
    }

    // If a part of your app calls show() without true/false, it defaults to normal.
    public static void show(Context c, String title, String text, int id) {
        show(c, title, text, id, false);
    }

    // Doctor Reminder 
    public static void showDoctorReminder(Context c, String note, String date) {
        String msg = "Doctor appointment: " + note + " on " + date;
        // Doctor reminders use the normal channel 
        show(c, "Doctor Appointment", msg, 8001, false);
    }
}
