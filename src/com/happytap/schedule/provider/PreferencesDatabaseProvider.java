package com.happytap.schedule.provider;

import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.happytap.schedule.database.PreferencesDatabaseHelper;

@Singleton
public class PreferencesDatabaseProvider implements Provider<SQLiteDatabase> {

	@Inject
	private PreferencesDatabaseHelper databaseHelper;
	
	private SQLiteDatabase database;
	
	@Override
	public SQLiteDatabase get() {
		if(database==null) {
			database = databaseHelper.getWritableDatabase();
		}
		return database;
	}

}
