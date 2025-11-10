package me.marcdoesntexists.nations.enums;

public enum CrimeType {
    THEFT("Theft", 1000),
    MURDER("Murder", 5000),
    ASSAULT("Assault", 2000),
    PROPERTY_DAMAGE("Property Damage", 1500),
    TREASON("Treason", 10000),
    TRESPASSING("Trespassing", 500),
    TAX_EVASION("Tax Evasion", 2000),
    SMUGGLING("Smuggling", 3000),
    ARSON("Arson", 4000),
    PERJURY("Perjury", 1500);

    private final String displayName;
    private final double baseFine;

    CrimeType(String displayName, double baseFine) {
        this.displayName = displayName;
        this.baseFine = baseFine;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseFine() {
        return baseFine;
    }
}
