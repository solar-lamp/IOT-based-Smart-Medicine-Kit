package com.example.pillmonitor.models;

public class ActivityItem {

    private String title;
    private String time;
    private int icon;

    public ActivityItem(String title, String time, int icon) {
        this.title = title;
        this.time = time;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }

    public int getIcon() {
        return icon;
    }
}