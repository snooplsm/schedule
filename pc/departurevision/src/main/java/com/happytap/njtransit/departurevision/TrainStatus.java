package com.happytap.njtransit.departurevision;

import java.io.Serializable;

public class TrainStatus implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String departs;
	private String train;
	private String dest;
	private String line;
	private String track;
	private String status;
	
	public String getDeparts() {
		return departs;
	}
	public void setDeparts(String departs) {
		this.departs = departs;
	}
	public String getTrain() {
		return train;
	}
	public void setTrain(String train) {
		this.train = train;
	}
	public String getDest() {
		return dest;
	}
	public void setDest(String dest) {
		this.dest = dest;
	}
	public String getLine() {
		return line;
	}
	public void setLine(String line) {
		this.line = line;
	}
	public String getTrack() {
		return track;
	}
	public void setTrack(String track) {
		this.track = track;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	@Override
	public String toString() {
		return "TrainStatus ["
				+ (departs != null ? "departs=" + departs + ", " : "")
				+ (train != null ? "train=" + train + ", " : "")
				+ (dest != null ? "dest=" + dest + ", " : "")
				+ (line != null ? "line=" + line + ", " : "")
				+ (track != null ? "track=" + track + ", " : "")
				+ (status != null ? "status=" + status : "") + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((departs == null) ? 0 : departs.hashCode());
		result = prime * result + ((dest == null) ? 0 : dest.hashCode());
		result = prime * result + ((line == null) ? 0 : line.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((track == null) ? 0 : track.hashCode());
		result = prime * result + ((train == null) ? 0 : train.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrainStatus other = (TrainStatus) obj;
		if (departs == null) {
			if (other.departs != null)
				return false;
		} else if (!departs.equals(other.departs))
			return false;
		if (dest == null) {
			if (other.dest != null)
				return false;
		} else if (!dest.equals(other.dest))
			return false;
		if (line == null) {
			if (other.line != null)
				return false;
		} else if (!line.equals(other.line))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (track == null) {
			if (other.track != null)
				return false;
		} else if (!track.equals(other.track))
			return false;
		if (train == null) {
			if (other.train != null)
				return false;
		} else if (!train.equals(other.train))
			return false;
		return true;
	}
	
	
	
	
	
}
