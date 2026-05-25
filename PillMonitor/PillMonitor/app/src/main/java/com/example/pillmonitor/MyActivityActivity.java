package com.example.pillmonitor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MyActivityActivity extends AppCompatActivity {

    EditText inputName, inputDoctor;
    TextView txtDate, txtTime;
    LinearLayout appointmentList;

    LinearLayout profileDisplay;
    TextView txtProfileName;
    ImageView btnEditProfile;

    String pickedDate = "";
    String pickedTime = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_activity);

        // Views
        inputName = findViewById(R.id.inputName);
        inputDoctor = findViewById(R.id.inputDoctor);
        txtDate = findViewById(R.id.txtDate);
        txtTime = findViewById(R.id.txtTime);
        appointmentList = findViewById(R.id.appointmentList);

        profileDisplay = findViewById(R.id.profileDisplay);
        txtProfileName = findViewById(R.id.txtProfileName);
        btnEditProfile = findViewById(R.id.btnEditProfile);

        // Load profile
        String savedName = StorageManager.getProfileName(this);
        if (!savedName.isEmpty()) {
            inputName.setVisibility(View.GONE);
            findViewById(R.id.btnSave).setVisibility(View.GONE);
            profileDisplay.setVisibility(View.VISIBLE);
            txtProfileName.setText(savedName);
        }

        // Load last picked time if any

        // Render existing appointments
        renderAppointments();

        // Save profile
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            StorageManager.saveProfileName(this, name);
            inputName.setVisibility(View.GONE);
            findViewById(R.id.btnSave).setVisibility(View.GONE);
            profileDisplay.setVisibility(View.VISIBLE);
            txtProfileName.setText(name);

            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
        });

        // Edit profile
        btnEditProfile.setOnClickListener(v -> {
            profileDisplay.setVisibility(View.GONE);
            inputName.setVisibility(View.VISIBLE);
            findViewById(R.id.btnSave).setVisibility(View.VISIBLE);
            inputName.requestFocus();
        });

        // Pick date
        findViewById(R.id.btnPickDate).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) -> {
                pickedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, (m + 1), y);
                txtDate.setText(pickedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Pick time
        findViewById(R.id.btnPickTime).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (tp, h, m) -> {
                pickedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                txtTime.setText(pickedTime);

            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        // Save appointment
        findViewById(R.id.btnSaveDoctor).setOnClickListener(v -> {
            String note = inputDoctor.getText().toString().trim();

            if (note.isEmpty() || pickedDate.isEmpty() || pickedTime.isEmpty()) {
                Toast.makeText(this, "Fill doctor note, date and time", Toast.LENGTH_SHORT).show();
                return;
            }

            StorageManager.addDoctorAppointment(this, note, pickedDate, pickedTime);
            AlarmScheduler.setDoctorReminder(this, pickedDate, pickedTime, note);

            inputDoctor.setText("");
            txtDate.setText("No date selected");
            txtTime.setText("--:--");
            pickedDate = "";
            pickedTime = "";

            renderAppointments();
            Toast.makeText(this, "Appointment saved & reminder set", Toast.LENGTH_SHORT).show();
        });
    }

    private void renderAppointments() {
        appointmentList.removeAllViews();

        java.util.ArrayList<DoctorAppt> list = StorageManager.getAllDoctorAppointments(this);

        for (int i = 0; i < list.size(); i++) {
            View v = getLayoutInflater().inflate(R.layout.item_appointment, appointmentList, false);

            // Using the brand new IDs we created!
            TextView name = v.findViewById(R.id.txtDocItemName);
            TextView date = v.findViewById(R.id.txtApptItemDate);
            ImageView menu = v.findViewById(R.id.btnMenu);

            int index = i;
            DoctorAppt appt = list.get(i);

            name.setText(appt.note);
            date.setText("Date: " + appt.date + "   Time: " + appt.time);

            menu.setOnClickListener(b -> {
                PopupMenu pm = new PopupMenu(this, b);
                pm.getMenuInflater().inflate(R.menu.menu_appointment, pm.getMenu());

                pm.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete) {
                        StorageManager.deleteDoctorAppointment(this, index);
                        renderAppointments();
                        return true;
                    }
                    if (item.getItemId() == R.id.action_edit) {
                        inputDoctor.setText(appt.note);
                        pickedDate = appt.date;
                        pickedTime = appt.time;
                        txtDate.setText(pickedDate);
                        txtTime.setText(pickedTime);

                        StorageManager.deleteDoctorAppointment(this, index);
                        renderAppointments();
                        Toast.makeText(this, "Edit and save again", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    return false;
                });

                pm.show();
            });

            appointmentList.addView(v);
        }
    }

}
