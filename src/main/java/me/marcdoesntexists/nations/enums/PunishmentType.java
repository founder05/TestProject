package me.marcdoesntexists.nations.enums;

public enum PunishmentType {
    GUILTY("Guilty - Convicted"),
    NOT_GUILTY("Not Guilty - Acquitted"),
    FINE("Fine - Pay money"),
    IMPRISONMENT("Imprisonment - Days in jail"),
    BANISHMENT("Banishment - Exile from society"),
    COMMUNITY_SERVICE("Community Service - Work hours"),
    EXECUTION("Execution - Death penalty"),
    HOUSE_ARREST("House Arrest - Confined to area");

    private final String description;

    PunishmentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
