package com.exchangepro.instrument;

import com.exchangepro.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A tradable security (an NSE-listed equity). Master data is seeded by Flyway (V3) and is read-only for now;
 * instrument management by admins arrives in Phase 9.
 */
@Entity
@Table(name = "instruments")
public class Instrument extends AuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20, updatable = false)
    private String symbol;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 8)
    private String exchange;

    @Column(nullable = false, length = 60)
    private String sector;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "tick_size", nullable = false, precision = 10, scale = 4)
    private BigDecimal tickSize;

    @Column(name = "lot_size", nullable = false)
    private int lotSize;

    /** Simulator seed, NOT a market price. Never expose it as a quote. */
    @Column(name = "reference_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal referencePrice;

    @Column(name = "annual_volatility", nullable = false, precision = 6, scale = 4)
    private BigDecimal annualVolatility;

    @Column(name = "avg_daily_volume", nullable = false)
    private long avgDailyVolume;

    @Column(nullable = false)
    private boolean active;

    protected Instrument() {
        // for JPA
    }

    public UUID getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getExchange() { return exchange; }
    public String getSector() { return sector; }
    public String getCurrency() { return currency; }
    public BigDecimal getTickSize() { return tickSize; }
    public int getLotSize() { return lotSize; }
    BigDecimal getReferencePrice() { return referencePrice; }
    BigDecimal getAnnualVolatility() { return annualVolatility; }
    long getAvgDailyVolume() { return avgDailyVolume; }
    public boolean isActive() { return active; }
}
