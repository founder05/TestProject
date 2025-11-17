package me.marcdoesntexists.realms.societies;

import me.marcdoesntexists.realms.enums.WarGoal;

public class WarGoalProgress {
    private final WarGoal goal;
    private final int targetValue;
    private int progress; // 0-100%
    private boolean achieved;

    public WarGoalProgress(WarGoal goal, int targetValue) {
        this.goal = goal;
        this.targetValue = targetValue;
        this.progress = 0;
        this.achieved = false;
    }

    public void addProgress(int amount) {
        if (!achieved) {
            progress = Math.min(100, progress + amount);
            if (progress >= 100) {
                achieved = true;
            }
        }
    }

    public WarGoal getGoal() {
        return goal;
    }

    public int getProgress() {
        return progress;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public boolean isAchieved() {
        return achieved;
    }

    public void setAchieved(boolean achieved) {
        this.achieved = achieved;
    }
}

