package com.happytap.schedule.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.happytap.schedule.database.TripInfo;
import com.happytap.schedule.database.TripInfo.Stop;
import com.njtransit.rail.R;

public class TripAdapter extends ArrayAdapter<TripInfo.Stop> {
	
	public TripAdapter(Context context) {
		super(context, R.layout.station_to_station_item);
	}
	
	public TripAdapter(Context context, List<TripInfo.Stop> stops) {
		super(context, R.layout.station_to_station_item, stops);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.trip, null);
			v.setMinimumHeight(0);
		}
				
		Stop sts = getItem(position);
		TextView time = (TextView)v.findViewById(R.id.time);
		time.setText(sts.name);
		String depart = ScheduleAdapter.time.format(sts.depart.getTime());
		TextView timeDescriptor = (TextView)v.findViewById(R.id.time_descriptor);
		timeDescriptor.setText(depart);
		return v;
	}
}
