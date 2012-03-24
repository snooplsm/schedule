package com.happytap.schedule.adapter;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Future;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.happytap.schedule.database.TripInfo;
import com.happytap.schedule.database.TripInfo.Stop;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.view.ProgressView;
import com.njtransit.rail.R;

public class TripAdapter extends ArrayAdapter<TripInfo.Stop> {
	
	private Schedule schedule;
	
	private Calendar startCal;
	
	public TripAdapter(Context context) {
		super(context, R.layout.station_to_station_item);
	}
	
	public TripAdapter(Context context, List<TripInfo.Stop> stops, Schedule schedule, Calendar startCal) {
		super(context, R.layout.station_to_station_item, stops);
		this.schedule = schedule;
		this.startCal = startCal;
		this.startCal.set(Calendar.HOUR_OF_DAY,0);
		this.startCal.set(Calendar.MINUTE,0);
		this.startCal.set(Calendar.SECOND,0);
		this.startCal.set(Calendar.MILLISECOND,0);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		ProgressView progress = null;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.trip, null);
			v.setMinimumHeight(0);
			v.setBackgroundDrawable(new ProgressView());
		}
		progress = (ProgressView) v.getBackground();
				
		Stop sts = getItem(position);
		Calendar departCalendar = Calendar.getInstance();
		Calendar arriveCalendar = Calendar.getInstance();
		departCalendar.setTimeInMillis(sts.depart.getTimeInMillis());
		
		if(position<getCount()-1) {
			Stop next = getItem(position+1);
			arriveCalendar.setTimeInMillis(next.arrive.getTimeInMillis());
		} else {
			arriveCalendar.setTimeInMillis(sts.arrive.getTimeInMillis());
		}
		
		Calendar dCal = Calendar.getInstance();
		dCal.setTimeInMillis(startCal.getTimeInMillis());
		dCal.add(Calendar.DAY_OF_YEAR, departCalendar.get(Calendar.DAY_OF_YEAR)-1);
		dCal.add(Calendar.HOUR_OF_DAY, departCalendar.get(Calendar.HOUR_OF_DAY));
		dCal.add(Calendar.MINUTE, departCalendar.get(Calendar.MINUTE));
		Calendar aCal = Calendar.getInstance();
		aCal.setTimeInMillis(startCal.getTimeInMillis());
		aCal.add(Calendar.DAY_OF_YEAR, arriveCalendar.get(Calendar.DAY_OF_YEAR)-1);
		aCal.add(Calendar.HOUR_OF_DAY, arriveCalendar.get(Calendar.HOUR_OF_DAY));
		aCal.add(Calendar.MINUTE, arriveCalendar.get(Calendar.MINUTE));
		System.out.println(departCalendar.getTime() + " vs " + dCal.getTime());
		System.out.println(arriveCalendar.getTime() + " vs " + aCal.getTime());
//		long arrive = sts.arrive.getTimeInMillis();
		if(dCal.getTimeInMillis()>System.currentTimeMillis()) {
			progress.setPercent(0);
		} else {
			long max = aCal.getTimeInMillis()-dCal.getTimeInMillis();
			long curr = aCal.getTimeInMillis() - System.currentTimeMillis();
			System.out.println("max:"+max+",cur"+curr);
			float percent = 1;
			if(curr<=0) {
				percent = 1;
			} else {
				percent = 1.0f-(curr/(float)max);
			}
			progress.setPercent(Math.min((float)1,percent));
		}
		TextView time = (TextView)v.findViewById(R.id.time);
		time.setText(sts.name);
		String dpt = ScheduleAdapter.time.format(sts.depart.getTime());
		TextView timeDescriptor = (TextView)v.findViewById(R.id.time_descriptor);
		timeDescriptor.setText(dpt);
		return v;
	}
}
