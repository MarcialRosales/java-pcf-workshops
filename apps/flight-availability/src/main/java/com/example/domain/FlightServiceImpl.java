package com.example.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FlightServiceImpl implements FlightService {

	@Autowired
	FlightRepository repo;

	@Autowired
	FareService fareService;

	public FlightServiceImpl(FlightRepository repo) {
		super();
		this.repo = repo;
	}

	@Override
	public Collection<Flight> find(String origin, String destination) {
		return repo.findByOriginAndDestination(origin, destination);
	}

	@Override
	public Collection<FaredFlight> fare(String origin, String destination) {
		Collection<Flight> flights = find(origin, destination);
		if (flights.isEmpty()) {
			return Collections.emptyList();
		}
		
		Flight[] flightArray = new Flight[flights.size()];
		
		String[] fares = fareService.fares(flights.toArray(flightArray));
		return IntStream.range(0, fares.length).mapToObj(i -> new FaredFlight(flightArray[i], fares[i])).collect(Collectors.toList());
		
	}

}
