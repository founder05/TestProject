package me.marcdoesntexists.nations.economy;

import java.util.UUID;

public class Salary {
    private UUID salaryId;
    private UUID playerId;
    private UUID jobId;
    private double amount;
    private long paymentDate;
    private double taxDeducted;
    private double netAmount;

    public Salary(UUID playerId, UUID jobId, double amount) {
        this.salaryId = UUID.randomUUID();
        this.playerId = playerId;
        this.jobId = jobId;
        this.amount = amount;
        this.paymentDate = System.currentTimeMillis();
        this.taxDeducted = 0;
        this.netAmount = amount;
    }

    public void calculateTax(double taxPercentage) {
        this.taxDeducted = amount * (taxPercentage / 100);
        this.netAmount = amount - taxDeducted;
    }

    public UUID getSalaryId() {
        return salaryId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public double getAmount() {
        return amount;
    }

    public long getPaymentDate() {
        return paymentDate;
    }

    public double getTaxDeducted() {
        return taxDeducted;
    }

    public double getNetAmount() {
        return netAmount;
    }
}
