package com.github.vagnerlg.main.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class OpenTelemetryAppenderConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void installOpenTelemetryAppender(ApplicationReadyEvent event) {
        OpenTelemetry openTelemetry = event.getApplicationContext().getBean(OpenTelemetry.class);
        OpenTelemetryAppender.install(openTelemetry);
    }
}
