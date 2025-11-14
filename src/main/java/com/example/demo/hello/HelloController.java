package com.example.demo.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hello")
public class HelloController {

	@GetMapping
	public Map<String, String> hello() {
		// Hardcoded secret (Security Hotspot)
        String apiKey = "SECRET_API_KEY_123";

        // Unused variable (Code Smell)
        int unusedValue = 42;
		
		return Map.of("message", "Welcome to DevSecOps World !!!");
	}
}


