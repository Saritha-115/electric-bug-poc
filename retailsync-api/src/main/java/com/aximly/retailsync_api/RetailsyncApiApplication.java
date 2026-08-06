package com.aximly.retailsync_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RetailsyncApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(RetailsyncApiApplication.class, args);
	}
}