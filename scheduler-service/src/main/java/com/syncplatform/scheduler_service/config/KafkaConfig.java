package com.syncplatform.scheduler_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic syncRunRequested() {
        return TopicBuilder.name("sync.run.requested")
                .partitions(10).replicas(1).build();
    }
}