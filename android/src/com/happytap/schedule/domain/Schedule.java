package com.happytap.schedule.domain;

import java.io.Serializable;
import java.text.SimpleDateFormat;
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

	public Map<String, Service> services;
	public Map<String, Trip> tripIdToTrip;
	public String departId;
	public String arriveId;
	public Map<String, Integer> transferEdges;
	public String[][] transfers;
	public Map<String[], List<ConnectionInterval>> connections;
	public Map<String, StationToStation> tripIdToBlockId;
	public Map<String, String> stopIdToName;
	public Map<String, Set<String>> blockIdToTripId;
	public Date start;
	public Date end;
	public Date userStart;
	public Date userEnd;
	public Map<String[], List<StationInterval>> stationIntervals;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	void traverse(List<? extends StationToStation> stationToStations,
			ScheduleTraverser traverser) {
		int size = stationToStations.size();
		for (int i = 0; i < size; i++) {
			StationInterval s = (StationInterval) stationToStations.get(i);
			System.out.println(s);
			// while(s.hasNext()) {
			// s = s.next();
			// System.out.println(s);
			// }
			System.out.println("\n\n");
			traverser.populateItem(i, s, size);
		}
	}

	void traversal(ScheduleTraverser traversal) {
		stationIntervals = new HashMap<String[], List<StationInterval>>();
		for (int i = 0; i < transfers.length; i++) {
			String[] pair = transfers[i];
			List<ConnectionInterval> k = connections.get(pair);
			// if k is null, this is a transfer edge.
			if (k == null) {
				continue;
			}
			Set<StationInterval> intervals = new HashSet<StationInterval>();
			
			for (ConnectionInterval interval : k) {
				Service service = services.get(interval.serviceId);
				if (service == null || service.dates == null) {
					continue;
				}
				for (Date date : service.dates) {
					if (date.getTime() >= start.getTime()
							&& date.getTime() <= end.getTime()) {
						try {
							Date arrive = sdf.parse(interval.arrival);
							Date depart = sdf.parse(interval.departure);
							// System.out.println(depart + " - " + arrive);
							Calendar arriveTime = convert(date, arrive);
							Calendar departTime = convert(date, depart);
							// System.out.println(depart + " - " + arrive);
							StationInterval si = new StationInterval();
							si.departId = pair[0];
							si.arriveId = pair[1];
							si.arriveTime = arriveTime;
							si.departTime = departTime;
							si.blockId = tripIdToBlockId(interval.tripId);
							si.level = i;
							si.schedule = this;
							intervals.add(si);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			List<StationInterval> ints = new ArrayList<StationInterval>(intervals);
			stationIntervals.put(pair, ints);
			Collections.sort(ints, new Comparator<StationInterval>() {

				@Override
				public int compare(StationInterval arg0, StationInterval arg1) {
					return arg0.departTime.compareTo(arg1.departTime);
				}

			});
		}
		traverse(stationIntervals.get(transfers[0]), traversal);
	}

	private Calendar convert(Date date, Date dateTime) {
		Calendar caldate = Calendar.getInstance();
		caldate.setTime(date);
		Calendar caldatetime = Calendar.getInstance();
		caldatetime.setTime(dateTime);
		caldate.add(Calendar.DAY_OF_YEAR,
				caldatetime.get(Calendar.DAY_OF_YEAR) - 1);
		caldate.set(Calendar.HOUR_OF_DAY, caldatetime.get(Calendar.HOUR_OF_DAY));
		caldate.set(Calendar.MINUTE, caldatetime.get(Calendar.MINUTE));
		caldate.set(Calendar.SECOND, caldatetime.get(Calendar.SECOND));
		return caldate;
	}

	void traversal(Set<String> orderedTripIds, ScheduleTraverser traversal) {
		TreeMap<Date, Set<String>> dateToTripIds = new TreeMap<Date, Set<String>>();
		for (String tripId : orderedTripIds) {
			// List<StopTime> stopTimes = tripIdToStopTimes.get(tripId);
			// StopTime depart = stopTimes.get(0);
			// Service service = services.get(depart.serviceId);
			// if(service.dates==null) {
			// continue;
			// }
			// for (Date date : service.dates) {
			// if (date.getTime() >= start.getTime()
			// && date.getTime() <= end.getTime()) {
			// Set<String> tripIds = dateToTripIds.get(date);
			// if (tripIds == null) {
			// tripIds = new HashSet<String>();
			// dateToTripIds.put(date, tripIds);
			// }
			// tripIds.add(tripId);
			// }
			// }
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
			blockIdToTripId = new HashMap<String, Set<String>>();
			for (String tripId : entry.getValue()) {
				// List<StopTime> stopTimes = tripIdToStopTimes.get(tripId);
				// StopTime depart = stopTimes.get(0);
				// StopTime arrive = stopTimes.get(1);
				// Calendar departTime = populate(date, depart.departure);
				// Calendar arriveTime = populate(date, arrive.arrival);
				// if(arriveTime.getTimeInMillis()<departTime.getTimeInMillis())
				// {
				// arriveTime.add(Calendar.DAY_OF_YEAR, 1);
				// }
				// StationToStation sts = new StationToStation();
				// sts.blockId = tripIdToBlockId(tripId);
				// sts.departTime = departTime;
				// sts.arriveTime = arriveTime;
				// sts.departId = depart.stopId;
				// sts.arriveId = arrive.stopId;
				// Set<String> trips = blockIdToTripId.get(sts.blockId);
				// if(trips==null) {
				// trips = new HashSet<String>();
				// blockIdToTripId.put(sts.blockId, trips);
				// }
				// trips.add(sts.blockId);
				// _stationToStations.add(sts);
			}
			Collections.sort(_stationToStations,
					new Comparator<StationToStation>() {

						@Override
						public int compare(StationToStation a,
								StationToStation b) {
							return a.departTime.compareTo(b.departTime);
						}

					});
			traverse(_stationToStations, traversal);
		}
	}

	public void inOrderTraversal(ScheduleTraverser t) {
		this.traversal(t);
	}

	public void inReverseOrderTraversal(ScheduleTraverser t) {
		// this.traversal(this.tripIdsInReverseOrder, t);
	}

	private Calendar populate(Date day, Date time) {
		Calendar dateCal = Calendar.getInstance();
		dateCal.setTime(day);
		Calendar departCal = Calendar.getInstance();
		departCal.setTime(time);
		Calendar departTime = Calendar.getInstance();
		departTime.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
		departTime.set(Calendar.DAY_OF_YEAR, dateCal.get(Calendar.DAY_OF_YEAR));
		departTime.set(Calendar.HOUR_OF_DAY,
				departCal.get(Calendar.HOUR_OF_DAY));
		departTime.set(Calendar.MINUTE, departCal.get(Calendar.MINUTE));
		departTime.set(Calendar.SECOND, departCal.get(Calendar.SECOND));
		return departTime;
	}

	public String tripIdToBlockId(String tripId) {
		if (tripIdToTrip == null || tripId == null) {
			return null;
		}
		Trip trip = tripIdToTrip.get(tripId);
		if (trip == null) {
			return null;
		}
		return trip.blockId;
	}

	public Set<String> blockidToTripId(String blockId) {
		Set<String> s = blockIdToTripId.get(blockId);
		if (s == null)
			return Collections.emptySet();
		return s;
	}
}
