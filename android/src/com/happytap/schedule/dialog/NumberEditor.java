package com.happytap.schedule.dialog;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;

public class NumberEditor extends EditTextPreference {

	public NumberEditor(Context context, AttributeSet attrs) {
		super(context, attrs);
		//setLayoutResource(R.layout.hour_minute);
		//findViewById(R.id.m)
	}
	
	@Override
	protected View onCreateDialogView() {
		// TODO Auto-generated method stub
		return super.onCreateDialogView();
	}

}
