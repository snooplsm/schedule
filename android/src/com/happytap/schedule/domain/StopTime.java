package com.happytap.schedule.domain;

import java.io.Serializable;
import java.util.Date;

public class StopTime implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String tripId;
	public int sequence;
	public String stopId;
	public Date departure;
	public Date arrival;
	public String serviceId;
	@Override
	public String toString() {
		return "StopTime [" + (stopId != null ? "stopId=" + stopId : "") + "]";
	}

	
	
}