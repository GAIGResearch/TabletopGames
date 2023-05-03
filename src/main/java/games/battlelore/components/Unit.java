package games.battlelore.components;

 import core.CoreConstants;
 import core.components.Component;
 import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

 import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
 import java.util.Objects;

public class Unit extends Component {

    //Each unit consist of 3 soldiers. Since a unit can only have one type of soldier, there is no need to create different classes.
    //Variables
    private String id;
    public String name;
    public String shortName;
    public Faction faction;
    public int moveRange;
    public int strength;
    public int health;
    protected boolean canMove;
    protected boolean canAttack;
    public boolean isMelee;

    public enum Faction {
        Uthuk_Yllan, Dakhan_Lords, NA;
    }

    public Unit() {
        super(CoreConstants.ComponentType.TOKEN, "");
        this.name = "";
        this.id = "";
        this.moveRange = 0;
        this.strength = 0;
        this.health = 0;
        this.faction = Faction.NA;
        this.shortName = "";
        this.canAttack = false;
        this.canMove = false;
        this.isMelee = true;
    }

    public Unit (CoreConstants.ComponentType type, String id, String name, int moveRange,
                 int strength, int health, Faction faction, String shortName, boolean isMelee) {
        super(type, name);
        this.name = name;
        this.id = id;
        this.moveRange = moveRange;
        this.strength = strength;
        this.health = health;
        this.faction = faction;
        this.shortName = shortName;
        this.isMelee = isMelee;
    }

    //Used by copy constructor only
    private Unit (int componentID, CoreConstants.ComponentType type, String id, String name, int moveRange,
                  int strength, int health, Faction faction, String shortName, boolean isMelee) {
        super(type, name, componentID);
        this.name = name;
        this.id = id;
        this.moveRange = moveRange;
        this.strength = strength;
        this.health = health;
        this.faction = faction;
        this.shortName = shortName;
        this.isMelee = isMelee;
    }


    public int getTotalStrength() {
        return strength;
    }

    public int getTotalHealth() {
        return health;
    }

    public boolean CanMove() {
        return this.canMove;
    }

    public boolean CanAttack() {
        return this.canAttack;
    }

    public void SetCanMove(boolean moveable) {
        this.canMove = moveable;
    }

    public void SetCanAttack(boolean canAttack) {
        this.canAttack = canAttack;
    }

    @Override
    public Component copy() {
        Unit newUnit = new Unit(componentID, type, id, name, moveRange, strength, health, faction, shortName, isMelee);
        newUnit.canMove = canMove;
        newUnit.canAttack = canAttack;
        return newUnit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Unit)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        Unit unit = (Unit) o;
        return id.equals(unit.id) &&
                componentID == unit.componentID &&
                type == unit.type &&
                name.equals(unit.name) &&
                moveRange == unit.moveRange &&
                strength == unit.strength &&
                health == unit.health &&
                faction == unit.faction &&
                isMelee == unit.isMelee &&
                Objects.equals(shortName, unit.shortName) &&
                canMove == unit.canMove &&
                canAttack == unit.canAttack;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Unit: Name=");
        sb.append(name.toString());
        sb.append("; MoveRange=");
        sb.append(moveRange);
        sb.append("; CanMove=");
        sb.append(canMove);
        sb.append("; CanAttack=");
        sb.append(canAttack);
        sb.append("; Health=");
        sb.append(health);

        return sb.toString();
    }

    public static List<Unit> loadUnits(String filename) {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Unit> units = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {
            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                Unit newUnit = new Unit();
                newUnit.loadUnit((JSONObject) o);
                units.add(newUnit);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return units;
    }

    public void loadUnit(JSONObject unit) {
        this.moveRange = ((Long) ( (JSONArray) unit.get("move")).get(1)).intValue();
        this.strength = ((Long) ( (JSONArray) unit.get("strength")).get(1)).intValue();
        this.health = ((Long) ( (JSONArray) unit.get("health")).get(1)).intValue();
        this.componentName = (String) unit.get("id");
        this.name = (String) unit.get("name");
        this.faction = parseFaction(((Long) ( (JSONArray) unit.get("faction")).get(1)).intValue());
        this.shortName = (String) unit.get("shortname");
        this.isMelee = (Boolean) ((JSONArray) unit.get("isMelee")).get(1);

        parseComponent(this, unit);
    }

    private Faction parseFaction(int faction) {
        if (faction == 1) {
            return Faction.Dakhan_Lords;
        }
        else if (faction == 0) {
            return Faction.Uthuk_Yllan;
        }
        else {
            return Faction.NA;
        }
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID, id, name, shortName, faction, moveRange, strength, health, canMove, canAttack, isMelee);
    }
}
