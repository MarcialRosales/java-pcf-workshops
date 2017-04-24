package com.example;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.HeadersBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.exception.HystrixRuntimeException;

@SpringBootApplication
@EnableCircuitBreaker
public class ClientApplication {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}
}

@RestController
class Client {

	@Autowired
	@Qualifier("serviceA")
	BusinessService plainService;

	@GetMapping("/req0")
	public String request0(@RequestParam String message, RequestEntity<?> req) {
		return this.plainService.request(buildRequest(req, message));
	}

	@Autowired
	@Qualifier("hystrixServiceAWithThread")
	BusinessService hystrixService1;

	@GetMapping("/req1")
	public String request1(@RequestParam String message, RequestEntity<?> req) {
		return this.hystrixService1.request(buildRequest(req, message));
	}

	@GetMapping("/req2")
	public String request2(@RequestParam String message, RequestEntity<?> req) {
		return this.hystrixService1.request2(buildRequest(req, message));
	}

	@ExceptionHandler({ HystrixRuntimeException.class })
	void handleBadRequests(HttpServletResponse response, HystrixRuntimeException e) throws IOException {
		System.err.println(e.getMessage());
		response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "forced by caller");
		
	}

	@Autowired
	@Qualifier("hystrixServiceAWithThread2")
	BusinessService hystrixService2;

	@GetMapping("/req3")
	public String request3(@RequestParam String message, RequestEntity<?> req) {
		return this.hystrixService2.request(buildRequest(req, message));

	}

	@GetMapping("/req4")
	public String request4(@RequestParam String message, RequestEntity<?> req) {
		return this.hystrixService2.request2(buildRequest(req, message));

	}

	@Autowired
	@Qualifier("hystrixServiceAWithSemaphore")
	BusinessService hystrixService3;

	@GetMapping("/req5")
	public String request5(@RequestParam String message, RequestEntity<?> req) {
		return this.hystrixService3.request(buildRequest(req, message));

	}

	private RequestEntity<?> buildRequest(RequestEntity<?> originator, String message) {
		URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost:8080").queryParam("message", message).build()
				.toUri();
		HeadersBuilder<?> rb = RequestEntity.get(uri);
		originator.getHeaders()
				.forEach((String h, java.util.List<String> v) -> rb.header(h, v.toArray(new String[v.size()])));
		return rb.build();
	}

}

@Service("hystrixServiceAWithThread")
@DefaultProperties(groupKey = "BusinesService1", ignoreExceptions = { BusinessException.class,
		ServiceNotAvailableException.class })
class BusinessService1 extends BusinessServiceImpl {

	@HystrixCommand(commandKey = "requestHandler11")
	public String request(RequestEntity<?> req) {
		return super.request(req);
	}

	@HystrixCommand(commandKey = "requestHandler12", fallbackMethod = "returnEmptyRequest")
	public String request2(RequestEntity<?> req) {
		System.err.println("calling request2");
		return super.request2(req);
	}

	private String returnEmptyRequest(RequestEntity<?> req) {
		return "";
	}

}

@Service("hystrixServiceAWithThread2")
@DefaultProperties(groupKey = "BusinesService2", ignoreExceptions = { BusinessException.class })
class BusinessService2 extends BusinessServiceImpl {

	@HystrixCommand(commandKey = "requestHandler21", fallbackMethod = "hystrixRequestHandlerFallback")
	public String request(RequestEntity<?> req) {
		return super.request(req);
	}

	@HystrixCommand(commandKey = "requestHandler22", fallbackMethod = "hystrixRequestHandlerFallback")
	public String request2(RequestEntity<?> req) {
		return super.request2(req);
	}

	private String hystrixRequestHandlerFallback(RequestEntity<?> req) {
		return "";
	}

}

@Service("hystrixServiceAWithSemaphore")
@DefaultProperties(groupKey = "BusinesService3", ignoreExceptions = { BusinessException.class }, commandProperties = {
		@HystrixProperty(name = "execution.isolation.strategy", value = "SEMAPHORE"),
		@HystrixProperty(name = "execution.isolation.semaphore.maxConcurrentRequests", value = "5"),

})
class BusinessService3 extends BusinessServiceImpl {

	@HystrixCommand(commandKey = "requestHandler31", fallbackMethod = "hystrixRequestHandlerFallback")
	public String request(RequestEntity<?> req) {
		return super.request(req);
	}

	@HystrixCommand(commandKey = "requestHandler32", fallbackMethod = "hystrixRequestHandlerFallback")
	public String request2(RequestEntity<?> req) {
		return super.request2(req);
	}

	private String hystrixRequestHandlerFallback(RequestEntity<?> req) {
		return "";
	}

}

interface BusinessService {
	String request(RequestEntity<?> req);

	String request2(RequestEntity<?> req);

}

@Service("serviceA")
class BusinessServiceImpl implements BusinessService {
	@Autowired
	RestTemplate template;

	public String request(RequestEntity<?> req) {
		try {
			ResponseEntity<String> response = this.template.exchange(req, String.class);
			if (response.getStatusCode().is4xxClientError()) {
				System.err.println("is4xxClientError");
				throw new BusinessException(response.getStatusCode().getReasonPhrase());
			}
			return response.getBody();
		} catch (HttpClientErrorException e) {
			if (HttpStatus.valueOf(e.getRawStatusCode()).is4xxClientError()) {
				System.err.println("is4xxClientError");
				throw new BusinessException(e.getMessage());
			} else {
				throw e;
			}
		}

	}

	public String request2(RequestEntity<?> req) {
		return request(req);
	}

}

class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

}

class ServiceNotAvailableException extends RuntimeException {

}