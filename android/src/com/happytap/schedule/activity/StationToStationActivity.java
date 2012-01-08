package com.happytap.schedule.activity;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import roboguice.inject.InjectView;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.inject.Inject;
import com.happytap.schedule.adapter.ScheduleAdapter;
import com.happytap.schedule.dialog.AlarmTimeDialog;
import com.happytap.schedule.dialog.AlarmTimeDialog.AlarmTimeListener;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.StationToStation;
import com.happytap.schedule.domain.TrainStatus;
import com.happytap.schedule.service.DepartureVision;
import com.happytap.schedule.service.DepartureVision.TrainStatusListener;
import com.happytap.schedule.util.date.DateUtils;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.njtransit.rail.R;

public class StationToStationActivity extends ScheduleActivity implements
		OnItemLongClickListener, OnItemClickListener, OnClickListener {

	@InjectView(android.R.id.list)
	ListView listView;

	@InjectView(R.id.ad_layout)
	LinearLayout adLayout;

	@InjectView(R.id.ad_fodder)
	View adFodder;

	@InjectView(R.id.departureText)
	TextView departureText;
	
	@InjectView(R.id.arrivalText)
	TextView arrivalText;
	
	@InjectView(R.id.reverse)
	ImageView reverse;
	
	@InjectView(R.id.departure)
	View departureView;
	
	@Inject
	AlarmManager alarmManager;

	@Inject
	NotificationManager notifications;

	AdView adView;

	public static final String SCHEDULE = Schedule.class.getName();
	public static final String DEPARTURE_STATION = "departure_station";
	public static final String ARRIVAL_STATION = "arrival_station";
	public static final String DEPARTURE_ID = "departure_id";
	public static final String ARRIVAL_ID = "arrival_id";
	public static final String DEPARTURE_DATE_START = "departure_date_start";
	public static final String DEPARTURE_DATE_END = "departure_date_end";
	
	private String departureStopId;
	private String arrivalStopId;

	private AsyncTask<Void, TrainStatus, Void> departureVisionTask;
	
	private boolean useDepartureVision() {
		Calendar start = Calendar.getInstance();
		start.setTime(schedule.start);
		return getSharedPreferences(getApplication().getPackageName()+"_preferences", Context.MODE_PRIVATE).getBoolean("useDepartureVision", true) && DateUtils.isToday(start);
	}

	boolean paused;
	
	private AsyncTask<Void, Integer, Void> last;
	
	protected void onResume() {
		super.onResume();
		paused = false;
		last = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					while(!isCancelled()) {
						Calendar c = Calendar.getInstance();
						c.add(Calendar.MINUTE, 1);
						c.set(Calendar.SECOND, 0);
						long diff = c.getTimeInMillis()-System.currentTimeMillis();
						if(diff<=0) {
							diff = 20000;
						}
						Thread.sleep(diff);
						publishProgress(1);
					}
				} catch (Exception e) {
					
				}
				return null;
			}
			
			@Override
			protected void onProgressUpdate(Integer... values) {
				listView.invalidate();
			}
		};
		listView.invalidate();
		if(!useDepartureVision()) {
			return;
		}
		departureVisionTask = newDepartureVisionTask();
		departureVisionTask.execute();
	}
	
	private AsyncTask<Void,TrainStatus,Void> newDepartureVisionTask() {
		return new AsyncTask<Void, TrainStatus, Void>() {

			private DepartureVision vision;

			@Override
			protected Void doInBackground(Void... arg0) {

				vision = new DepartureVision();
				vision.addListener(new TrainStatusListener() {

					@Override
					public void onTrainStatus(TrainStatus status) {
						publishProgress(status);
					}

				});
				try {
					String stationId = departureStopId;
					String[] abbreviatedNames = getResources().getStringArray(
							R.array.abbreviated_names);
					String start = stationId + ",";
					String abbreviatedName = null;
					for (String name : abbreviatedNames) {
						if (name.startsWith(start)) {
							abbreviatedName = name.split(",")[1];
							break;
						}
					}
					if (abbreviatedName != null) {
						vision.startDepartures(abbreviatedName);
					}
				} catch (IOException e) {
				}
				return null;
			}

			protected void onProgressUpdate(TrainStatus... values) {
				ScheduleAdapter adapter = (ScheduleAdapter) listView
						.getAdapter();
				adapter.onStatus(values[0]);
			};

			protected void onCancelled() {
				if (vision != null) {
					vision.cancel();
				}
			}
		};
	}

	protected void onPause() {
		super.onPause();
		if(last!=null) {
			last.cancel(false);
		}
		if(!useDepartureVision()) {
			return;
		}
		departureVisionTask.cancel(true);
	}

	MenuItem shareItem;
	
	MenuItem rate;
	
	MenuItem email;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		shareItem = menu.add("Share");
		shareItem.setIcon(getResources().getDrawable(
				android.R.drawable.ic_menu_share));
		rate = menu.add("Rate");
		rate.setIcon(R.drawable.ic_menu_star);
		email = menu.add("Email us");
		email.setIcon(android.R.drawable.ic_menu_send);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if(listView!=null && listView.getAdapter()!=null && listView.getAdapter().getCount()>0) {
			rate.setVisible(true);
			shareItem.setVisible(true);
		} else {
			shareItem.setVisible(false);
			rate.setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.equals(shareItem)) {
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			Resources r = getResources();
			String url = r.getString(R.string.application_url);
			String name = r.getString(R.string.app_name);
			String date = new SimpleDateFormat("MMMM dd, yyyy")
					.format(schedule.end);
			String depart = getIntent().getStringExtra(DEPARTURE_STATION);
			String arrive = getIntent().getStringExtra(ARRIVAL_STATION);
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "["
					+ name + "] " + depart + " to " + arrive + " " + date);
			StringBuilder b = new StringBuilder();
			b.append(depart + " to " + arrive + " for " + date).append("\n\n");
			ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
			DateFormat df = new SimpleDateFormat("MM/dd/yy");
			boolean tomorrow = false;
			for (int i = 0; i < adapter.getCount(); i++) {
				StationToStation sts = adapter.getItem(i);
				if (!DateUtils.isToday(sts.departTime)) {
					if (!tomorrow) {
						b.append('\n');
					}
					tomorrow = true;
				}
				b.append(ScheduleAdapter.time(sts));
				if (!DateUtils.isToday(sts.departTime)) {
					b.append(" (").append(df.format(sts.departTime.getTime()))
							.append(")");
				}
				b.append('\n');
			}
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, b
					.toString());
			startActivity(Intent.createChooser(shareIntent, "Share"));
			return true;
		}
		if (item.equals(share)) {
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			String depart = getIntent().getStringExtra(DEPARTURE_STATION);
			String arrive = getIntent().getStringExtra(ARRIVAL_STATION);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, depart
					+ " to " + arrive + " " + currentItemDescription);
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, depart
					+ " to " + arrive + " " + currentItemDescription);
			startActivity(Intent.createChooser(shareIntent, "Share"));
			return true;
		}
		StationToStation sts = getAdapter().getItem(currentItemPosition);
		if (item.equals(alarmArrive)) {
			showDialog(DIALOG_ARRIVE);
			return true;
		}
		if (item.equals(alarmDepart)) {
			showDialog(DIALOG_DEPART);
			return true;
		}
		if(item.equals(rate)) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
			startActivity(intent);
			return true;
		}
		if(item.equals(email)) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("plain/text");
			i.putExtra(Intent.EXTRA_EMAIL, new String[] {"njtransitrail-feedback@wmwm.us"});
			StringBuilder b= new StringBuilder("NJTransit Rail Feedback " + getIntent().getStringExtra(DEPARTURE_STATION) + " : " + getIntent().getStringExtra(ARRIVAL_STATION));
			try {
				b.append(" version:" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			} catch (Exception e) {
				
			}
			i.putExtra(Intent.EXTRA_SUBJECT, b.toString());
			i.putExtra(Intent.EXTRA_TEXT, "");
			startActivity(i);
			return true;
		}
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.station_to_station);

		adView = new AdView(this, AdSize.BANNER,
				getString(R.string.publisherId));
		AdRequest req = new AdRequest();
		final View orAd =  getLayoutInflater().inflate(R.layout.our_ad, null);
		int rand = 0 + (int)(Math.random()*3);
		if(rand==1) {
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
				if(index>=0) {
					adLayout.removeViewAt(index);
				}
			}
			
		});
		adFodder.setVisibility(View.GONE);

		schedule = (Schedule) getIntent().getSerializableExtra(SCHEDULE);
		ScheduleAdapter adapter;
		listView.setAdapter(adapter = new ScheduleAdapter(this, schedule));
		registerForContextMenu(listView);
		listView.setOnItemLongClickListener(this);
		listView.setOnItemClickListener(this);
		listView.setItemsCanFocus(true);
		int index = adapter.findIndexOfCurrent();
		if(index>0) {
			listView.setSelectionFromTop(index-1, 0);
		}
		
		departureStopId = getIntent().getStringExtra(DEPARTURE_ID);
		arrivalStopId = getIntent().getStringExtra(ARRIVAL_ID);
		departureText.setText(getIntent().getStringExtra(DEPARTURE_STATION));
		arrivalText.setText(getIntent().getStringExtra(ARRIVAL_STATION));
		
		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.reload);
		reverse.setImageDrawable(svg.createPictureDrawable());
		departureView.setOnClickListener(this);
		
	}

	private static final int DIALOG_ARRIVE = 1;
	private static final int DIALOG_DEPART = 2;

	@Override
	protected Dialog onCreateDialog(final int id) {
		final StationToStation sts = getAdapter().getItem(currentItemPosition);
		Calendar alarm = id==DIALOG_DEPART ? sts.departTime : sts.arriveTime;
		final Calendar alarmTime = Calendar.getInstance();
		alarmTime.setTimeInMillis(alarm.getTimeInMillis());
		AlarmTimeDialog d = 
		new AlarmTimeDialog(this, new AlarmTimeListener() {

			@Override
			public void onMinutesBefore(int mins) {				
				alarmTime.add(Calendar.MINUTE, -mins);
				doAlarm(id==DIALOG_DEPART ? AlarmActivity.TYPE_DEPART : AlarmActivity.TYPE_ARRIVE, alarmTime);
			}
			
		});
		d.setTitle(id==DIALOG_DEPART ? ("Set depart alarm " + ScheduleAdapter.time.format(sts.departTime.getTime())) : ("Arrive alarm " + ScheduleAdapter.time.format(sts.arriveTime.getTime()))); 
		return d;
	}

	private Schedule schedule;

	@Override
	protected void onDestroy() {
		adView.destroy();
		super.onDestroy();
	}

	private int currentItemPosition;
	private String currentItemDescription;

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		// getCurrentStopId(adapter,position);
		currentItemPosition = position;
		currentItemDescription = ""
				+ ((TextView) view.findViewById(R.id.time)).getText();
		openContextMenu(adapterView);
		return true;
	}

	MenuItem alarmDepart;
	MenuItem alarmArrive;
	MenuItem share;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(currentItemDescription);
		alarmDepart = menu.add("Add Depart Alarm");
		alarmArrive = menu.add("Add Arrive Alarm");
		final StationToStation sts = getAdapter().getItem(currentItemPosition);
		if(sts.arriveTime.before(Calendar.getInstance())) {
			alarmArrive.setVisible(false);
		}
		if(sts.departTime.before(Calendar.getInstance())) {
			alarmDepart.setVisible(false);
		}
		share = menu.add("Share");
		share.setIcon(getResources().getDrawable(
				android.R.drawable.ic_menu_share));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	void doAlarm(String type, Calendar time) {
		Intent intent = new Intent(this, AlarmActivity.class);
		intent.putExtra(AlarmActivity.TYPE, AlarmActivity.TYPE_ARRIVE);
		if (AlarmActivity.TYPE_ARRIVE.equals(type)) {
			intent.putExtra(AlarmActivity.TIME, time);
		} else {
			intent.putExtra(AlarmActivity.TIME, time);
		}
		PendingIntent pi = PendingIntent.getActivity(this, 1, intent, 0);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return false;
	}

	private ScheduleAdapter getAdapter() {
		return (ScheduleAdapter) listView.getAdapter();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onClick(View arg0) {
		boolean departureVision = useDepartureVision();
		if(departureVision) {
			departureVisionTask.cancel(true);
		}
//		CharSequence temp = departureText.getText();
//		departureText.setText(arrivalText.getText());
//		arrivalText.setText(temp);
//		String temp2 = departureStopId;
//		departureStopId = arrivalStopId;
//		arrivalStopId = temp2;
		Intent intent = new Intent(StationToStationActivity.this, LoadScheduleActivity.class);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION, arrivalText.getText());
		intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION, departureText.getText());		
		Calendar c = Calendar.getInstance();
		c.setTime(schedule.start);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START, c);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, arrivalStopId);
		intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, departureStopId);
		if(DateUtils.isToday(c)) {
			Calendar tom = Calendar.getInstance();
			tom.setTimeInMillis(c.getTimeInMillis());
			tom.add(Calendar.DAY_OF_YEAR,1);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
		} else {
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, c);
		}
		startActivity(intent);
		
	}
}
