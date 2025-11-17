package me.marcdoesntexists.realms.enums;

public enum AllianceStatus {
    PROPOSED("Proposed"),
    ACTIVE("Active"),
    HOSTILE("Hostile"),
    EXPIRED("Expired"),
    TERMINATED("Terminated");

    private final String displayName;

    AllianceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
