package com.happytap.transit;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.annotate.JsonProperty;

import au.com.bytecode.opencsv.CSVReader;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.happytap.transit.ReverseGeocode.AddressComponent;
import com.happytap.transit.ReverseGeocode.Result;
import com.happytap.transit.ReverseGeocode.Results;

/**
 * Hello world!
 * 
 */
public class App {

	private final File gtfsFolder;
	private final File outputFolder;
	private final File databaseFile;

	private Map<String, Station> stations;
	private Map<String, Route> routes;
	private Map<String, Trip> trips;
	private List<? extends Service> services;
	private Map<String, DayService> dayServices;
	private List<DateService> dateServices;
	private List<DayService> calServices;
	private List<StopTime> stopTimes;
	private List<SimpleStopTime> simpleStopTimes;
	private Map<String, Set<Route>> stationIdToRoutes = new HashMap<String, Set<Route>>();
	private Map<String, Set<Station>> routeToStations = new HashMap<String, Set<Station>>();

	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	
	public static void main(String... args) {
//		try {
//			System.out.println(Float.parseFloat("1.2261943e-7"));
//			System.exit(1);
//			System.out.println(sdf.parse("00:01:00"));
//			System.out.println(sdf.parse("24:01:00"));
//			System.out.println(sdf.parse("25:01:00"));
//			System.exit(1);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		File gtfsFolder = new File(args[0]);
		File outputFolder = new File(args[1]);
		new App(gtfsFolder, outputFolder).makeSchedule();
	}

	public App(File gtfsFolder, File outputFolder) {
		this.gtfsFolder = gtfsFolder;
		this.outputFolder = outputFolder;
		databaseFile = new File(outputFolder, "database.sqlite");
		databaseFile.delete();
	}

	public void makeSchedule() {
		makeStations();
		makeRoutes();
		makeServices();
		makeTrips();
		makeStopTimes();
		reformSchedule();
		createDatabase();
		splitDatabase();
	}

