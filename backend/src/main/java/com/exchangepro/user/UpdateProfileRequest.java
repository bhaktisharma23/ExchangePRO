package com.exchangepro.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 2, max = 40)
        @Pattern(regexp = UserValidation.DISPLAY_NAME_PATTERN, message = UserValidation.DISPLAY_NAME_MESSAGE)
        String displayName) {}
