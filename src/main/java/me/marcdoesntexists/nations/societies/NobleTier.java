package me.marcdoesntexists.nations.societies;

public enum NobleTier {
    COMMONER(0, 0.0, 0),
    KNIGHT(1, 0.05, 100),
    BARON(2, 0.10, 500),
    COUNT(3, 0.15, 1000),
    DUKE(4, 0.20, 2500),
    PRINCE(5, 0.25, 5000),
    KING(6, 0.30, 10000);

    private final int level;
    private final double taxBenefitPercentage;
    private final int requiredLandValue;

    NobleTier(int level, double taxBenefitPercentage, int requiredLandValue) {
        this.level = level;
        this.taxBenefitPercentage = taxBenefitPercentage;
        this.requiredLandValue = requiredLandValue;
    }

    public static NobleTier getByLevel(int level) {
        for (NobleTier tier : NobleTier.values()) {
            if (tier.level == level) return tier;
        }
        return COMMONER;
    }

    public int getLevel() {
        return level;
    }

    public double getTaxBenefitPercentage() {
        return taxBenefitPercentage;
    }

    public int getRequiredLandValue() {
        return requiredLandValue;
    }
}
