package com.example.pillmonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class AlarmScheduler {

    public static void set(Context context, int hour, int minute, int alarmId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("alarmId", alarmId);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);

        // If the time has already passed today, set it for tomorrow
        if (c.getTimeInMillis() < System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        try {
            // The VIP Pass for Pill Reminders
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        } catch (SecurityException e) {
            e.printStackTrace(); // Fails safely if Android blocks the alarm
        }
    }

    public static void setDoctorReminder(Context c, String date, String time, String note) {
        try {
            String[] d = date.split("/");
            String[] t = time.split(":");

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(d[1]) - 1); // Months are 0-indexed!
            cal.set(Calendar.YEAR, Integer.parseInt(d[2]));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(t[1]));
            cal.set(Calendar.SECOND, 0);

            Intent i = new Intent(c, AlarmReceiver.class);
            // We pass this text to the receiver so it knows it's a Doctor Appointment!
            i.putExtra("title", "Doctor Appointment");
            i.putExtra("text", note + " at " + time);

            PendingIntent pi = PendingIntent.getBroadcast(
                    c, 3001, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

            try {
                // The VIP Pass for Doctor Appointments
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}