package com.github.vagnerlg.product;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:8"));
    }

    @Bean
    KafkaContainer kafkaContainer() {
        var container = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
        container.start();
        return container;
    }

    @Bean
    DynamicPropertyRegistrar kafkaProperties(KafkaContainer kafka) {
        return registry -> registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
