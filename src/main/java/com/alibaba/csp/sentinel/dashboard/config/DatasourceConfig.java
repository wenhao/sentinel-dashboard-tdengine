package com.alibaba.csp.sentinel.dashboard.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatasourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.tdengine-datasource")
    @ConditionalOnProperty(prefix = "tdengine", value = "enabled", havingValue = "true")
    public DataSource tdEngineDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    @ConditionalOnProperty(prefix = "tdengine", value = "enabled", havingValue = "false")
    public DataSource h2DataSource() {
        return DataSourceBuilder.create().build();
    }
}
