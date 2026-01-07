package com.gringotts.banking.transaction;

/**
 * Defines the types of financial movements in the system.
 */
public enum TransactionType {

    /**
     * Money added via Branch or physical cash.
     */
    CASH_DEPOSIT,

    /**
     * Money added via ATM using a Debit Card.
     */
    CARD_DEPOSIT,

    /**
     * Money sent from one internal account to another.
     */
    TRANSFER,

    /**
     * Money spent using a Debit Card (POS/Online).
     */
    CARD_PURCHASE
}