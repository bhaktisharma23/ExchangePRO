package com.exchangepro.user;

import com.exchangepro.common.error.ErrorCode;
import com.exchangepro.common.error.ExchangeProException;

public class EmailAlreadyRegisteredException extends ExchangeProException {

    public EmailAlreadyRegisteredException() {
        super(ErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this e-mail address already exists");
    }
}
