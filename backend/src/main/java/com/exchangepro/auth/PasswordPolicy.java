package com.exchangepro.auth;

import java.nio.charset.StandardCharsets;

/**
 * Password rules. The maximum exists because BCrypt only uses the first 72 BYTES of its input; anything after
 * that would be silently ignored, so longer passwords are rejected rather than truncated.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;
    public static final int MAX_BYTES = 72;
    /** At least one letter and one digit. */
    public static final String PATTERN = "^(?=.*\\p{L})(?=.*\\p{N}).*$";
    public static final String MESSAGE = "must contain at least one letter and one digit";

    private PasswordPolicy() {}

    /** Non-ASCII characters take several UTF-8 bytes, so 72 characters can exceed BCrypt's 72-byte limit. */
    public static boolean fitsBcrypt(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
