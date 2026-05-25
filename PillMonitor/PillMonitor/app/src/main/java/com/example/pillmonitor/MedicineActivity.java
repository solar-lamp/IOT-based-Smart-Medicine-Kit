package com.example.pillmonitor;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;

public class MedicineActivity extends AppCompatActivity {

    TextView txtTotalPills;
    LinearLayout medicineList;

    HashMap<String, ArrayList<Medicine>> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine);

        txtTotalPills = findViewById(R.id.txtTotalPills);
        medicineList = findViewById(R.id.medicineList);

        // LOAD REAL DATA
        data = StorageManager.load(this);

        if (data == null) {
            data = new HashMap<>();
        }

        loadMedicines();
        setupBottomNav();
    }

    // LOAD MEDICINES FROM STORAGE
    private void loadMedicines() {
        medicineList.removeAllViews();
        int totalPills = 0;

        for (String slot : data.keySet()) {
            ArrayList<Medicine> meds = data.get(slot);
            if (meds == null) continue;

            for (Medicine m : meds) {
                addMedicineCard(m.name, m.count);
                totalPills += m.count;
            }
        }

        txtTotalPills.setText(String.valueOf(totalPills));
    }

    // 🌟 UPDATED: THE CARD GENERATOR 🌟
    private void addMedicineCard(String name, int pills) {
        // 1. Grab the beautiful XML design you already made
        View view = LayoutInflater.from(this).inflate(R.layout.item_medicine, medicineList, false);

        // Find the root CardView so we can change its background color
        androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) view;

        // 2. Find the text boxes and progress bar inside that card
        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtCount = view.findViewById(R.id.txtCount);
        ProgressBar progress = view.findViewById(R.id.progress);
        ImageView btnMenu = view.findViewById(R.id.btnMenu);

        // 3. Set the text
        txtName.setText(name);
        txtCount.setText(pills + " pills remaining");

        // 4. Make the progress bar dynamic!
        int maxPills = Math.max(pills, 30);
        progress.setMax(maxPills);
        progress.setProgress(pills);

        // 5. Color-code the progress bar AND the Card background!
        if (pills <= 5) {
            // CRITICAL (5 or less): Bright red bar with a richer, brighter red card background
            progress.setProgressTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#FF4D4D")));
            cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#5A2222"));

        } else if (pills <= 10) {
            // WARNING (6 to 10): Bright yellow bar with a richer, amber/gold card background
            progress.setProgressTintList(ColorStateList.valueOf(android.graphics.Color.parseColor("#FFC107")));
            cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#5A4B1D"));

        } else {
            // HEALTHY (11+): Green bar with a richer, brighter forest green card background
            progress.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green_primary)));
            cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#1F452B"));
        }

        // Hide the menu dots on this specific screen to keep it looking like a clean dashboard
        btnMenu.setVisibility(View.GONE);

        // 6. Add the finished card to the screen!
        medicineList.addView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // REFRESH DATA WHEN RETURNING TO SCREEN
        data = StorageManager.load(this);
        loadMedicines();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_medicine);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_medicine) {
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
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}