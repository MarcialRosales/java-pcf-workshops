package com.example.web;

public class Flight {

	private String id;
	
	private String origin;
	private String destination;
	
	
	public Flight() {
		
	}
	public Flight(String name) {
		origin = name.split("/")[0];
		destination = name.split("/")[1];
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
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
	
}
