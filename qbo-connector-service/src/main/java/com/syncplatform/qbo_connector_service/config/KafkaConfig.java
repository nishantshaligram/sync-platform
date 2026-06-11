package com.syncplatform.qbo_connector_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic qboCommands() {
        return TopicBuilder.name("qbo.commands")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic qboCommandResults() {
        return TopicBuilder.name("qbo.command.results")
                .partitions(10)
                .replicas(1)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}