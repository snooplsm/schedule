package com.happytap.schedule.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.happytap.schedule.domain.Schedule;

@Singleton
public class CurrentScheduleProvider implements Provider<Schedule> {

	private Schedule schedule;
	
	public void setSchedule(Schedule schedule) {		
		this.schedule = schedule;
	}

	@Inject
	public CurrentScheduleProvider() {
		
	}
	
	@Override
	public Schedule get() {
		return schedule;
	}

}
