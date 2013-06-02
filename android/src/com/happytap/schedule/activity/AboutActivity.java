package com.happytap.schedule.activity;

import roboguice.inject.InjectView;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

import us.wmwm.njrail.R;

public class AboutActivity extends ScheduleActivity {

	@InjectView(R.id.doug)
	View doug;
	
	@InjectView(R.id.ryan)
	View ryan;
	
	@InjectView(R.id.happytap)
	View happy;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setBackgroundDrawable(null);
		getSupportActionBar().setTitle(getString(R.string.activity_title_about));
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.about);		
		OnClickListener clickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				String url;
				if(v==doug) {
					url = "http://twitter.com/softprops";
				}else
				if(v==ryan) {
					url = "http://twitter.com/rdgravener";
				}
				else {
					url = "http://twitter.com/happytap";
				}
				Intent intent = new Intent(Intent.ACTION_VIEW ,Uri.parse(url));
				startActivity(intent);
			}			
		};
		doug.setOnClickListener(clickListener);
		ryan.setOnClickListener(clickListener);
		happy.setOnClickListener(clickListener);
	}
	
}
