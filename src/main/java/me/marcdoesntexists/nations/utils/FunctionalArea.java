package me.marcdoesntexists.nations.utils;

public class FunctionalArea {
    private final String chunkKey;
    private final AreaType type;
    private long createdAt;
    private double productivity;

    public enum AreaType {
        FARM("Farm", 1.0),
        MINE("Mine", 1.5),
        TEMPLE("Temple", 0.8),
        BARRACKS("Barracks", 1.2),
        MARKET("Market", 1.3),
        LIBRARY("Library", 0.9),
        HOUSING("Housing", 0.5),
        WAREHOUSE("Warehouse", 1.1);

        private final String displayName;
        private final double baseProductivity;

        AreaType(String displayName, double baseProductivity) {
            this.displayName = displayName;
            this.baseProductivity = baseProductivity;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getBaseProductivity() {
            return baseProductivity;
        }
    }

    public FunctionalArea(String chunkKey, AreaType type) {
        this.chunkKey = chunkKey;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.productivity = type.getBaseProductivity();
    }

    public String getChunkKey() {
        return chunkKey;
    }

    public AreaType getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public double getProductivity() {
        return productivity;
    }

    public void setProductivity(double productivity) {
        this.productivity = productivity;
    }

    public void addProductivity(double amount) {
        this.productivity += amount;
    }
}
