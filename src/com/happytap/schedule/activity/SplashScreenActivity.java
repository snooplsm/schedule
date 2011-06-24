package com.happytap.schedule.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashSet;

import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.AsyncTask.Status;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.googlecode.android.widgets.DateSlider.DateSlider;
import com.googlecode.android.widgets.DateSlider.DefaultDateSlider;
import com.googlecode.android.widgets.DateSlider.DateSlider.OnDateSetListener;
import com.happytap.schedule.database.DatabaseHelper;
import com.happytap.schedule.database.PreferencesDao;
import com.happytap.schedule.database.DatabaseHelper.InstallDatabaseMeter;
import com.happytap.schedule.provider.PreferencesDatabaseProvider;
import com.happytap.schedule.provider.SQLiteDatabaseProvider;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.njtransit.rail.R;

public class SplashScreenActivity extends ScheduleActivity {
	
	private static final int CHANGE_DATE_DIALOG=1970;
	
	public static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
	
	@InjectView(R.id.arrival)
	protected View arrival;
	
	@InjectView(R.id.get_schedule)
	protected View getSchedule;
	
	private String arrivalStopId;
	
	@InjectView(R.id.arrivalText)
	private TextView arrivalText;
	
	@InjectView(R.id.splash)
	private View splashContainer;
	
	@InjectView(R.id.loading)
	private ImageView splashImage;
	
	@Inject
	SharedPreferences preferences;
	
