package games.descent2e;

import core.AbstractGameData;
import core.components.*;
import core.properties.PropertyString;
import games.descent2e.actions.tokens.TokenAction;
import games.descent2e.components.*;
import games.descent2e.components.cards.SearchCard;
import games.descent2e.components.tokens.DToken;
import games.descent2e.concepts.DescentReward;
import games.descent2e.concepts.GameOverCondition;
import games.descent2e.concepts.Quest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Vector2D;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static games.descent2e.DescentConstants.archetypeHash;

public class DescentGameData extends AbstractGameData {
    List<GridBoard> tiles;
    List<GraphBoard> boardConfigurations;
    List<Hero> heroes;
    List<Deck<Card>> decks;
    Deck<Card> searchCards;
    List<Quest> quests;
    List<Quest> sideQuests;
    HashMap<String, HashMap<String, Monster>> monsters;

    @Override
    public void load(String dataPath) {
        tiles = GridBoard.loadBoards(dataPath + "tiles.json");
        boardConfigurations = GraphBoard.loadBoards(dataPath + "boards.json");

        DescentDice.loadDice(dataPath + "/components/dice.json");
        heroes = Hero.loadHeroes(dataPath + "heroes.json");
        monsters = loadMonsters(dataPath + "monsters.json");

        quests = loadQuests(dataPath + "mainQuests.json");
//        sideQuests = loadQuests(dataPath + "sideQuests.json");

        searchCards = SearchCard.loadCards(dataPath + "searchCards.json");

        decks = new ArrayList<>();
        // Read all class decks
        File classesPath = new File(dataPath + "classes/");
        File[] filesList = classesPath.listFiles();
        if (filesList != null) {
            for (File f: filesList) {
                decks.addAll(Deck.loadDecksOfCards(f.getAbsolutePath()));
            }
        }
    }

    @Override
    public GridBoard findGridBoard(String name) {
        for (GridBoard gb: tiles) {
            if (gb.getComponentName().equalsIgnoreCase(name)) {
                return gb;
            }
        }
        return null;
    }

    @Override
    public GraphBoard findGraphBoard(String name) {
        for (GraphBoard gb: boardConfigurations) {
            if (gb.getComponentName().equalsIgnoreCase(name)) {
                return gb;
            }
        }
        return null;
    }

