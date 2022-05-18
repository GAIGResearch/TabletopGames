package games.descent2e.components;

import core.components.Card;
import core.components.Deck;
import core.properties.Property;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static games.descent2e.DescentConstants.*;

// TODO: figure out how to do ability/heroic-feat
public class Hero extends Figure {
    Deck<Card> skills;
    Deck<Card> handEquipment;
    Card armor;
    Deck<Card> otherEquipment;
    HashMap<String, Integer> equipSlotsAvailable;

    int fatigue;  // TODO: reset this every quest to max fatigue

    public Hero(String name) {
        super(name);

        skills = new Deck<>("Skills");
        handEquipment = new Deck<>("Hands");
        otherEquipment = new Deck<>("OtherItems");

        equipSlotsAvailable = new HashMap<>();
        equipSlotsAvailable.put("hand", 2);
        equipSlotsAvailable.put("armor", 1);
        equipSlotsAvailable.put("other", 2);

        tokenType = "Hero";
    }

    protected Hero(String name, int ID) {
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

    public int getFatigue() {
        return fatigue;
    }

    public void setFatigue(int fatigue) {
        this.fatigue = fatigue;
    }

    @Override
    public Hero copy() {
        Hero copy = new Hero(componentName, componentID);
        copy.equipSlotsAvailable = new HashMap<>();
        for (Map.Entry<String, Integer> e: equipSlotsAvailable.entrySet()) {
            copy.equipSlotsAvailable.put(e.getKey(), e.getValue());
        }
        copy.skills = skills.copy();
        copy.handEquipment = handEquipment.copy();
        copy.otherEquipment = otherEquipment.copy();
        if (armor != null) {
            copy.armor = armor.copy();
        }
        copy.fatigue = fatigue;
        super.copyComponentTo(copy);
        return copy;
    }

    /**
     * Creates a Token objects from a JSON object.
     * @param figure - JSON to parse into Figure object.
     */
    protected void loadHero(JSONObject figure) {
        super.loadFigure(figure);
        // TODO: custom load of figure properties
        this.fatigue = ((PropertyInt)getProperty(fatigueHash)).value;
    }

    /**
     * Loads all figures from a JSON file.
     * @param filename - path to file.
     * @return - List of Figure objects.
     */
    public static List<Hero> loadHeroes(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Hero> figures = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Hero newFigure = new Hero("");
                newFigure.loadHero((JSONObject) o);
                figures.add(newFigure);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return figures;
    }
}
