package games.descent;

import utilities.Hash;

import java.util.HashSet;

public class DescentConstants {

    public final static int connectionHash = Hash.GetInstance().hash("connections");

    public enum TerrainType {
        Edge,
        Open,
        Plain,
        Water,
        Lava,
        Hazard,
        Pit,
        Block,
        Null;

        public static HashSet<TerrainType> getWalkableTiles() {
            return new HashSet<TerrainType>() {{
                add(Plain);
                add(Water);
                add(Lava);
                add(Hazard);
                add(Pit);
            }};
        }

        public static HashSet<String> getWalkableStringTiles() {
            HashSet<String> walkable = new HashSet<>();
            for (TerrainType t: getWalkableTiles()) {
                walkable.add(t.name().toLowerCase());
            }
            return walkable;
        }

        public static boolean isWalkable(String terrain) {
            return getWalkableStringTiles().contains(terrain);
        }
    }
}
