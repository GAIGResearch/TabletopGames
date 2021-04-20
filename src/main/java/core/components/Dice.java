package core.components;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils.ComponentType;

public class Dice extends Component {
    private int nSides;  // Number of sides
    private int value;  // Current value after last roll

    public Dice() {
        this(6);  // By default d6
    }

    public Dice(int nSides) {
        super(ComponentType.DICE);
        this.nSides = nSides;
    }

    private Dice(int nSides, int value, int ID) {
        super(ComponentType.DICE, ID);
        this.nSides = nSides;
        this.value = value;
    }

    /**
     * @return current value shown by this die
     */
    public int getValue() {
        return value;
    }

    /**
     * @return number of sides for this die.
     */
    public int  getNumberOfSides() {
        return this.nSides;
    }

    /**
     * Sets the number of sides for this die.
     * @param number_of_sides - new number of sides.
     */
    public void setNumberOfSides(int number_of_sides) {
        this.nSides = number_of_sides;
    }

    /**
     * Rolls the die and returns result for roll in range [1, nSides].
     * @param r - random generator.
     * @return - int, value of roll.
     */
    public int roll(Random r) {
        return r.nextInt(this.nSides) + 1;
    }

    @Override
    public Dice copy() {
        Dice copy = new Dice(nSides, value, componentID);
        copyComponentTo(copy);
        return copy;
    }

    /**
     * Loads all dice from a JSON file.
     * @param filename - path to file.
     * @return - List of Dice objects.
     */
    public static List<Dice> loadDice(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Dice> dice = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Dice newDice = new Dice();
                newDice.loadDie((JSONObject) o);
                dice.add(newDice);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return dice;
    }

    /**
     * Creates a new Dice object with properties from a JSON object.
     * @param dice - new Dice object parsed from JSON.
     */
    public void loadDie(JSONObject dice) {
        this.nSides = ((Long) ( (JSONArray) dice.get("count")).get(1)).intValue();
        parseComponent(this, dice);
    }


    @Override
    public final int hashCode() {
        return componentID;
    }
}
