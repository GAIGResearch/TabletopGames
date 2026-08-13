package games.descent2e.pcg_clean.spatial;

import java.util.Locale;

/** Selects the bounded repair budget independently of the evolutionary algorithm. */
public enum RepairMode {
    /** Required pieces plus at most two component-joining moves. */
    BASIC,
    /** Basic repair followed by loop closing and exposed-port completion. */
    TOPOLOGY;

    public static RepairMode parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "basic" -> BASIC;
            case "topology", "advanced" -> TOPOLOGY;
            default -> throw new IllegalArgumentException(
                    "Unknown repair mode '" + value + "' (expected basic or topology)");
        };
    }
}
