package games.descent2e.components;

import core.CoreConstants;
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
    private DiceType colour;
    private int result;
    private int damage;
    private int surge;
    private int range;
    private int shielding;
    private int nSides;

    private Map<Integer, Map<String, Integer>> sides;
    public static List<DescentDice> masterDice;

    public DescentDice(){
        super(CoreConstants.ComponentType.DICE);
        this.sides = new HashMap<>();
    }

    private DescentDice(int ID){
        super(CoreConstants.ComponentType.DICE, ID);
    }

    public int roll(Random r){
        int roll = r.nextInt(this.nSides) + 1;
        setFace(roll);
        return 1;
    }
    public void setFace(int face) {
        this.result = face;
        this.damage = sides.get(face).get("damage");
        this.surge = sides.get(face).get("surge");
        this.range = sides.get(face).get("range");
        this.shielding = sides.get(face).get("shielding");
    }
    public int getFace() {
        return result;
    }

    public static void loadDice(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        masterDice = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                DescentDice newDice = new DescentDice();
                newDice.loadDie((JSONObject) o);
                masterDice.add(newDice);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        masterDice = Collections.unmodifiableList(masterDice);
    }
    public void loadDie(JSONObject dice) {
        this.nSides = ((Long) ( (JSONArray) dice.get("count")).get(1)).intValue();
        for (int i = 1; i <= nSides; i++) {
            String diceDescriptor = (String) ((JSONArray) dice.get("colour")).get(1);
            this.colour = DiceType.valueOf(diceDescriptor.toUpperCase(Locale.ROOT));
            Map<String, Integer> sideMap = new HashMap<>();
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

    public int getShielding() {
        return shielding;
    }

    public DiceType getColour() {
        return colour;
    }

    @Override
    public DescentDice copy() {
        DescentDice  copy = new DescentDice(this.componentID);
        copy.colour = this.colour;
        copy.shielding = this.shielding;
        copy.nSides = this.nSides;
        copy.result = this.result;
        copy.damage = this.damage;
        copy.surge = this.surge;
        copy.range = this.range;
        copy.sides = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Integer>> i: sides.entrySet()) {
            Map<String, Integer> m = new HashMap<>(i.getValue());
            copy.sides.put(i.getKey(), m);
        }
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DescentDice) {
            DescentDice dice = (DescentDice) obj;
            return dice.colour == this.colour && dice.result == this.result && dice.damage == this.damage &&
                    dice.surge == this.surge && dice.range == this.range && dice.shielding == this.shielding &&
                    dice.nSides == this.nSides && dice.sides.equals(this.sides);
        }
        return false;
    }
    @Override
    public int hashCode() {
        return Objects.hash(colour, result, damage, surge, range, shielding, nSides, sides);
    }

}
