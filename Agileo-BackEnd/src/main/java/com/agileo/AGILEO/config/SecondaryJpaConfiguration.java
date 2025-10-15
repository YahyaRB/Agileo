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
        entityManagerFactoryRef = "secondaryEntityManagerFactory",
        transactionManagerRef = "secondaryTransactionManager",
        basePackages = {"com.agileo.AGILEO.repository.secondary"}
)
public class SecondaryJpaConfiguration {

    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("secondaryDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();

        // ✅ NONE : Aucune validation car base de données legacy non modifiable
        properties.put("hibernate.hbm2ddl.auto", "none");

        // 🔧 STRATÉGIE DE NOMMAGE : Utiliser les noms physiques tels quels
        properties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

        properties.put("hibernate.format_sql", false);
        properties.put("hibernate.show_sql", false);

        return builder
                .dataSource(dataSource)
                .packages("com.agileo.AGILEO.entity.secondary")
                .persistenceUnit("secondary")
                .properties(properties)
                .build();
    }

    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory secondaryEntityManagerFactory) {
        return new JpaTransactionManager(secondaryEntityManagerFactory);
    }
}