package me.marcdoesntexists.nations.law;

import me.marcdoesntexists.nations.enums.PunishmentType;

import java.util.UUID;

public class Trial {
    private UUID trialId;
    private Crime crime;
    private UUID judgeId;
    private UUID defendantId;
    private TrialStatus status;
    private long startDate;
    private long endDate;
    private PunishmentType verdict;
    private double punishment;
    private String reason;

    public Trial(Crime crime, UUID judgeId, UUID defendantId) {
        this.trialId = UUID.randomUUID();
        this.crime = crime;
        this.judgeId = judgeId;
        this.defendantId = defendantId;
        this.status = TrialStatus.SCHEDULED;
        this.startDate = System.currentTimeMillis();
    }

    public void conclude(PunishmentType verdict, double punishment, String reason) {
        this.verdict = verdict;
        this.punishment = punishment;
        this.reason = reason;
        this.status = TrialStatus.CONCLUDED;
        this.endDate = System.currentTimeMillis();
    }

    public UUID getTrialId() {
        return trialId;
    }

    public Crime getCrime() {
        return crime;
    }

    public UUID getJudgeId() {
        return judgeId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public TrialStatus getStatus() {
        return status;
    }

    public void setStatus(TrialStatus status) {
        this.status = status;
    }

    public PunishmentType getVerdict() {
        return verdict;
    }

    public double getPunishment() {
        return punishment;
    }

    public String getReason() {
        return reason;
    }

    public enum TrialStatus {
        SCHEDULED, IN_PROGRESS, CONCLUDED, APPEALED
    }
}
