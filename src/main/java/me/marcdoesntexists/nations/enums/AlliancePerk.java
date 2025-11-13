package me.marcdoesntexists.nations.enums;

public enum AlliancePerk {
    SHARED_MARKET("Trade bonuses between alliance members", 1000),
    EMERGENCY_TROOPS("Call for military backup from allies", 2500),
    FAST_TRAVEL("Teleport to allied capitals", 1500),
    COMBINED_WAR_SCORE("Pool war scores in joint conflicts", 2000),
    SHARED_INTELLIGENCE("See enemy movements and plans", 3000);

    private final String description;
    private final int activationCost;

    AlliancePerk(String description, int activationCost) {
        this.description = description;
        this.activationCost = activationCost;
    }

    public String getDescription() {
        return description;
    }

    public int getActivationCost() {
        return activationCost;
    }
}

