package com.github.vagnerlg.observability.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class HttpObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "httpLoggingFilterRegistration")
    FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilterRegistration(
            @Value("${observability.http.max-body-bytes:4096}") int maxBodyBytes) {
        var filter = new HttpLoggingFilter(maxBodyBytes);
        var registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
