package com.happytap.schedule.provider;

import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.happytap.schedule.database.DatabaseHelper;

@Singleton
public class SQLiteDatabaseProvider implements Provider<SQLiteDatabase> {

	@Inject
	private DatabaseHelper databaseHelper;
	
	private SQLiteDatabase database;
	
	@Inject
	public SQLiteDatabaseProvider(DatabaseHelper databaseHelper) {
		this.databaseHelper = databaseHelper;
	}
	
	public SQLiteDatabase get() {
		if(database==null) {
			database =  databaseHelper.getReadableDatabase();
		}
		return database;
	}

}
