package me.marcdoesntexists.nations.enums;


public enum TreatyStatus {
    PROPOSED("Treaty Proposed"),
    ACTIVE("Treaty Active"),
    EXPIRED("Treaty Expired"),
    BROKEN("Treaty Broken"),
    TERMINATED("Treaty Terminated");

    private final String displayName;

    TreatyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
