package me.marcdoesntexists.realms.enums;

public enum WarGoal {
    CONQUEST("Conquer territory from the enemy", 100),
    REPARATIONS("Extract economic compensation", 50),
    REGIME_CHANGE("Force leadership change", 75),
    HUMILIATION("Damage enemy prestige and reputation", 30);

    private final String description;
    private final int baseReward;

    WarGoal(String description, int baseReward) {
        this.description = description;
        this.baseReward = baseReward;
    }

    public String getDescription() {
        return description;
    }

    public int getBaseReward() {
        return baseReward;
    }
}

