package com.syncplatform.sync_core_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SyncCoreServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncCoreServiceApplication.class, args);
	}

}
