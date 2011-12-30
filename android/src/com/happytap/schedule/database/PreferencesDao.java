package com.happytap.schedule.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.happytap.schedule.adapter.StationAdapter;
import com.happytap.schedule.domain.Favorite;
import com.happytap.schedule.provider.PreferencesDatabaseProvider;

@Singleton
public class PreferencesDao {

	private SQLiteDatabase database;

	@Inject
	public PreferencesDao(PreferencesDatabaseProvider provider) {
		super();
		this.database = provider.get();
	}
	
	public void saveScheduleRequest(String departId, String departName, String arriveId, String arriveName) {
		Cursor cursor = database.rawQuery("select occurrences from history where depart_id=? and arrive_id=?", new String[] {departId, arriveId});
		int count = cursor.getCount();
		ContentValues cv = new ContentValues();
		cv.put("depart_id", departId);
		cv.put("arrive_id", arriveId);
		cv.put("depart_name", departName);
		cv.put("arrive_name", arriveName);
		if(count==0) {
			cv.put("occurrences", 1);
			database.insert("history", null, cv);
		} else {
			cursor.moveToFirst();
			int occurrence = cursor.getInt(0);
			cv.put("occurrences", occurrence);
			database.update("history", cv, "depart_id=? and arrive_id=?", new String[] {departId, arriveId});
		}
		cursor.close();
	}
	
	public List<Favorite> topHistory() {
		Cursor cursor = database.rawQuery("select depart_id, arrive_id from history order by occurrences desc limit 4",null);
		List<Favorite> favs = new ArrayList<Favorite>(cursor.getCount());
		while(cursor.moveToNext()) {
			String depart = cursor.getString(0);
			String arrive = cursor.getString(1);
			Favorite f = new Favorite();
			f.sourceId = depart;
			f.targetId = arrive;
			favs.add(f);
		}
		cursor.close();
		return favs;

	}
	
}
