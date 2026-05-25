package com.example.pillmonitor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Locale;

public class AppointmentActivity extends AppCompatActivity {

    EditText etDoctorName;
    TextView tvDate, tvTime;
    Button btnBook;
    LinearLayout previousApptContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        etDoctorName = findViewById(R.id.etDoctorName);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        btnBook = findViewById(R.id.btnBookAppt);
        previousApptContainer = findViewById(R.id.previousApptContainer);

        setupDateAndTimePickers();

        btnBook.setOnClickListener(v -> saveAppointment());

        loadAppointments();
        setupBottomNav();
    }

    private void setupDateAndTimePickers() {
        tvDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
                tvDate.setText(String.format(Locale.getDefault(), "%d/%d/%d", d, m + 1, y));
                tvDate.setTextColor(getResources().getColor(R.color.text_primary));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        tvTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(this, (view, h, m) -> {
                String period = h < 12 ? "AM" : "PM";
                int formattedHour = h > 12 ? h - 12 : h;
                if (formattedHour == 0) formattedHour = 12;
                tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", formattedHour, m, period));
                tvTime.setTextColor(getResources().getColor(R.color.text_primary));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
            dialog.show();
        });
    }

    private void saveAppointment() {
        String doc = etDoctorName.getText().toString().trim();
        String date = tvDate.getText().toString().trim();
        String time = tvTime.getText().toString().trim();

        if (doc.isEmpty() || date.equals("Select Date") || time.equals("Select Time")) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("Appts", MODE_PRIVATE);
        String oldAppts = prefs.getString("list", "");
        String newEntry = doc + "::" + date + " @ " + time + "||" + oldAppts;
        prefs.edit().putString("list", newEntry).apply();

        etDoctorName.setText("");
        tvDate.setText("Select Date");
        tvDate.setTextColor(getResources().getColor(R.color.text_secondary));
        tvTime.setText("Select Time");
        tvTime.setTextColor(getResources().getColor(R.color.text_secondary));

        loadAppointments();
        Toast.makeText(this, "Appointment saved", Toast.LENGTH_SHORT).show();
    }

    private void loadAppointments() {
        previousApptContainer.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("Appts", MODE_PRIVATE);
        String listString = prefs.getString("list", "");

        if (listString.isEmpty()) return;

        String[] appointments = listString.split("\\|\\|");
        for (int i = 0; i < appointments.length; i++) {
            String appt = appointments[i];
            if (appt.trim().isEmpty()) continue;

            String[] parts = appt.split("::");
            if (parts.length == 2) {
                drawAppointmentItem(parts[0], parts[1], i);
            }
        }
    }

    private void drawAppointmentItem(String doctor, String dateAndTime, int index) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_appointment, null);

        TextView txtDocItem = view.findViewById(R.id.txtDocItemName);
        TextView txtDateItem = view.findViewById(R.id.txtApptItemDate);
        ImageView btnMenu = view.findViewById(R.id.btnMenu);

        txtDocItem.setText("Dr. " + doctor);
        txtDateItem.setText(dateAndTime);

        btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(AppointmentActivity.this, btnMenu);
            // Add options to the menu (ID 1 for Edit, ID 2 for Delete)
            popup.getMenu().add(0, 1, 0, "Edit");
            popup.getMenu().add(0, 2, 0, "Delete");

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    // EDIT CLICKED
                    editAppointment(doctor, dateAndTime, index);
                    return true;
                } else if (item.getItemId() == 2) {
                    // DELETE CLICKED
                    deleteAppointment(index);
                    return true;
                }
                return false;
            });
            popup.show();
        });

        previousApptContainer.addView(view);
    }

    private void editAppointment(String doctor, String dateAndTime, int index) {
        etDoctorName.setText(doctor);
        String[] dtParts = dateAndTime.split(" @ ");
        if (dtParts.length == 2) {
            tvDate.setText(dtParts[0]);
            tvDate.setTextColor(getResources().getColor(R.color.text_primary));
            tvTime.setText(dtParts[1]);
            tvTime.setTextColor(getResources().getColor(R.color.text_primary));
        }

        // 3. Delete the old entry so it doesn't duplicate when they hit Save
        deleteAppointment(index);
        Toast.makeText(this, "Make changes and click Book Appointment", Toast.LENGTH_LONG).show();
    }

    private void deleteAppointment(int indexToRemove) {
        SharedPreferences prefs = getSharedPreferences("Appts", MODE_PRIVATE);
        String listString = prefs.getString("list", "");
        if (listString.isEmpty()) return;

        String[] appointments = listString.split("\\|\\|");
        StringBuilder newAppts = new StringBuilder();
        for (int i = 0; i < appointments.length; i++) {
            if (i != indexToRemove && !appointments[i].trim().isEmpty()) {
                newAppts.append(appointments[i]).append("||");
            }
        }
        prefs.edit().putString("list", newAppts.toString()).apply();
        loadAppointments();
        Toast.makeText(this, "Appointment deleted", Toast.LENGTH_SHORT).show();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_appointment);
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
