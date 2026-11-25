package com.exchangepro.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Balances are serialized as JSON <b>strings</b> ("100000.0000"), not numbers: JavaScript parses JSON numbers
 * into 64-bit floats, which would silently lose precision for large or fractional amounts.
 */
public record AccountResponse(
        UUID accountId,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal availableBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reservedBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalBalance) {

    static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getCurrency(), account.getAvailableBalance(),
                account.getReservedBalance(), account.getTotalBalance());
    }
}
