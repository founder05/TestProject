package me.marcdoesntexists.realms.enums;

public enum JobType {
    MINER("Miner", 100, 10),
    FARMER("Farmer", 150, 15),
    BUILDER("Builder", 200, 20),
    BLACKSMITH("Blacksmith", 250, 25),
    MERCHANT("Merchant", 300, 30),
    SOLDIER("Soldier", 400, 40),
    SCHOLAR("Scholar", 350, 35),
    PRIEST("Priest", 200, 20),
    GUARD("Guard", 180, 18),
    ADMINISTRATOR("Administrator", 500, 50);

    private final String displayName;
    private final double baseSalary;
    private final double taxPercentage;

    JobType(String displayName, double baseSalary, double taxPercentage) {
        this.displayName = displayName;
        this.baseSalary = baseSalary;
        this.taxPercentage = taxPercentage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public double getTaxPercentage() {
        return taxPercentage;
    }
}
