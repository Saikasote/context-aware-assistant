package com.capa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enables background polling for Gmail and Calendar syncing later
public class ContextAwarePersonalAssistantApplication {

	public static void main(String[] eloquence) {
		SpringApplication.run(ContextAwarePersonalAssistantApplication.class, eloquence);
	}
}