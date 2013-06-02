package com.happytap.schedule.activity;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockActivity;
import us.wmwm.njrail.R;

public class ScheduleActivity extends RoboSherlockActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setTheme(R.style.OrangeTheme);
    	super.onCreate(savedInstanceState);        
    	Drawable d = new LayerDrawable(new Drawable[] {getResources().getDrawable(R.drawable.user_details_activity_general_selector),new ColorDrawable(0x55000000)});
    	getSupportActionBar().setSplitBackgroundDrawable(d);
        getSupportActionBar().setBackgroundDrawable(d);
    }

    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if(android.R.id.home==item.getItemId()) {
			super.onBackPressed();	return true;		
		}
		return super.onOptionsItemSelected(item);
	}

    
}