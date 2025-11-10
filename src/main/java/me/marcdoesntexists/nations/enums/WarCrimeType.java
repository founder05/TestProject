package me.marcdoesntexists.nations.enums;

public enum WarCrimeType {
    MASSACRE("Massacre of civilians", 10000),
    CHEMICAL_WEAPONS("Use of chemical weapons", 15000),
    EXECUTION_OF_PRISONERS("Execution of prisoners of war", 9000),
    FORCED_LABOR("Forced labor", 5000);

    private final String description;
    private final double severity;

    WarCrimeType(String description, double severity) {
        this.description = description;
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public double getSeverity() {
        return severity;
    }
}
