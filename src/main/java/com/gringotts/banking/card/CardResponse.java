package com.gringotts.banking.card;

public class CardResponse {
    private Long id;
    private Long accountId;
    private String cardNumber;
    private String cvv;
    private String expiry;
    private String tempPin; // returned once
    private String cardType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }

    public String getTempPin() { return tempPin; }
    public void setTempPin(String tempPin) { this.tempPin = tempPin; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
}
