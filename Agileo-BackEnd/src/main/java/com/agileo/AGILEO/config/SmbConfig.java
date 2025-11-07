package com.agileo.AGILEO.config;

import com.hierynomus.smbj.SMBClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SmbProps.class)
public class SmbConfig {
    @Bean
    public SMBClient smbClient() {
        return new SMBClient(); // timeouts et tuning par défaut conviennent dans 95% des cas
    }
}