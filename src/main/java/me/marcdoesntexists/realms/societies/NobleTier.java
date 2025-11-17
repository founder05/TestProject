package me.marcdoesntexists.realms.societies;

public enum NobleTier {
    COMMONER(0, 0.0, 0, 0),      // land-claim-limit: 0 (cannot own subclaims)
    KNIGHT(1, 0.05, 100, 20),    // land-claim-limit: 20
    BARON(2, 0.10, 500, 50),     // land-claim-limit: 50
    COUNT(3, 0.15, 1000, 100),   // land-claim-limit: 100
    DUKE(4, 0.20, 2500, 200),    // land-claim-limit: 200
    PRINCE(5, 0.25, 5000, 500),  // land-claim-limit: 500
    KING(6, 0.30, 10000, 1000);  // land-claim-limit: 1000

    private final int level;
    private final double taxBenefitPercentage;
    private final int requiredLandValue;
    private final int landClaimLimit;

    NobleTier(int level, double taxBenefitPercentage, int requiredLandValue, int landClaimLimit) {
        this.level = level;
        this.taxBenefitPercentage = taxBenefitPercentage;
        this.requiredLandValue = requiredLandValue;
        this.landClaimLimit = landClaimLimit;
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

    public int getLandClaimLimit() {
        return landClaimLimit;
    }

    public boolean canOwnLand() {
        return landClaimLimit > 0;
    }
}
