package com.agileo.AGILEO.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager",
        basePackages = {"com.agileo.AGILEO.repository.primary"}
)
public class PrimaryJpaConfiguration {

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") DataSource dataSource) {

        Map<String, Object> properties = new HashMap<>();

        // ✅ NONE : Aucune validation car base de données legacy non modifiable
        // Hibernate n'essaiera pas de valider ou modifier le schéma
        properties.put("hibernate.hbm2ddl.auto", "none");

        // 🔧 STRATÉGIE DE NOMMAGE : Utiliser les noms physiques tels quels
        // Cela évite la conversion automatique en minuscules
        properties.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

        // Configuration d'affichage SQL (désactiver en production)
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.use_sql_comments", false);

        return builder
                .dataSource(dataSource)
                .packages("com.agileo.AGILEO.entity.primary")
                .persistenceUnit("primary")
                .properties(properties)
                .build();
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory primaryEntityManagerFactory) {
        return new JpaTransactionManager(primaryEntityManagerFactory);
    }
}