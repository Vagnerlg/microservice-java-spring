package com.github.vagnerlg.observability.logback;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@ConditionalOnClass(OpenTelemetryAppender.class)
public class OtelLogbackAppenderConfiguration {

    @EventListener(ApplicationReadyEvent.class)
    public void install(ApplicationReadyEvent event) {
        OpenTelemetry openTelemetry = event.getApplicationContext().getBean(OpenTelemetry.class);
        OpenTelemetryAppender.install(openTelemetry);
    }
}
