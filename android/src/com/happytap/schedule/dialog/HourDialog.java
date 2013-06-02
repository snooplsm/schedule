package com.happytap.schedule.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import us.wmwm.njrail.R;

public abstract class HourDialog extends Dialog implements
		android.view.View.OnClickListener {

	private View plus, minus;
	private EditText val;

	private int min, max;
	private int current;
	private int daysAhead;

	public HourDialog(Context context, int daysAhead, int min, int max) {
		super(context);
		setContentView(R.layout.hour);
		this.min = min;
		this.max = max;
		plus = findViewById(R.id.plus);
		minus = findViewById(R.id.minus);
		val = (EditText) findViewById(R.id.edit);
		current = daysAhead;
		this.daysAhead = daysAhead;
		val.setText(String.valueOf(daysAhead));
		plus.setOnClickListener(this);
		minus.setOnClickListener(this);
		View.OnClickListener listener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if(v.getId()==android.R.id.button1) {
					onChange(current);
				}
				dismiss();
			}
		};
		findViewById(android.R.id.button1).setOnClickListener(listener);
		findViewById(android.R.id.button2).setOnClickListener(listener);
	}

	protected abstract void onChange(int current);
	
	Toast last;

	@Override
	public void onClick(View v) {
		if (last != null) {
			last.cancel();
		}
		if (v == plus) {
			if (current + 1 <= max) {
				current = current + 1;
				val.setText(String.valueOf(current));
			} else {
				last = Toast.makeText(getContext(), max + " is the max",
						Toast.LENGTH_SHORT);
			}
		} else {
			if (current - 1 >= min) {
				current = current - 1;
				val.setText(String.valueOf(current));
			} else {
				last = Toast.makeText(getContext(), min + " is the min",
						Toast.LENGTH_SHORT);
			}
		}
	}

}
