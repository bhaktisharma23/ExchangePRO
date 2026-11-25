package com.exchangepro.common.error;

/**
 * Base class for expected, domain-level failures (e.g. "insufficient funds", "instrument not found").
 * The {@link GlobalExceptionHandler} maps it to an HTTP response using its {@link ErrorCode}.
 * Unexpected failures should NOT use this class; they surface as {@code INTERNAL_ERROR}.
 */
public class ExchangeProException extends RuntimeException {

    private final ErrorCode errorCode;

    public ExchangeProException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
