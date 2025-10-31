package com.agileo.AGILEO.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DivaltoDataSourceConfig {

    @Bean(name = "divaltoDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.divalto") // ✅ Simplifier
    public DataSource divaltoDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }
}