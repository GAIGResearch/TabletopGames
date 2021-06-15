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

    //Each unit consist of 3 soldiers. Since a unit can only have one type of soldier, there is no need to create different classes.
    //Variables
    private String id;
    private String name;
    public Faction faction;
    public int move;
    public int strength;
    public int health;
    //protected int unitCount;
    //Add special power

    public enum Faction
    {
        NA, Dakhan_Lords, Uthuk_Yllan
    }

    public Unit()
    {
        super(Utils.ComponentType.TOKEN, "");
        this.name = "";
        this.id = "";
        this.move = 0;
        this.strength = 0;
        this.health = 0;
        //this.unitCount = 0;
        this.faction = Faction.NA;
    }

    public Unit(Utils.ComponentType type, String id, String name, int move, int strength, int health, Faction faction)//, int unitCount)
    {
        super(type, name);
        this.name = name;
        this.id = id;
        this.move = move;
        this.strength = strength;
        this.health = health;
        this.faction = faction;
        //this.unitCount = unitCount;
    }


    public int getTotalStrength()
    {
        return strength; //* unitCount;
    }

    public int getTotalHealth()
    {
        return health;// * unitCount;
    }

    @Override
    public Component copy()
    {
        return new Unit(type, id, name, move, strength, health, faction);//, unitCount);
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
                faction == unit.faction;//&&
                //unitCount == unit.unitCount;
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
        this.faction = parseFaction(((Long) ( (JSONArray) unit.get("faction")).get(1)).intValue());
        //this.unitCount = ((Long) ( (JSONArray) unit.get("unitCount")).get(1)).intValue();

        //this.type = (String) unit.get("type");
        //this.special = (String) unit.get("special");
        //this.id =  (String) unit.get("id");

        parseComponent(this, unit);
    }

    private Faction parseFaction(int faction)
    {
        if(faction == 1)
        {
            return Faction.Dakhan_Lords;
        }
        else if (faction == 0)
        {
            return Faction.Uthuk_Yllan;
        }
        else
        {
            return Faction.NA;
        }
    }

    @Override
    public final int hashCode() {
        return componentID;
    }
}
