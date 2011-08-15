package com.happytap.transit;

import java.io.Serializable;

public class ReverseGeocode {

	public static class Results implements Serializable {
		private Result[] results;

		public Result[] getResults() {
			return results;
		}

		public void setResults(Result[] results) {
			this.results = results;
		}
	}
	
	public static class Result implements Serializable {
		private AddressComponent[] addressComponents;

		public AddressComponent[] getAddressComponents() {
			return addressComponents;
		}

		public void setAddressComponents(AddressComponent[] addressComponents) {
			this.addressComponents = addressComponents;
		}
	}
	
	public static class AddressComponent implements Serializable {
	
		private String longName;
		private String shortName;
		private String[] types;
		public String getLongName() {
			return longName;
		}
		public void setLongName(String longName) {
			this.longName = longName;
		}
		public String getShortName() {
			return shortName;
		}
		public void setShortName(String shortName) {
			this.shortName = shortName;
		}
		public String[] getTypes() {
			return types;
		}
		public void setTypes(String[] types) {
			this.types = types;
		}
	
		
		
	}
	
}
