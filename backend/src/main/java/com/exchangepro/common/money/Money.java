package com.exchangepro.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Platform-wide rules for monetary values.
 *
 * <ul>
 *   <li>Always {@link BigDecimal}; never {@code double}/{@code float}, which cannot represent most decimal
 *       fractions exactly (0.1 + 0.2 != 0.3).</li>
 *   <li>Stored as {@code NUMERIC(19,4)}: 4 decimal places give headroom for average prices while balances
 *       themselves are in whole paise.</li>
 *   <li>Rounding is {@link RoundingMode#HALF_EVEN} ("banker's rounding"), which avoids the systematic upward
 *       bias of HALF_UP when many values are rounded.</li>
 * </ul>
 */
public final class Money {

    public static final int SCALE = 4;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final String CURRENCY_INR = "INR";

    private Money() {}

    /** Returns the amount at the platform scale. Rejects null. */
    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        return amount.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }
}
