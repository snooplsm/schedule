package com.happytap.schedule.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;

import roboguice.inject.InjectView;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import com.happytap.schedule.service.ScheduleService;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.njtransit.rail.R;

public class SplashScreenActivity extends ScheduleActivity {
	
	private static final int CHANGE_DATE_DIALOG=1970;
	
	public static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
	public static SimpleDateFormat LONG_DATE = new SimpleDateFormat("MMMM d, yyyy");
	
	private static final int TOAST = 1;
	
	MenuItem about;
	
	@InjectView(R.id.arrival)
	protected View arrival;
	
	private String arrivalStopId;
	
	@InjectView(R.id.arrivalText)
	private TextView arrivalText;
	
	@InjectView(R.id.scheduleEnd)
	private TextView scheduleEnd;
	
	private OnClickListener clickStationListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(SplashScreenActivity.this,StationListActivity.class);
			SplashScreenActivity.this.startActivityForResult(intent, v.getId());
		}
		
	};
	

	
	@Inject
	private DatabaseHelper databaseHelper;
	
	MenuItem departAt;	@InjectView(R.id.departure)
	protected View departure;	@InjectView(R.id.departureDate)
	TextView departureDateText;;
	
	private String departureStopId;
	
	@InjectView(R.id.departureText)
	private TextView departureText;
	
	LinkedHashSet<Character> enabledCharacters;
	
	@InjectView(R.id.get_schedule)
	protected View getSchedule;
	
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

	private boolean canShowScheduleExpiration() {
		return getSharedPreferences(getApplication().getPackageName()+"_preferences", Context.MODE_PRIVATE).getBoolean("showScheduleExpiration", false);
	}

	
	@Inject
	Injector injector;
	
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
	

	private OnLongClickListener longClickStationListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			openContextMenu(v);
			return true;
		}
		
	};
	

	
	private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            System.out.println("attached");
            try {
                Message msg = Message.obtain(null,
                        ScheduleService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;               
                mService.send(msg);
                msg = Message.obtain(null, ScheduleService.CHECK_FOR_UPGRADE);
                msg.replyTo = mMessenger;                
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

	boolean mIsBound;
	
	class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final Object obj = msg.obj;
        	int what = msg.what;        	
//        	if(what==ScheduleService.FOUND_SCHEDULE) {
//        		new Thread() {
//        			public void run() {
//        				Intent intent = new Intent(LoadScheduleActivity.this, StationToStationActivity.class);
//                		intent.putExtra(DEPARTURE_STATION, getIntent().getStringExtra(StationToStationActivity.DEPARTURE_STATION));
//                		intent.putExtra(ARRIVAL_STATION, getIntent().getStringExtra(StationToStationActivity.ARRIVAL_STATION));
//                		intent.putExtra(DEPARTURE_ID, getIntent().getStringExtra(StationToStationActivity.DEPARTURE_ID));
//                		intent.putExtra(ARRIVAL_ID, getIntent().getStringExtra(StationToStationActivity.ARRIVAL_ID));
//                		intent.putExtra(StationToStationActivity.SCHEDULE, (Schedule)obj);
//                		startActivity(intent);
//                		finish();
//        			};
//        		}.start();
//        		
//        	}
        }
    }
	
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	private Messenger mService = null;
	
	@Inject
	SharedPreferences preferences;	MenuItem preferencesItem;	@Inject
	private PreferencesDatabaseProvider preferencesProvider;

	@Inject
	private SQLiteDatabaseProvider provider;

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
	
	MenuItem share;;
	
	@InjectView(R.id.splash)
	private View splashContainer;
	
	@InjectView(R.id.loading)
	private ImageView splashImage;
	
	private Calendar userDefinedDate;
	
	private void displayDate() {
		if(DateUtils.isToday(userDefinedDate.getTimeInMillis())) {
			departureDateText.setText("for Today");
		} else {
			departureDateText.setText("for " + df.format(userDefinedDate.getTime()));
		}	
	}
	void doBindService() {
    	boolean bound =  bindService(new Intent(SplashScreenActivity.this,
                ScheduleService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
	
	void doUnbindService() {
    if (mIsBound) {
        if (mService != null) {
            try {
                Message msg = Message.obtain(null,
                        ScheduleService.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }

        unbindService(mConnection);
        mIsBound = false;
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
	
	private void showScheduleEnd(boolean toast) {
		Long min = preferences.getLong("minimumCalendarDate", 0);
		Long max = preferences.getLong("maximumCalendarDate",0);
		if(min!=0 && max!=0) {
			long diff = max - System.currentTimeMillis();
			if(diff<=0) {
				if(toast)
					Toast.makeText(SplashScreenActivity.this, "Bro, your schedule is out of date, download an update!", Toast.LENGTH_LONG).show();
			} else {
				long days = diff/86400000;
				if(toast)
					Toast.makeText(SplashScreenActivity.this, String.format("schedule is good for %s days or until changed",days), Toast.LENGTH_LONG).show();
				if(canShowScheduleExpiration()) {
					scheduleEnd.setText("valid til " + LONG_DATE.format(new Date(max)));
					scheduleEnd.setVisibility(View.VISIBLE);
				} else {
					scheduleEnd.setVisibility(View.GONE);
				}
			}
		} else {
			scheduleEnd.setVisibility(View.GONE);
		}
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
		if(preferences.getBoolean("checkForUpdates", true)) {
			//doBindService();
		}
		showScheduleEnd(true);
		onActivityResult(0,0,null);
	}

	
	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
		PreferencesDao dao = injector.getInstance(PreferencesDao.class);
		
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
	}    @Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
		loadingTask.cancel(false);
	}

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
	
	protected void onResume() {
		super.onResume();
		if(loadingTask.getStatus()==Status.PENDING) {
			loadingTask.execute();
		}
		displayDate();
		showScheduleEnd(false);
		
		
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
