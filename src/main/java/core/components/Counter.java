package core.components;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils.ComponentType;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Counter extends Component implements ICounter {
    private int count;      //By default, counters go from min to max, and are initialized at min.
    private int minimum;
    private int maximum;
    private String id;

    public Counter() {
        super.type = ComponentType.COUNTER;
        this.properties = new HashMap<>();
    }

    @Override
    public Counter copy() {
        Counter copy = new Counter();
        copy.minimum = minimum;
        copy.maximum = maximum;
        copy.count = count;
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public Boolean isMinimum()  { return this.count == this.minimum; }
    @Override
    public Boolean isMaximum()  { return this.count == this.maximum; }
    @Override
    public int getValue() { return this.count;                 }

    @Override
    public void increment(int value) {
        this.count += value;
        if (this.count > this.maximum) {
            this.count = this.maximum;
        }
    }

    @Override
    public void decrement(int value) {
        this.count -= value;
        if (this.count < this.minimum) {
            this.count = this.minimum;
        }
    }

    @Override
    public void setValue(int i) {
        this.count = i;
    }


    public void loadCounter(JSONObject counter) {

        this.minimum = ((Long) ( (JSONArray) counter.get("min")).get(1)).intValue();
        this.maximum = ((Long) ( (JSONArray) counter.get("max")).get(1)).intValue();
        this.id = (String) counter.get("id");

        if(counter.get("count") == null)
            this.count = this.minimum;
        else
            this.count = ((Long) ( (JSONArray) counter.get("count")).get(1)).intValue();

        parseComponent(this, counter);
    }

    public static List<Counter> loadCounters(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Counter> counters = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {

                Counter newCounter = new Counter();
                newCounter.loadCounter((JSONObject) o);
                counters.add(newCounter);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return counters;
    }

    @Override
    public String getID() {
        return id;
    }

}
