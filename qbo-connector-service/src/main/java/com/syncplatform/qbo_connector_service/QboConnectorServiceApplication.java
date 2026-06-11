package com.syncplatform.qbo_connector_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class QboConnectorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QboConnectorServiceApplication.class, args);
	}

}
