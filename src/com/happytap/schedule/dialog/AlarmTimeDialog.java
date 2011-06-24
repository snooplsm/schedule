package com.happytap.schedule.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.njtransit.rail.R;

public class AlarmTimeDialog extends Dialog {

	public AlarmTimeDialog(Context context, AlarmTimeListener listener) {
		super(context);		
		this.listener = listener;
	}
	
	public static interface AlarmTimeListener {
		void onMinutesBefore(int mins);
	}
	
	private final AlarmTimeListener listener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Display disp = getWindow().getWindowManager().getDefaultDisplay();
		LayoutParams lp = new LayoutParams(disp.getWidth(),disp.getHeight());
		setContentView(getLayoutInflater().inflate(R.layout.time, null),lp);
		RadioGroup timeView = (RadioGroup) findViewById(R.id.time_group);
		timeView.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				for(int i = 0; i < group.getChildCount(); i++) {
					View child = group.getChildAt(i);
					if(child.getId()==checkedId) {
						listener.onMinutesBefore(5-i);
						cancel();
					}
				}
			}
			
		});
		for(int i = 0; i < timeView.getChildCount()-1; i++) {
			
		}
	}
}
