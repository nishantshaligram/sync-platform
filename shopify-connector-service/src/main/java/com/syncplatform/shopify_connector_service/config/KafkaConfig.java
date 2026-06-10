package com.syncplatform.shopify_connector_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic shopifyWebhooksRaw() {
        return TopicBuilder.name("shopify.webhooks.raw")
                .partitions(10)
                .replicas(1)
                .build();
    }
}