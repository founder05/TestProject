package me.marcdoesntexists.nations.utils;

public class CustomMessages {
    
    public static class Settlement {
        public static final String TOWN_CREATED = "Town %s has been created!";
        public static final String KINGDOM_UPGRADED = "%s has been upgraded to a Kingdom!";
        public static final String EMPIRE_UPGRADED = "%s has been upgraded to an Empire!";
        public static final String INSUFFICIENT_CLAIMS = "You need %d claims to upgrade to a Kingdom";
        public static final String INSUFFICIENT_FUNDS = "Insufficient funds to perform this action";
        public static final String PLAYER_NOT_MAYOR = "Only the mayor can perform this action";
    }

    public static class War {
        public static final String WAR_DECLARED = "%s has declared war on %s!";
        public static final String WAR_ENDED = "War between %s and %s has ended";
        public static final String WAR_CRIME_RECORDED = "War crime recorded: %s";
        public static final String INVALID_TARGET = "Cannot declare war on %s";
    }

    public static class Treaty {
        public static final String TREATY_SIGNED = "Treaty signed between %s and %s";
        public static final String TREATY_BROKEN = "%s broke their treaty with %s";
        public static final String TREATY_EXPIRED = "Treaty between %s and %s has expired";
    }

    public static class Law {
        public static final String CRIME_RECORDED = "Crime recorded: %s";
        public static final String TRIAL_STARTED = "Trial started for %s";
        public static final String VERDICT_GUILTY = "%s has been found guilty";
        public static final String VERDICT_NOT_GUILTY = "%s has been found not guilty";
        public static final String SENTENCE_APPLIED = "%s has been sentenced to %s";
    }

    public static class Economy {
        public static final String TAX_COLLECTED = "Taxes collected: %d coins";
        public static final String SALARY_PAID = "%s has been paid %d coins";
        public static final String TRANSACTION_RECORDED = "Transaction recorded: %d coins";
        public static final String INSUFFICIENT_TREASURY = "Insufficient treasury funds";
    }

    public static class Religion {
        public static final String RELIGION_CREATED = "Religion %s has been created";
        public static final String FOLLOWER_GAINED = "%s now follows %s";
        public static final String ALTAR_BUILT = "Altar built for %s at %s";
    }

    public static class Alliance {
        public static final String ALLIANCE_CREATED = "Alliance %s has been created";
        public static final String KINGDOM_JOINED_ALLIANCE = "%s joined the %s alliance";
        public static final String LEFT_ALLIANCE = "%s left the alliance";
    }

    public static class Feudal {
        public static final String VASSAL_CREATED = "%s is now a vassal of %s";
        public static final String VASSAL_BETRAYED = "%s betrayed their lord %s";
        public static final String TRIBUTE_PAID = "Tribute of %d coins paid to %s";
    }

    public static class Military {
        public static final String RANK_ASSIGNED = "%s assigned rank %s";
        public static final String TROOP_RECRUITED = "%d troops recruited";
        public static final String CASUALTY_RECORDED = "%d casualties recorded";
    }
}
