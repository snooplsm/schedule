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
    
    private InputStream stream;
    
    private Gson gson;
    
    private boolean cancelled = false;
    
    StringBuilder b = new StringBuilder();
    
    public void cancel() {
    	cancelled = true;
    }

    public DepartureVision() {
		gson = new Gson();
    }
    
    private LinkedHashSet<TrainStatusListener> listeners = new LinkedHashSet<TrainStatusListener>();
    
    public void addListener(TrainStatusListener listener) {
    	listeners.add(listener);
    }
    
    public void removeListener(TrainStatusListener listener) {
    	listeners.remove(listener);
    }
    
    public void stopDepartures() throws IOException {
    	if(stream!=null) {
    		stream.close();
    		stream = null;
    	}
    }

    public void startDepartures(String id) throws IOException {
    	stopDepartures();
    	cancelled = false;
    	URL u = new URL("http://technically.us:7979/njt/"+id.toLowerCase());
    	
		stream = u.openStream();    
		while (!cancelled) {
			int k = stream.read();
			if (k == -1) {
				continue;
			}
			char c = (char) k;
			if (c == '\n') {
				TrainStatus status = gson.fromJson(
						b.toString(),
						TrainStatus.class);
				if (status.getTrack() != null) {													
					status.setTrack(status.getTrack().replace("Track", ""));
				}
				if (status.getStatus() != null
						&& status.getStatus()
								.trim().length() == 0) {
					status.setStatus(null);
				}
				onTrainStatus(status);
				b.setLength(0);
			} else {
				b.append(c);
			}
		}
    }
    
    private void onTrainStatus(TrainStatus status) {
    	for(TrainStatusListener l : listeners) {
    		l.onTrainStatus(status);
    	}
    }
}
