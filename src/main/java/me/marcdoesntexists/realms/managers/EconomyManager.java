package me.marcdoesntexists.realms.managers;


import me.marcdoesntexists.realms.economy.Job;
import me.marcdoesntexists.realms.economy.Salary;
import me.marcdoesntexists.realms.economy.Transaction;

import java.util.*;

public class EconomyManager {
    private static EconomyManager instance;
    private final Map<UUID, Job> jobs;
    private final Map<UUID, Salary> salaries;
    private final Map<UUID, Transaction> transactions;
    private final Map<String, Double> treasuries;

    private EconomyManager() {
        this.jobs = new HashMap<>();
        this.salaries = new HashMap<>();
        this.transactions = new HashMap<>();
        this.treasuries = new HashMap<>();
    }

    public static EconomyManager getInstance() {
        if (instance == null) {
            instance = new EconomyManager();
        }
        return instance;
    }

    public void registerJob(Job job) {
        jobs.put(job.getJobId(), job);
    }

    public Job getJob(UUID id) {
        return jobs.get(id);
    }

    public Collection<Job> getAllJobs() {
        return new ArrayList<>(jobs.values());
    }

    public void recordSalary(Salary salary) {
        salaries.put(salary.getSalaryId(), salary);
    }

    public void recordTransaction(Transaction transaction) {
        transactions.put(transaction.getTransactionId(), transaction);
        String treasuryKey = "GLOBAL";
        treasuries.putIfAbsent(treasuryKey, 0.0);
        treasuries.put(treasuryKey, treasuries.get(treasuryKey) + transaction.getAmount());
    }

    public double getTreasuryBalance(String treasury) {
        return treasuries.getOrDefault(treasury, 0.0);
    }

    public void updateTreasury(String treasury, double amount) {
        treasuries.put(treasury, amount);
    }

    public Collection<Transaction> getTransactions() {
        return new ArrayList<>(transactions.values());
    }
}
