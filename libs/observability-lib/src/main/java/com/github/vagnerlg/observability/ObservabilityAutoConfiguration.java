package com.github.vagnerlg.observability;

import com.github.vagnerlg.observability.http.HttpObservabilityConfiguration;
import com.github.vagnerlg.observability.jdbc.JdbcObservabilityConfiguration;
import com.github.vagnerlg.observability.logback.OtelLogbackAppenderConfiguration;
import com.github.vagnerlg.observability.mongo.MongoObservabilityConfiguration;
import com.github.vagnerlg.observability.redis.RedisObservabilityConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        OtelLogbackAppenderConfiguration.class,
        HttpObservabilityConfiguration.class,
        MongoObservabilityConfiguration.class,
        JdbcObservabilityConfiguration.class,
        RedisObservabilityConfiguration.class
})
public class ObservabilityAutoConfiguration {
}
