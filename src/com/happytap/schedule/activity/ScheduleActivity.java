package com.happytap.schedule.activity;

import roboguice.activity.RoboActivity;
import android.os.Bundle;

public class ScheduleActivity extends RoboActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Light);
    }
}