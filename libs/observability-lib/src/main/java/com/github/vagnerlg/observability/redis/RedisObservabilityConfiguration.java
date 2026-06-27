package com.github.vagnerlg.observability.redis;

import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({LettuceConnectionFactory.class, DefaultClientResources.class})
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisObservabilityConfiguration {

    // Spring Boot 4.x ships LettuceObservationAutoConfiguration which wires Redis command
    // TRACES (OTel spans) and METRICS automatically when micrometer-tracing is on the classpath.
    // This bean serves as a hook point: override "redisObservabilityCustomizer" in any service
    // that needs extra Lettuce configuration (e.g. custom ClientResources, pool tuning).
    @Bean
    @ConditionalOnMissingBean(name = "redisObservabilityCustomizer")
    LettuceClientConfigurationBuilderCustomizer redisObservabilityCustomizer() {
        return builder -> builder.clientResources(DefaultClientResources.create());
    }
}
