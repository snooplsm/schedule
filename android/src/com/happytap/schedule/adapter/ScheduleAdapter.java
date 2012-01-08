package com.happytap.schedule.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import roboguice.util.Strings;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.happytap.schedule.activity.SplashScreenActivity;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.ScheduleTraverser;
import com.happytap.schedule.domain.StationInterval;
import com.happytap.schedule.domain.StationToStation;
import com.happytap.schedule.domain.TrainStatus;
import com.happytap.schedule.util.date.DateUtils;
import com.njtransit.rail.R;

public class ScheduleAdapter extends ArrayAdapter<StationToStation> {

	Schedule schedule;

	private boolean isToday;
	private Calendar limit;
	private boolean reversed = false;

	private void traverse() {
		limit = Calendar.getInstance();
		limit.setTime(schedule.end);
		Calendar start = Calendar.getInstance();
		start.setTime(schedule.start);
		isToday = DateUtils.isToday(start);
		if (isToday) {
			limit.set(Calendar.HOUR_OF_DAY, 9);
			limit.set(Calendar.MINUTE, 0);
		} else {
			limit.add(Calendar.DAY_OF_YEAR, 0);
			limit.set(Calendar.HOUR_OF_DAY, 0);
			limit.set(Calendar.MINUTE, 0);
			limit.set(Calendar.SECOND, 0);
			limit.set(Calendar.MILLISECOND, 0);
		}

		final Calendar priorLimit = Calendar.getInstance();
		priorLimit.add(Calendar.HOUR_OF_DAY, -2);

		ScheduleTraverser traverser = new ScheduleTraverser() {

			@Override
			public void populateItem(int index,
					StationToStation stationToStation, int total) {
				if (!isToday) {
					if (!stationToStation.departTime.before(limit)) {
						add(stationToStation);
					}
				} else if (!stationToStation.departTime.after(limit)) {
					if (!stationToStation.arriveTime.before(priorLimit)) {
						add(stationToStation);
					}

				}
			}
		};
		if (reversed) {
			schedule.inReverseOrderTraversal(traverser);
		} else {
			schedule.inOrderTraversal(traverser);
		}
	}

	public ScheduleAdapter(Context context, Schedule schedule) {
		super(context, R.layout.station_to_station_item);
		this.schedule = schedule;

		traverse();

	}

