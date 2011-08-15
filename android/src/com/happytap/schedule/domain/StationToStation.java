package com.happytap.schedule.domain;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

public class StationToStation implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		public String departId;
		public String arriveId;
		public Calendar departTime;
		public Calendar arriveTime;		
		public String blockId;
		
		public List<StationToStation> connections;
		
		public Calendar getDepartTime() {
			return departTime;
		}
		
		public Calendar getArriveTime() {
			return arriveTime;
		}

		@Override
		public String toString() {
			return "StationToStation ["
					+ (departTime != null ? "departTime=" + departTime.getTime() + ", "
							: "")
					+ (arriveTime != null ? "arriveTime=" + arriveTime.getTime() + ", "
							: "") + (blockId != null ? "tripId=" + blockId : "")
					+ "]";
		}		
		
	}