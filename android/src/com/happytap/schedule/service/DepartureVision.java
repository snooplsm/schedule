package com.happytap.schedule.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashSet;

import com.google.gson.Gson;
import com.happytap.schedule.domain.TrainStatus;

/**
 * Created by IntelliJ IDEA.
 * User: rgravener
 * Date: 2/14/11
 * Time: 11:50 PM
 */
public class DepartureVision {

	public interface TrainStatusListener {
		
		void onTrainStatus(TrainStatus status);
		
	}
    
	private DeparturePoller poller;
    
    StringBuilder b = new StringBuilder();
    
    public void cancel() {
    	poller = null;
    }

    public DepartureVision() {		
    }
    
    private LinkedHashSet<TrainStatusListener> listeners = new LinkedHashSet<TrainStatusListener>();
    
    public void addListener(TrainStatusListener listener) {
    	listeners.add(listener);
    }
    
    public void removeListener(TrainStatusListener listener) {
    	listeners.remove(listener);
    }
    
    private boolean cancelled;
    
    public void stopDepartures() throws IOException {
    	poller = null;
    	cancelled = true;
    }

    public void startDepartures(String id) throws IOException {
    	stopDepartures();
    	poller = new DeparturePoller();
    	cancelled = false;
		while (!cancelled) {
			for(TrainStatus status : poller.getTrainStatuses(id)) {
				onTrainStatus(status);
			}
			try {
				Thread.sleep(9000);
			} catch (InterruptedException e) {
				cancelled = true;
			}
		}
    }
    
    private void onTrainStatus(TrainStatus status) {
    	for(TrainStatusListener l : listeners) {
    		l.onTrainStatus(status);
    	}
    }
}
