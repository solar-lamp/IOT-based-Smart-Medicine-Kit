package com.example.pillmonitor;

import android.content.Context;
import android.os.Handler;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ActivityLogActivity extends AppCompatActivity {

    TextView txtAdherence, txtTaken, txtMissed;
    LinearLayout activityContainer;
    Handler handler = new Handler();
    boolean lastDoseState = false;


    public static void logEvent(Context context, String title, String subtitle) {
        SharedPreferences prefs = context.getSharedPreferences("PillMonitorLogs", Context.MODE_PRIVATE);
        String oldLogs = prefs.getString("history", "");
        String newLog = title + "::" + subtitle + "||" + oldLogs;
        prefs.edit().putString("history", newLog).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        txtAdherence = findViewById(R.id.txtAdherence);
        txtTaken = findViewById(R.id.txtTaken);
        txtMissed = findViewById(R.id.txtMissed);
        activityContainer = findViewById(R.id.activityContainer);

        loadActivityData();
        loadRecentActivity();
        startESPMonitoring();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_activity);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ActivityLogActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_medicine) {
                startActivity(new Intent(ActivityLogActivity.this, MedicineActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_activity) {
                return true;
            } else if (id == R.id.nav_appointment) {
                startActivity(new Intent(ActivityLogActivity.this, AppointmentActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(ActivityLogActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadActivityData() {
        // 1. Open the Diary
        SharedPreferences prefs = getSharedPreferences("PillMonitorLogs", MODE_PRIVATE);
        String history = prefs.getString("history", "");

        int taken = 0;
        int missed = 0;

        // 2. Read through the Diary 
        String[] logs = history.split("\\|\\|");
        for (String log : logs) {
            if (log.contains("✅")) {
                taken++;
            } else if (log.contains("❌")) {
                missed++;
            }
        }

        // 3. Do the math
        int total = taken + missed;
        int adherence = 0;

        if (total > 0) {
            adherence = (taken * 100) / total;
        }

        // 4. Update the big numbers on the screen
        txtTaken.setText(String.valueOf(taken));
        txtMissed.setText(String.valueOf(missed));
        txtAdherence.setText(adherence + "%");
    }

    public void loadRecentActivity() {
        activityContainer.removeAllViews();
        SharedPreferences prefs = getSharedPreferences("PillMonitorLogs", MODE_PRIVATE);
        String history = prefs.getString("history", "");

        if (history.isEmpty()) {
            addActivity("📡 Waiting for sensor data...", "Live");
            return;
        }

        // Cut the diary up into individual logs and put them on the screen
        String[] logs = history.split("\\|\\|");
        for (int i = 0; i < logs.length; i++) {
            if (i >= 15) break; // Only show the latest 15 so it doesn't get too crowded
            String[] parts = logs[i].split("::");
            if (parts.length == 2) {
                addActivity(parts[0], parts[1]);
            }
        }
    }

    private void checkDoseStatus() {
        EspClient.getStatus(new EspClient.Callback() {
            @Override
            public void onResult(String res) {
                if ("1".equals(res)) {
                    // Write the event into the diary
                    logEvent(ActivityLogActivity.this, "✅ Medicine taken", "Just now");

                    SharedPreferences prefs = getSharedPreferences("PillMonitor", MODE_PRIVATE);
                    int currentTaken = prefs.getInt("taken_count", 0);
                    prefs.edit().putInt("taken_count", currentTaken + 1).apply();

                    EspClient.clear();
                    loadActivityData();
                    loadRecentActivity(); // Refresh the screen from the diary
                }
            }
        });

        EspClient.getMissed(new EspClient.Callback() {
            @Override
            public void onResult(String res) {
                if ("1".equals(res)) {
                    // Write the event into the diary!
                    logEvent(ActivityLogActivity.this, "❌ Medicine missed", "Just now");

                    SharedPreferences prefs = getSharedPreferences("PillMonitor", MODE_PRIVATE);
                    int currentMissed = prefs.getInt("missed_count", 0);
                    prefs.edit().putInt("missed_count", currentMissed + 1).apply();

                    EspClient.clear();
                    loadActivityData();
                    loadRecentActivity(); // Refresh the screen from the diary
                }
            }
        });
    }

    private void addActivity(String title, String time) {
        View view = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_2, null);
        TextView text1 = view.findViewById(android.R.id.text1);
        TextView text2 = view.findViewById(android.R.id.text2);
        text1.setText(title);
        text2.setText(time);
        text1.setTextColor(getResources().getColor(R.color.text_primary));
        text2.setTextColor(getResources().getColor(R.color.text_secondary));
        text1.setTextSize(15);
        text2.setTextSize(12);
        activityContainer.addView(view);
    }

    private void startESPMonitoring() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!EspClient.hasBaseUrl()) {
                    handler.postDelayed(this, 2000);
                    return;
                }
                loadActivityData();
                loadRecentActivity();

                checkDoseStatus();
                handler.postDelayed(this, 3000);
            }
        }, 3000);
    }
}
