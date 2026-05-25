package com.example.pillmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {

        // 1. Try to grab the custom text (This will only exist if it's a Doctor Appointment)
        String title = i.getStringExtra("title");
        String text = i.getStringExtra("text");

        if (title != null && text != null) {
            // 🌟 DOCTOR APPOINTMENT 🌟
            // Uses the 'false' flag so it plays your gentle/normal sound
            NotificationHelper.show(c, title, text, 8001, false);

        } else {
            // 🌟 PILL REMINDER 🌟
            // Uses the 'true' flag so it plays your loud/urgent beep!
            NotificationHelper.show(c, "Pill Reminder", "Time to take your medicine", 1001, true);
        }
    }
}