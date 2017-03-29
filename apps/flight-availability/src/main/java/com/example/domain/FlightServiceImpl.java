package com.example.domain;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FlightServiceImpl implements FlightService {

	@Autowired
	FlightRepository repo;

	public FlightServiceImpl(FlightRepository repo) {
		super();
		this.repo = repo;
	}

	@Override
	public Collection<Flight> find(String origin, String destination) {
		return repo.findByOriginAndDestination(origin, destination);
	}

}
