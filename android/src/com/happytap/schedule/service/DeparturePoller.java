package com.happytap.schedule.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.happytap.schedule.domain.TrainStatus;

/**
 * Hello world!
 * 
 */
public class DeparturePoller {
	
	public List<TrainStatus> getTrainStatuses(String station) throws IOException {
		URL url = null;
		try {
			url = new URL("http://dv.njtransit.com/mobile/tid-mobile.aspx?sid="+station);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(20000);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_0 like Mac OS X; en-us) AppleWebKit/532.9 (KHTML, like Gecko) Version/4.0.5 Mobile/8A293 Safari/6531.22.7");
			if(conn.getResponseCode()!=200) {
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
			int index = data.indexOf("<table");
			int endIndex = data.indexOf(">",index);		
			//System.out.println(index);
			//System.out.println(endIndex);
			//System.out.println(data.substring(endIndex));
			while(endIndex!=-1) {				
				iterations++;
				if(iterations==30) {
					break;
				}
				int tr = data.indexOf("<tr",endIndex);
				if(tr==-1) {
					endIndex=-1;
					continue;
				}
				endIndex = tr;
				int endTr = data.indexOf("</tr>",tr);
				if(endTr==-1) {
					endIndex=-1;
					continue;
				}
				line = data.substring(tr,endTr);
				endIndex = endTr;
				
				int train = line.indexOf("train=");
				if(train==-1) {
					continue;
				}
				train+="train=".length();
				int trainEnd = line.indexOf("\"",train);
				String trainStr = line.substring(train,trainEnd);
				//System.out.println(trainStr);
				int tdSkip = line.indexOf("<td",trainEnd);
				if(tdSkip==-1) {
					continue;
				}
				tdSkip+="<td".length();
				tdSkip = line.indexOf("<td",tdSkip);
				if(tdSkip==-1) {
					continue;
				}
				tdSkip+="<td".length();
//				tdSkip = line.indexOf("<td",tdSkip);
//				if(tdSkip==-1) {
//					continue;
//				}
//				tdSkip+="<td".length();
				int trackBegin = line.indexOf(">",tdSkip);
				if(trackBegin==-1) {
					continue;
				}
				trackBegin+=">".length();
				int trackEnd = line.indexOf("</td",trackBegin);
				if(trackEnd==-1) {
					continue;
				}
				String track = line.substring(trackBegin,trackEnd);
				//System.out.println(track);
				tdSkip = line.indexOf("<td",trackEnd);
				if(tdSkip==-1) {
					continue;
				}
				tdSkip+="<td".length();
				tdSkip = line.indexOf("<td",tdSkip);
				if(tdSkip==-1) {
					continue;
				}
				tdSkip+="<td".length();
				tdSkip = line.indexOf("<td",tdSkip);
				if(tdSkip==-1) {
					continue;
				}
				tdSkip+="<td".length();
				int statusBegin = line.indexOf(">",tdSkip);
				if(statusBegin==-1) {
					continue;
				}
				statusBegin+=">".length();
				int statusEnd = line.indexOf("<",statusBegin);
				if(statusEnd==-1) {
					continue;
				}
				String status = line.substring(statusBegin,statusEnd);
				if(trainStr==null) {
					continue;
				}
				if(track!=null || status!=null) {
					TrainStatus tstatus = new TrainStatus();
					tstatus.setStatus(status);
					tstatus.setTrack(track);
					tstatus.setTrain(trainStr);
					statuses.add(tstatus);
				}
			}
			return statuses;
		} catch (IOException e) {
			throw e;
		}
	}
}
