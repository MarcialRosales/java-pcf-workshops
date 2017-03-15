package com.example;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.domain.Flight;
import com.example.domain.FlightService;

@SpringBootApplication
public class FlightAvailabilityApplication {

	@Bean
	FlightService loadFlights() {
		return (String from, String destination) -> {
			return Stream.of("MAD/GTW", "MAD/FRA", "MAD/LHR", "MAD/ACE").map(name -> new Flight(name))
					.collect(Collectors.toList());

		};
	}

	public static void main(String[] args) {
		SpringApplication.run(FlightAvailabilityApplication.class, args);
	}
}
