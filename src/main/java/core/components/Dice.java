package core.components;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import core.CoreConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import static core.components.Dice.Type.*;

public class Dice extends Component {
    public enum Type{
        d3(3),
        d4(4),
        d6(6),
        d8(8),
        d10(10),
        d12(12),
        d20(20),
        dCustom(-1);
        public final int nSides;
        Type(int nSides) {
            this.nSides = nSides;
        }
        static Type sidesToType(int nSides) {
            for (Type t: values()) if (t.nSides == nSides) return t;
            return dCustom;
        }
    }
    public static Map<Type, Dice> StandardDice = new HashMap<Type, Dice>() {{
        put(d3, new Dice(d3));
        put(d4, new Dice(d4));
        put(d6, new Dice(d6));
        put(d8, new Dice(d8));
        put(d10, new Dice(d10));
        put(d12, new Dice(d12));
        put(d20, new Dice(d20));
    }};

    public final Type type;
    public final int nSides; // Number of sides
    protected int value;  // Current value after last roll

    public Dice() {
        this(d6);  // By default d6
    }

    public Dice(Type type) {
        super(CoreConstants.ComponentType.DICE);
        this.type = type;
        this.nSides = type.nSides;
    }
    public Dice(int nSides) {
        super(CoreConstants.ComponentType.DICE);
        this.nSides = nSides;
        this.type = Type.sidesToType(nSides);
    }

    private Dice(Type type, int nSides, int value, int ID) {
        super(CoreConstants.ComponentType.DICE, ID);
        this.type = type;
        this.nSides = nSides;
        this.value = value;
    }

    /**
     * @return current value shown by this die
     */
    public int getValue() {
        return value;
    }

    public void setValue(int v) {
        if (v > nSides || v < 1)
            throw new IllegalArgumentException("Invalid number for die : " + v);
        value = v;
    }

    /**
     * @return number of sides for this die.
     */
    public int getNumberOfSides() {
        return this.nSides;
    }

    /**
     * Rolls the die and returns result for roll in range [1, nSides].
     *
     * @param r - random generator.
     */
    public void roll(Random r) {
        value = r.nextInt(this.nSides) + 1;
    }

    @Override
    public Dice copy() {
        Dice copy = new Dice(type, nSides, value, componentID);
        copyComponentTo(copy);
        return copy;
    }

    /**
     * Loads all dice from a JSON file.
     *
     * @param filename - path to file.
     * @return - List of Dice objects.
     */
    public static List<Dice> loadDice(String filename) {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Dice> dice = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for (Object o : data) {
                dice.add(loadDie((JSONObject) o));
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return dice;
    }

    /**
     * Creates a new Dice object with properties from a JSON object.
     *
     * @param dice - new Dice object parsed from JSON.
     */
    public static Dice loadDie(JSONObject dice) {
        Dice newDice = new Dice(((Long) ((JSONArray) dice.get("count")).get(1)).intValue());
        parseComponent(newDice, dice);
        return newDice;
    }


    @Override
    public final int hashCode() {
        return componentID;
    }
}
