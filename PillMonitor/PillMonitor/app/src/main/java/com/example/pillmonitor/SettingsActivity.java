package com.example.pillmonitor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // Added import
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    EditText etPatientName, etPatientInfo;
    Button btnSaveProfile;
    TextView txtAvatar;
    ImageView btnAbout; // Added ImageView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Initialize the views
        etPatientName = findViewById(R.id.etPatientName);
        etPatientInfo = findViewById(R.id.etPatientInfo);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        txtAvatar = findViewById(R.id.txtAvatar);
        btnAbout = findViewById(R.id.btnAbout); // Initialize About icon

        // 2. Load the saved profile when the screen opens
        loadProfile();

        // 3. Save the profile when the button is clicked
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // 4. About button listener
        btnAbout.setOnClickListener(v -> showAboutDialog());

        setupBottomNav();
    }

    // 🌟 ADDED: ABOUT DIALOG WITH EASTER EGG 🌟
    private void showAboutDialog() {
        String creators = "Creators: Baador, Potol, Baccha, OGSatarup, and Palm Leaf Soldier";
        String aboutText = "PillMonitor & MEdISyNC Kit\n\n" +
                "An intelligent medicine management system designed for automated tracking, timely reminders, and adherence monitoring.\n\n" +
                "---------------------------\n" +
                creators;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About MEdISyNC")
                .setMessage(aboutText)
                .setPositiveButton("Close", null)
                .show();
    }

    private void saveProfile() {
        String name = etPatientName.getText().toString().trim();
        String info = etPatientInfo.getText().toString().trim();

        // Write to the Diary
        SharedPreferences prefs = getSharedPreferences("PatientData", MODE_PRIVATE);
        prefs.edit().putString("patient_name", name).putString("patient_info", info).apply();

        // Update the Avatar to the first letter of their name!
        if (!name.isEmpty()) {
            txtAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        Toast.makeText(this, "Profile Saved Successfully", Toast.LENGTH_SHORT).show();

        // Clear focus so the keyboard hides nicely
        etPatientName.clearFocus();
        etPatientInfo.clearFocus();
    }

    private void loadProfile() {
        SharedPreferences prefs = getSharedPreferences("PatientData", MODE_PRIVATE);
        String name = prefs.getString("patient_name", "");
        String info = prefs.getString("patient_info", "");

        etPatientName.setText(name);
        etPatientInfo.setText(info);

        if (!name.isEmpty()) {
            txtAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            txtAvatar.setText("👤");
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_medicine) {
                startActivity(new Intent(this, MedicineActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_activity) {
                startActivity(new Intent(this, ActivityLogActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_appointment) {
                startActivity(new Intent(this, AppointmentActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }
}