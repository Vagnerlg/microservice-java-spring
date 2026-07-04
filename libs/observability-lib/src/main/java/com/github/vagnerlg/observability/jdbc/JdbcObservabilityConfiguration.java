package com.github.vagnerlg.observability.jdbc;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
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
    static BeanPostProcessor jdbcProxyBeanPostProcessor(ObjectProvider<ObservationRegistry> registryProvider) {
        return new JdbcProxyPostProcessor(registryProvider);
    }

    private static final class JdbcProxyPostProcessor implements BeanPostProcessor {

        private final ObjectProvider<ObservationRegistry> registryProvider;

        JdbcProxyPostProcessor(ObjectProvider<ObservationRegistry> registryProvider) {
            this.registryProvider = registryProvider;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof DataSource ds && !(bean instanceof ProxyDataSource)) {
                return ProxyDataSourceBuilder.create(ds)
                        .name("observed-" + beanName)
                        .listener(new ObservationQueryListener(registryProvider))
                        .build();
            }
            return bean;
        }
    }

    private static final class ObservationQueryListener implements QueryExecutionListener {

        private static final Logger log = LoggerFactory.getLogger("jdbc.query");
        private static final String OBS_KEY = "jdbc.observation";

        private final ObjectProvider<ObservationRegistry> registryProvider;

        ObservationQueryListener(ObjectProvider<ObservationRegistry> registryProvider) {
            this.registryProvider = registryProvider;
        }

        @Override
        public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            var registry = registryProvider.getIfAvailable();
            if (registry == null) return;

            var sql = queryInfoList.isEmpty() ? "" : queryInfoList.get(0).getQuery();
            var operation = sql.isBlank() ? "UNKNOWN" : sql.stripLeading().split("\\s+")[0].toUpperCase();
            var obs = Observation.createNotStarted("db.query", registry)
                    .lowCardinalityKeyValue("db.system", "postgresql")
                    .lowCardinalityKeyValue("db.operation", operation)
                    .highCardinalityKeyValue("db.statement", sql)
                    .start();
            execInfo.addCustomValue(OBS_KEY, obs);
        }

        @Override
        public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
            var obs = execInfo.getCustomValue(OBS_KEY, Observation.class);
            if (obs != null) {
                if (!execInfo.isSuccess() && execInfo.getThrowable() != null) {
                    obs.error(execInfo.getThrowable());
                }
                obs.stop();
            }
            log.atInfo()
                    .addKeyValue("jdbc.query", queryInfoList.isEmpty() ? "" : queryInfoList.get(0).getQuery())
                    .addKeyValue("jdbc.duration_ms", execInfo.getElapsedTime())
                    .addKeyValue("jdbc.success", execInfo.isSuccess())
                    .log("JDBC {}ms", execInfo.getElapsedTime());
        }
    }
}
