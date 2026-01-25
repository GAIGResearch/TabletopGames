package games.descent2e;

import core.components.GridBoard;
import games.descent2e.components.Figure;
import games.descent2e.concepts.Quest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.List;

import static games.descent2e.components.Figure.Attribute.Health;
import static games.descent2e.components.Figure.Attribute.MovePoints;

public class DescentTypes {

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

    // TODO: params for move costs and damage
    public enum TerrainType {
        // Tile margins
        Edge(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1000);}}),
        Open(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1);}}),
        Null(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1000);}}),

        // Inside tile
        Plain(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1);}}),
        Water(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 2);}}),
//        Sludge(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 2);}}),  // TODO: when a figure starts its turn or activation so that each space it occupies is a sludge space, its Speed is considered to be 1 and cannot be increased above 1 until the end of that turn or activation.
        Lava(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1); put(Health, 1);}}),
        Hazard(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1); put(Health, 1);}}),
        Pit(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1); put(Health, 2);}}),
        Block(new HashMap<Figure.Attribute, Integer>() {{put(MovePoints, 1000);}});

        HashMap<Figure.Attribute, Integer> moveCosts;
        static HashSet<TerrainType> walkableTerrains = new HashSet<TerrainType>() {{
            add(Plain);
            add(Water);
            add(Lava);
            add(Hazard);
//                add(Pit);
        }};
        static HashSet<TerrainType> marginTerrains = new HashSet<TerrainType>() {{
            add(Edge);
            add(Open);
            add(Null);
        }};
        TerrainType(HashMap<Figure.Attribute, Integer> moveCosts) {
            this.moveCosts = moveCosts;
        }

        public HashMap<Figure.Attribute, Integer> getMoveCosts() {
            return moveCosts;
        }
        public static int getMovePointsCost(String terrain) {
            TerrainType t = Utils.searchEnum(TerrainType.class, terrain);
            if (t != null) {
                return t.moveCosts.get(MovePoints);
            }
            return -1;
        }

        public static int getMovePointsCost(TerrainType tt)
        {
            return tt.moveCosts.get(MovePoints);
        }

        public static HashSet<TerrainType> getWalkableTerrains() {
            return walkableTerrains;
        }

        public static HashSet<String> getWalkableStringTerrains() {
            HashSet<String> walkable = new HashSet<>();
            for (TerrainType t: getWalkableTerrains()) {
                walkable.add(t.name().toLowerCase());
            }
            return walkable;
        }

        public static HashSet<TerrainType> getMarginTerrains() {
            return marginTerrains;
        }

        public static HashSet<String> getMarginStringTerrains() {
            HashSet<String> margins = new HashSet<>();
            for (TerrainType t: getMarginTerrains()) {
                margins.add(t.name().toLowerCase());
            }
            return margins;
        }

        public static boolean isWalkableTerrain(String terrain) {
            return terrain != null && walkableTerrains.contains(Utils.searchEnum(TerrainType.class, terrain));
        }

        public static boolean isInsideTerrain(String terrain) {
            return terrain != null && !marginTerrains.contains(Utils.searchEnum(TerrainType.class, terrain));
        }

        public static boolean isMarginTerrain(String terrain) {
            return terrain != null && marginTerrains.contains(Utils.searchEnum(TerrainType.class, terrain));
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
        Mage(new Color(246, 191, 73)),
        Healer(new Color(58, 134, 183)),
        Scout(new Color(78, 168, 22, 255)),
        Warrior(new Color(220, 76, 76));

        final Color color;
        Archetype(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }
    }

    // TODO: Necromancer, Spiritspeaker, Knight and Wildlander are all included in the base game
    public enum HeroClass {
        Runemaster (Archetype.Mage),
//        Necromancer (Archetype.Mage),
//        Battlemage (Archetype.Mage),
//        Conjurer (Archetype.Mage),
//        Elementalist (Archetype.Mage),
//        Geomancer (Archetype.Mage),
//        Hexer (Archetype.Mage),
//        Lorekeeper (Archetype.Mage),
//        Truthseer (Archetype.Mage),
        Disciple (Archetype.Healer),
//        Spiritspeaker (Archetype.Healer),
//        Apothecary (Archetype.Healer),
//        Bard (Archetype.Healer),
//        Crusader (Archetype.Healer),
//        Heretic (Archetype.Healer),
//        Prophet (Archetype.Healer),
//        SoulReaper (Archetype.Healer),
//        Watchman (Archetype.Healer),
        Berserker (Archetype.Warrior),
//        Knight (Archetype.Warrior),
//        Avenger (Archetype.Warrior),
//        Beastmaster (Archetype.Warrior),
//        Champion (Archetype.Warrior),
//        Marshal (Archetype.Warrior),
//        Raider (Archetype.Warrior),
//        Skirmisher (Archetype.Warrior),
//        Steelcaster (Archetype.Warrior),
        Thief (Archetype.Scout),
//        Wildlander (Archetype.Scout),
//        BountyHunter (Archetype.Scout),
//        Monk (Archetype.Scout),
//        Ravager (Archetype.Scout),
//        ShadowWalker (Archetype.Scout),
//        Stalker (Archetype.Scout),
//        TreasureHunter (Archetype.Scout),
//        Trickster (Archetype.Scout)
        ;

        Archetype archetype;
        HeroClass(Archetype archetype) {
            this.archetype = archetype;
        }

        public Archetype getArchetype() {
            return archetype;
        }

        public static HeroClass[] getClassesForArchetype(Archetype archetype) {
            List<HeroClass> heroClassList = new ArrayList<>();
            for (HeroClass heroClass: values()) {
                if (heroClass.archetype == archetype) heroClassList.add(heroClass);
            }
            return heroClassList.toArray(new HeroClass[0]);
        }
    }

    public enum AttackType {
        NONE, MELEE, RANGED, BLAST, BOTH;
    }

}
