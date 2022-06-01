package games.descent2e;

import core.components.GridBoard;
import games.descent2e.concepts.Quest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class DescentTypes {

    public final static HashSet<Character> direction = new HashSet<Character>() {{
        add('N');
        add('S');
        add('W');
        add('E');
    }};

    public enum Campaign {
        HeirsOfBlood;

        private Quest[] quests;
        public Quest[] getQuests() {
            return quests;
        }

        public void load(DescentGameData _data, String filePath) {
            JSONParser jsonParser = new JSONParser();
            ArrayList<GridBoard> gridBoards = new ArrayList<>();

            try (FileReader reader = new FileReader(filePath + "campaigns/" + name() + ".json")) {
                JSONObject data = (JSONObject) jsonParser.parse(reader);
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

        public static HashSet<TerrainType> getWalkableTerrains() {
            return new HashSet<TerrainType>() {{
                add(Plain);
                add(Water);
                add(Lava);
                add(Hazard);
//                add(Pit);
            }};
        }

        public static HashSet<String> getWalkableStringTerrains() {
            HashSet<String> walkable = new HashSet<>();
            for (TerrainType t: getWalkableTerrains()) {
                walkable.add(t.name().toLowerCase());
            }
            return walkable;
        }

        public static HashSet<TerrainType> getMarginTerrains() {
            return new HashSet<TerrainType>() {{
                add(Edge);
                add(Open);
                add(Null);
            }};
        }

        public static HashSet<String> getMarginStringTerrains() {
            HashSet<String> margins = new HashSet<>();
            for (TerrainType t: getMarginTerrains()) {
                margins.add(t.name().toLowerCase());
            }
            return margins;
        }

        public static boolean isWalkableTerrain(String terrain) {
            return terrain != null && (getWalkableStringTerrains().contains(terrain));
        }

        public static boolean isInsideTerrain(String terrain) {
            return terrain != null && (!getMarginStringTerrains().contains(terrain));
        }

        public static boolean isMarginTerrain(String terrain) {
            return terrain != null && getMarginStringTerrains().contains(terrain);
        }
    }

    public enum DescentCondition {
        Immobilize,
        Poison,
        Stun,
        Disease,
//        Bleed,
//        Burn,
//        Curse,
//        Weaken,
//        Doom,
//        Terrify
    }

    public enum DescentToken {
        Search ("search.png", 1),
        Villager ("villager.png", 2),
        WhiteObjective("whiteobjective.png", 1);
        String imgPath; int nImgOptions;
        DescentToken(String imgPath, int nImgOptions) {
            this.imgPath = imgPath;
            this.nImgOptions = nImgOptions;
        }
        public String getImgPath(Random rnd) {
            String path = imgPath;
            if (nImgOptions > 1) {
                String[] split = imgPath.split("\\.");
                path = split[0] + rnd.nextInt(nImgOptions) + "." + split[1];
            }
            return "tokens/" + path;
        }
    }


    public enum Archetype {
        Mage(Color.orange),
        Healer(Color.blue);
//        Scout(Color.green),
//        Warrior(Color.red);

        Color color;
        Archetype(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    public enum HeroClass {
        Necromancer,
        Runemaster,
        Battlemage,
        Conjurer,
        Elementalist,
        Geomancer,
        Hexer,
        Lorekeeper,
        Truthseer,
        Disciple,
        Spiritspeaker,
        Apothecary,
        Bard,
        Crusader,
        Heretic,
        Prophet,
        SoulReaper,
        Watchman,
        Berserker,
        Knight,
        Avenger,
        Beastmaster,
        Champion,
        Marshal,
        Raider,
        Skirmisher,
        Steelcaster,
        Thief,
        Wildlander,
        BountyHunter,
        Monk,
        Ravager,
        ShadowWalker,
        Stalker,
        TreasureHunter,
        Trickster;

        public static HeroClass[] getClassesForArchetype(Archetype archetype) {
            switch(archetype) {
                case Mage:
                    return new HeroClass[] {
                            Runemaster,
//                            Necromancer,
//                            Battlemage,
//                            Conjurer,
//                            Elementalist,
//                            Geomancer,
//                            Hexer,
//                            Lorekeeper,
//                            Truthseer
                    };
                case Healer:
                    return new HeroClass[] {
                            Disciple,
//                           Spiritspeaker,
//                           Apothecary,
//                           Bard,
//                           Crusader,
//                           Heretic,
//                           Prophet,
//                           SoulReaper,
//                           Watchman
                    };
//                case Scout:
//                    return new HeroClass[]{
////                           Thief,
////                           Wildlander,
////                           Bounty Hunter,
////                           Monk,
////                           Ravager,
////                           Shadow Walker,
////                           Stalker,
////                           Treasure Hunter,
////                           Trickster
//                    };
//                case Warrior:
//                    return new HeroClass[]{
////                           Berserker,
////                           Knight,
////                           Avenger,
////                           Beastmaster,
////                           Champion,
////                           Marshal,
////                           Raider,
////                           Skirmisher,
////                           Steelcaster
//                    };
            }
            return null;
        }
    }

}
