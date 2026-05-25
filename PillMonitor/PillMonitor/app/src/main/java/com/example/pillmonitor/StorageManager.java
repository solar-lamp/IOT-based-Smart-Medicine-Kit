package com.example.pillmonitor;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

public class StorageManager {

    private static final String PREF = "pill_monitor";
    private static final String KEY_DATA = "data";
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_DOCTOR = "doctor_note";

    public static void save(Context c, HashMap<String, ArrayList<Medicine>> data) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_DATA, new Gson().toJson(data)).apply();
    }

    public static HashMap<String, ArrayList<Medicine>> load(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_DATA, null);
        if (json == null) return new HashMap<>();
        Type t = new TypeToken<HashMap<String, ArrayList<Medicine>>>() {}.getType();
        return new Gson().fromJson(json, t);
    }

    // Profile name
    public static void saveProfileName(Context c, String name) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NAME, name)
                .apply();
    }

    public static String getProfileName(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_NAME, "");
    }

    // Doctor appointment
    public static void saveDoctorNote(Context c, String note) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DOCTOR, note)
                .apply();
    }

    public static String getDoctorNote(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_DOCTOR, "");
    }

    // Time save/load (THIS FIXES YOUR ERROR)
    public static void saveTime(Context c, String key, String time) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(key, time)
                .apply();
    }

    public static String getTime(Context c, String key) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(key, "--:--");
    }

    public static void saveDoctorDate(Context c, String date) {
        c.getSharedPreferences("pill", 0).edit().putString("doc_date", date).apply();
    }
    public static String getDoctorDate(Context c) {
        return c.getSharedPreferences("pill", 0).getString("doc_date", "No date selected");
    }
    // ===== Doctor Appointments (Multiple with Date + Time) =====

    private static final String KEY_APPTS = "doctor_appts"; // JSON array

    public static void addDoctorAppointment(Context c, String note, String date, String time) {
        ArrayList<DoctorAppt> list = getAllDoctorAppointments(c);
        list.add(new DoctorAppt(note, date, time));
        saveAllDoctorAppointments(c, list);
    }

    public static ArrayList<DoctorAppt> getAllDoctorAppointments(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_APPTS, null);
        if (json == null) return new ArrayList<>();

        try {
            java.lang.reflect.Type t = new com.google.gson.reflect.TypeToken<ArrayList<DoctorAppt>>(){}.getType();
            return new com.google.gson.Gson().fromJson(json, t);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void deleteDoctorAppointment(Context c, int index) {
        ArrayList<DoctorAppt> list = getAllDoctorAppointments(c);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            saveAllDoctorAppointments(c, list);
        }
    }

    public static void updateDoctorAppointment(Context c, int index, String note, String date, String time) {
        ArrayList<DoctorAppt> list = getAllDoctorAppointments(c);
        if (index >= 0 && index < list.size()) {
            list.get(index).note = note;
            list.get(index).date = date;
            list.get(index).time = time;
            saveAllDoctorAppointments(c, list);
        }
    }

    private static void saveAllDoctorAppointments(Context c, ArrayList<DoctorAppt> list) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = new com.google.gson.Gson().toJson(list);
        sp.edit().putString(KEY_APPTS, json).apply();
    }

}
