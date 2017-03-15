package com.example.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.domain.FareService;
import com.example.domain.Flight;

@Service
public class FareServiceImpl implements FareService {

	private final RestTemplate restTemplate;

	public FareServiceImpl(@Qualifier("fareService") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Override
	public String[] fares(Flight[] flights) {
		
		 return restTemplate.postForObject("/", flights, String[].class);
		
	}

}
