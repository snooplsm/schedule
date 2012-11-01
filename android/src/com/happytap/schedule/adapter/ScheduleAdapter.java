package com.happytap.schedule.adapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.util.Strings;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.happytap.schedule.activity.LoadScheduleActivity;
import com.happytap.schedule.activity.SplashScreenActivity;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.ScheduleTraverser;
import com.happytap.schedule.domain.StationInterval;
import com.happytap.schedule.domain.StationToStation;
import com.happytap.schedule.domain.TrainStatus;
import com.happytap.schedule.util.date.DateUtils;
import com.njtransit.rail.R;

public class ScheduleAdapter extends BaseAdapter {

	Schedule schedule;

	private boolean isToday;
	private Calendar limit;
	private boolean reversed = false;
	private Context context;
	private List<StationToStation> stations = new ArrayList<StationToStation>();

	@Override
	public int getCount() {
		return stations.size();
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

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

				// if(stationToStation.departTime.get(Calendar.HOUR_OF_DAY)==1)
				// {
				// System.out.println("what");
				// System.out.println(stationToStation.departTime.getTime());
				// System.out.println("!"+stationToStation.departTime.getTime()+".after("+limit.getTime()+")");
				// }
				// stations.add(stationToStation);
				if (!isToday) {
					if (!stationToStation.departTime.before(limit)) {
						// System.out.println(stationToStation.departTime.getTime());
						stations.add(stationToStation);
					}
				} else {
					if (!stationToStation.departTime.before(priorLimit)
							&& !stationToStation.departTime.after(limit))
						stations.add(stationToStation);

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
		super();
		this.schedule = schedule;
		this.context = context;
		traverse();

	}

	@Override
	public StationToStation getItem(int position) {
		return stations.get(position);
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
			LayoutInflater vi = LayoutInflater.from(context);
			v = vi.inflate(R.layout.station_to_station_item, null);
		}

		StationToStation sts = getItem(position);
		TextView textView = (TextView) v.findViewById(R.id.time);
		TextView duration = (TextView) v.findViewById(R.id.duration);
		TextView departsIn = (TextView) v.findViewById(R.id.away);
		TextView connections = (TextView) v.findViewById(R.id.connections);
		TextView tomorrow = (TextView) v.findViewById(R.id.time_descriptor);
		TextView fare = (TextView) v.findViewById(R.id.fare);
		if (sts == null) {
			textView.setText("");
			tomorrow.setText("");
			duration.setText("");
			departsIn.setText("");
			connections.setText("");
			fare.setText("Fare: " + LoadScheduleActivity.df.format(this.fares.get("Adult")));
			fare.setVisibility(View.VISIBLE);
			// v.setClickable(false);
			return v;
		} else {
			// v.setClickable(true);
			fare.setVisibility(View.GONE);
			v.setId(View.NO_ID);
		}
		if (sts.tripId != null && tripIdForAlarm != null
				&& sts.tripId.equals(tripIdForAlarm)) {
			v.setBackgroundDrawable(context.getResources().getDrawable(
					R.drawable.alarm_background));
		} else {
			v.setBackgroundDrawable(context.getResources().getDrawable(
					android.R.drawable.list_selector_background));
		}

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
				populateExtraInfo(connections, sts2);
			}

			tomorrow(tomorrow, sts);
			TrainStatus status = trainStatuses.get(sts.blockId);
			if (status == null) {
				status = trainStatuses.get(sts.tripId);
			}
			if (status == null) {
				onNoTrainStatus(v, sts);
				return v;
			}
			trainStatus(v, sts, status);
		} else {
			connections.setVisibility(View.GONE);
			textView.setText(time(sts));
			duration.setText(duration(sts));
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
		if (sts.routeId != null) {
			b.append(sts.schedule.routeIdToName.get(sts.routeId));
			b.append(" ");
		}
		if (sts.blockId != null && sts.blockId.trim().length() > 0) {
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
			if (isTransfer
					|| (sts2.tripId != null && sts2.tripId.equals(lastTripId))) {
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
				if (!(sts2.tripId != null & sts2.tripId.equals(nextTripId))) {
					String arrive = time.format(sts2.getArriveTime().getTime())
							.toLowerCase();
					arrive = arrive.substring(0, arrive.length() - 1).replace(
							" ", "");
					b.append("(");
					b.append(arrive);
					b.append(")");
					b.append(" ");
					b.append(sts2.schedule.stopIdToName.get(sts2.arriveId));

					if (sts2.blockId != null
							&& sts2.blockId.trim().length() > 0) {
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
					if (sts2.tripId != null && sts2.tripId.equals(lastTripId)) {

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

		if (sts2.tripId != null && !sts2.tripId.equals(lastTripId)) {
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
		if (sts2.blockId != null && sts2.blockId.trim().length() > 0) {
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
		Calendar tomorrow2 = Calendar.getInstance();
		tomorrow2.add(Calendar.DAY_OF_YEAR, 2);
		tomorrow2.set(Calendar.HOUR_OF_DAY, 0);
		tomorrow2.set(Calendar.MINUTE, 0);
		tomorrow2.set(Calendar.SECOND, 0);
		tomorrow2.set(Calendar.MILLISECOND, 0);
		if (!isToday) {
			tomorrowView.setText(SplashScreenActivity.df.format(sts.departTime
					.getTime()));
			tomorrowView.setVisibility(View.VISIBLE);
		} else {
			if (!android.text.format.DateUtils.isToday(sts.departTime
					.getTimeInMillis()) && sts.departTime.after(tomorrow)) {
				tomorrowView.setVisibility(View.VISIBLE);
				if (sts.departTime.after(tomorrow2)) {
					tomorrowView.setText((sts.departTime.get(Calendar.MONTH)
							+ 1 + "/" + sts.departTime
							.get(Calendar.DAY_OF_MONTH)));
				} else {
					tomorrowView.setText("next day");
				}
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
		String orig = String.format("%s - %s #%s", depart, arrive, sts.blockId)
				.toLowerCase();
		SpannableStringBuilder ssb = new SpannableStringBuilder(orig);
		int pound = orig.indexOf("#");
		if (pound >= 0) {
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
		String orig = String.format("%s - %s", depart, arrive).toLowerCase();
		SpannableStringBuilder ssb = new SpannableStringBuilder(orig);
		return ssb;
	}

	private Map<String, TrainStatus> trainStatuses = new HashMap<String, TrainStatus>();

	private Map<String, TrainStatus> reversedTrainStatuses;

	private Map<String, TrainStatus> normalTrainStatuses = trainStatuses;

	private String tripIdForAlarm;

	public String getTripIdForAlarm() {
		return tripIdForAlarm;
	}

	public void setTripIdForAlarm(String tripIdForAlarm) {
		this.tripIdForAlarm = tripIdForAlarm;
		notifyDataSetChanged();
	}

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

	public void clear() {
		stations.clear();
	}

	int fareAnchor = -1;
	Map<String,Double> fares;

	public void setFareAnchor(Map<String,Double> fares, int i) {
		this.fares = fares;
		this.fareAnchor = i;
		stations.add(i, null);
		notifyDataSetChanged();
	}
}
