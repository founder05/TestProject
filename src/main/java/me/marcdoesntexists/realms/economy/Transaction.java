package me.marcdoesntexists.realms.economy;

import java.util.UUID;

public class Transaction {
    private UUID transactionId;
    private UUID senderId;
    private UUID recipientId;
    private double amount;
    private TransactionType type;
    private long timestamp;
    private String reason;

    public Transaction(UUID senderId, UUID recipientId, double amount, TransactionType type, String reason) {
        this.transactionId = UUID.randomUUID();
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public Transaction(UUID playerId, double amount, String reason) {
        this.transactionId = UUID.randomUUID();
        this.senderId = playerId;
        this.recipientId = null;
        this.amount = amount;
        this.type = TransactionType.OTHER;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReason() {
        return reason;
    }

    public enum TransactionType {
        SALARY, TAX, FINE, TRADE, TRIBUTE, GIFT, BANK, OTHER
    }
}
