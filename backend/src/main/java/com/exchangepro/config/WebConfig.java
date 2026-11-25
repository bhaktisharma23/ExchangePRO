package com.exchangepro.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS rules for the REST API. Origins come from the {@code CORS_ALLOWED_ORIGINS} environment variable. */
@Configuration
class WebConfig implements WebMvcConfigurer {

    private final ExchangeProProperties properties;

    WebConfig(ExchangeProProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Correlation-Id")
                .maxAge(3600);
    }
}
