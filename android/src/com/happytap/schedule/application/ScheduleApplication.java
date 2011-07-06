package com.happytap.schedule.application;

import java.util.List;

import roboguice.application.RoboApplication;

import com.google.inject.Module;
import com.happytap.schedule.module.ScheduleModule;

public class ScheduleApplication extends RoboApplication {

	@Override
	protected void addApplicationModules(List<Module> modules) {
		
		modules.add(new ScheduleModule());
		
	}

}
