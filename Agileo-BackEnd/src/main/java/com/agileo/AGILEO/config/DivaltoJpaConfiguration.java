package com.agileo.AGILEO.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        entityManagerFactoryRef = "divaltoEntityManagerFactory",
        transactionManagerRef = "divaltoTransactionManager",
        basePackages = {"com.agileo.AGILEO.repository.divalto"}
)
public class DivaltoJpaConfiguration {

    @Bean(name = "divaltoEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean divaltoEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("divaltoDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();

        // ✅ CORRECTION PRINCIPALE : Ajouter le dialect SQL Server
        properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");

        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        properties.put("hibernate.show_sql", false);

        // ✅ Propriétés supplémentaires pour assurer la compatibilité
        properties.put("hibernate.jdbc.batch_size", "20");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.format_sql", "false");

        return builder
                .dataSource(dataSource)
                .packages("com.agileo.AGILEO.entity.divalto")
                .persistenceUnit("divalto")
                .properties(properties)
                .build();
    }

    @Bean(name = "divaltoTransactionManager")
    public PlatformTransactionManager divaltoTransactionManager(
            @Qualifier("divaltoEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}