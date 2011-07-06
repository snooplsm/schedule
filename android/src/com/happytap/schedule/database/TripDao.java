package com.happytap.schedule.database;

import java.util.Date;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TripDao {

	private SQLiteDatabase database;

	public TripDao(SQLiteDatabase database) {
		super();
		this.database = database;
	}
	
	public String getTripIdsForDateRange(Date begin, Date end) {
		Cursor c = database.rawQuery("select id from trips where service_id in (select service_id from calendar_dates where calendar_date between ? and ?)", new String[] {String.valueOf(begin.getTime()), String.valueOf(end.getTime())});
		StringBuilder b = new StringBuilder();
		while(c.moveToNext()) {
			b.append(c.getString(0));
			if(!c.isLast()) {
				b.append(",");
			}
		}
		c.close();
		System.out.println(b.toString());
		return b.toString();
	}
	
}
