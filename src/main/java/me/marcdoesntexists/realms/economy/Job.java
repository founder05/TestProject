package me.marcdoesntexists.realms.economy;

import me.marcdoesntexists.realms.enums.JobType;

import java.util.UUID;

public class Job {
    private UUID jobId;
    private UUID playerId;
    private JobType jobType;
    private String townId;
    private long hireDate;
    private double salary;
    private boolean active;

    public Job(UUID playerId, JobType jobType, String townId) {
        this.jobId = UUID.randomUUID();
        this.playerId = playerId;
        this.jobType = jobType;
        this.townId = townId;
        this.hireDate = System.currentTimeMillis();
        this.salary = jobType.getBaseSalary();
        this.active = true;
    }

    public void terminate() {
        this.active = false;
    }

    public void updateSalary(double newSalary) {
        this.salary = newSalary;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getTownId() {
        return townId;
    }

    public long getHireDate() {
        return hireDate;
    }

    public double getSalary() {
        return salary;
    }

    public boolean isActive() {
        return active;
    }
}