	public int findIndexOfCurrent() {
		long now = System.currentTimeMillis();
		for (int i = 0; i < getCount(); i++) {
			StationToStation sts = getItem(i);
			if (sts.departTime.getTimeInMillis() >= now) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.station_to_station_item, null);
		}
		StationToStation sts = getItem(position);
		TextView textView = (TextView) v.findViewById(R.id.time);
		TextView duration = (TextView) v.findViewById(R.id.duration);
		TextView departsIn = (TextView) v.findViewById(R.id.away);
		TextView connections = (TextView) v.findViewById(R.id.connections);
		textView.setText(time(sts));
		departs(departsIn, sts);
		if (sts instanceof StationInterval) {
			StationInterval sts2 = (StationInterval) sts;
			textView.setText(time(sts2));
			duration.setText(duration(sts2));
			connections.setVisibility(View.VISIBLE);
			if (sts2.schedule.transfers.length > 1) {
				populateConnections(connections, sts2);
			} else {
				populateExtraInfo(connections,sts2);				
			}
			TextView tomorrow = (TextView) v.findViewById(R.id.time_descriptor);
			tomorrow(tomorrow, sts);
			TrainStatus status = trainStatuses.get(sts.blockId);
			if (status == null) {
				onNoTrainStatus(v, sts);
				return v;
			}
			trainStatus(v, sts, status);
		} else {
			connections.setVisibility(View.GONE);
			textView.setText(time(sts));
			duration.setText(duration(sts));
			TextView tomorrow = (TextView) v.findViewById(R.id.time_descriptor);
			tomorrow(tomorrow, sts);
			
			TrainStatus status = trainStatuses.get(sts.blockId);
			if (status == null) {
				onNoTrainStatus(v, sts);
				return v;
			}
			trainStatus(v, sts, status);
		}
		return v;
	}

	private void populateExtraInfo(TextView connections, StationInterval sts) {
		StringBuilder b = new StringBuilder();
		if(sts.routeId!=null) {
			b.append(sts.schedule.routeIdToName.get(sts.routeId));
			b.append(" ");
		}
		if(sts.blockId!=null) {
			b.append("#").append(sts.blockId);
		}
		connections.setText(b);
	}

	private void populateConnections(TextView connections, StationInterval sts2) {
		StringBuilder b = new StringBuilder();
		boolean added = false;
		String lastTripId = null;
		String nextTripId = null;
		while (sts2.hasNext()) {
			boolean isTransfer = sts2.isTransfer();
			nextTripId = sts2.next().tripId;
			if (isTransfer || (sts2.tripId!=null && sts2.tripId.equals(lastTripId))) {
				// String arrive = time.format(sts2.getArriveTime().getTime())
				// .toLowerCase();
				// arrive = arrive.substring(0, arrive.length() - 2);
				// b.append(sts2.schedule.stopIdToName.get(sts2.arriveId));
				// b.append(" ");
				added = false;

			} else {
				added = true;
				String depart = time.format(sts2.getDepartTime().getTime())
						.toLowerCase();
				depart = depart.substring(0, depart.length() - 1).replace(" ",
						"");
				b.append("(");
				b.append(depart);
				b.append(")");
				b.append(" ");
				b.append(sts2.schedule.stopIdToName.get(sts2.departId));
				b.append(" ");
				b.append("↝");

				b.append(" ");
				if (!(sts2.tripId!=null & sts2.tripId.equals(nextTripId))) {
					String arrive = time.format(sts2.getArriveTime().getTime())
							.toLowerCase();
					arrive = arrive.substring(0, arrive.length() - 1).replace(
							" ", "");
					b.append("(");
					b.append(arrive);
					b.append(")");
					b.append(" ");
					b.append(sts2.schedule.stopIdToName.get(sts2.arriveId));
					
					if(sts2.blockId!=null) {
						b.append(" #");
						b.append(sts2.blockId);
					}
						
					
				}
			}

			if (sts2.hasNext()) {
				lastTripId = sts2.tripId;
				sts2 = sts2.next();
				if (added) {
					b.append(" ");
					if (sts2.tripId!=null && sts2.tripId.equals(lastTripId)) {

					} else {
						if (sts2.isTransfer()) {
							b.append("↻\n");
						} else {
							b.append("↝\n");
						}
					}

				}

			} else {
				break;
			}
		}

		if (sts2.tripId!=null && !sts2.tripId.equals(lastTripId)) {
			String depart = time.format(sts2.getDepartTime().getTime())
					.toLowerCase();
			depart = depart.substring(0, depart.length() - 1).replace(" ", "");
			b.append("(");
			b.append(depart);
			b.append(")");
			b.append(" ");
			b.append(sts2.schedule.stopIdToName.get(sts2.departId));
			b.append(" ");
			b.append("↝");
			b.append(" ");
		}
		b.append("(");
		String arrive = time.format(sts2.getArriveTime().getTime())
				.toLowerCase();
		arrive = arrive.substring(0, arrive.length() - 1).replace(" ", "");
		b.append(arrive);
		b.append(") ");
		b.append(sts2.schedule.stopIdToName.get(sts2.arriveId));
		if(sts2.blockId!=null) {
			b.append(" #").append(sts2.blockId);
		}
		connections.setText(b.toString());
	}

	private void tomorrow(TextView tomorrowView, StationToStation sts) {
		Calendar tomorrow = Calendar.getInstance();
		tomorrow.add(Calendar.DAY_OF_YEAR, 1);
		tomorrow.set(Calendar.HOUR_OF_DAY, 0);
		tomorrow.set(Calendar.MINUTE, 0);
		tomorrow.set(Calendar.SECOND, 0);
		tomorrow.set(Calendar.MILLISECOND, 0);
		if (!isToday) {
			tomorrowView.setText(SplashScreenActivity.df.format(sts.departTime
					.getTime()));
			tomorrowView.setVisibility(View.VISIBLE);
		} else {
			if (sts.departTime.after(tomorrow)) {
				tomorrowView.setText("next day");
				tomorrowView.setVisibility(View.VISIBLE);
			} else {
				tomorrowView.setText("");
				tomorrowView.setVisibility(View.GONE);
			}
		}
	}

	private void onNoTrainStatus(View view, StationToStation sts) {
		TextView timeDescriptor = (TextView) view
				.findViewById(R.id.time_descriptor);
		if (timeDescriptor.getVisibility() == View.VISIBLE) {

		} else {
			timeDescriptor.setVisibility(View.GONE);
		}
	}

	private void trainStatus(View view, StationToStation sts, TrainStatus status) {
		TextView timeDescriptor = (TextView) view
				.findViewById(R.id.time_descriptor);
		StringBuilder b = new StringBuilder();
		if (!Strings.isEmpty(status.getStatus())) {
			b.append(status.getStatus().toLowerCase());
		}
		if (!Strings.isEmpty(status.getTrack())) {
			if (b.length() > 0) {
				b.append(' ');
			}
			b.append("on track ").append(status.getTrack().toLowerCase());
		}
		if (timeDescriptor.getVisibility() == View.VISIBLE) {
			timeDescriptor.setText(timeDescriptor.getText() + " "
					+ b.toString());
		} else {
			timeDescriptor.setText(b.toString());
			timeDescriptor.setVisibility(View.VISIBLE);
		}

	}

	public static DateFormat time = new SimpleDateFormat("h:mm aa");

	private String duration(StationToStation sts) {
		long diff = sts.arriveTime.getTimeInMillis()
				- sts.departTime.getTimeInMillis();
		return "" + diff / 60000 + " minutes";
	}

	private String duration(StationInterval sts) {
		StationInterval sts2 = sts;
		while (sts2.hasNext()) {
			// TODO: optimize by just jumping to the end...
			sts2 = sts2.next();
		}
		long diff = sts2.arriveTime.getTimeInMillis()
				- sts.departTime.getTimeInMillis();
		return "" + diff / 60000 + " minutes";
	}

	private void departs(TextView departsIn, StationToStation sts) {
		long now = System.currentTimeMillis();
		long depart = sts.departTime.getTimeInMillis();
		departsIn.setVisibility(View.GONE);
		if (depart < now) {
			departsIn.setVisibility(View.GONE);
		} else {
			long diff = depart - now;
			int mins = (int) (diff / 60000);

			if (mins <= 100) {
				departsIn.setText("departs in " + mins + " minutes");
				departsIn.setVisibility(View.VISIBLE);
			}
		}
	}

	public static CharSequence time(StationToStation sts) {
		String depart = time.format(sts.getDepartTime().getTime());
		String arrive = time.format(sts.getArriveTime().getTime());
		String orig = String.format("%s - %s #%s", depart, arrive, sts.blockId).toLowerCase();
		SpannableStringBuilder ssb = new SpannableStringBuilder(orig);
		int pound = orig.indexOf("#");
		if(pound>=0) {
			ssb.setSpan(new SuperscriptSpan(), pound, orig.length(), 0);
		}
		return ssb;
	}

	public static CharSequence time(StationInterval sts) {
		String depart = time.format(sts.getDepartTime().getTime());
		StationInterval sts2 = sts;
		while (sts2.hasNext()) {
			// TODO: optimize by just jumping to the end...
			sts2 = sts2.next();
		}
		if (sts2 == null || sts2.getArriveTime() == null) {
		}
		String arrive = time.format(sts2.getArriveTime().getTime());
		String orig = String.format("%s - %s", depart,arrive).toLowerCase();
		SpannableStringBuilder ssb = new SpannableStringBuilder(orig);
		return ssb;
	}

	private Map<String, TrainStatus> trainStatuses = new HashMap<String, TrainStatus>();

	private Map<String, TrainStatus> reversedTrainStatuses;

	private Map<String, TrainStatus> normalTrainStatuses = trainStatuses;

	public final void onStatus(TrainStatus status) {
		trainStatuses.put(status.getTrain(), status);
		notifyDataSetChanged();
	}

	/**
	 * reverse the schedule
	 */
	public void reverse() {
		reversed = !reversed;
		if (!reversed) {
			trainStatuses = reversedTrainStatuses;
		} else {
			trainStatuses = normalTrainStatuses;
		}

		if (trainStatuses == null) {
			trainStatuses = new HashMap<String, TrainStatus>();
		}
		clear();
		traverse();
		notifyDataSetChanged();
	}
}
