package com.github.vagnerlg.observability.jdbc;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ProxyDataSourceBuilder.class, DataSource.class})
public class JdbcObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "jdbcProxyBeanPostProcessor")
    static BeanPostProcessor jdbcProxyBeanPostProcessor() {
        return new JdbcProxyPostProcessor();
    }

    private static final class JdbcProxyPostProcessor implements BeanPostProcessor {

        private static final Logger log = LoggerFactory.getLogger("jdbc.query");

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof DataSource ds && !(bean instanceof ProxyDataSource)) {
                return ProxyDataSourceBuilder.create(ds)
                        .name("observed-" + beanName)
                        .listener(new LoggingQueryListener())
                        .build();
            }
            return bean;
        }
    }

    private static final class LoggingQueryListener implements QueryExecutionListener {

        private static final Logger log = LoggerFactory.getLogger("jdbc.query");

        @Override
        public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {}

        @Override
        public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            for (var qi : queryInfoList) {
                log.atInfo()
                        .addKeyValue("jdbc.query", qi.getQuery())
                        .addKeyValue("jdbc.params", qi.getQueryArgsList())
                        .addKeyValue("jdbc.duration_ms", execInfo.getElapsedTime())
                        .addKeyValue("jdbc.success", execInfo.isSuccess())
                        .log("JDBC {}ms", execInfo.getElapsedTime());
            }
        }
    }
}
