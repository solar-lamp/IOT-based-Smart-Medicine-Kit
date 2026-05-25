package com.example.pillmonitor;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.Context;

import android.os.Handler;

import java.net.HttpURLConnection;
import java.net.URL;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//IMPORTS FOR NSD
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

    HashMap<String, ArrayList<Medicine>> data;

    // --- NSD VARIABLES ---
    private NsdManager nsdManager;
    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String TAG = "MediSync mDNS";
    private boolean isDiscoveryStarted = false;
    private Handler statusHandler = new Handler();
    private long lastSeenTime = 0;
    private Runnable statusRunnable;
    private TextView txtEspStatus;
    private TextView txtWifi;
    private TextView txtRTC;
    private TextView txtAdherence;
    private int espFailCount = 0;
    private int totalScheduledDoses = 0;
    private int totalTakenDoses = 0;
    private int networkStrikes = 0;



    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        // This forces the screen to stay awake so Android never throttles your 2-second loop!
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        NotificationHelper.createChannels(this);

        txtEspStatus = findViewById(R.id.txtEspStatus);
        txtWifi = findViewById(R.id.txtWifi);
        txtRTC = findViewById(R.id.txtRTC);
        txtAdherence = findViewById(R.id.txtAdherence);

        // Force the app to assume it is disconnected the second it opens
        txtEspStatus.setText("🔴 Offline");
        txtEspStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        txtRTC.setText("⏳ Waiting...");
        txtRTC.setTextColor(getResources().getColor(android.R.color.darker_gray));

        // START THE WATCHDOG ENGINE
        startHeartbeat();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // HOME
            if (id == R.id.nav_home) {
                return true;
            }


            // MEDICINE
            else if (id == R.id.nav_medicine) {

                startActivity(new Intent(
                        MainActivity.this,
                        MedicineActivity.class
                ));

                overridePendingTransition(0, 0);

                return true;
            }

            // ACTIVITY
            else if (id == R.id.nav_activity) {

                startActivity(new Intent(
                        MainActivity.this,
                        ActivityLogActivity.class
                ));

                overridePendingTransition(0, 0);

                return true;
            }

            // APPOINTMENT
            else if (id == R.id.nav_appointment) {

                startActivity(new Intent(
                        MainActivity.this,
                        AppointmentActivity.class
                ));

                overridePendingTransition(0, 0);

                return true;
            }

            // SETTINGS
            else if (id == R.id.nav_settings) {

                startActivity(new Intent(
                        MainActivity.this,
                        SettingsActivity.class
                ));

                overridePendingTransition(0, 0);

                return true;
            }

            return false;
        });
        requestNotificationPermissionIfNeeded();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                android.content.Intent intent =
                        new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        data = StorageManager.load(this);
        if (data == null) data = new HashMap<>();
        if (!data.containsKey("morning")) data.put("morning", new ArrayList<>());
        if (!data.containsKey("noon")) data.put("noon", new ArrayList<>());
        if (!data.containsKey("night")) data.put("night", new ArrayList<>());

        setupSection(findViewById(R.id.morning), "morning", 1, "time_morning");
        setupSection(findViewById(R.id.noon), "noon", 2, "time_noon");
        setupSection(findViewById(R.id.night), "night", 3, "time_night");



        // --- START NSD DISCOVERY ---
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        discoverMediSync();

        // Start polling (It will only actually ping when the IP is found)
        startHeartbeat();
        startStatusMonitoring();
    }
    private void startStatusMonitoring() {

        statusHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                updateWifiStatus();


                statusHandler.postDelayed(this, 5000);
            }

        }, 1000);
    }
    private void updateWifiStatus() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Network network = cm.getActiveNetwork();

        if (network != null) {

            NetworkCapabilities capabilities =
                    cm.getNetworkCapabilities(network);

            if (capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {

                txtWifi.setText("📶 Stable");
                txtWifi.setTextColor(getResources().getColor(R.color.green_primary));

            } else {

                txtWifi.setText("❌ Offline");
                txtWifi.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }

        } else {

            txtWifi.setText("❌ Offline");
            txtWifi.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }

    // --- NSD METHODS ---
    private void discoverMediSync() {
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            isDiscoveryStarted = true;
        } catch (Exception e) {
            Log.e(TAG, "Discovery failed to start: " + e.getMessage());
        }
    }

    NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "Service found: " + service.getServiceName());
            if (service.getServiceName().toLowerCase().contains("medisync")) {
                nsdManager.resolveService(service, resolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.e(TAG, "Service lost: " + service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            isDiscoveryStarted = false;
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Start discovery failed: " + errorCode);
            nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Stop discovery failed: " + errorCode);
            nsdManager.stopServiceDiscovery(this);
        }
    };

    NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            String espIpAddress = serviceInfo.getHost().getHostAddress();
            Log.d(TAG, "RESOLVED! ESP8266 IP Address is: " + espIpAddress);

            // Pass the dynamically found IP to your EspClient
            String newBaseUrl = "http://" + espIpAddress;
            EspClient.setBaseUrl(newBaseUrl);

            // Show a quick toast on the main thread so you know it connected
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Kit Connected!", Toast.LENGTH_SHORT).show());
        }
    };

    @Override
    protected void onDestroy() {
        if (nsdManager != null && isDiscoveryStarted) {
            try { nsdManager.stopServiceDiscovery(discoveryListener); }
            catch (Exception e) { e.printStackTrace(); }
        }
        super.onDestroy();
    }
    // --- END NSD METHODS ---

    private void setupSection(View section, String key, int alarmId, String timeKey) {
        TextView title = section.findViewById(R.id.txtTitle);
        TextView timeText = section.findViewById(R.id.txtTime);
        Button setTime = section.findViewById(R.id.btnSetTime);
        LinearLayout list = section.findViewById(R.id.list);
        Button add = section.findViewById(R.id.btnAdd);

        title.setText(key.toUpperCase());
        timeText.setText(StorageManager.getTime(this, timeKey));

        setTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (tp, h, m) -> {
                String t = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                StorageManager.saveTime(this, timeKey, t);
                timeText.setText(t);
                AlarmScheduler.set(this, h, m, alarmId);
                EspClient.setTime(
                        key.equals("morning") ? 0 : key.equals("noon") ? 1 : 2,
                        h,
                        m
                );

                Toast.makeText(this, "Time set: " + t, Toast.LENGTH_SHORT).show();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        add.setOnClickListener(v -> showAddDialog(key, list));

        render(list, data.get(key), key);
    }

    private void showAddDialog(String key, LinearLayout list) {
        View d = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null);

        new AlertDialog.Builder(this)
                .setTitle("Add Medicine")
                .setView(d)
                .setPositiveButton("Add", (di, w) -> {
                    try {
                        String name = ((EditText) d.findViewById(R.id.inputName)).getText().toString().trim();
                        String countStr = ((EditText) d.findViewById(R.id.inputCount)).getText().toString().trim();

                        if (name.isEmpty() || countStr.isEmpty()) {
                            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int count = Integer.parseInt(countStr);
                        data.get(key).add(new Medicine(name, count));
                        StorageManager.save(this, data);
                        render(list, data.get(key), key);

                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001
                );
            }
        }
    }

    private void render(LinearLayout list, ArrayList<Medicine> meds, String key) {
        list.removeAllViews();

        for (Medicine m : meds) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_medicine, list, false);

            TextView name = v.findViewById(R.id.txtName);
            TextView count = v.findViewById(R.id.txtCount);
            ProgressBar prog = v.findViewById(R.id.progress);
            ImageView menu = v.findViewById(R.id.btnMenu);

            name.setText(m.name);
            count.setText("Pills left: " + m.count);
            prog.setMax(10);
            prog.setProgress(Math.max(0, m.count));

            menu.setOnClickListener(btn -> {
                PopupMenu pm = new PopupMenu(this, btn);
                pm.getMenuInflater().inflate(R.menu.menu_medicine, pm.getMenu());
                pm.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete) {
                        meds.remove(m);
                        StorageManager.save(this, data);
                        render(list, meds, key);
                        return true;
                    }

                    if (item.getItemId() == R.id.action_edit) {
                        showEditDialog(key, m, list);
                        return true;
                    }

                    return false;
                });

                pm.show();
            });

            list.addView(v);
        }
    }

    private void showEditDialog(String key, Medicine m, LinearLayout list) {
        View d = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null);

        EditText nameInput = d.findViewById(R.id.inputName);
        EditText countInput = d.findViewById(R.id.inputCount);

        nameInput.setText(m.name);
        countInput.setText(String.valueOf(m.count));

        new AlertDialog.Builder(this)
                .setTitle("Edit Medicine")
                .setView(d)
                .setPositiveButton("Save", (di, w) -> {
                    try {
                        String newName = nameInput.getText().toString().trim();
                        String newCountStr = countInput.getText().toString().trim();

                        if (newName.isEmpty() || newCountStr.isEmpty()) {
                            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int newCount = Integer.parseInt(newCountStr);
                        m.name = newName;
                        m.count = newCount;

                        StorageManager.save(this, data);
                        render(list, data.get(key), key);

                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private android.os.Handler espHandler = new android.os.Handler();
    private boolean lastDoseFlag = false;

    private void startHeartbeat() {
        // Set initial time so it doesn't panic immediately
        lastSeenTime = System.currentTimeMillis();

        statusRunnable = new Runnable() {
            @Override
            public void run() {
                // THE WATCHDOG 
                // If the ESP is completely silent for 8 seconds, force offline.
                if (System.currentTimeMillis() - lastSeenTime > 8000) {
                    txtEspStatus.setText("🔴 Offline");
                    txtEspStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    txtRTC.setText("❌ RTC Error");
                    txtRTC.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }

                // If no connection yet, retry fast
                if (!EspClient.hasBaseUrl()) {
                    statusHandler.postDelayed(this, 2000);
                    return;
                }

                updateAdherence();

                //  THE FAST POLL (Instant Notifications)
                EspClient.getStatus(res -> {
                    // Only act if we get a perfect, clean response
                    if (res != null && !res.equals("ERR") && !res.isEmpty()) {
                        // SUCCESS: Feed the Watchdog so it doesn't trigger!
                        lastSeenTime = System.currentTimeMillis();

                        txtEspStatus.setText("🟢 Online");
                        txtEspStatus.setTextColor(getResources().getColor(R.color.green_primary));
                        txtRTC.setText("⏰ Synced");
                        txtRTC.setTextColor(getResources().getColor(R.color.green_primary));

                        // --- INSTANT DOSE LOGIC ---
                        if ("1".equals(res) && !lastDoseFlag) {
                            totalScheduledDoses++;
                            lastDoseFlag = true;

                            EspClient.getDose(doseStr -> {
                                int dose;
                                try { dose = Integer.parseInt(doseStr.trim()); }
                                catch (Exception e) { return; }

                                handleDoseTaken(dose);
                                EspClient.clear(); // Clear the ESP memory instantly
                            });
                        }
                        if ("0".equals(res)) lastDoseFlag = false;

                    }
                    // If a packet drops,The Watchdog will catch real disconnects.
                });

                // --- INSTANT MISSED DOSE LOGIC ---
                EspClient.getMissed(res -> {
                    if ("1".equals(res)) {
                        // Notice the 'true' at the end! This means URGENT!
                        NotificationHelper.show(MainActivity.this, "Dose missed", "You missed your scheduled medicine", 7004, true);
                    }
                });


                // Loop every 2 seconds for that instant, real-time feel!
                statusHandler.postDelayed(this, 2000);
            }
        };

        statusHandler.post(statusRunnable);
    }

    private LinearLayout getListForSlot(String slot) {
        View section = slot.equals("morning") ? findViewById(R.id.morning)
                : slot.equals("noon") ? findViewById(R.id.noon)
                : findViewById(R.id.night);

        if (section == null) return null;

        return section.findViewById(R.id.list);
    }

    private void handleDoseTaken(int dose) {
        String slot = dose == 1 ? "morning" : dose == 2 ? "noon" : "night";

        ArrayList<Medicine> meds = data.get(slot);
        if (meds == null || meds.isEmpty()) return;

        for (Medicine m : meds) {
            if (m.count > 0) m.count--;
        }

        StorageManager.save(this, data);

        LinearLayout list = getListForSlot(slot);
        if (list != null) {
            render(list, meds, slot);
        }
        totalTakenDoses++;

        updateAdherence();

        NotificationHelper.show(this, "Dose taken", "Pill count updated from kit", 7001);

        ActivityLogActivity.logEvent(this, "✅ Dose taken", "Pill count updated from kit");

        boolean low = false;
        for (Medicine m : meds) {
            if (m.count < 5) low = true;

            ActivityLogActivity.logEvent(this, "💊 " + m.name, m.count + " pills remaining");
        }

        if (low) {
            NotificationHelper.show(this, "Refill medicine", "One of your medicines is running low", 7003, true);

            ActivityLogActivity.logEvent(this, "⚠️ Refill Required", "A medicine is running low");
        }
    }
    private void updateAdherence() {
        android.content.SharedPreferences prefs = getSharedPreferences("PillMonitorLogs", MODE_PRIVATE);
        String history = prefs.getString("history", "");

        int taken = 0;
        int missed = 0;
        String[] logs = history.split("\\|\\|");
        for (String log : logs) {
            if (log.contains("✅")) taken++;
            else if (log.contains("❌")) missed++;
        }

        int total = taken + missed;
        int adherence = 0;
        if (total > 0) {
            adherence = (taken * 100) / total;
        }

        txtAdherence.setText("💊 " + adherence + "%");

        if (adherence >= 90) {
            txtAdherence.setTextColor(getResources().getColor(R.color.green_primary));
        } else if (adherence >= 70) {
            txtAdherence.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            txtAdherence.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        }
    }
    private String getCurrentSlot() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (h < 12) return "morning";
        if (h < 18) return "noon";
        return "night";
    }
}
