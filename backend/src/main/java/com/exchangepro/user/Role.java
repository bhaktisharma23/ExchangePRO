package com.exchangepro.user;

/** Authorization roles. Spring Security sees them as authorities {@code ROLE_USER} / {@code ROLE_ADMIN}. */
public enum Role {
    /** A trader: owns a virtual account and can place orders. */
    USER,
    /** An operator: can monitor users and the system. Does not trade. */
    ADMIN
}
