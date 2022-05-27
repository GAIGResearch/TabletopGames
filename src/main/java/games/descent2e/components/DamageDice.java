package games.descent2e.components;

import core.components.Dice;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DamageDice extends Dice {

    private int damage;
    private int surge;
    private int range;

    private HashMap<Integer, HashMap<String, Integer>> sides;

    public DamageDice(){
        this.sides = new HashMap<>();
    }

    public int roll(Random r){
        int roll = r.nextInt(this.nSides) + 1;
        this.damage = sides.get(roll).get("damage");
        this.surge = sides.get(roll).get("surge");
        this.range = sides.get(roll).get("range");
        return 1;
    }

    public static List<DamageDice> loadDice(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<DamageDice> dice = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                DamageDice newDice = new DamageDice();
                newDice.loadDie((JSONObject) o);
                dice.add(newDice);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return dice;
    }
    @Override
    public void loadDie(JSONObject dice) {
        this.nSides = ((Long) ( (JSONArray) dice.get("count")).get(1)).intValue();
        for (int i = 1; i <= nSides; i++) {
            HashMap<String, Integer> sideMap = new HashMap<>();
            JSONArray diceArray = (JSONArray) ((JSONArray) dice.get(Integer.toString(i))).get(1);
            Long tempDamage = (Long) diceArray.get(2);
            Long tempRange = (Long) diceArray.get(0);
            Long tempSurge = (Long) diceArray.get(1);
            sideMap.put("damage", tempDamage.intValue());
            sideMap.put("range", tempRange.intValue());
            sideMap.put("surge", tempSurge.intValue());
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
}
