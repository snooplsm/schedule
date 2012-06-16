package com.happytap.schedule.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import roboguice.inject.InjectView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.ads.AdRequest.ErrorCode;
import com.google.inject.Inject;
import com.happytap.schedule.adapter.TripAdapter;
import com.happytap.schedule.database.ScheduleDao;
import com.happytap.schedule.database.TripInfo;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.StationInterval;
import com.happytap.schedule.provider.CurrentScheduleProvider;
import com.happytap.schedule.service.ThreadHelper;
import com.njtransit.rail.R;

public class TripActivity extends ScheduleActivity {

	private String[] tripIds;

	private String departId;
	private String arriveId;

	private Schedule schedule;

	private long start;
	
	@Inject
	CurrentScheduleProvider provider;

	@Inject
	ScheduleDao dao;

	AdView adView;

	@InjectView(android.R.id.list)
	ListView listView;

	@InjectView(R.id.ad_layout)
	LinearLayout adLayout;

	@InjectView(R.id.ad_fodder)
	View adFodder;
	
	private TripAdapter adapter;
	
	private Future<?> refreshFuture;
	
	private Runnable refresh = new Runnable() {
		@Override
		public void run() {
			runOnUiThread(uiRefresh);
		}
	};
	
	private Runnable uiRefresh = new Runnable() {
		public void run() {
			adapter.notifyDataSetChanged();
		};
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray("tripIds", tripIds);
		outState.putString("departId", departId);
		outState.putString("arriveId", arriveId);
		outState.putSerializable("schedule", schedule);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.station_to_station);
		if (showAds()) {
			adView = new AdView(this, AdSize.BANNER,
					getString(R.string.publisherId));
			AdRequest req = new AdRequest();
			final View orAd = getLayoutInflater()
					.inflate(R.layout.our_ad, null);
			int rand = 0 + (int) (Math.random() * 3);
			if (rand == 1) {
				adLayout.addView(orAd);
			}
			adLayout.addView(adView);
			adView.loadAd(req);
			adView.setAdListener(new AdListener() {

				@Override
				public void onDismissScreen(Ad arg0) {

				}

				@Override
				public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {

				}

				@Override
				public void onLeaveApplication(Ad arg0) {

				}

				@Override
				public void onPresentScreen(Ad arg0) {

				}

				@Override
				public void onReceiveAd(Ad arg0) {
					int index = adLayout.indexOfChild(orAd);
					if (index >= 0) {
						adLayout.removeViewAt(index);
					}
				}

			});
		}
		adFodder.setVisibility(View.GONE);
		if (savedInstanceState == null) {
			String id = getIntent().getStringExtra("tripId");
			schedule = provider.get();
			getSupportActionBar().setTitle("Trip #" + id);
			getSupportActionBar().setSubtitle(schedule.stopIdToName.get(schedule.departId) + " to " + schedule.stopIdToName.get(schedule.arriveId));

			StationInterval interval = schedule.getStationIntervalForTripId(id);
			Stack<StationInterval> intervals = new Stack<StationInterval>();
			List<TripInfo.Stop> stops = new ArrayList<TripInfo.Stop>();
			intervals.push(interval);
			while (!intervals.isEmpty()) {
				interval = intervals.pop();
				if (interval.tripId != null) {
					TripInfo tripInfo = dao.getStationTimesForTripId(
							interval.tripId, interval.departSequence,
							interval.arriveSequence);
					stops.addAll(tripInfo.stops);
				}
				if (interval.hasNext()) {
					intervals.push(interval.next());
				}
				//System.out.println(stops);
			}
			start = getIntent().getLongExtra("start", 0);
			Calendar startCal = Calendar.getInstance();
			startCal.setTimeInMillis(start);
			adapter = new TripAdapter(this, stops,schedule,startCal);
			listView.setAdapter(adapter);
			// TripInfo tinfo = dao.getStationTimesForTripId(id);
			// boolean foundFirst = false;
			// List<TripInfo.Stop> stops = new ArrayList<TripInfo.Stop>();
			// stops.addAll(tinfo.stops);
			//
			// while (interval.hasNext()) {
			// interval = interval.next();
			// tinfo = dao.getStationTimesForTripId(id,interval.departSequence,
			// interval.arriveSequence);
			// TripInfo.Stop first = null;
			// TripInfo.Stop last = null;
			// for (int i = 0; i < tinfo.stops.size(); i++) {
			// TripInfo.Stop curr = tinfo.stops.get(i);
			// if (first == null) {
			// if (interval.departId.equals(curr.id)) {
			// first = curr;
			// stops.add(curr);
			// for (int j = i + 1; j < tinfo.stops.size(); j++) {
			// curr = tinfo.stops.get(j);
			// if (interval.arriveId.equals(curr.id)) {
			// last = curr;
			// stops.add(last);
			// break;
			// } else {
			// stops.add(curr);
			// }
			// }
			// break;
			// }
			// }
			// }
			// }
		}

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

			}

		});
		// for(TripInfo in : info) {
		// for(TripInfo.Stop stop : in.stops) {
		// adapter.add(stop);
		// }
		// }

	}
	

	@Override
	protected void onPause() {
		super.onPause();
		
		if(refreshFuture!=null) {
			refreshFuture.cancel(true);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(refreshFuture!=null) {
			refreshFuture.cancel(true);
		}
		refreshFuture = ThreadHelper.getScheduler().scheduleAtFixedRate(refresh, 0, 1, TimeUnit.SECONDS);
	}

	private boolean showAds() {
		if(getResources().getBoolean(R.bool.paidApp)) {
			return false;
		}
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"showAds", true);
	}

	private void setShowAds(boolean show) {
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean("showAds", show).commit();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		tripIds = savedInstanceState.getStringArray("tripIds");
		departId = savedInstanceState.getString("departId");
		arriveId = savedInstanceState.getString("arriveId");
		schedule = (Schedule) savedInstanceState.getSerializable("schedule");
	}
}
