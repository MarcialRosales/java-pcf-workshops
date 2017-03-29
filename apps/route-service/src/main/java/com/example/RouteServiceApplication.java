package com.example;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class RouteServiceApplication {

	 @Bean
	    RestOperations restOperations() {
	        RestTemplate restTemplate = new RestTemplate();
	        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
	        return restTemplate;
	    }

	   
	public static void main(String[] args) {
		SpringApplication.run(RouteServiceApplication.class, args);
	}
}

@RestController
class RouteService {

	static final String FORWARDED_URL = "X-CF-Forwarded-Url";

	static final String PROXY_METADATA = "X-CF-Proxy-Metadata";

	static final String PROXY_SIGNATURE = "X-CF-Proxy-Signature";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final RestOperations restOperations;

	@Autowired
	RouteService(RestOperations restOperations) {
		this.restOperations = restOperations;
	}

	@RequestMapping(headers = { FORWARDED_URL, PROXY_METADATA, PROXY_SIGNATURE })
	ResponseEntity<?> service(RequestEntity<byte[]> incoming, @RequestHeader(name = "Authorization", required = false) String jwtToken ) {
		if (jwtToken == null) {
			this.logger.error("Incoming Request missing JWT Token: {}", incoming);
			return badRequest();
		}else if (!isValid(jwtToken)) {
			this.logger.error("Incoming Request missing or not valid JWT Token: {}", incoming);
			return notAuthorized();
		}

		RequestEntity<?> outgoing = getOutgoingRequest(incoming);
		this.logger.debug("Outgoing Request: {}", outgoing);
		

		return this.restOperations.exchange(outgoing,  byte[].class);
	}
	private static boolean isValid(String jwtToken) {
		return jwtToken.contains("Bearer"); // TODO add JWT Validation
	}
	private static ResponseEntity<?> notAuthorized() {
		return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
	}
	private static ResponseEntity<?> badRequest() {
		return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}
	private static RequestEntity<?> getOutgoingRequest(RequestEntity<?> incoming) {
		HttpHeaders headers = new HttpHeaders();
		headers.putAll(incoming.getHeaders());

		URI uri = headers.remove(FORWARDED_URL).stream().findFirst().map(URI::create)
				.orElseThrow(() -> new IllegalStateException(String.format("No %s header present", FORWARDED_URL)));
		headers.remove("Authorization");
		
		return new RequestEntity<>(incoming.getBody(), headers, incoming.getMethod(), uri);
	}
}

