package games.battlelore;

import core.AbstractGameData;
import core.components.*;
import games.battlelore.components.Unit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BattleloreData extends AbstractGameData
{
    private GridBoard board;
    private List<Deck<Card>> decks;
    private List<Unit> units;

    @Override
    public void load(String dataPath)
    {
        //units = Unit.parseComponent();

    }

    public static List<Component> loadUnits(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Component> units = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                //Component newGraphBoard = new Component();
                //newGraphBoard.loadBoard((JSONObject) o);
                //Component.add(newGraphBoard);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return units;
    }
}
