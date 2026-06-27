package com.github.vagnerlg.observability.mongo;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.observability.ContextProviderFactory;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MongoObservationCommandListener.class)
@ConditionalOnBean(ObservationRegistry.class)
public class MongoObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MongoClientSettingsBuilderCustomizer mongoObservationCustomizer(ObservationRegistry observationRegistry) {
        return settings -> settings
                .contextProvider(ContextProviderFactory.create(observationRegistry))
                .addCommandListener(new MongoObservationCommandListener(observationRegistry));
    }
}
