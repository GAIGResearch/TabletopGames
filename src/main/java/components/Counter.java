package components;

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

public class Counter extends Component {
    private int count;      //By default, counters go from min to max, and are initialized at min.
    private int minimum;
    private int maximum;

    public Counter() {
        super.type = ComponentType.COUNTER;
        this.properties = new HashMap<>();
    }

    public Counter copy() {
        Counter copy = new Counter();
        copy.minimum = minimum;
        copy.maximum = maximum;
        copy.count = count;
        copyComponentTo(copy);
        return copy;
    }

    public Boolean isMinimum()  { return this.count == this.minimum; }
    public Boolean isMaximum()  { return this.count == this.maximum; }
    public int     getCounter() { return this.count;                 }

    public void increment(int value) {
        this.count += value;
        if (this.count > this.maximum) {
            this.count = this.maximum;
        }
    }

    public void decrement(int value) {
        this.count -= value;
        if (this.count < this.minimum) {
            this.count = this.minimum;
        }
    }

    public void setValue(int i) {
        this.count = i;
    }


    public void loadCounter(JSONObject token) {

        this.minimum = ((Long) ( (JSONArray) token.get("min")).get(1)).intValue();
        this.maximum = ((Long) ( (JSONArray) token.get("max")).get(1)).intValue();
        this.count = this.minimum;

        parseComponent(this, token);
    }

    public static List<Component> loadCounters(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Component> counters = new ArrayList<>();

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

}
