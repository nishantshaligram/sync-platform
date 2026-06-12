package com.syncplatform.sync_core_service.config;

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

    @Bean
    public NewTopic syncRunCompleted() {
        return TopicBuilder.name("sync.run.completed")
                .partitions(10).replicas(1).build();
    }

    @Bean
    public NewTopic shopifyWebhooksRaw() {
        return TopicBuilder.name("shopify.webhooks.raw")
                .partitions(10).replicas(1).build();
    }

    @Bean
    public NewTopic qboCommands() {
        return TopicBuilder.name("qbo.commands")
                .partitions(10).replicas(1).build();
    }

    @Bean
    public NewTopic qboCommandResults() {
        return TopicBuilder.name("qbo.command.results")
                .partitions(10).replicas(1).build();
    }
}