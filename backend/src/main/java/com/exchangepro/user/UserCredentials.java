package com.exchangepro.user;

import java.util.UUID;

/**
 * What the auth module needs to verify a login. Contains the password HASH, so it must never be serialized
 * into an HTTP response (it is deliberately not used by any controller).
 */
public record UserCredentials(UUID id, String email, String passwordHash, String displayName, Role role,
                              boolean enabled) {

    @Override
    public String toString() {
        // Keep the hash out of logs and debugger output.
        return "UserCredentials[id=" + id + ", email=" + email + ", role=" + role + ", enabled=" + enabled + "]";
    }
}
