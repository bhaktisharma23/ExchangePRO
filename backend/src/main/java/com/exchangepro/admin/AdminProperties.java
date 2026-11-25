package com.exchangepro.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional bootstrap administrator ({@code ADMIN_EMAIL}, {@code ADMIN_PASSWORD}). When both are set and no
 * user with that e-mail exists, an ADMIN user is created at startup. There is no default admin password.
 */
@ConfigurationProperties(prefix = "exchangepro.admin")
public record AdminProperties(String email, String password) {

    boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "AdminProperties[email=" + email + ", password=****]";
    }
}
