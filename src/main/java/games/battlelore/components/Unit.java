package games.battlelore.components;

import core.components.Component;
import core.components.Counter;
import core.interfaces.IComponentContainer;
import games.coltexpress.components.Loot;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Unit extends Component
{
    //Variables
    private String id;
    private String name;
    public int faction;
    public int move;
    public int strength;
    public int health;
    //Add special power

    public Unit()
    {
        super(Utils.ComponentType.TOKEN, "");
        this.name = "";
        this.id = "";
        this.move = 0;
        this.strength = 0;
        this.health = 0;
        this.faction = -1;
    }

    public Unit(Utils.ComponentType type, String id, String name, int move, int strength, int health, int faction)
    {
        super(type, name);
        this.name = name;
        this.id = id;
        this.move = move;
        this.strength = strength;
        this.health = health;
        this.faction = faction;
    }


    @Override
    public Component copy()
    {
        return new Unit(type, id, name, move, strength, health, faction);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof Loot))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }
        Unit unit = (Unit) o;
        return id == unit.id &&
                type == unit.type &&
                name == unit.name &&
                move == unit.move &&
                strength == unit.strength &&
                health == unit.health &&
                faction == unit.faction;
    }

    public static List<Unit> loadUnits(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Unit> units = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Unit newUnit = new Unit();
                newUnit.loadUnit((JSONObject) o);
                units.add(newUnit);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return units;
    }

    public void loadUnit(JSONObject unit)
    {
        this.move = ((Long) ( (JSONArray) unit.get("move")).get(1)).intValue();
        this.strength = ((Long) ( (JSONArray) unit.get("strength")).get(1)).intValue();
        this.health = ((Long) ( (JSONArray) unit.get("health")).get(1)).intValue();
        this.componentName = (String) unit.get("id");
        this.name = (String) unit.get("name");
        this.faction = ((Long) ( (JSONArray) unit.get("faction")).get(1)).intValue();;

        //this.type = (String) unit.get("type");
        //this.special = (String) unit.get("special");
        //this.id =  (String) unit.get("id");

        parseComponent(this, unit);
    }

    @Override
    public final int hashCode() {
        return componentID;
    }
}
