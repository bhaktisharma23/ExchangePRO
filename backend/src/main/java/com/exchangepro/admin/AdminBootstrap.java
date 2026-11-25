package com.exchangepro.admin;

import com.exchangepro.auth.PasswordPolicy;
import com.exchangepro.user.Role;
import com.exchangepro.user.UserService;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account from environment variables, because an admin cannot be created through the
 * public registration endpoint (which always creates USERs). Admins do not receive a trading account.
 * An existing user is never modified or promoted.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final Pattern PASSWORD = Pattern.compile(PasswordPolicy.PATTERN);

    private final AdminProperties properties;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    AdminBootstrap(AdminProperties properties, UserService userService, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("No bootstrap admin configured (ADMIN_EMAIL / ADMIN_PASSWORD not set)");
            return;
        }
        if (userService.findCredentialsByEmail(properties.email()).isPresent()) {
            log.info("Bootstrap admin {} already exists; nothing to do", UserService.normalizeEmail(properties.email()));
            return;
        }
        String password = properties.password();
        if (password.length() < PasswordPolicy.MIN_LENGTH || !PasswordPolicy.fitsBcrypt(password)
                || !PASSWORD.matcher(password).matches()) {
            throw new IllegalStateException("ADMIN_PASSWORD does not satisfy the password policy "
                    + "(8-72 characters, at least one letter and one digit)");
        }
        userService.createUser(properties.email(), passwordEncoder.encode(password), "Administrator", Role.ADMIN);
        log.info("Created bootstrap admin {}", UserService.normalizeEmail(properties.email()));
    }
}