	private OnClickListener clickStationListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(SplashScreenActivity.this,StationListActivity.class);
			SplashScreenActivity.this.startActivityForResult(intent, v.getId());
		}
		
	};
	
	private OnLongClickListener longClickStationListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			openContextMenu(v);
			return true;
		}
		
	};
	
	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
		PreferencesDao dao = injector.getInstance(PreferencesDao.class);
		
	};
	
	@Inject
	Injector injector;
	
	@Inject
	private DatabaseHelper databaseHelper;
	
	MenuItem departAt;
	
	MenuItem share;
	
	@InjectView(R.id.departure)
	protected View departure;
	
	@InjectView(R.id.departureDate)
	TextView departureDateText;
	
	private String departureStopId;
	@InjectView(R.id.departureText)
	private TextView departureText;
	
	LinkedHashSet<Character> enabledCharacters;	
	
	@Inject
	private SQLiteDatabaseProvider provider;
	
	@Inject
	private PreferencesDatabaseProvider preferencesProvider;
	
	@InjectView(R.id.reverse)
	private ImageView reverse;
	
	private OnClickListener reverseListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			CharSequence temp = arrivalText.getText();
			if(departureStopId!=null) {
				arrivalText.setText(departureText.getText());
			} else {
				arrivalText.setText(getString(R.string.arrival_text));
			}
			if(arrivalStopId!=null) { 
				departureText.setText(temp);
			} else {
				departureText.setText(getString(R.string.departure_text));
			}
			temp = arrivalStopId;
			SplashScreenActivity.this.arrivalStopId = departureStopId;
			if(temp!=null) {
				SplashScreenActivity.this.departureStopId = temp.toString();
			} else {
				SplashScreenActivity.this.departureStopId = null;
			}						
		}
		
	};
	
	private OnClickListener getScheduleClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(departureStopId!=null && arrivalStopId!=null) {
				
			} else {
				Toast.makeText(SplashScreenActivity.this, "choose stations", Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = new Intent(SplashScreenActivity.this, LoadScheduleActivity.class);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION, departureText.getText());
			intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION, arrivalText.getText());			
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START, userDefinedDate);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, departureStopId);
			intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, arrivalStopId);
			if(DateUtils.isToday(userDefinedDate.getTimeInMillis())) {
				Calendar tom = Calendar.getInstance();
				tom.setTimeInMillis(userDefinedDate.getTimeInMillis());
				tom.add(Calendar.DAY_OF_YEAR,1);
				intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
			} else {
				intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, userDefinedDate);
			}
			startActivity(intent);				
		}
		
	};

	private Calendar userDefinedDate;

	private void displayDate() {
		if(DateUtils.isToday(userDefinedDate.getTimeInMillis())) {
			departureDateText.setText("for Today");
		} else {
			departureDateText.setText("for " + df.format(userDefinedDate.getTime()));
		}	
	}

	protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
		if(resultCode==RESULT_OK) {
			String stopId = data.getStringExtra(StationListActivity.STOP_ID);
			String name = data.getStringExtra(StationListActivity.STOP_NAME);
			
			TextView text;
			
			if(requestCode==R.id.arrival) {
				text = arrivalText;
				arrivalStopId = stopId;
				preferences.edit().putString("lastArrivalStopId", stopId).putString("lastArrivalStopText", name).commit();
			} else {
				text = departureText;
				departureStopId = stopId;
				preferences.edit().putString("lastDepartureStopId",stopId).putString("lastDepartureStopText", name).commit();
			}
			text.setText(name);
		}
		
	}
	
	private AsyncTask<Void,Message,Void> loadingTask = new AsyncTask<Void,Message,Void>() {

		@Override
		protected Void doInBackground(Void... params) {			
			databaseHelper.setInstallMeter(new InstallDatabaseMeter() {

				@Override
				public void onBeforeCopy() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onFinishedCopying() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onPercentCopied(long copySize, float percent,
						long totalBytesCopied) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onSizeToBeCopiedCalculated(long copySize) {
					// TODO Auto-generated method stub
					
				}
				
			});
			provider.get();
			preferencesProvider.get();
			return null;
		}
		 
		protected void onPostExecute(Void result) {
			splashContainer.setVisibility(View.GONE);
		};
	};
	
	protected void onResume() {
		super.onResume();
		if(loadingTask.getStatus()==Status.PENDING) {
			loadingTask.execute();
		}
	};
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		loadingTask.cancel(false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.main);
		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.newjersey);
		splashImage.setImageDrawable(svg.createPictureDrawable());
		svg = SVGParser.getSVGFromResource(getResources(), R.raw.reload);
		reverse.setImageDrawable(svg.createPictureDrawable());	
		arrival.setOnClickListener(clickStationListener);		
		departure.setOnClickListener(clickStationListener);
		reverse.setOnClickListener(reverseListener);
		getSchedule.setOnClickListener(getScheduleClickListener);
		userDefinedDate = Calendar.getInstance();		
		
		if(savedInstanceState==null) {
			arrivalStopId = preferences.getString("lastArrivalStopId", null);
			departureStopId = preferences.getString("lastDepartureStopId", null);
			arrivalText.setText(preferences.getString("lastArrivalStopText",getString(R.string.arrival_text)));
			departureText.setText(preferences.getString("lastDepartureStopText",getString(R.string.departure_text)));
		}
		onActivityResult(0,0,null);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id==CHANGE_DATE_DIALOG) {
			return new DefaultDateSlider(this,new OnDateSetListener() {

				@Override
				public void onDateSet(DateSlider view, Calendar selectedDate) {
					userDefinedDate = selectedDate;		
					displayDate();
				}
				
			},userDefinedDate);
		}
		return super.onCreateDialog(id);
	}
	
	MenuItem preferencesItem;
	MenuItem about;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		departAt = menu.add("Depart on");
		departAt.setIcon(android.R.drawable.ic_menu_month);
		share = menu.add("Share");
		share.setIcon(android.R.drawable.ic_menu_share);
		preferencesItem = menu.add("Preferences");
		preferencesItem.setIcon(android.R.drawable.ic_menu_preferences);
		about = menu.add("About");
		about.setIcon(android.R.drawable.ic_menu_info_details);
		return super.onCreateOptionsMenu(menu);
	}
	

	
	private static final int TOAST = 1;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		if(item.equals(departAt)) {
			showDialog(CHANGE_DATE_DIALOG);
		}
		if(item.equals(share)) {
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			Resources r = getResources();
			String url = r.getString(R.string.application_url);
			String name = r.getString(R.string.app_name);
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, name  + " " + url);
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Get " + name + " for Android @ " + url);
			startActivity(Intent.createChooser(shareIntent, "Share"));
		}
		if(item.equals(preferencesItem)) {
			Intent intent = new Intent(this, SchedulePreferenceActivity.class);
			startActivityForResult(intent, 0);
		}
		if(item.equals(about)) {
			Intent intent = new Intent(this,AboutActivity.class);
			startActivity(intent);
		}
		return true;
	}

	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(savedInstanceState!=null) {
			String departureStopId = savedInstanceState.getString("departureStopId");
			if(departureStopId!=null) {
				this.departureStopId = departureStopId;
				departureText.setText(savedInstanceState.getString("departureStopName"));
			}
			String arrivalStopId = savedInstanceState.getString("arrivalStopId");
			if(arrivalStopId!=null) {
				this.arrivalStopId = arrivalStopId;
				arrivalText.setText(savedInstanceState.getString("arrivalStopName"));
			}
		} else {
			
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("departureStopId", departureStopId);
		outState.putString("arrivalStopId",arrivalStopId);
		outState.putString("departureStopName", departureText.getText().toString());
		outState.putString("arrivalStopName", arrivalText.getText().toString());
	};

}
