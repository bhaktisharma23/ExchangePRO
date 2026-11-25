package com.exchangepro.auth;

import com.exchangepro.common.error.ErrorCode;
import com.exchangepro.common.error.ExchangeProException;

/** The refresh token is missing, unknown, expired, revoked or was reused. The client must log in again. */
public class InvalidRefreshTokenException extends ExchangeProException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN, "Session expired. Please sign in again.");
    }
}
