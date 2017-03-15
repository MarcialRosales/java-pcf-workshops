package com.example.domain;

public class FaredFlight {
	String flightId;
	String fare;
	
	private String origin;
	private String destination;
	
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}
	public FaredFlight(Flight flight, String fare) {
		this.flightId = flight.getId().toString();
		this.origin = flight.getOrigin();
		this.destination = flight.getDestination();
		this.fare = fare;
	}

	public String getId() {
		return flightId;
	}
	public void setId(String flightId) {
		this.flightId = flightId;
	}
	public String getFare() {
		return fare;
	}
	public void setFare(String fare) {
		this.fare = fare;
	}
	
}
