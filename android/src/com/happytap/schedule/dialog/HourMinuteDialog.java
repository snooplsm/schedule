package com.happytap.schedule.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.happytap.schedule.dialog.AlarmTimeDialog.AlarmTimeListener;

public class HourMinuteDialog extends Dialog {

	public HourMinuteDialog(Context ctx, AlarmTimeListener listener) {
		super(ctx);
		this.listener = listener;
	}
	
	private final AlarmTimeListener listener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
}
