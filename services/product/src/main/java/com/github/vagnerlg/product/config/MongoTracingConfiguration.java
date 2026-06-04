package com.github.vagnerlg.product.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;

@Configuration
class MongoTracingConfiguration {

    @Bean
    MongoClientSettingsBuilderCustomizer mongoObservationCommandListenerCustomizer(ObservationRegistry observationRegistry) {
        return settings -> settings
                .contextProvider(ContextProviderFactory.create(observationRegistry))
                .addCommandListener(new MongoObservationCommandListener(observationRegistry));
    }
}
