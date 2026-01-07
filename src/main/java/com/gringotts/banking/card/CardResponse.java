package com.gringotts.banking.card;

/**
 * Data Transfer Object (DTO) for Card Creation.
 * * Purpose:
 * Returns the details of a newly created card to the frontend.
 * Critical: This is the ONLY place where the 'tempPin' is exposed in plain text
 * so the user can see it once. It is never stored in plain text in the database.
 */
public class CardResponse {

    private Long id;
    private Long accountId;
    private String cardNumber;
    private String cvv;
    private String expiry;
    private String tempPin; // Sensitive: Shown only once
    private String cardType;

    // --- GETTERS AND SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public String getTempPin() {
        return tempPin;
    }

    public void setTempPin(String tempPin) {
        this.tempPin = tempPin;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }
}