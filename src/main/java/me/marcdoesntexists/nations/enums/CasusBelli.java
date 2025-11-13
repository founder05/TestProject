package me.marcdoesntexists.nations.enums;

public enum CasusBelli {
    CLAIMS("Territorial claims on enemy land", 10),
    INSULT("Treaty broken or diplomatic insult", 20),
    VASSAL_DEFENSE("Protecting a vassal from aggression", 5),
    LIBERATION("Freeing an oppressed vassal", 15),
    HOLY_WAR("Religious conflict between faiths", 30),
    RECONQUEST("Retaking historically owned lands", 10),
    UNJUSTIFIED("No legitimate justification", 50);

    private final String description;
    private final int infamyCost;

    CasusBelli(String description, int infamyCost) {
        this.description = description;
        this.infamyCost = infamyCost;
    }

    public String getDescription() {
        return description;
    }

    public int getInfamyCost() {
        return infamyCost;
    }

    public boolean isValid() {
        return this != UNJUSTIFIED;
    }
}

