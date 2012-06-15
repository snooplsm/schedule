package com.happytap.schedule.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.TrainStatus;

public class MetroNorthPoller extends DeparturePoller{

	public List<TrainStatus> getTrainStatuses(Schedule schedule, String station) throws IOException {
		URL url = null;
		try {
			url = new URL("http://as0.mta.info/mnr/mstations/station_status_display.cfm");
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", 
			           "application/x-www-form-urlencoded");
			String urlParams = "P_AVIS_ID="+schedule.departId;
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Length", "" + urlParams.getBytes().length);
			conn.setReadTimeout(20000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7");
			OutputStream os = conn.getOutputStream();
			os.write(urlParams.getBytes());
			os.flush();
			int resp = conn.getResponseCode();
			if(resp!=200) {
				return Collections.emptyList();
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder data = new StringBuilder();
			String line = null;
			List<TrainStatus> statuses = new ArrayList<TrainStatus>(10);
			int iterations = 0;
			while ((line = br.readLine()) != null) {
				data.append(line.toLowerCase());
			}
			br.close();
			Document document = Jsoup.parse(data.toString());
			Elements elements = document.select("tr");
			for(int i = 1; i < elements.size(); i++) {
				Element element = elements.get(i);
				Elements trainName = element.select("input[name=train_name]");
				if(trainName.size()>0 && trainName.first().attr("value")!=null) {
					Elements status = element.select(":eq(2)");
					if(status.size()>0) {
						String track = status.first().text();
						if(track!=null && track.trim().length()>0) {
							TrainStatus s = new TrainStatus();
							s.setTrack(track.trim());
							s.setTrain(trainName.first().attr("value"));
							statuses.add(s);
						}
					}
				}
			}
			return statuses;
		} catch (IOException e) {
			throw e;
		}
	}
	
}
