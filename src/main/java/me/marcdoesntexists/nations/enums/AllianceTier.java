package me.marcdoesntexists.nations.enums;

public enum AllianceTier {
    DEFENSIVE_PACT("Join only defensive wars", 2),
    FULL_ALLIANCE("Join offensive and defensive wars", 5),
    FEDERATION("Shared resources and military coordination", 10);

    private final String description;
    private final int minMembers;

    AllianceTier(String description, int minMembers) {
        this.description = description;
        this.minMembers = minMembers;
    }

    public String getDescription() {
        return description;
    }

    public int getMinMembers() {
        return minMembers;
    }

    public boolean allowsOffensiveWars() {
        return this == FULL_ALLIANCE || this == FEDERATION;
    }

    public boolean allowsResourceSharing() {
        return this == FEDERATION;
    }
}

