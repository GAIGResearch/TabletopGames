package games.descent;

import core.AbstractGameData;
import core.components.*;
import core.properties.PropertyString;
import games.descent.components.Figure;
import games.descent.concepts.Quest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Vector2D;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static games.descent.DescentConstants.archetypeHash;


public class DescentGameData extends AbstractGameData {
    List<GridBoard> tiles;
    List<GraphBoard> boardConfigurations;
    List<Figure> figures;
    List<Deck<Card>> decks;
    List<Quest> quests;
    List<Quest> sideQuests;

    @Override
    public void load(String dataPath) {
        tiles = GridBoard.loadBoards(dataPath + "tiles.json");
        boardConfigurations = GraphBoard.loadBoards(dataPath + "boards.json");

        figures = Figure.loadFigures(dataPath + "heroes.json");
//        figures.addAll(Figure.loadFigures(dataPath + "monsters.json"));

        quests = loadQuests(dataPath + "mainQuests.json");
//        sideQuests = loadQuests(dataPath + "sideQuests.json");

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
        for (Token t: figures) {
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

    public List<Figure> findHeroes(String archetype) {
        List<Figure> heroes = new ArrayList<>();
        for (Figure f: figures) {
            if (f.getTokenType().equalsIgnoreCase("hero")) {
                String arch = ((PropertyString)f.getProperty(archetypeHash)).value;
                if (arch != null && arch.equalsIgnoreCase(archetype)) {
                    heroes.add(f.copy());
                }
            }
        }
        return heroes;
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
                HashMap<String, ArrayList<Vector2D>> startingLocations = new HashMap<>();
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

                // Quest read complete
                quests.add(q);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return quests;
    }
}
