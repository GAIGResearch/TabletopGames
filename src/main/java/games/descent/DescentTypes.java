package games.descent;

import core.components.GridBoard;
import games.descent.concepts.Quest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class DescentTypes {

    public final static HashSet<Character> direction = new HashSet<Character>() {{
        add('N');
        add('S');
        add('W');
        add('E');
    }};

    public enum Campaign {
        HeirsOfBlood("data/descent/campaigns/heirsOfBlood.json");

        private String dataPath;
        private String name;
        private Quest[] quests;

        Campaign(String path) {
            this.dataPath = path;
        }

        public String getDataPath() {
            return dataPath;
        }
        public String getName() {
            return name;
        }
        public Quest[] getQuests() {
            return quests;
        }

        public void load(DescentGameData _data) {
            JSONParser jsonParser = new JSONParser();
            ArrayList<GridBoard> gridBoards = new ArrayList<>();

            try (FileReader reader = new FileReader(dataPath)) {
                JSONObject data = (JSONObject) jsonParser.parse(reader);
                name = (String) data.get("name");
                JSONArray qs = (JSONArray) data.get("quests");
                quests = new Quest[qs.size()];
                int i = 0;
                for (Object o : qs) {
                    quests[i] = _data.findQuest((String)o);
                    i++;
                }

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

        }
    }

    public enum TerrainType {
        // Tile margins
        Edge,
        Open,
        Null,

        // Inside tile
        Plain,
        Water,
        Lava,
        Hazard,
        Pit,
        Block;

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

        public static HashSet<TerrainType> getMarginTiles() {
            return new HashSet<TerrainType>() {{
                add(Edge);
                add(Open);
                add(Null);
            }};
        }

        public static HashSet<String> getMarginStringTiles() {
            HashSet<String> margins = new HashSet<>();
            for (TerrainType t: getMarginTiles()) {
                margins.add(t.name().toLowerCase());
            }
            return margins;
        }

        public static boolean isWalkable(String terrain) {
            return terrain != null && (getWalkableStringTiles().contains(terrain));
        }

        public static boolean isInsideTile(String terrain) {
            return terrain != null && (!getMarginStringTiles().contains(terrain));
        }
    }
}
