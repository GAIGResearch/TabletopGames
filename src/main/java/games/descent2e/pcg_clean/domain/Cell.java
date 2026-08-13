package games.descent2e.pcg_clean.domain;

public enum Cell {
    VOID(false), OPEN(true), PLAIN(true), WATER(true), PIT(true), BLOCK(false), OTHER(true);

    private final boolean traversable;

    Cell(boolean traversable) { this.traversable = traversable; }
    public boolean traversable() { return traversable; }

    public static Cell fromJson(String value) {
        return switch (value.toLowerCase()) {
            case "null" -> VOID;
            case "open" -> OPEN;
            case "plain" -> PLAIN;
            case "water" -> WATER;
            case "pit" -> PIT;
            case "block" -> BLOCK;
            default -> OTHER;
        };
    }
}
