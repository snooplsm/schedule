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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jackson.annotate.JsonProperty;

import au.com.bytecode.opencsv.CSVReader;

import com.google.gson.Gson;

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
	private List<StopTime> stopTimes;
	private List<SimpleStopTime> simpleStopTimes;
	private Map<String,Set<Route>> stationIdToRoutes = new HashMap<String, Set<Route>>();
	private Map<String,Set<Station>> routeToStations = new HashMap<String, Set<Station>>();

	public static void main(String... args) {
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
		b.command("/usr/bin/split", "-b", "100k", databaseFile.getName(),
				databaseFile.getName() + "_");
		try {
			b.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		databaseFile.delete();
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
				prep.setInt(5, t.direction);
				prep.setString(6, t.blockId);
				prep.addBatch();
			}
			prep.executeBatch();
			conn.commit();
			prep.close();
			prep = conn
					.prepareStatement("insert into stops(id,name) values(?,?)");
			conn.setAutoCommit(false);
			for (Station s : stations.values()) {
				prep.setString(1, s.id);
				prep.setString(2, s.name);
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
			prep = conn
					.prepareStatement("insert into routes(id,name) values(?,?)");
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
			sst.stopId = stopTime.stop.id;
			stopTime.trip.stations.put(stopTime.sequence, stopTime.stop);
			simpleStopTimes.add(sst);
			stopTime.trip.route.stations.add(stopTime.stop);
			stopTime.stop.routes.add(stopTime.trip.route);			
		}
		
		TrainGraph graph = new TrainGraph();

		for(Trip trip : trips.values()) {
			Iterator<Station> iter = trip.stations.values().iterator();
			Station prev = null;
			while(prev!=null || iter.hasNext()) {
				Station station = iter.next();
				if(prev!=null) {
					graph.addEdge(prev, station, trip.route);
				}
				if(iter.hasNext()) {
					prev = station;
				} else {
					prev = null;
				}
				
			}
			System.out.println("alright");
		}
		
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
		
		
		
//		for(Route departRoute : depart.routes) {
//			for(Route arriveRoute : arrive.routes) {
//				BreadthFirstIterator<Route,DefaultEdge> i = new BreadthFirstIterator<Route,DefaultEdge>(graph,departRoute) {
//					protected void finishVertex(Route arg0) {
//						super.finishVertex(arg0);
//						this.
//					}
//				};
//			}
//		}
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
				time.trip = trips.get(row[tripIdPos]);
				time.arrive(row[arrivalTimeId]);
				time.depart(row[departureTimeId]);
				time.stop = stations.get(row[stopIdPos]);
				time.sequence = toInt(row[stopSequencePos]);
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
				DateService service = new DateService();
				service.id = row[serviceIdPos];
				service.setDate(row[datePos]);
				service.exceptionType = toInt(row[exceptionTypePos]);
				dateServices.add(service);
			}
			File file = new File(gtfsFolder, "calendar.txt");
			if (!file.exists()) {
				services = dateServices;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	class Service implements Serializable {
		String id;
	}

	class DayService extends Service {
		static final int monday = 2;
		static final int tuesday = 4;
		static final int wednesday = 16;
		static final int thursday = 32;
		static final int friday = 64;
		static final int saturday = 128;
		static final int sunday = 256;
		int flag;

		void set(int flag, String val) {
			if ("0".equals(val)) {
				this.flag |= flag;
			} else {
				this.flag &= flag;
			}
		}
	}

	static DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	class DateService extends Service implements Serializable {
		int exceptionType;
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
			directionIdPos = find(directionId, headers);
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				Trip trip = new Trip();
				trip.id = row[tripIdPos];
				trip.name = row[tripNamePos];
				trip.route = routes.get(row[routeIdPos]);
				trip.serviceId = row[serviceIdPos];
				trip.direction = toInt(row[directionIdPos]);
				trip.blockId = row[blockIdPos];
				System.out.println(trip.blockId);
				trips.put(trip.id, trip);
			}

		} catch (Exception e) {
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
		
		TreeMap<Integer,Station> stations = new TreeMap<Integer,Station>();
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
				route.name = row[routeNamePos];
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
		int stopIdPos = -1;
		int stopNamePos = -1;
		try {
			CSVReader reader = new CSVReader(new FileReader(new File(
					gtfsFolder, "stops.txt")));
			String[] headers = reader.readNext();
			stopIdPos = find(stopId, headers);
			stopNamePos = find(stopName, headers);
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				Station station = new Station();
				station.id = row[stopIdPos];
				station.name = row[stopNamePos];
				stations.put(station.id, station);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	int toInt(String val) {
		return Integer.parseInt(val);
	}

	int find(String key, String[] data) {
		for (int i = 0; i < data.length; i++) {
			if (data[i].equals(key)) {
				return i;
			}
		}
		return -1;
	}

	class Station {
		String name;
		String id;
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
