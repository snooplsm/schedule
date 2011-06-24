package com.happytap.schedule.activity;

import roboguice.activity.RoboPreferenceActivity;
import roboguice.inject.InjectPreference;
import android.os.Bundle;
import android.preference.CheckBoxPreference;

import com.njtransit.rail.R;

public class SchedulePreferenceActivity extends RoboPreferenceActivity {

	@InjectPreference("showJumper")
	protected CheckBoxPreference showJumper;
	
	@InjectPreference("useDepartureVision")
	protected CheckBoxPreference useDepartureVision;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preference);
	}

}
