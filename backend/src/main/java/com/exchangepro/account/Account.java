package com.exchangepro.account;

import com.exchangepro.common.money.Money;
import com.exchangepro.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A user's virtual-money trading account.
 *
 * <p>The account references the user by id only (no {@code @ManyToOne User}) to keep the account and user
 * modules independent. Balance-changing operations (reserve, release, settle) are added in Phase 5 together
 * with the double-entry ledger; until then the balance only changes when the account is opened.
 */
@Entity
@Table(name = "accounts")
public class Account extends AuditableEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = Money.SCALE)
    private BigDecimal availableBalance;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = Money.SCALE)
    private BigDecimal reservedBalance;

    protected Account() {
        // for JPA
    }

    static Account open(UUID userId, BigDecimal openingBalance) {
        if (openingBalance.signum() < 0) {
            throw new IllegalArgumentException("opening balance must not be negative");
        }
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.userId = userId;
        account.currency = Money.CURRENCY_INR;
        account.availableBalance = Money.normalize(openingBalance);
        account.reservedBalance = Money.zero();
        return account;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getReservedBalance() {
        return reservedBalance;
    }

    /** Available + reserved: everything the user owns in cash. */
    public BigDecimal getTotalBalance() {
        return availableBalance.add(reservedBalance);
    }
}
