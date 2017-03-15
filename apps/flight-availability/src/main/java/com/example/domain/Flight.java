package com.example.domain;

public class Flight {

	private Long id;
	
	private String origin;
	private String destination;
	
	
	Flight() {
		
	}
	public Flight(String name) {
		origin = name.split("/")[0];
		destination = name.split("/")[1];
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
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
