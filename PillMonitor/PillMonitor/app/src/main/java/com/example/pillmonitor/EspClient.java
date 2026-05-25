package com.example.pillmonitor;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EspClient {

    // REMOVED hardcoded IP. This is now populated dynamically via mDNS.
    private static String BASE_URL = null;

    public interface Callback {
        void onResult(String res);
    }

    // --- NEW METHODS FOR DYNAMIC IP ---
    public static void setBaseUrl(String url) {
        BASE_URL = url;
    }

    public static boolean hasBaseUrl() {
        return BASE_URL != null;
    }

    // 1️⃣ Read dose taken flag (0/1)
    public static void getStatus(Callback cb) {
        if (!hasBaseUrl()) { cb.onResult("ERR"); return; } // Safety check
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/status");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String res = br.readLine();
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(res));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult("ERR"));
            }
        }).start();
    }

    public static void clear() {
        if (!hasBaseUrl()) return;
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/clear");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.getResponseCode();
            } catch (Exception ignored) {
            }
        }).start();
    }

    // 2️⃣ Send alarm time to ESP (slot: 0 breakfast, 1 lunch, 2 dinner)
    public static void setTime(int slot, int h, int m) {
        if (!hasBaseUrl()) return;
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/settime?slot=" + slot + "&h=" + h + "&m=" + m);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.getResponseCode();
            } catch (Exception ignored) {}
        }).start();
    }

    // 3️⃣ Read which dose was active (1/2/3)
    public static void getDose(Callback cb) {
        if (!hasBaseUrl()) { cb.onResult("ERR"); return; }
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/dose");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String res = br.readLine();
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(res));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult("ERR"));
            }
        }).start();
    }

    public static void clearEsp() {
        if (!hasBaseUrl()) return;
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/clear");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.getResponseCode();
            } catch (Exception ignored) {}
        }).start();
    }

    public static void getMissed(Callback cb) {
        if (!hasBaseUrl()) { cb.onResult("ERR"); return; }
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/missed");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String res = br.readLine();
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(res));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult("ERR"));
            }
        }).start();
    }

    public static String getDoseStatus() {
        // Safety check: if radar hasn't found the IP yet, return 0
        if (!hasBaseUrl()) {
            return "0";
        }

        try {
            // Use the discovered IP instead of the .local address
            URL url = new URL(BASE_URL + "/dose");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            return reader.readLine();

        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

}