    @Override
    public Token findToken(String name) {
        for (Token t: heroes) {
            if (t.getComponentName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public Deck<Card> findDeck(String name) {
        for (Deck<Card> d: decks) {
            if (name.equalsIgnoreCase(d.getComponentName())) {
                return d.copy();
            }
        }
        return null;
    }

    public Quest findQuest(String name) {
        for (Quest q: quests) {
            if (q.getName().equalsIgnoreCase(name)) {
                return q.copy();
            }
        }
        return null;
    }

    public Hero findHero(String name) {
        for (Hero h: heroes) {
            if (h.getComponentName().equalsIgnoreCase(name)) {
                return h.copy();
            }
        }
        throw new IllegalArgumentException("Hero not found: " + name);
    }

    public List<Hero> findHeroes(DescentTypes.Archetype archetype) {
        List<Hero> heroes = new ArrayList<>();
        for (Hero f: this.heroes) {
            if (f.getTokenType().equalsIgnoreCase("hero")) {
                String arch = ((PropertyString)f.getProperty(archetypeHash)).value;
                if (arch != null && arch.equalsIgnoreCase(archetype.name())) {
                    heroes.add(f.copy());
                }
            }
        }
        return heroes;
    }

    public HashMap<String, Monster> findMonster(String name) {
        return monsters.get(name);
    }

    private static ArrayList<Quest> loadQuests(String dataPath) {

        JSONParser jsonParser = new JSONParser();
        ArrayList<Quest> quests = new ArrayList<>();

        try (FileReader reader = new FileReader(dataPath)) {
            JSONArray data = (JSONArray) jsonParser.parse(reader);

            for (Object o : data) {
                JSONObject obj = (JSONObject) o;
                Quest q = new Quest();
                q.setName((String) obj.get("id"));

                // Find act
                int act = 1;
                Object actObj = obj.get("act");
                if (actObj != null) {
                    act = (int) (long) actObj;
                }
                q.setAct(act);

                // Find all boards for the quest
                ArrayList<String> boards = new ArrayList<>();
                JSONArray bs = (JSONArray) obj.get("boards");
                if (bs != null) {
                    for (Object o2 : bs) {
                        boards.add((String) o2);
                    }
                    q.setBoards(boards);
                }

                // Find starting locations for players, maps to a board
                Map<String, List<Vector2D>> startingLocations = new HashMap<>();
                JSONArray ls = (JSONArray) obj.get("starting-locations");
                if (ls != null) {
                    int i = 0;
                    for (Object b: ls) {
                        JSONArray board = (JSONArray) b;
                        ArrayList<Vector2D> locations = new ArrayList<>();
                        for (Object o2: board) {
                            JSONArray arr = (JSONArray) o2;
                            locations.add(new Vector2D((int)(long)arr.get(0), (int)(long)arr.get(1)));
                        }
                        startingLocations.put(boards.get(i), locations);
                        i++;
                    }
                    q.setStartingLocations(startingLocations);
                }

                // Find monsters
                ArrayList<String[]> qMonsters = new ArrayList<>();
                JSONArray ms = (JSONArray) obj.get("monsters");
                if (ms != null) {
                    for (Object o1 : ms) {
                        JSONArray mDef = (JSONArray) o1;
                        qMonsters.add((String[]) mDef.toArray(new String[0]));
                    }
                    q.setMonsters(qMonsters);
                }

                // Find tokens
                ArrayList<DToken.DTokenDef> qTokens = new ArrayList<>();
                JSONArray ts = (JSONArray) obj.get("tokens");
                if (ts != null) {
                    for (Object o1 : ts) {
                        JSONArray tDef = (JSONArray) o1;
                        DToken.DTokenDef def = new DToken.DTokenDef();
                        def.setTokenType(DescentTypes.DescentToken.valueOf((String) tDef.get(0)));
                        def.setAltName((String) tDef.get(1));
                        def.setSetupHowMany((String) tDef.get(2));
                        def.setLocations(jsonArrayToStringArray((JSONArray) tDef.get(3)));

                        String[] rules = ((String) tDef.get(4)).split(";");
                        ArrayList<TokenAction> effects = new ArrayList<>();
                        HashMap<Figure.Attribute, Integer> attributeModifiers = new HashMap<>();
                        for (String rule: rules) {
                            if (rule.contains("AttributeModifier")) {
                                // An attribute modifier
                                String[] split = rule.split(":");
                                attributeModifiers.put(Figure.Attribute.valueOf(split[1]), Integer.parseInt(split[2]));
                            } else if (rule.contains("Effect")) {
                                // An effect, needs a no-arg constructor
                                try {
                                    Class<?> clazz = Class.forName("games.descent2e.actions.tokens." + rule.split(":")[1]);
                                    TokenAction effect = (TokenAction) clazz.getDeclaredConstructor().newInstance();
                                    effects.add(effect);
                                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        def.setEffects(effects);
                        def.setAttributeModifiers(attributeModifiers);
                        qTokens.add(def);
                    }
                    q.setTokens(qTokens);
                }

                // Find special rules

                // Find game over conditions
                JSONArray gos = (JSONArray) obj.get("game-over");
                ArrayList<GameOverCondition> conditions = new ArrayList<>();
                if (gos != null) {
                    for (Object o1 : gos) {
                        JSONObject def = (JSONObject) o1;
                        String conditionClass = (String) def.get("id");
                        try {
                            // Create instance of class based on name
                            Class<?> clazz = Class.forName("games.descent2e.concepts." + conditionClass);
                            GameOverCondition condition = (GameOverCondition) clazz.getDeclaredConstructor().newInstance();
                            condition.parse(def);  // Fill in details
                            conditions.add(condition);  // Add to list
                        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                }
                q.setGameOverConditions(conditions);

                // Find rewards
                JSONArray crs = (JSONArray) obj.get("common-rewards");
                ArrayList<DescentReward> commonRewards = new ArrayList<>();
                if (crs != null) {
                    for (Object o1 : crs) {
                        commonRewards.add(DescentReward.parse((JSONObject) o1));
                    }
                }
                q.setCommonRewards(commonRewards);
                JSONArray hrs = (JSONArray) obj.get("hero-rewards");
                ArrayList<DescentReward> heroRewards = new ArrayList<>();
                if (hrs != null) {
                    for (Object o1 : hrs) {
                        commonRewards.add(DescentReward.parse((JSONObject) o1));
                    }
                }
                q.setHeroRewards(heroRewards);
                JSONArray ors = (JSONArray) obj.get("overlord-rewards");
                ArrayList<DescentReward> overlordRewards = new ArrayList<>();
                if (ors != null) {
                    for (Object o1 : ors) {
                        commonRewards.add(DescentReward.parse((JSONObject) o1));
                    }
                }
                q.setOverlordRewards(overlordRewards);

                // Quest read complete
                quests.add(q);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return quests;
    }

    private static String[] jsonArrayToStringArray(JSONArray ar) {
        String[] ar2 = new String[ar.size()];
        for (int i = 0; i < ar.size(); i++) {
            ar2[i] = (String) ar.get(i);
        }
        return ar2;
    }

    private static HashMap<String, HashMap<String, Monster>> loadMonsters(String dataPath) {
        HashMap<String, HashMap<String, Monster>> monsters = new HashMap<>();

        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(dataPath)) {
            JSONArray data = (JSONArray) jsonParser.parse(reader);

            for (Object o : data) {
                JSONObject obj = (JSONObject) o;

                String key = (String) obj.get("id");
                Monster superT = new Monster();
                HashSet<String> ignoreKeys = new HashSet<String>(){{
                    add("act1");
                    add("act2");
                    add("id");
                }};

                superT.loadFigure(obj, ignoreKeys);

                ignoreKeys.clear();
                ignoreKeys.add("type");
                ignoreKeys.add("id");

                HashMap<String, Monster> monsterDef = new HashMap<>();
                Monster act1m = new Monster();
                act1m.loadFigure((JSONObject) ((JSONArray)obj.get("act1")).get(0), ignoreKeys);
                Monster act1M = new Monster();
                act1M.loadFigure((JSONObject) ((JSONArray)obj.get("act1")).get(1), ignoreKeys);

                Monster act2m = new Monster();
                act2m.loadFigure((JSONObject) ((JSONArray)obj.get("act2")).get(0), ignoreKeys);
                Monster act2M = new Monster();
                act2M.loadFigure((JSONObject) ((JSONArray)obj.get("act2")).get(1), ignoreKeys);

                String attackType = ((JSONArray)obj.get("attackType")).get(1).toString();
                //System.out.println(attackType);
                act1m.setAttackType(attackType);
                act1M.setAttackType(attackType);
                act2m.setAttackType(attackType);
                act2M.setAttackType(attackType);
                superT.setAttackType(attackType);

                monsterDef.put("1-minion", act1m);
                monsterDef.put("1-master", act1M);
                monsterDef.put("2-minion", act2m);
                monsterDef.put("2-master", act2M);
                monsterDef.put("super", superT);

                monsters.put(key, monsterDef);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return monsters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DescentGameData that = (DescentGameData) o;
        return Objects.equals(tiles, that.tiles) && Objects.equals(boardConfigurations, that.boardConfigurations) && Objects.equals(heroes, that.heroes) && Objects.equals(decks, that.decks) && Objects.equals(searchCards, that.searchCards) && Objects.equals(quests, that.quests) && Objects.equals(sideQuests, that.sideQuests) && Objects.equals(monsters, that.monsters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tiles, boardConfigurations, heroes, decks, searchCards, quests, sideQuests, monsters);
    }

    public DescentGameData copy() {
        DescentGameData copy = new DescentGameData();
        copy.tiles = new ArrayList<>();
        for (GridBoard gb: tiles) {
            copy.tiles.add(gb.copy());
        }
        copy.boardConfigurations = new ArrayList<>();
        for (GraphBoard gb: boardConfigurations) {
            copy.boardConfigurations.add(gb.copy());
        }
        copy.heroes = new ArrayList<>();
        for (Hero h: heroes) {
            copy.heroes.add(h.copy());
        }
        copy.decks = new ArrayList<>();
        for (Deck<Card> d: decks) {
            copy.decks.add(d.copy());
        }
        copy.searchCards = searchCards.copy();
        copy.quests = new ArrayList<>();
        for (Quest q: quests) {
            copy.quests.add(q.copy());
        }
        if (sideQuests != null) {
            copy.sideQuests = new ArrayList<>();
            for (Quest q : sideQuests) {
                copy.sideQuests.add(q.copy());
            }
        }
        copy.monsters = new HashMap<>();
        for (String k: monsters.keySet()) {
            HashMap<String, Monster> monsterDef = new HashMap<>();
            for (String k2: monsters.get(k).keySet()) {
                monsterDef.put(k2, monsters.get(k).get(k2).copy());
            }
            copy.monsters.put(k, monsterDef);
        }
        return copy;
    }
}
