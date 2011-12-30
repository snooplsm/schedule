package com.happytap.schedule.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.happytap.schedule.adapter.StationAdapter;
import com.happytap.schedule.domain.ConnectionInterval;
import com.happytap.schedule.domain.Favorite;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.Service;
import com.happytap.schedule.domain.StopTime;
import com.happytap.schedule.domain.Trip;
import com.njtransit.rail.R;

@Singleton
public class ScheduleDao {

	private final SQLiteDatabase database;
	private Context context;
	@Inject
	SharedPreferences preferences;

	@Inject
	public ScheduleDao(SQLiteDatabase database, Context context) {
		super();
		this.database = database;
		this.context = context;
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

	private String[] getConnectionStations(String departId, String arriveId) {
		String[] connections = context.getResources().getStringArray(
				R.array.connections);
		String to = departId + "," + arriveId;
		String from = arriveId + "," + departId;
		for (String connection : connections) {
			if (connection.startsWith(to) || connection.startsWith(from)) {
				int index = connection.lastIndexOf(",");
				String sub = connection.substring(index + 1);
				return sub.split("\\|");
			}
		}
		return null;
	}
	
	private static final SimpleDateFormat SIMPLE = new SimpleDateFormat("yyyyMMdd");
	/**
	 * 
	 * 
	 * @param d
	 * @return YYMMDD
	 */
	private String simpleDate(Date d) {
		return SIMPLE.format(d);
	}

	public Schedule getSchedule(final String departStationId,
			final String arriveStationId, Date start, Date end) {
		Cursor cur = database.rawQuery("select a,b from schedule_path where source=? and target=? and level=0 order by sequence asc", new String[]{departStationId,arriveStationId});
		String[][] pairs = new String[cur.getCount()][2];
		int i = 0;
		Set<String> p = new HashSet<String>();
		while(cur.moveToNext()) {
			pairs[i][0]=cur.getString(0);
			pairs[i][1]=cur.getString(1);
			System.out.println(pairs[i][0] + " - " + pairs[i][1]);
			p.add(pairs[i][0]);
			p.add(pairs[i][1]);
			i++;
		}
		cur.close();
		cur = database.rawQuery(String.format("select stop_id, name from stop where stop_id in (%s)",join(p,",")),
				null);
		Map<String,String> idToName = new HashMap<String,String>();
		while(cur.moveToNext()) {
			String id = cur.getString(0);
			String name = StationAdapter.makePretty(cur.getString(1));
			idToName.put(id, name);
		}
		cur.close();
		Date startDate = new Date(clearExtraFields(start));
		Date endDate = new Date(clearExtraFields(end));
		String startString = simpleDate(startDate);
		String endString = simpleDate(endDate);
		
		
		Map<String[],List<ConnectionInterval>> pairToTimes = new HashMap<String[],List<ConnectionInterval>>();
		Map<String,Integer> transferEdges = new HashMap<String,Integer>();
		Map<String, List<StopTime>> tripToResult = new HashMap<String, List<StopTime>>();
		Set<String> serviceIds = new HashSet<String>();
		
		Map<String, Service> services = new HashMap<String, Service>();
		Set<String> tripIds = new HashSet<String>();
		for(i=0; i < pairs.length; i++) {
			String query = "select a1.depart,a2.arrive,a1.service_id,a1.trip_id from nested_trip a1 join nested_trip a2 on (a1.trip_id=a2.trip_id and a1.stop_id=? and a2.stop_id=? and a1.lft < a2.lft) where a1.service_id in (select service_id from service where date=? or date=?) order by a1.depart asc";			
			Cursor rur = database.rawQuery("select duration from transfer_edge where source=? and target=?", new String[] {pairs[i][0],pairs[i][1]});
			if(rur.moveToNext()) {
				Integer duration = rur.getInt(0);
				transferEdges.put(pairs[i][0]+"-"+pairs[i][1], duration);
				continue; 
			}
			Cursor qur = database.rawQuery(query, new String[]{pairs[i][0],pairs[i][1],startString,endString});			
			List<ConnectionInterval> intervals = new ArrayList<ConnectionInterval>();
			pairToTimes.put(pairs[i], intervals);
			
			while(qur.moveToNext()) {
				String depart = qur.getString(0);
				String arrive = qur.getString(1);
				String serviceId = qur.getString(2);
				String tripId = qur.getString(3);
				
				ConnectionInterval interval = new ConnectionInterval();
				interval.tripId = tripId;
				tripIds.add(tripId);
				interval.serviceId = serviceId;
				interval.departure = depart;
				interval.arrival = arrive;
				interval.sourceId = pairs[i][0];
				interval.targetId = pairs[i][1];
				serviceIds.add(serviceId);				
				intervals.add(interval);
			}
			qur.close();
		}
		
		cur.close();
		cur = database.rawQuery("select date,service_id from service where date=? or date=?", new String[]{startString, endString});
		while(cur.moveToNext()) {
			String serviceId = cur.getString(1);
			Service s = services.get(serviceId);
			if (s == null) {
				s = new Service();
				s.serviceId = serviceId;
				services.put(s.serviceId, s);
			} else {
			}
			Calendar cal = Calendar.getInstance();
			try {
				cal.setTimeInMillis(SIMPLE.parse(cur.getString(0)).getTime());
			}catch (Exception e) {
				throw new RuntimeException(e);
			}
			Date dateD = cal.getTime();
			if (dateD.getTime() > endDate.getTime()
					|| dateD.getTime() < startDate.getTime()) {
			} else {
				s.dates.add(dateD);
			}
		}
		cur.close();
				
		Map<String, Trip> tripIdToTrips = new HashMap<String, Trip>();
		if (tripToResult.size() > 0) {
			String query = String.format(
					"select id, block_id from trips where id = %s",
					join(tripIds, " or id="));
			Cursor cursor = database.rawQuery(query, null);
			int count = cursor.getCount();
			while (cursor.moveToNext()) {
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
		s.departId = departStationId;
		s.arriveId = arriveStationId;
		s.transfers = pairs;
		s.services = services;
		s.connections = pairToTimes;
		s.transferEdges = transferEdges;
		s.stopIdToName = idToName;
		s.start = startDate;
		s.end = endDate;
		s.userEnd = end;
		s.userStart = start;
		return s;
	}
	
	public void name(List<Favorite> favs) {
		Set<String> ids = new HashSet<String>();
		for(Favorite f : favs) {
			ids.add(f.sourceId);
			ids.add(f.targetId);
		}
		if(ids.isEmpty()) {
			return;
		}
		Cursor cursor = database.rawQuery(String.format("select stop_id,name from stop where stop_id in (%s)",ScheduleDao.join(ids, ",")),null);
		
		Map<String,String> kk = new HashMap<String,String>();
		while(cursor.moveToNext()) {
			String id = cursor.getString(0);
			String name = cursor.getString(1);
			name = StationAdapter.makePretty(name);
			kk.put(id,name);
		}
		for(Favorite f : favs) {
			f.sourceName = kk.get(f.sourceId);
			f.targetName = kk.get(f.targetId);
		}
	}

	public static String join(String delimiter, Object... s) {
		return join(Arrays.asList(s), delimiter);
	}

	public static String join(Collection<?> s, String delimiter) {
		StringBuffer buffer = new StringBuffer();
		Iterator<?> iter = s.iterator();
		while (iter.hasNext()) {
			Object nxt = iter.next();
			if (nxt instanceof String) {
				buffer.append('\'');
			}
			buffer.append(nxt);
			if (nxt instanceof String) {
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
