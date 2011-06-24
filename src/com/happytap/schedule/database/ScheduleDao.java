package com.happytap.schedule.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.Service;
import com.happytap.schedule.domain.StopTime;
import com.happytap.schedule.domain.Trip;

@Singleton
public class ScheduleDao {

	private final SQLiteDatabase database;

	@Inject
	public ScheduleDao(SQLiteDatabase database) {
		super();
		this.database = database;
	}

	private static DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	private Long clearExtraFields(Date d) {
		Calendar c = Calendar.getInstance();
		c.clear();
		Calendar dc = Calendar.getInstance();
		dc.setTime(d);
		c.set(Calendar.YEAR, dc.get(Calendar.YEAR));
		c.set(Calendar.DAY_OF_YEAR, dc.get(Calendar.DAY_OF_YEAR));
		return c.getTimeInMillis();
	}

	public Schedule getSchedule(final String departStationId,
			final String arriveStationId, Date start, Date end) {

		Date startDate = new Date(clearExtraFields(start));
		Date endDate = new Date(clearExtraFields(end));
		String startString = clearExtraFields(start).toString();
		String endString = clearExtraFields(end).toString();

		Cursor cursor = database
				.rawQuery(
						"select trip_id, sequence, stop_id, service_id, departure, arrival from stop_times where (stop_id=? or stop_id=?) and service_id in (select service_id from calendar_dates where (calendar_date between ? and ?) and exception_type=1)",
						new String[] { departStationId, arriveStationId,
								startString, endString });
		Map<String, List<StopTime>> tripToResult = new HashMap<String, List<StopTime>>();
		Set<String> serviceIds = new HashSet<String>();
		while (cursor.moveToNext()) {
			StopTime r = new StopTime();
			r.tripId = cursor.getString(0);
			r.sequence = cursor.getInt(1);
			r.stopId = cursor.getString(2);
			r.serviceId = cursor.getString(3);
			try {
				r.departure = timeFormat.parse(cursor.getString(4));
				r.arrival = timeFormat.parse(cursor.getString(5));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			List<StopTime> result = tripToResult.get(r.tripId);
			if (result == null) {
				result = new ArrayList<StopTime>(2);
				tripToResult.put(r.tripId, result);
			}
			result.add(r);
			serviceIds.add(r.serviceId);
		}
		cursor.close();
		cursor = database
				.rawQuery(
						"select service_id, calendar_date from calendar_dates where calendar_date between ? and ? and exception_type=1",
						new String[] { startString, endString });
		Map<String, Service> services = new HashMap<String, Service>();
		while (cursor.moveToNext()) {
			String serviceId = cursor.getString(0);
			Long date = cursor.getLong(1);
			Service s = services.get(serviceId);
			if (s == null) {
				s = new Service();
				s.serviceId = serviceId;
				services.put(s.serviceId, s);
			} else {
			}
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(date);
			cal.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date dateD = cal.getTime();
			if (dateD.getTime() > endDate.getTime()
					|| dateD.getTime() < startDate.getTime()) {
			} else {
				s.dates.add(dateD);
			}

		}
		Set<String> regular = new HashSet<String>();
		Set<String> reverse = new HashSet<String>();
		for (Map.Entry<String, List<StopTime>> entry : tripToResult.entrySet()) {
			if (entry.getValue().size() != 2) {
				continue;
			} else {
				StopTime a = entry.getValue().get(0);
				StopTime b = entry.getValue().get(1);
				if (a.stopId.equals(departStationId)) {
					if (a.sequence < b.sequence) {
						regular.add(entry.getKey());
					} else {
						Collections.reverse(entry.getValue());
						reverse.add(entry.getKey());
					}
				} else {
					if (a.sequence < b.sequence) {
						reverse.add(entry.getKey());
					} else {
						Collections.reverse(entry.getValue());
						regular.add(entry.getKey());
					}
				}
			}
		}
		cursor.close();
		Map<String,Trip> tripIdToTrips = new HashMap<String,Trip>();
		if(tripToResult.size()>0) {			
			String query = String.format("select id, block_id from trips where id = %s",join(tripToResult.keySet()," or id="));
			cursor = database.rawQuery(query,null);
			int count = cursor.getCount();
			while(cursor.moveToNext()) {
				String id = cursor.getString(0);
				String blockId = cursor.getString(1);
				Trip trip = new Trip();
				trip.id = id;
				trip.blockId = blockId;
				tripIdToTrips.put(trip.id, trip);
			}
			cursor.close();
		}
		Schedule s = new Schedule();
		s.tripIdToTrip = tripIdToTrips;
		s.tripIdToStopTimes = tripToResult;
		s.services = services;
		s.tripIdsInProperOrder = regular;
		s.tripIdsInReverseOrder = reverse;
		s.start = startDate;
		s.end = endDate;
		s.userEnd = end;
		s.userStart = start;
		return s;
	}
	
	public static String join(Collection<?> s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
        	Object nxt = iter.next();
        	if(nxt instanceof String) {
        		buffer.append('\'');
        	}
            buffer.append(nxt);
            if(nxt instanceof String) {
            	buffer.append('\'');
            }
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

	static DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	

	





	


}
