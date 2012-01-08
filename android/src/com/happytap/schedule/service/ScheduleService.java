package com.happytap.schedule.service;

import java.util.ArrayList;
import java.util.Calendar;

import roboguice.service.RoboService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.google.inject.Inject;
import com.happytap.schedule.database.PreferencesDao;
import com.happytap.schedule.database.ScheduleDao;
import com.happytap.schedule.domain.Schedule;

public class ScheduleService extends RoboService {

	public static final int GET_SCHEDULE = 3;
	
	public static final int CHECK_FOR_UPGRADE = 4;
		
	@Inject
	private ScheduleDao scheduleDao;
	
	@Inject
	private PreferencesDao preferencesDao;
	
	public static class ScheduleRequest {

		private final String departureStopId;
		private final String arrivalStopId;
		private final String departureStopName;
		private final String arrivalStopName;
		private final Calendar departureDateStart;
		
		public ScheduleRequest(String departureStopId, String departureStopName, String arrivalStopId, String arrivalStopName,
				Calendar departureDateStart, Calendar departureDateEnd) {
			super();
			this.departureStopId = departureStopId;
			this.arrivalStopId = arrivalStopId;
			this.departureDateStart = departureDateStart;
			this.departureDateEnd = departureDateEnd;
			this.departureStopName = departureStopName;
			this.arrivalStopName = arrivalStopName;
		}
		private final Calendar departureDateEnd;

		@Override
		public String toString() {
			return "ScheduleRequest ["
					+ (arrivalStopId != null ? "arrivalStopId=" + arrivalStopId
							+ ", " : "")
					+ (departureDateEnd != null ? "departureDateEnd="
							+ departureDateEnd + ", " : "")
					+ (departureDateStart != null ? "departureDateStart="
							+ departureDateStart + ", " : "")
					+ (departureStopId != null ? "departureStopId="
							+ departureStopId : "") + "]";
		}
		
		
	}
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case GET_SCHEDULE:
				getSchedule((ScheduleRequest)msg.obj);
				break;
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case CHECK_FOR_UPGRADE:
				//checkForUpgrade();
				break;
			}
		}
	}
	
    private void sendMessage(int code, Object msg) {

        for(int i = mClients.size()-1;i>=0;i--) {
            Messenger messenger = mClients.get(i);
            try {
                messenger.send(Message.obtain(null,code,msg));
            } catch (RemoteException e) {
                mClients.remove(i);
            }
        }
    }
    
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    public static final int MSG_REGISTER_CLIENT = 1;

    public static final int MSG_UNREGISTER_CLIENT = 2;
    
    public static final int FOUND_SCHEDULE=4;
	
	private void getSchedule(final ScheduleRequest req) {
		new Thread() {
			
			public void run() {
				Schedule schedule = scheduleDao.getSchedule(req.departureStopId, req.arrivalStopId, req.departureDateStart.getTime(), req.departureDateEnd.getTime());
				preferencesDao.saveScheduleRequest(req.departureStopId, req.departureStopName, req.arrivalStopId, req.arrivalStopName);
				sendMessage(FOUND_SCHEDULE, schedule);
			}
		}.start();
	}
	
	Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

}
