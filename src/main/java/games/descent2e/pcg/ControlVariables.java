package games.descent2e.pcg;

import java.util.ArrayList;
import java.util.List;

public class ControlVariables {

    public static final List<String> TRAITS = List.of("Building", "Cave", "Civilized", "Cold", "Cursed", "Dark", "Hot", "Mountain", "Water", "Wilderness");

    public static final int CROSSOVER = 10;
    public static final int TILE_REMOVE = 10;
    public static final int TILE_ROTATE = 10;

    // Board Size Ranges
    public static final int SIZE_MIN = 100;
    public static final int SIZE_MAX = 300;

    // Likelihood for Monster Mutation
    public static final int MONSTER_MUTATE = 10;
    public static final int LIEUTENANT = 25;
    public static final int OPENGROUP = 5;
    public static final int SMALLOPENGROUP = 1;

    public static final int ADD_GROUP = 30;
    public static final int REMOVE_GROUP = 30;

    // Group Count Ranges
    public static final int GROUP_MIN = 2;
    public static final int GROUP_MAX = 10;

    // Likelihood for Traits Mutation
    public static final int TRAITS_MUTATE = 10;
    public static final int ADD_TRAITS = 30;
    public static final int REMOVE_TRAITS = 30;
    public static final int REPLACE_TRAITS = 30;

    // Monster Traits Ranges
    public static final int TRAITS_MIN = 3;
    public static final int TRAITS_MAX = 5;

    // Likelihood of mutating Positions, Act and XP/Gold
    public static final int POSITION_MUTATE = 10;
    public static final int ACT_MUTATE = 10;
    public static final int XP_MUTATE = 10;
}
