package com.happytap.schedule.domain;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class StationInterval extends StationToStation {

	public int level;

	public int transferDuration;
	
	public int arriveSequence;
	public int departSequence;

	public Schedule schedule;

	private transient Boolean transfer;

	public int getTransferDuration() {
		return transferDuration;
	}

	public void setTransferDuration(int transferDuration) {
		this.transferDuration = transferDuration;
	}

	public final boolean isTransfer() {
		if (transfer == null)
			transfer = schedule.transferEdges.containsKey(departId + "-"
					+ arriveId);
		return transfer;
	}

	public StationInterval next() {
		int nextLevel = level + 1;
		String[] pair = schedule.transfers[nextLevel];
		Integer transferDuration = schedule.transferEdges.get(pair[0] + "-"
				+ pair[1]);
		StationInterval si = new StationInterval();
		si.departId = pair[0];
		si.arriveId = pair[1];
		si.level = nextLevel;
		si.schedule = schedule;
		if (transferDuration != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(arriveTime.getTimeInMillis());
			cal.add(Calendar.SECOND, transferDuration);
			si.departTime = arriveTime;
			si.arriveTime = cal;
		} else {
			List<StationInterval> k = schedule.stationIntervals.get(pair);
			StationInterval tmp = new StationInterval();
			tmp.departTime = arriveTime;
			int pos = Integer.MAX_VALUE;
			try {
				pos = Math.abs(Collections.binarySearch(k, tmp,
						new Comparator<StationInterval>() {

							@Override
							public int compare(StationInterval lhs,
									StationInterval rhs) {
								return lhs.departTime.compareTo(rhs.departTime);
							}
						}));
			} catch (Exception e) {
				e.printStackTrace();
			}
			pos = pos - 1;

			if (pos >= 0 && pos < k.size()) {
				StationInterval v = k.get(pos);
				if (tmp.departTime.getTimeInMillis() < v.departTime
						.getTimeInMillis()) {
					return v;
				} else {
					if (pos + 1 < k.size()) {
						return k.get(pos + 1);
					}
					return null;
				}
			}
		}
		return si;
	}

	private Calendar convert(Date date, Date dateTime) {
		Calendar caldate = Calendar.getInstance();
		caldate.setTime(date);
		Calendar caldatetime = Calendar.getInstance();
		caldatetime.setTime(dateTime);
		caldate.add(Calendar.DAY_OF_YEAR,
				caldatetime.get(Calendar.DAY_OF_YEAR) - 1);
		caldate.set(Calendar.HOUR_OF_DAY, caldatetime.get(Calendar.HOUR_OF_DAY));
		caldate.set(Calendar.MINUTE, caldatetime.get(Calendar.MINUTE));
		caldate.set(Calendar.SECOND, caldatetime.get(Calendar.SECOND));
		return caldate;
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	public boolean hasNext() {
		if (level == schedule.transfers.length - 1) {
			return false;
		}
		String[] pair = schedule.transfers[level + 1];
		Integer transferDuration = schedule.transferEdges.get(pair[0] + "-"
				+ pair[1]);
		if (transferDuration != null) {
			return true;
		}
		List<StationInterval> k = schedule.stationIntervals.get(pair);
		if (k == null) {
			return false;
		}
		StationInterval tmp = new StationInterval();
		tmp.departTime = arriveTime;
		int pos = Integer.MAX_VALUE;
		try {
			pos = Math.abs(Collections.binarySearch(k, tmp,
					new Comparator<StationInterval>() {

						@Override
						public int compare(StationInterval lhs,
								StationInterval rhs) {
							return lhs.departTime.compareTo(rhs.departTime);
						}
					}));
			pos = pos - 1;

			if (pos >= 0 && pos < k.size()) {
				StationInterval v = k.get(pos);
				if (tmp.departTime.getTimeInMillis() < v.departTime
						.getTimeInMillis()) {
					return true;
				} else {
					return pos + 1 < k.size();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pos < k.size();
	}

	public int totalMinutes() {
		int tot = 0;
		StationInterval k = this;
		Calendar d = k.departTime;
		Calendar a = k.arriveTime;
		while (k.hasNext()) {
			k = k.next();
			a = k.arriveTime;
		}
		if(a==null || d == null || !k.arriveId.equals(schedule.arriveId)) {
			return -1;
		}
		return (int) ((a.getTimeInMillis() - d.getTimeInMillis()) / 60000);
	}

}
