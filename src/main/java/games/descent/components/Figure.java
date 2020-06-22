package games.descent.components;

import core.components.Card;
import core.components.Deck;
import core.components.Token;
import core.properties.Property;
import core.properties.PropertyStringArray;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static games.descent.DescentConstants.costHash;
import static games.descent.DescentConstants.equipSlotHash;

// TODO: figure out how to do ability/heroic-feat
public class Figure extends Token {
    int xp;
    Deck<Card> skills;
    Deck<Card> handEquipment;
    Card armor;
    Deck<Card> otherEquipment;

    HashMap<String, Integer> equipSlotsAvailable;

    public Figure(String name) {
        super(name);
        xp = 0;

        skills = new Deck<>("Skills");
        handEquipment = new Deck<>("Hands");
        otherEquipment = new Deck<>("OtherItems");

        equipSlotsAvailable = new HashMap<>();
        equipSlotsAvailable.put("hand", 2);
        equipSlotsAvailable.put("armor", 1);
        equipSlotsAvailable.put("other", 2);
    }

    protected Figure(String name, int ID) {
        super(name, ID);
    }

    public boolean equip(Card c) {
        // Check if equipment
        Property cost = c.getProperty(costHash);
        if (cost != null) {
            // Equipment! Check if it's legal to equip
            String[] equip = ((PropertyStringArray)c.getProperty(equipSlotHash)).getValues();
            boolean canEquip = true;
            HashMap<String, Integer> equipSlots = new HashMap<>(equipSlotsAvailable);
            for (String e: equip) {
                if (equipSlots.get(e) < 1) {
                    canEquip = false;
                    break;
                } else {
                    equipSlots.put(e, equipSlots.get(e)-1);
                }
            }
            if (canEquip) {
                equipSlotsAvailable = equipSlots;
                switch (equip[0]) {
                    case "armor":
                        armor = c;
                        break;
                    case "hand":
                        handEquipment.add(c);
                        break;
                    case "other":
                        otherEquipment.add(c);
                        break;
                }
                return true;
            }
            return false;
        } else {
            // A skill
            skills.add(c);
            return true;
        }
    }

    public int getXP() {
        return xp;
    }

    public void setXP(int xp) {
        this.xp = xp;
    }

    @Override
    public Figure copy() {
        Figure copy = new Figure(componentName, componentID);
        copy.xp = xp;
        // TODO: copy all
        copyComponentTo(copy);
        return copy;
    }

    /**
     * Creates a Token objects from a JSON object.
     * @param figure - JSON to parse into Figure object.
     */
    protected void loadFigure(JSONObject figure) {
        this.componentName = (String) figure.get("id");
        // TODO: custom load of figure properties
        parseComponent(this, figure);
    }

    /**
     * Loads all figures from a JSON file.
     * @param filename - path to file.
     * @return - List of Figure objects.
     */
    public static List<Figure> loadFigures(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Figure> figures = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Figure newFigure = new Figure("");
                newFigure.loadFigure((JSONObject) o);
                figures.add(newFigure);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return figures;
    }
}
