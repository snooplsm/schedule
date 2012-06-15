package com.happytap.schedule.provider;

import roboguice.inject.ContextSingleton;
import android.database.sqlite.SQLiteDatabase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.happytap.schedule.database.DatabaseHelper;

@ContextSingleton
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
