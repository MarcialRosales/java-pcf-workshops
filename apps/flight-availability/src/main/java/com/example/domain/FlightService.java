package com.example.domain;

import java.util.Collection;

public interface FlightService {

	public Collection<Flight> find(String origin, String destination);
	public Collection<FaredFlight> fare(String origin, String destination);
}
