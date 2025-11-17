package me.marcdoesntexists.realms.managers;

/**
 * LawManager - removed from this build.
 * This is a placeholder stub to maintain compatibility.
 */
public class LawManager {
    private static LawManager instance;

    private LawManager() {
    }

    public static LawManager getInstance() {
        if (instance == null) {
            instance = new LawManager();
        }
        return instance;
    }
}

