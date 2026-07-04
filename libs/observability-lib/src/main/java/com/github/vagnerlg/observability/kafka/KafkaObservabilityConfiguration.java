package com.github.vagnerlg.observability.kafka;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({AbstractKafkaListenerContainerFactory.class, KafkaTemplate.class})
public class KafkaObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "kafkaObservabilityBeanPostProcessor")
    static BeanPostProcessor kafkaObservabilityBeanPostProcessor() {
        return new KafkaObservabilityPostProcessor();
    }

    private static final class KafkaObservabilityPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof AbstractKafkaListenerContainerFactory<?, ?, ?> factory) {
                factory.getContainerProperties().setObservationEnabled(true);
            } else if (bean instanceof KafkaTemplate<?, ?> template) {
                template.setObservationEnabled(true);
            }
            return bean;
        }
    }
}
