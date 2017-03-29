package com.example;

import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import com.example.domain.Flight;
import com.example.domain.FlightRepository;

import io.pivotal.demo.cups.cloud.WebServiceInfo;

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
@Profile({"cloud"})
class CloudConfig  {
	
	@Bean
	Cloud cloud() {
		return new CloudFactory().getCloud();
	}
	
    @Bean
    public WebServiceInfo fareServiceInfo(Cloud cloud) {
        ServiceInfo info = cloud.getServiceInfo("fare-service");
        if (info instanceof WebServiceInfo) {
        	return (WebServiceInfo)info;
        }else {
        	throw new IllegalStateException("fare-service is not of type WebServiceInfo. Did you miss the tag attribute?");
        }
    }
    
    @Bean(name = "fareService")
	public RestTemplate fareService(RestTemplateBuilder builder, WebServiceInfo fareService) {
		return builder.basicAuthorization(fareService.getUserName(), fareService.getPassword()).rootUri(fareService.getUri()).build();
		
	}
}

@Configuration
@Profile("{!cloud}")
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