	private void splitDatabase() {
		ProcessBuilder b = new ProcessBuilder();
		b.directory(databaseFile.getParentFile());
		b.command("/usr/bin/split", "-b", "100k", "-a", "3",
				databaseFile.getName(), databaseFile.getName()
						.replace(".", "_") + "_");
		try {
			b.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// databaseFile.delete();
	}

	class TimeBean {
		String depart;
		String arrive;

		public String getDepart() {
			return depart;
		}

		public void setDepart(String depart) {
			this.depart = depart;
		}

		public String getArrive() {
			return arrive;
		}

		public void setArrive(String arrive) {
			this.arrive = arrive;
		}

	}

	private void createDatabase() {
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC");
			databaseFile.getParentFile().mkdirs();
			for (File file : outputFolder.listFiles()) {
				file.delete();
			}
			conn = DriverManager.getConnection("jdbc:sqlite:"
					+ databaseFile.getAbsolutePath());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Statement stat;
		try {
			stat = conn.createStatement();
		} catch (SQLException e2) {
			throw new RuntimeException(e2);
		}
		String[] creates = new String[] {
				"create table if not exists trips(id varchar(50), route_id varchar(50), service_id varchar(50), headsign varchar(255), direction int, block_id varchar(100))",
				"create table if not exists stops(id varchar(50), name varchar(255), lat real, lon real, zone_id varchar(50), alternate_id varchar(50))",
				"create table if not exists stop_times(trip_id varchar(50), service_id varchar(50), route_id varchar(50), arrival varchar(10), departure varchar(10), stop_id varchar(100), sequence int, pickup_type int, drop_off_type int)",
				"create table if not exists routes(id varchar(50), agency_id varchar(100), name varchar(255), route_type integer, timezone varchar(100))",
				"create table if not exists calendar(service_id varchar(100), monday int, tuesday int, wednesday int, thursday int, friday int, saturday int, sunday int, start varchar(10), end varchar(10))",
				// agency_id,agency_name,agency_url,agency_timezone
				"create table if not exists calendar_dates(service_id varchar(100), calendar_date integer, exception_type int)",
				// "create table if not exists agency(id varchar(100), name varchar(255), url varchar(255), timezone varchar(100))",
				"CREATE TABLE if not exists android_metadata (locale TEXT DEFAULT 'en_US')",
				"INSERT INTO android_metadata VALUES ('en_US')",
				"create index stop_index on stop_times(stop_id)",
				"create index trip_index on stop_times(trip_id)" };

		try {
			for (String createTable : creates) {
				stat.executeUpdate(createTable);
			}
			PreparedStatement prep = conn
					.prepareStatement("insert into stop_times(trip_id,service_id,route_id,arrival,departure,stop_id,sequence) values(?,?,?,?,?,?,?)");
			conn.setAutoCommit(false);
			for (SimpleStopTime s : simpleStopTimes) {
				prep.setString(1, s.tripId);
				prep.setString(2, s.serviceId);
				prep.setString(3, s.routeId);
				prep.setString(4, s.arrive);
				prep.setString(5, s.depart);
				prep.setString(6, s.stopId);
				prep.setInt(7, s.sequence);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.close();
			prep = conn
					.prepareStatement("insert into trips(id,route_id, service_id, headsign, direction, block_id) values(?,?,?,?,?,?)");
			conn.setAutoCommit(false);
			for (Trip t : trips.values()) {
				prep.setString(1, t.id);
				prep.setString(2, t.route.id);
				prep.setString(3, t.serviceId);
				prep.setString(4, t.name);
				if (t.direction != null) {
					prep.setInt(5, t.direction);
				}
				prep.setString(6, t.blockId);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.close();
			prep = conn
					.prepareStatement("insert into stops(id,name,lat,lon) values(?,?,?,?)");
			conn.setAutoCommit(false);
			for (Station s : stations.values()) {
				prep.setString(1, s.id);
				prep.setString(2, s.name);
				prep.setFloat(3, s.lat);
				prep.setFloat(4, s.lon);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.close();
			prep = conn
					.prepareStatement("insert into calendar_dates(service_id,calendar_date,exception_type) values(?,?,?)");
			conn.setAutoCommit(false);
			for (DateService s : dateServices) {
				prep.setString(1, s.id);
				prep.setLong(2, s.date.getTime());
				prep.setInt(3, s.exceptionType);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.clearBatch();
			prep = conn
					.prepareStatement("insert into calendar(service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start,end) values (?,?,?,?,?,?,?,?,?,?)");

			for (DayService s : calServices) {
				prep.setString(1, s.id);
				prep.setInt(2, s.monday);
				prep.setInt(3, s.tuesday);
				prep.setInt(4, s.wednesday);
				prep.setInt(5, s.thursday);
				prep.setInt(6, s.friday);
				prep.setInt(7, s.saturday);
				prep.setInt(8, s.sunday);
				prep.setLong(9, s.start.getTime());
				prep.setLong(10, s.end.getTime());
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep = conn
					.prepareStatement("insert into routes(id,name) values(?,?)");
			conn.setAutoCommit(false);
			for (Route r : routes.values()) {
				prep.setString(1, r.id);
				prep.setString(2, r.name);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			conn.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void reformSchedule() {
		simpleStopTimes = new ArrayList<SimpleStopTime>(stopTimes.size());
		stationIdToRoutes = new HashMap<String, Set<Route>>();
		routeToStations = new HashMap<String, Set<Station>>();
		for (StopTime stopTime : stopTimes) {
			SimpleStopTime sst = new SimpleStopTime();
			sst.arrive = timeFormat.format(stopTime.arrive);
			sst.depart = timeFormat.format(stopTime.depart);
			sst.tripId = stopTime.trip.id;
			sst.serviceId = stopTime.trip.serviceId;
			sst.routeId = stopTime.trip.route.id;
			sst.sequence = stopTime.sequence;
			if(stopTime.stop==null) {
				System.out.println("foo");
			}
			sst.stopId = stopTime.stop.id;
			stopTime.trip.stations.put(stopTime.sequence, stopTime.stop);
			simpleStopTimes.add(sst);
			stopTime.trip.route.stations.add(stopTime.stop);
			stopTime.stop.routes.add(stopTime.trip.route);
		}
		TrainGraph g = new TrainGraph();
		for (Trip trip : trips.values()) {
			Station previous = null;
			for (Map.Entry<Integer, Station> entry : trip.stations.entrySet()) {
				Station station = entry.getValue();
				if (previous == null) {
					previous = station;
					continue;
				}
				g.addEdge(previous, station, trip.route, 1.0);
			}
		}
		//
		// TrainGraph graph = new TrainGraph();
		//
		// for(Trip trip : trips.values()) {
		// Iterator<Station> iter = trip.stations.values().iterator();
		// Station prev = null;
		// while(prev!=null || iter.hasNext()) {
		// Station station = iter.next();
		// if(prev!=null) {
		// graph.addEdge(prev, station, trip.route);
		// }
		// if(iter.hasNext()) {
		// prev = station;
		// } else {
		// prev = null;
		// }
		//
		// }
		// Station newyork = stations.get("105");
		// Station radburn = stations.get("126");
		// graph.findShortestPath(newyork, radburn);
		// System.out.println("alright");
		// }

		for (final Map.Entry<String, Set<Station>> entry : routeToStations
				.entrySet()) {
			System.out.print(routes.get(entry.getKey()).name + "\n\t");
			for (Station station : entry.getValue()) {
				System.out.print(station.name + "\n\t");
			}
			System.out.print('\n');
		}

		Station jerseyAve = stations.get("32906");
		Station arrive = stations.get("105");

		// for(Route departRoute : depart.routes) {
		// for(Route arriveRoute : arrive.routes) {
		// BreadthFirstIterator<Route,DefaultEdge> i = new
		// BreadthFirstIterator<Route,DefaultEdge>(graph,departRoute) {
		// protected void finishVertex(Route arg0) {
		// super.finishVertex(arg0);
		// this.
		// }
		// };
		// }
		// }
	}

	private void makeStopTimes() {
		try {
			stopTimes = new ArrayList<StopTime>();
			String tripId = "trip_id";
			String arrivalTime = "arrival_time";
			String departureTime = "departure_time";
			String stopId = "stop_id";
			String stopSequence = "stop_sequence";
			CSVReader reader = new CSVReader(new FileReader(new File(
					gtfsFolder, "stop_times.txt")));
			String[] headers = reader.readNext();
			int tripIdPos = find(tripId, headers);
			int arrivalTimeId = find(arrivalTime, headers);
			int departureTimeId = find(departureTime, headers);
			int stopIdPos = find(stopId, headers);
			int stopSequencePos = find(stopSequence, headers);
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				StopTime time = new StopTime();
				time.trip = trips.get(row[tripIdPos].trim());
				time.arrive(row[arrivalTimeId].trim());
				time.depart(row[departureTimeId].trim());
				Station stop = stations.get(row[stopIdPos].trim());
				if(stop==null) {
					System.out.println("foo");
				}
				time.stop = stop;
				time.sequence = toInt(row[stopSequencePos].trim());
				stopTimes.add(time);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	class StopRow implements Serializable {

		String tripId;
		String routeId;
		String serviceId;
		String stopId;

	}

	class SimpleStopTime implements Serializable {
		String tripId;
		String serviceId;
		String routeId;
		String depart;
		String arrive;
		String stopId;
		int sequence;

		@JsonProperty("t")
		public String getTripId() {
			return tripId;
		}

		public void setTripId(String tripId) {
			this.tripId = tripId;
		}

		@JsonProperty("c")
		public String getServiceId() {
			return serviceId;
		}

		public void setServiceId(String serviceId) {
			this.serviceId = serviceId;
		}

		@JsonProperty("r")
		public String getRouteId() {
			return routeId;
		}

		public void setRouteId(String routeId) {
			this.routeId = routeId;
		}

		@JsonProperty("d")
		public String getDepart() {
			return depart;
		}

		public void setDepart(String depart) {
			this.depart = depart;
		}

		@JsonProperty("a")
		public String getArrive() {
			return arrive;
		}

		public void setArrive(String arrive) {
			this.arrive = arrive;
		}

		@JsonProperty("s")
		public String getStopId() {
			return stopId;
		}

		public void setStopId(String stopId) {
			this.stopId = stopId;
		}

		@JsonProperty("q")
		public int getSequence() {
			return sequence;
		}

		public void setSequence(int sequence) {
			this.sequence = sequence;
		}

	}

	class StopTime {

		Trip trip;
		Date depart;
		Date arrive;
		Station stop;
		int sequence;

		void depart(String departs) {
			try {
				depart = timeFormat.parse(departs);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		void arrive(String arrives) {
			try {
				arrive = timeFormat.parse(arrives);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private void makeServices() {
		try {
			dateServices = new ArrayList<DateService>();
			calServices = new ArrayList<DayService>();
			String serviceId = "service_id";
			int serviceIdPos;
			String date = "date";
			int datePos;
			String exceptionType = "exception_type";
			int exceptionTypePos;
			CSVReader reader = new CSVReader(new FileReader(new File(
					gtfsFolder, "calendar_dates.txt")));
			String[] headers = reader.readNext();
			serviceIdPos = find(serviceId, headers);
			datePos = find(date, headers);
			exceptionTypePos = find(exceptionType, headers);
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				if(row.length!=3) {
					continue;
				}
				DateService service = new DateService();
				service.id = row[serviceIdPos];
				service.setDate(row[datePos]);
				service.exceptionType = toInt(row[exceptionTypePos]);
				dateServices.add(service);
			}
			reader.close();

			File file = new File(gtfsFolder, "calendar.txt");

			if (!file.exists()) {
				return;
				// services = dateServices;
			}
			reader = new CSVReader(new FileReader(file));
			headers = reader.readNext();
			String monday = "monday";
			String tuesday = "tuesday";
			String wednesday = "wednesday";
			String thursday = "thursday";
			String friday = "friday";
			String saturday = "saturday";
			String sunday = "sunday";
			String start = "start_date";
			String end = "end_date";
			int mPos = find(monday, headers);
			int tuPos = find(tuesday, headers);
			int wPos = find(wednesday, headers);
			int thPos = find(thursday, headers);
			int fPos = find(friday, headers);
			int saPos = find(saturday, headers);
			int suPos = find(sunday, headers);
			int sPos = find(start, headers);
			int ePos = find(end, headers);
			List<DateService> newServices = new LinkedList<DateService>();
			while ((row = reader.readNext()) != null) {
				DayService service = new DayService();
				service.id = row[serviceIdPos];
				service.monday = toInt(row[mPos].trim());
				service.tuesday = toInt(row[tuPos].trim());
				service.wednesday = toInt(row[wPos].trim());
				service.thursday = toInt(row[thPos].trim());
				service.friday = toInt(row[fPos].trim());
				service.saturday = toInt(row[saPos].trim());
				service.sunday = toInt(row[suPos].trim());
				service.setStart(row[sPos].trim());
				service.setEnd(row[ePos].trim());
				Calendar current = Calendar.getInstance();
				for (Date curr = service.start; curr.getTime() <= service.end
						.getTime();) {
					current.setTime(curr);
					int day = current.get(Calendar.DAY_OF_WEEK);
					if ((day == Calendar.MONDAY && service.monday == 1)
							|| (day == Calendar.TUESDAY && service.tuesday == 1)
							|| (day == Calendar.WEDNESDAY && service.wednesday == 1)
							|| (day == Calendar.THURSDAY && service.thursday == 1)
							|| (day == Calendar.FRIDAY && service.friday == 1)
							|| (day == Calendar.SATURDAY && service.saturday == 1)
							|| (day == Calendar.SUNDAY && service.sunday == 1)) {
						DateService s = new DateService();
						s.id = service.id;
						s.date = curr;
						s.exceptionType = 1;
						newServices.add(s);
					}
					current.add(Calendar.DAY_OF_YEAR, 1);
					curr = current.getTime();

				}
				calServices.add(service);
			}
			for (DateService dateService : dateServices) {
				if (dateService.exceptionType == 2) {
					for (Iterator<DateService> i = newServices.iterator(); i
							.hasNext();) {
						DateService check = i.next();
						if (check.id.equals(dateService.id)
								&& check.date.equals(dateService.date)) {
							System.out.println(dateService.date);
							i.remove();
						}
					}
				}
			}
			dateServices.addAll(newServices);
			calServices.clear();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	class Service implements Serializable {
		String id;
		int exceptionType;
	}

	class DayService extends Service {
		int monday, tuesday, wednesday, thursday, friday, saturday, sunday;
		Date start, end;

		void setStart(String date) {
			try {
				this.start = dateFormat.parse(date);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		void setEnd(String date) {
			try {
				this.end = dateFormat.parse(date);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		int flag;
	}

	static DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	class DateService extends Service implements Serializable {

		Date date;

		void setDate(String date) {
			try {
				this.date = dateFormat.parse(date);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void makeTrips() {
		try {
			trips = new HashMap<String, Trip>();
			String tripId = "trip_id";
			String tripName = "trip_headsign";
			String routeId = "route_id";
			String serviceId = "service_id";
			String blockId = "block_id";
			String directionId = "direction_id";
			int tripIdPos;
			int tripNamePos;
			int routeIdPos;
			int serviceIdPos;
			int directionIdPos;
			int blockIdPos;
			CSVReader reader = new CSVReader(new FileReader(new File(
					gtfsFolder, "trips.txt")));
			String[] headers = reader.readNext();
			tripIdPos = find(tripId, headers);
			tripNamePos = find(tripName, headers);
			routeIdPos = find(routeId, headers);
			serviceIdPos = find(serviceId, headers);
			blockIdPos = find(blockId, headers);
			if (blockIdPos == -1) {
				blockIdPos = find("trip_short_name", headers);
			}
			directionIdPos = find(directionId, headers);
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				Trip trip = new Trip();
				trip.id = row[tripIdPos].trim();
				trip.name = row[tripNamePos].trim();
				trip.route = routes.get(row[routeIdPos].trim());
				trip.serviceId = row[serviceIdPos].trim();
				if (directionIdPos >= 0) {
					trip.direction = toInt(row[directionIdPos].trim());
				}
				trip.blockId = row[blockIdPos].trim();
				System.out.println(trip.blockId);
				trips.put(trip.id, trip);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	class Trip {
		String id;
		Route route;
		String name;
		String serviceId;
		String blockId;
		Integer direction;

		TreeMap<Integer, Station> stations = new TreeMap<Integer, Station>();
	}

	private void makeRoutes() {
		try {
			routes = new HashMap<String, Route>();
			String routeId = "route_id";
			String routeName = "route_long_name";
			int routeIdPos;
			int routeNamePos;
			CSVReader reader = new CSVReader(new FileReader(new File(
					gtfsFolder, "routes.txt")));
			String[] headers = reader.readNext();
			routeIdPos = find(routeId, headers);
			routeNamePos = find(routeName, headers);
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				Route route = new Route();
				route.id = row[routeIdPos];
				route.name = row[routeNamePos].trim();
				routes.put(route.id, route);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void makeStations() {
		stations = new HashMap<String, Station>();
		String stopId = "stop_id";
		String stopName = "stop_name";
		String lat = "stop_lat";
		String lon = "stop_lon";
		int stopIdPos = -1;
		int stopNamePos = -1;
		int latPos = -1;
		int lonPos = -1;
		try {
			CSVReader reader = new CSVReader(new FileReader(new File(
					gtfsFolder, "stops.txt")));
			String[] headers = reader.readNext();
			stopIdPos = find(stopId, headers);
			stopNamePos = find(stopName, headers);
			latPos = find(lat, headers);
			lonPos = find(lon, headers);
			String[] row = null;
			Gson gson = new GsonBuilder().setFieldNamingPolicy(
					FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
			Map<String, AtomicInteger> counts = new HashMap<String, AtomicInteger>();
			while ((row = reader.readNext()) != null) {
				Station station = new Station();
				station.id = row[stopIdPos];

				station.name = row[stopNamePos].trim();
				// station.lat = Float.parseFloat(row[latPos]);
				// station.lon = Float.parseFloat(row[lonPos]);
				File cache = new File(gtfsFolder, station.id);
				// if(!cache.exists()) {
				// try {
				// URL u = new
				// URL("http://maps.googleapis.com/maps/api/geocode/json?latlng="+row[latPos]+","+row[lonPos]+"&sensor=true");
				// HttpURLConnection conn =
				// (HttpURLConnection)u.openConnection();
				// conn.setRequestProperty("User-Agent",
				// "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_7) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.107 Safari/535.1");
				// if(conn.getResponseCode()>=200 && conn.getResponseCode()<300)
				// {
				// FileOutputStream fos = new FileOutputStream(cache);
				// fos.write(IOUtils.readFully(conn.getInputStream(), -1,
				// true));
				// fos.close();
				// long len = cache.length();
				// if(len<=4096) {
				// cache.delete();
				// System.exit(1);
				// }
				// conn.disconnect();
				// Thread.sleep(90);
				// }
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// }
				// String addy = findAddress(gson,cache);
				// if(addy==null) {
				// findAddress(gson,cache);
				// } else {
				// if(addy.length()>27) {
				// System.out.println(addy);
				// }
				// }
				stations.put(station.id, station);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String findAddress(Gson gson, File cache) throws Exception {
		Results r = gson.fromJson(new FileReader(cache), Results.class);
		for (Result result : r.getResults()) {
			AddressComponent toReturn = null;
			AddressComponent toReturn2 = null;
			AddressComponent toReturn3 = null;
			for (AddressComponent add : result.getAddressComponents()) {
				for (String type : add.getTypes()) {
					if (type.equals("sublocality")) {
						// System.out.println(add.getLongName() + " or " +
						// add.getShortName());
						return add.getLongName();
					}
					if (type.equals("locality")) {
						toReturn = add;
					}
					if (type.equals("neighborhood")) {
						toReturn2 = add;
					}
					if (type.equals("administrative_area_level_3")) {
						toReturn3 = add;
					}
				}
				if (toReturn != null) {
					return toReturn.getShortName();
				}
				if (toReturn2 != null) {
					return toReturn2.getShortName();
				}
				if (toReturn3 != null) {
					return toReturn3.getShortName();
				}
			}
		}
		return null;
	}

	int toInt(String val) {
		return Integer.parseInt(val);
	}

	int find(String key, String[] data) {
		for (int i = 0; i < data.length; i++) {
			if (data[i].trim().equals(key)) {
				return i;
			}
		}
		return -1;
	}

	class Station {
		String name;
		String id;
		float lat, lon;
		Set<Route> routes = new HashSet<Route>();

		public String toString() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + Float.floatToIntBits(lat);
			result = prime * result + Float.floatToIntBits(lon);
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Station other = (Station) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (Float.floatToIntBits(lat) != Float.floatToIntBits(other.lat))
				return false;
			if (Float.floatToIntBits(lon) != Float.floatToIntBits(other.lon))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		private App getOuterType() {
			return App.this;
		}

	}

	class Route {
		String name;
		String id;
		Set<Station> stations = new HashSet<Station>();

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Route other = (Route) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return name;
		}

		private App getOuterType() {
			return App.this;
		}
	}

}
