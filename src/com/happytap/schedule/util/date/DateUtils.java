package com.happytap.schedule.util.date;

import java.util.Calendar;

public class DateUtils {

	public static boolean isToday(Calendar calendar) {
		Calendar now = Calendar.getInstance();
		if(now.get(Calendar.DAY_OF_YEAR)==calendar.get(Calendar.DAY_OF_YEAR)) {
			return now.get(Calendar.YEAR)==calendar.get(Calendar.YEAR);
		}
		return false;
	}
	
}
