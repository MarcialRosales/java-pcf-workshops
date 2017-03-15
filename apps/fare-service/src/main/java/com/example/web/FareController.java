package com.example.web;

import java.util.Arrays;
import java.util.Random;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FareController {
	
	private Random random = new Random(System.currentTimeMillis());
	
	@PostMapping("/")
	public String[] applyFares(@RequestBody Flight[] flights) {
		return Arrays.stream(flights).map(f -> Double.toString(random.nextDouble())).toArray(String[]::new);
	}
}
