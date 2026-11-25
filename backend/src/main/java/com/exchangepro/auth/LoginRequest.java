package com.exchangepro.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Size(max = 254) String email,
                           @NotBlank @Size(max = PasswordPolicy.MAX_LENGTH) String password) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
