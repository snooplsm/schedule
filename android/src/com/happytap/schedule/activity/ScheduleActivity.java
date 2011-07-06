package com.happytap.schedule.activity;

import roboguice.activity.RoboActivity;
import android.os.Bundle;

import com.njtransit.rail.R;

public class ScheduleActivity extends RoboActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.OrangeTheme);
    }
}