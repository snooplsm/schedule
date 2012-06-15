package com.happytap.schedule.module;

import android.database.sqlite.SQLiteDatabase;

import com.google.inject.AbstractModule;
import com.happytap.schedule.provider.SQLiteDatabaseProvider;

public class ScheduleModule extends AbstractModule {


	@Override
	protected void configure() {
		
		bind(SQLiteDatabase.class).toProvider(SQLiteDatabaseProvider.class);
		
	}

}
