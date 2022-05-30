package games.descent2e.components;

import core.components.Component;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DescentDice extends Component {
    private String colour;
    private int damage;
    private int surge;
    private int range;
    private int shielding;
    private int nSides;

    private HashMap<Integer, HashMap<String, Integer>> sides;

    public DescentDice(){
        super(Utils.ComponentType.DICE);
        this.sides = new HashMap<>();
    }

    public int roll(Random r){
        int roll = r.nextInt(this.nSides) + 1;
        this.damage = sides.get(roll).get("damage");
        this.surge = sides.get(roll).get("surge");
        this.range = sides.get(roll).get("range");
        this.shielding = sides.get(roll).get("shielding");
        return 1;
    }

    public static ArrayList<DescentDice> loadDice(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<DescentDice> dice = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                DescentDice newDice = new DescentDice();
                newDice.loadDie((JSONObject) o);
                String colour = newDice.colour;
                dice.add(newDice);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return dice;
    }
    public void loadDie(JSONObject dice) {
        this.nSides = ((Long) ( (JSONArray) dice.get("count")).get(1)).intValue();
        for (int i = 1; i <= nSides; i++) {
            this.colour = (String) ((JSONArray) dice.get("colour")).get(1);
            HashMap<String, Integer> sideMap = new HashMap<>();
            JSONArray diceArray = (JSONArray) ((JSONArray) dice.get(Integer.toString(i))).get(1);
            Long tempDamage = (Long) diceArray.get(2);
            Long tempRange = (Long) diceArray.get(0);
            Long tempSurge = (Long) diceArray.get(1);
            Long tempShield = (Long) diceArray.get(3);
            sideMap.put("damage", tempDamage.intValue());
            sideMap.put("range", tempRange.intValue());
            sideMap.put("surge", tempSurge.intValue());
            sideMap.put("shielding", tempShield.intValue());
            this.sides.put(i, sideMap);
        }
        parseComponent(this, dice);
    }

    public int getRange() {
        return range;
    }

    public int getSurge() {
        return surge;
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public DescentDice copy() {
        DescentDice  copy = new DescentDice();
        copy.colour = this.colour;
        copy.shielding = this.shielding;
        copy.nSides = this.nSides;
        copy.damage = this.damage;
        copy.surge = this.surge;
        copy.range = this.range;
        copy.sides = new HashMap<>();
        for (Map.Entry<Integer, HashMap<String, Integer>> i: sides.entrySet()) {
            HashMap<String, Integer> m = new HashMap<>(i.getValue());
            sides.put(i.getKey(), m);
        }
        copyComponentTo(copy);
        return copy;
    }

    public int getShielding() {
        return shielding;
    }

    public String getColour() {
        return colour;
    }
}
