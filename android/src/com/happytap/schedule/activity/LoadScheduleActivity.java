package com.happytap.schedule.activity;

import java.text.DecimalFormat;
import java.util.Calendar;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.ads.AdView;
import com.google.inject.Inject;
import com.happytap.schedule.database.ScheduleDao;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.provider.CurrentScheduleProvider;
import com.happytap.schedule.service.ScheduleService;
import com.njtransit.rail.R;

public class LoadScheduleActivity extends RoboActivity {
	
	private Messenger mService = null;
	
	boolean mIsBound;
	
	@Inject
	private SharedPreferences pref;
	
	@InjectView(R.id.loading_dot_dot)
	private TextView dotdotdot;
	
	@InjectView(R.id.departureText)
	private TextView departureText;
	
	@InjectView(R.id.arrivalText)
	private TextView arrivalText;
	
	@InjectView(R.id.fare)
	private TextView fareText;
	
	@InjectView(R.id.ad_layout)
	LinearLayout adLayout;
	
	@InjectView(R.id.ad_fodder)
	View adFodder;
	
	@Inject
	ScheduleDao dao;
	
	@Inject
	CurrentScheduleProvider scheduleProvider;
	
	
	public static final String DEPARTURE_STATION = "departure_station";
	public static final String ARRIVAL_STATION = "arrival_station";
	public static final String DEPARTURE_ID = "departure_id";
	public static final String ARRIVAL_ID = "arrival_id";
	public static final String DEPARTURE_DATE_START = "departure_date_start";
	public static final String DEPARTURE_DATE_END = "departure_date_end";
	
	private AdView adView;
	
private static DecimalFormat df = new DecimalFormat("$0.00");
	
	private AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {
		
		Double fare;
	
		
		@Override
		protected Void doInBackground(Void... arg0) {
			String departId = getIntent().getStringExtra(StationToStationActivity.DEPARTURE_ID);
			String arriveId = getIntent().getStringExtra(StationToStationActivity.ARRIVAL_ID);
			fare = dao.getFair(departId,arriveId);
			publishProgress();
			while(!isCancelled()) {
				publishProgress();
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
				}
			}
			return null;
		}
		
		private boolean reverse;
		
		protected void onProgressUpdate(Void... values) {
			if(dotdotdot.getText().length()<3) {
				if(!reverse) {
					dotdotdot.setText(dotdotdot.getText()+".");
				} else {
					if(dotdotdot.getText().length()==0) {
						dotdotdot.setText(".");
						reverse = !reverse;
					} else {
						dotdotdot.setText(dotdotdot.getText().subSequence(0, dotdotdot.getText().length()-1));
					}
				}
				
								
			} else {
				reverse = !reverse;
				dotdotdot.setText("..");
			}
			if(fare!=null) {
				fareText.setVisibility(View.VISIBLE);
				fareText.setText("Fare: " + df.format(fare));
			}
			dotdotdot.refreshDrawableState();
		}		
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.loading);		
		doBindService();
		fareText.setVisibility(View.INVISIBLE);
		departureText.setText(getIntent().getStringExtra(DEPARTURE_STATION));
		arrivalText.setText(getIntent().getStringExtra(ARRIVAL_STATION));
		
		if(showAds()) {
			final View orAd =  getLayoutInflater().inflate(R.layout.our_ad, null);
			adFodder.setVisibility(View.GONE);
			adLayout.addView(orAd);			
		} else {
			adLayout.setVisibility(View.GONE);
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		task.cancel(true);
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		task.execute();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		task.cancel(true);
		doUnbindService();
	}
	
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	final Object obj = msg.obj;
        	int what = msg.what;
        	if(what==ScheduleService.FOUND_SCHEDULE) {
        		new Thread() {
        			public void run() {
        				Intent intent = new Intent(LoadScheduleActivity.this, StationToStationActivity.class);
                		intent.putExtra(DEPARTURE_STATION, getIntent().getStringExtra(StationToStationActivity.DEPARTURE_STATION));
                		intent.putExtra(ARRIVAL_STATION, getIntent().getStringExtra(StationToStationActivity.ARRIVAL_STATION));
                		intent.putExtra(DEPARTURE_ID, getIntent().getStringExtra(StationToStationActivity.DEPARTURE_ID));
                		intent.putExtra(ARRIVAL_ID, getIntent().getStringExtra(StationToStationActivity.ARRIVAL_ID));
                		Schedule schedule = (Schedule)obj;
                		scheduleProvider.schedule = schedule;
                		startActivity(intent);
                		finish();
        			};
        		}.start();
        		
        	}
        }
    }

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null,
                        ScheduleService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;               
                mService.send(msg);
                msg = Message.obtain(null, ScheduleService.GET_SCHEDULE);
                msg.replyTo = mMessenger;
                msg.obj = new ScheduleService.ScheduleRequest(getIntent().getStringExtra(DEPARTURE_ID), getIntent().getStringExtra(DEPARTURE_STATION), getIntent().getStringExtra(ARRIVAL_ID), getIntent().getStringExtra(ARRIVAL_STATION), (Calendar)getIntent().getSerializableExtra(DEPARTURE_DATE_START), (Calendar)getIntent().getSerializableExtra(DEPARTURE_DATE_END));
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    void doBindService() {
    	boolean bound =  bindService(new Intent(LoadScheduleActivity.this,
                ScheduleService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
	private boolean showAds() {
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("showAds", true);
	}

	private void setShowAds(boolean show) {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("showAds", show)
				.commit();
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
	
}
