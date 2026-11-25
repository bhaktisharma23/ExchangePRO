package com.exchangepro.auth;

import com.exchangepro.user.UserValidation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String password,

        @NotBlank @Size(min = 2, max = 40)
        @Pattern(regexp = UserValidation.DISPLAY_NAME_PATTERN, message = UserValidation.DISPLAY_NAME_MESSAGE)
        String displayName) {

    /** Trims surrounding whitespace before validation (a pasted " a@b.com " is still a valid e-mail). */
    public RegisterRequest {
        email = email == null ? null : email.trim();
        displayName = displayName == null ? null : displayName.trim();
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", displayName=" + displayName + ", password=****]";
    }
}
