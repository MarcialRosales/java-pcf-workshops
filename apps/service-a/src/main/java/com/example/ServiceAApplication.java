package com.example;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ServiceAApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceAApplication.class, args);
	}
}
@RestController
@RefreshScope
class ServiceA {
	
	@Value("${defaultDelay:0}") int defaultDelay;
	@Value("${defaultResponse:200}") int defaultResponse;
	
	@GetMapping
	public String echo(@RequestParam String message, @RequestHeader(defaultValue = "0") int  delay,
			@RequestHeader(defaultValue = "200") int response) {
		HttpStatus status = HttpStatus.valueOf(response);

		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			}
		}
		if (status.is2xxSuccessful()) {
			return message;
		}else {
			throw new ExpectedHttpError(status);
		}
	}
	@ExceptionHandler({ExpectedHttpError.class})
	void handleBadRequests(HttpServletResponse response, Exception e) throws IOException {
	    response.sendError(((ExpectedHttpError)e).error.value(), "forced by caller");
	    
	}
}
class ExpectedHttpError extends RuntimeException {
	HttpStatus error;

	public ExpectedHttpError(HttpStatus error) {
		super();
		this.error = error;
	}
	
}