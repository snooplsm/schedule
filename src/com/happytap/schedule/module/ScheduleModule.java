package com.happytap.schedule.module;

import roboguice.config.AbstractAndroidModule;
import android.database.sqlite.SQLiteDatabase;

import com.happytap.schedule.provider.SQLiteDatabaseProvider;

public class ScheduleModule extends AbstractAndroidModule {

	
	
	@Override
	protected void configure() {
		
		bind(SQLiteDatabase.class).toProvider(SQLiteDatabaseProvider.class);
		
	}

}
