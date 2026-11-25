package com.exchangepro.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void opensWithInitialBalanceAtPlatformScaleAndNothingReserved() {
        UUID userId = UUID.randomUUID();

        Account account = Account.open(userId, new BigDecimal("100000"));

        assertThat(account.getUserId()).isEqualTo(userId);
        assertThat(account.getCurrency()).isEqualTo("INR");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("100000").hasScaleOf(4);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0");
        assertThat(account.getTotalBalance()).isEqualByComparingTo("100000.0000");
    }

    @Test
    void rejectsNegativeOpeningBalance() {
        assertThatThrownBy(() -> Account.open(UUID.randomUUID(), new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
