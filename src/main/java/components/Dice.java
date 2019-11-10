package components;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils.ComponentType;

public class Dice extends Component {
    private int number_of_sides;  //By default 1d6

    public Dice() {
        super.type = ComponentType.DICE;
        this.number_of_sides = 0;
        this.properties = new HashMap<>();
    }

    public int  getNumberOfSides()                    { return this.number_of_sides;            }
    public void setNumberOfSides(int number_of_sides) { this.number_of_sides = number_of_sides; }

    public Dice copy() {
        Dice copy = new Dice();
        copy.number_of_sides = number_of_sides;
        copyComponentTo(copy);
        return copy;
    }

    public void loadDice(JSONObject dice) {
        this.number_of_sides = ((Long) ( (JSONArray) dice.get("count")).get(1)).intValue();
        parseComponent(this, dice);
    }


    public static List<Dice> loadDice(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Dice> dice = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Dice newDice = new Dice();
                newDice.loadDice((JSONObject) o);
                dice.add(newDice);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return dice;
    }


    public int roll() {
        Random r = new Random();
        return r.nextInt(this.number_of_sides) + 1;
    }

}
