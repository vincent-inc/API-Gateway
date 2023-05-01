package com.vincentcrop.vshop.APIGateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@RestController
public class ApiGatewayApplication {

	@Value("${spring.profiles.active}")
	private String env = "?";

	public static void main(String[] args) 
	{
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@GetMapping("/_status/healthz")
	public String healthCheck() {
		return String.format("API Gateway %s is up and running", env);
	}
}
