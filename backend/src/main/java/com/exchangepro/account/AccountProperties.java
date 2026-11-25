package com.exchangepro.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** @param initialBalance virtual funds credited to every new trading account (INR 1,00,000 by default) */
@Validated
@ConfigurationProperties(prefix = "exchangepro.account")
public record AccountProperties(@NotNull @PositiveOrZero BigDecimal initialBalance) {}
