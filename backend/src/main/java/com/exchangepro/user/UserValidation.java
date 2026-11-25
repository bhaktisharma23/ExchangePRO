package com.exchangepro.user;

/** Validation rules shared by registration and profile updates. */
public final class UserValidation {

    /**
     * Letters, combining marks, digits, spaces and . _ - ' only; no control characters or markup.
     * {@code \p{M}} is required for Indic scripts: Devanagari vowel signs (e.g. the "ा" in "आशा") are
     * combining marks, not letters.
     */
    public static final String DISPLAY_NAME_PATTERN = "^[\\p{L}\\p{M}\\p{N} ._'-]+$";
    public static final String DISPLAY_NAME_MESSAGE = "may contain only letters, digits, spaces and . _ ' -";

    private UserValidation() {}
}
