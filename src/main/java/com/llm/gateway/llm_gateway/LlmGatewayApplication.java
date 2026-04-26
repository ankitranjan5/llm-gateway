package com.llm.gateway.llm_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LlmGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmGatewayApplication.class, args);
	}

}
