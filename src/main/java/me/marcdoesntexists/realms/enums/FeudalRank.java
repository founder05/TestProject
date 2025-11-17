package me.marcdoesntexists.realms.enums;

public enum FeudalRank {
    GRAND_DUCHY("Powerful vassal lord with many subjects", 3),
    KINGDOM("Standard independent or vassal kingdom", 1),
    PETTY_KINGDOM("Minor kingdom with reduced obligations", 0),
    TRIBUTARY_STATE("Economic vassal with minimal ties", 0);

    private final String description;
    private final int minVassals;

    FeudalRank(String description, int minVassals) {
        this.description = description;
        this.minVassals = minVassals;
    }

    public static FeudalRank calculateRank(int vassalCount, boolean hasSuzerain) {
        if (vassalCount >= 3) return GRAND_DUCHY;
        if (vassalCount >= 1) return KINGDOM;
        if (hasSuzerain) return PETTY_KINGDOM;
        return KINGDOM;
    }

    public String getDescription() {
        return description;
    }

    public int getMinVassals() {
        return minVassals;
    }

    public double getTributeModifier() {
        return switch (this) {
            case GRAND_DUCHY -> 0.75; // -25% tribute
            case KINGDOM -> 1.0;
            case PETTY_KINGDOM -> 0.5; // -50% tribute
            case TRIBUTARY_STATE -> 1.5; // +50% tribute
        };
    }
}

