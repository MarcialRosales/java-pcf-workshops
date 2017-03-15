package com.example.web;

public class FaredFlight {
	String flightId;
	String fare;

	public FaredFlight(String flightId, double fare) {
		this.flightId = flightId;
		this.fare = Double.toString(fare);
	}

	public String getFlightId() {
		return flightId;
	}
	public void setFlightId(String flightId) {
		this.flightId = flightId;
	}
	public String getFare() {
		return fare;
	}
	public void setFare(String fare) {
		this.fare = fare;
	}
	
}
