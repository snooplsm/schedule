package com.happytap.schedule.activity;

import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import com.njtransit.rail.R;

public class ScheduleActivity extends RoboSherlockActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Sherlock);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.user_details_activity_general_selector));
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}
    
    
}