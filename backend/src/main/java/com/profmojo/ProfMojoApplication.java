package com.profmojo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class
ProfMojoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProfMojoApplication.class, args);
	}
}
 