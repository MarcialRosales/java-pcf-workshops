package com.example;

import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.domain.Flight;
import com.example.domain.FlightRepository;

@SpringBootApplication
public class FlightAvailabilityApplication {

	@Bean
	CommandLineRunner loadFlights(FlightRepository flightRepository) {
		return args -> {
			Stream.of("MAD/GTW", "MAD/FRA", "MAD/LHR", "MAD/ACE")
					.forEach(name -> flightRepository.save(new Flight(name)));
			flightRepository.findAll().forEach(System.out::println);
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(FlightAvailabilityApplication.class, args);
	}
}
