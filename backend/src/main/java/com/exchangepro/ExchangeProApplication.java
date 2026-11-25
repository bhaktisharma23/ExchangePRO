package com.exchangepro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the EXCHANGE PRO backend.
 *
 * <p>The backend is a <b>modular monolith</b>: one deployable application whose top-level packages
 * ({@code auth}, {@code account}, {@code matching}, {@code ledger}, ...) are treated as separate modules
 * with explicit boundaries, so any of them can later be extracted into its own service.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ExchangeProApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeProApplication.class, args);
    }
}
