package games.descent;

import core.AbstractGameData;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.components.Token;
import core.properties.PropertyString;
import games.descent.components.Figure;
import games.descent.concepts.Quest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static games.descent.DescentConstants.archetypeHash;


public class DescentGameData extends AbstractGameData {
    List<GridBoard> tiles;
    List<GraphBoard> boardConfigurations;
    List<Token> figures;
    List<Quest> quests;
    List<Quest> sideQuests;

    @Override
    public void load(String dataPath) {
        tiles = GridBoard.loadBoards(dataPath + "tiles.json");
        figures = Figure.loadTokens(dataPath + "heroes.json");
        boardConfigurations = GraphBoard.loadBoards(dataPath + "boards.json");
        quests = loadQuests(dataPath + "mainQuests.json");
//        sideQuests = loadQuests(dataPath + "sideQuests.json");
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

    public Quest findQuest(String name) {
        for (Quest q: quests) {
            if (q.getName().equalsIgnoreCase(name)) {
                return q.copy();
            }
        }
        return null;
    }

    public List<Token> findHeroes(String archetype) {
        List<Token> heroes = new ArrayList<>();
        for (Token f: figures) {
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

                ArrayList<String> boards = new ArrayList<>();
                JSONArray bs = (JSONArray) obj.get("boards");
                if (bs != null) {
                    for (Object o2 : bs) {
                        boards.add((String) o2);
                    }
                    q.setBoards(boards);
                }

                quests.add(q);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return quests;
    }
}
