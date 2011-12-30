package com.happytap.schedule.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StationDao {

	private final SQLiteDatabase database;
	
	private final SharedPreferences preferences;

	@Inject
	public StationDao(SQLiteDatabase database, SharedPreferences preferences) {
		super();
		this.database = database;
		this.preferences = preferences;
	}
	
	//public static final String ID="id";
	public static final String NAME="name";
	
	public Cursor getStations() {
		List<String> ignore = new ArrayList<String>();
		for(Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
			if(entry.getKey().startsWith("h_")) {
				ignore.add("'"+(String)entry.getValue()+"'");
			}
		}
		return database.rawQuery("select stop_id as _id, name from stop where _id not in ("+ScheduleDao.join(ignore, ",")+") order by lower(name)", null);
//		Cursor cursor =  database.query("stops", new String[]{ID + " as _id", NAME}, ignore.isEmpty() ? null : "_id not in (?)", ignore.isEmpty() ? null : new String[]{ScheduleDao.join(ignore,",")}, null, null, NAME);
	}
	
	/**
	 * 
	 * @return station first letter grouped by first letter and ordered
	 */
	public Cursor getStationLetters() {
		String name = String.format("substr(%s,1,1)",NAME);
		return database.query("stop", new String[]{name}, null, null, name, null, name);
	}
	
}
