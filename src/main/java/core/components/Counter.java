package core.components;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils.ComponentType;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Counter extends Component {
    protected int value;  // Current value of this counter
    protected int minimum;  // Minimum value (inclusive)
    protected int maximum;  // Maximum value (inclusive)

    public Counter() {
        this(0, 0, 0, "");
    }

    public Counter(int value, int minimum, int maximum, String name) {
        super(ComponentType.COUNTER, name);
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    protected Counter(int value, int minimum, int maximum, String name, int ID) {
        super(ComponentType.COUNTER, name, ID);
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public Counter copy() {
        Counter copy = new Counter(value, minimum, maximum, componentName, componentID);
        copyComponentTo(copy);
        return copy;
    }

    /**
     * Increment the value of this counter by the specified value.
     * @param value - how much to add to this counter.
     */
    public void increment(int value) {
        this.value += value;
        if (this.value > this.maximum) {
            this.value = this.maximum;
        }
    }

    /**
     * Decrement the value of this counter.
     * @param value - how much to decrease this counter by.
     */
    public void decrement(int value) {
        this.value -= value;
        if (this.value < this.minimum) {
            this.value = this.minimum;
        }
    }

    /**
     * Checks if this counter is at its minimum value.
     * @return true if minimum value, false otherwise.
     */
    public Boolean isMinimum()  {
        return this.value == this.minimum;
    }

    /**
     * Checks if this counter is at its maximum value.
     * @return true if maximum value, false otherwise.
     */
    public Boolean isMaximum()  {
        return this.value == this.maximum;
    }

    /**
     * @return minimum value of this counter.
     */
    public int getMinimum() {
        return minimum;
    }

    /**
     * @return maximum value of this counter.
     */
    public int getMaximum() {
        return maximum;
    }

    /**
     * @return the value of this counter.
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Sets the maximum value for this counter.
     * @param maximum - new maximum value.
     */
    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }

    /**
     * Sets the maximum value for this counter.
     * @param minimum - new minimum value.
     */
    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    /**
     * Sets the value of this counter.
     * @param i - new value for the counter.
     */
    public void setValue(int i) {
        this.value = i;
    }

    /**
     * Loads all counter from a JSON file.
     * @param filename - path to file.
     * @return - List of Counter objects.
     */
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

    /**
     * Creates a new Counter object from a JSON object.
     * @param counter - JSON to parse into a Counter object.
     */
    public void loadCounter(JSONObject counter) {
        this.minimum = ((Long) ( (JSONArray) counter.get("min")).get(1)).intValue();
        this.maximum = ((Long) ( (JSONArray) counter.get("max")).get(1)).intValue();
        this.componentName = (String) counter.get("id");

        // By default, counters go from min to max, and are initialized at min.
        if (counter.get("count") == null)
            this.value = this.minimum;
        else
            this.value = ((Long) ( (JSONArray) counter.get("count")).get(1)).intValue();

        parseComponent(this, counter);
    }

    @Override
    public int hashCode() {
        return componentID;
    }
}
