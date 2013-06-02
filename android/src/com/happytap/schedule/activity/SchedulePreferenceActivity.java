package com.happytap.schedule.activity;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import us.wmwm.njrail.R;

public class SchedulePreferenceActivity extends SherlockPreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.OrangeTheme);
		super.onCreate(savedInstanceState);
		//PreferenceManager.setDefaultValues(this, R.xml.preference, false);
		//getSupportActionBar().set
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle("Preferences");
		addPreferencesFromResource(R.xml.preference);
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
