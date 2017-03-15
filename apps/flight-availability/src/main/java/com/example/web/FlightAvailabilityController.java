package com.example.web;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.domain.FaredFlight;
import com.example.domain.Flight;
import com.example.domain.FlightService;

@RestController
class FlightAvailabilityController {

	@Autowired
	private FlightService flightService;

	
	public FlightAvailabilityController(FlightService flightService) {
		super();
		this.flightService = flightService;
	}


	@RequestMapping("/")
	Collection<Flight> search(@RequestParam String origin, @RequestParam String destination) {
		if (origin == null) {
			throw new IllegalArgumentException("missing origin");
		}
		if (destination == null) {
			throw new IllegalArgumentException("missing destination");
		}

		return flightService.find(origin, destination);
	}
	@RequestMapping("/fares")
	Collection<FaredFlight> fare(@RequestParam String origin, @RequestParam String destination) {
		if (origin == null) {
			throw new IllegalArgumentException("missing origin");
		}
		if (destination == null) {
			throw new IllegalArgumentException("missing destination");
		}

		return flightService.fare(origin, destination);
	}

}
