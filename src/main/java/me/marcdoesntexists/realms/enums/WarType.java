package me.marcdoesntexists.realms.enums;

public enum WarType {
    STANDARD("Regular warfare between kingdoms"),
    INDEPENDENCE_WAR("Vassal seeking freedom from suzerain"),
    HOLY_WAR("Religious conflict"),
    CIVIL_WAR("Internal kingdom conflict");

    private final String description;

    WarType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

