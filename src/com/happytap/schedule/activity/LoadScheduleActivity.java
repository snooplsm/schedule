package com.happytap.schedule.activity;

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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.inject.Inject;
import com.happytap.schedule.domain.Schedule;
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
	
	@InjectView(R.id.ad_layout)
	LinearLayout adLayout;
	
	@InjectView(R.id.ad_fodder)
	View adFodder;
	
	
	public static final String DEPARTURE_STATION = "departure_station";
	public static final String ARRIVAL_STATION = "arrival_station";
	public static final String DEPARTURE_ID = "departure_id";
	public static final String ARRIVAL_ID = "arrival_id";
	public static final String DEPARTURE_DATE_START = "departure_date_start";
	public static final String DEPARTURE_DATE_END = "departure_date_end";
	
	private AdView adView;
	
	private AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>() {

		@Override
		protected Void doInBackground(Void... arg0) {
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
			dotdotdot.refreshDrawableState();
		}		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.loading);
		doBindService();
		departureText.setText(getIntent().getStringExtra(DEPARTURE_STATION));
		arrivalText.setText(getIntent().getStringExtra(ARRIVAL_STATION));
		adView = new AdView(this, AdSize.BANNER, getString(R.string.publisherId));
		adView.loadAd(new AdRequest());
		adFodder.setVisibility(View.GONE);
		adLayout.addView(adView);
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
        	System.out.println(obj + "" +  what);
        	if(what==ScheduleService.FOUND_SCHEDULE) {
        		new Thread() {
        			public void run() {
        				Intent intent = new Intent(LoadScheduleActivity.this, StationToStationActivity.class);
                		intent.putExtra(DEPARTURE_STATION, getIntent().getStringExtra(StationToStationActivity.DEPARTURE_STATION));
                		intent.putExtra(ARRIVAL_STATION, getIntent().getStringExtra(StationToStationActivity.ARRIVAL_STATION));
                		intent.putExtra(DEPARTURE_ID, getIntent().getStringExtra(StationToStationActivity.DEPARTURE_ID));
                		intent.putExtra(ARRIVAL_ID, getIntent().getStringExtra(StationToStationActivity.ARRIVAL_ID));
                		intent.putExtra(StationToStationActivity.SCHEDULE, (Schedule)obj);
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
            System.out.println("attached");
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
