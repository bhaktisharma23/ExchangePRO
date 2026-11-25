package com.exchangepro.user;

import java.time.Instant;
import java.util.UUID;

/** Public view of a user, returned by the API. Never contains the password hash. */
public record UserProfileResponse(UUID id, String email, String displayName, Role role, boolean enabled,
                                  Instant createdAt) {

    static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                user.isEnabled(), user.getCreatedAt());
    }
}
