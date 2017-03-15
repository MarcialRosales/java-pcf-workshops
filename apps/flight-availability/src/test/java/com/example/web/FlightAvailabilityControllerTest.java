package com.example.web;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.example.domain.FlightService;

public class FlightAvailabilityControllerTest {

	FlightService service = mock(FlightService.class);
	FlightAvailabilityController controller;
	
	@Before
	public void init() {
		controller = new FlightAvailabilityController(service);
	}
	
	@Test
	public void shallReturnEmptyCollectionWhenThereAreNoFlights() {
		String origin = "MAD";
		String destination = "FRA";
		
		when(service.find(origin, destination)).thenReturn(Collections.emptyList());
		assertTrue(service.find(origin, destination).isEmpty());
		
	}

}
