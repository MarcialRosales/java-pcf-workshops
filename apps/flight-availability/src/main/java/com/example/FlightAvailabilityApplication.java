package com.example;

import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
@Configuration
@ConfigurationProperties(prefix = "fare-service")
class FareServiceConfig {
	String uri;
	String username;
	String password;
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	@Bean(name = "fareService")
	public RestTemplate fareService(RestTemplateBuilder builder, FareServiceConfig fareService) {
		return builder.basicAuthorization(getUsername(), getPassword()).rootUri(getUri()).build();
	}
}