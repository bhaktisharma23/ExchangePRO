package com.exchangepro.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated user's id (the JWT {@code sub} claim) into a controller parameter:
 * <pre>{@code public AccountResponse myAccount(@CurrentUserId UUID userId)}</pre>
 * The id is taken from the cryptographically verified token, never from user-controlled input.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {}
