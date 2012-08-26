package com.happytap.schedule.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PreferencesDatabaseHelper extends SQLiteOpenHelper {

	private static final String NAME = "preferences.sqlite";
	private static final int VERSION = 3;

	@Inject
	public PreferencesDatabaseHelper(Context context) {
		super(context, NAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table history(depart_id varchar(50), depart_name varchar(50), arrive_id varchar(50), arrive_name varchar(50), occurrences integer, last_updated integer)");
		db.execSQL("create table purchases(key varchar(100), value integer)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion<2) {
			db.execSQL("create table purchases(key varchar(100), value integer)");	
		}
		if(oldVersion<3) {
			db.execSQL("alter table history add column last_updated integer");
			db.execSQL("update history set last_updated = CURRENT_TIMESTAMP");
		}
	}

}
