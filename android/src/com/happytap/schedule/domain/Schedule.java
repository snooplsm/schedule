package com.happytap.schedule.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Schedule implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public Map<String, List<StopTime>> tripIdToStopTimes;
	public Set<String> tripIdsInProperOrder;
	public Set<String> tripIdsInReverseOrder;
	public Map<String, Service> services;
	public Map<String, Trip> tripIdToTrip;
	public String departId;
	public String arriveId;
	public String[] connections;
	public Map<String, StationToStation> tripIdToBlockId;
	public Map<String, Set<String>> blockIdToTripId;
	public Date start;
	public Date end;
	public Date userStart;
	public Date userEnd;

	void traverse(List<StationToStation> stationToStations, ScheduleTraverser traverser) {
		int size = stationToStations.size();
		for(int i =0; i < size; i++) {
			StationToStation s = stationToStations.get(i);
			System.out.println("time:" + s.departTime.getTime());
			traverser.populateItem(i, stationToStations.get(i), size);
		}
	}
	
	void traversal(
			Set<String> orderedTripIds, ScheduleTraverser traversal) {
		TreeMap<Date, Set<String>> dateToTripIds = new TreeMap<Date, Set<String>>();
		for (String tripId : orderedTripIds) {
			List<StopTime> stopTimes = tripIdToStopTimes.get(tripId);
			StopTime depart = stopTimes.get(0);
			Service service = services.get(depart.serviceId);
			if(service.dates==null) {
				continue;
			}
			for (Date date : service.dates) {
				if (date.getTime() >= start.getTime()
						&& date.getTime() <= end.getTime()) {
					Set<String> tripIds = dateToTripIds.get(date);
					if (tripIds == null) {
						tripIds = new HashSet<String>();
						dateToTripIds.put(date, tripIds);
					}
					tripIds.add(tripId);
				}
			}
		}
		TreeMap<Date, List<StationToStation>> stationToStation = new TreeMap<Date, List<StationToStation>>();
		for (Map.Entry<Date, Set<String>> entry : dateToTripIds.entrySet()) {
			Date date = entry.getKey();
			Calendar dateCal = Calendar.getInstance();
			dateCal.setTime(date);
			List<StationToStation> _stationToStations = stationToStation
					.get(date);
			if (_stationToStations == null) {
				_stationToStations = new ArrayList<StationToStation>();
				stationToStation.put(date, _stationToStations);
			}
			blockIdToTripId = new HashMap<String,Set<String>>();
			for (String tripId : entry.getValue()) {
				List<StopTime> stopTimes = tripIdToStopTimes.get(tripId);
				StopTime depart = stopTimes.get(0);
				StopTime arrive = stopTimes.get(1);
				Calendar departTime = populate(date, depart.departure);
				Calendar arriveTime = populate(date, arrive.arrival);
				if(arriveTime.getTimeInMillis()<departTime.getTimeInMillis()) {
					arriveTime.add(Calendar.DAY_OF_YEAR, 1);
				}
				StationToStation sts = new StationToStation();
				sts.blockId = tripIdToBlockId(tripId);
				sts.departTime = departTime;
				sts.arriveTime = arriveTime; 
				sts.departId = depart.stopId;
				sts.arriveId = arrive.stopId;
				Set<String> trips = blockIdToTripId.get(sts.blockId);
				if(trips==null) {
					trips = new HashSet<String>();
					blockIdToTripId.put(sts.blockId, trips);
				}
				trips.add(sts.blockId);
				_stationToStations.add(sts);
			}
			Collections.sort(_stationToStations,
					new Comparator<StationToStation>() {

						@Override
						public int compare(StationToStation a,
								StationToStation b) {
							return a.departTime.compareTo(b.departTime);
						}

					});
			if(connections!=null) {
				convertToConnections(_stationToStations);
			}
			traverse(_stationToStations, traversal);
		}
	}
	
	private void convertToConnections(List<StationToStation> stationToStation) {
		List<StationToStation> connections = new ArrayList<StationToStation>();
		int maxThreshold = 60*2000; // 1 hr
		int minThreshold = 0;
		
		for(StationToStation s : stationToStation) {
			if(s.departId.equals(departId) && !s.arriveId.equals(arriveId)) {
				
			} else {
				connections.add(s);
			}
		}
		stationToStation.removeAll(connections);
		List<StationToStation> removeMe = new ArrayList<StationToStation>();
		for(StationToStation s : stationToStation) {
			for(StationToStation connection : connections) {
				long diff = connection.departTime.getTimeInMillis() - s.arriveTime.getTimeInMillis();
				if(diff<maxThreshold && diff > minThreshold) {
					if(s.connections == null) {
						s.connections = new ArrayList<StationToStation>(2);						
					}
					s.connections.add(connection);
				}
			}
			if(s.connections==null) {
				removeMe.add(s);
			}
		}
		stationToStation.removeAll(removeMe);
	}

	public void inOrderTraversal(ScheduleTraverser t) {
		this.traversal(this.tripIdsInProperOrder, t);
	}
	
	public void inReverseOrderTraversal(ScheduleTraverser t) {
		this.traversal(this.tripIdsInReverseOrder, t);
	}
	
	private Calendar populate(Date day, Date time) {
		Calendar dateCal = Calendar.getInstance();
		dateCal.setTime(day);
		Calendar departCal = Calendar.getInstance();
		departCal.setTime(time);
		Calendar departTime = Calendar.getInstance();
		departTime.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
		departTime.set(Calendar.DAY_OF_YEAR, dateCal.get(Calendar.DAY_OF_YEAR));
		departTime.set(Calendar.HOUR_OF_DAY, departCal
				.get(Calendar.HOUR_OF_DAY));
		departTime.set(Calendar.MINUTE, departCal.get(Calendar.MINUTE));
		departTime.set(Calendar.SECOND, departCal.get(Calendar.SECOND));
		return departTime;
	}
	
	public String tripIdToBlockId(String tripId) {
		Trip trip = tripIdToTrip.get(tripId);
		if(trip==null) {
			return null;
		}
		return trip.blockId;
	}
	
	public Set<String> blockidToTripId(String blockId) {
		Set<String> s = blockIdToTripId.get(blockId);
		if(s==null) return Collections.emptySet();
		return s;
	}
}
