package me.marcdoesntexists.realms.enums;

public enum FeudalObligation {
    MILITARY_SERVICE("Must send troops when suzerain calls", 50),
    TRIBUTE_PAYMENT("Regular economic payments to suzerain", 100),
    CONSTRUCTION_AID("Help build suzerain's projects", 30),
    DIPLOMATIC_SUPPORT("Cannot ally with suzerain's enemies", 20),
    MARRIAGE_PACT("Noble families connected through marriage", 75);

    private final String description;
    private final int loyaltyRequirement;

    FeudalObligation(String description, int loyaltyRequirement) {
        this.description = description;
        this.loyaltyRequirement = loyaltyRequirement;
    }

    public String getDescription() {
        return description;
    }

    public int getLoyaltyRequirement() {
        return loyaltyRequirement;
    }
}

