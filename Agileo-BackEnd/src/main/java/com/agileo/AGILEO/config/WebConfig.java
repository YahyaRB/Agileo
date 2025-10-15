package com.agileo.AGILEO.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Fichiers JavaScript et CSS
        registry.addResourceHandler("/*.js", "/*.css", "/*.ico", "/*.png", "/*.jpg", "/*.svg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);

        // Assets Angular
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600);

        // Routes Angular avec fallback vers index.html
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // Si c'est une vraie ressource, la retourner
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // Pour les routes Angular (pas les API), retourner index.html
                        if (!resourcePath.startsWith("api/") &&
                                !resourcePath.startsWith("actuator/")) {
                            return new ClassPathResource("/static/index.html");
                        }

                        return null;
                    }
                });
    }
}