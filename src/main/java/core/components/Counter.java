package core.components;

import core.CoreConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Counter extends Component {
    protected int[] values;

    protected int valueIdx;  // Current value of this counter
    protected int minimum;  // Minimum value (inclusive)
    protected int maximum;  // Maximum value (inclusive)

    public Counter() {
        this(0, 0, Integer.MAX_VALUE, "");
    }
    public Counter(String name) {
        this(0, 0, Integer.MAX_VALUE, name);
    }
    public Counter(int max, String name) {
        this(0, 0, max, name);
    }

    public Counter(int valueIdx, int minimum, int maximum, String name) {
        super(CoreConstants.ComponentType.COUNTER, name);
        this.valueIdx = valueIdx;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public Counter(int[] values, String name) {
        this(0, 0, values.length-1, name);
        this.values = values;
    }

    protected Counter(int[] values, int valueIdx, int minimum, int maximum, String name, int ID) {
        super(CoreConstants.ComponentType.COUNTER, name, ID);
        this.values = values;
        this.valueIdx = valueIdx;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public Counter copy() {
        Counter copy = new Counter(values != null? values.clone() : null, valueIdx, minimum, maximum, componentName, componentID);
        copyComponentTo(copy);
        return copy;
    }

    /**
     * Increment the value of this counter by the specified value.
     * @param amount - how much to add to this counter.
     * @return - true if succeeded, false if capped at max
     */
    public boolean increment(int amount) {
        this.valueIdx += amount;
        return clamp();
    }
    public boolean increment() {
        return increment(1);
    }

    /**
     * Decrement the value of this counter.
     * @param amount - how much to decrease this counter by.
     * @return - true if succeeded, false if capped at min
     */
    public boolean decrement(int amount) {
        this.valueIdx -= amount;
        return clamp();
    }
    public boolean decrement() {
        return decrement(1);
    }

    private boolean clamp() {
        if (this.valueIdx > this.maximum) {
            this.valueIdx = this.maximum;
            return false;
        }
        if (this.valueIdx < this.minimum) {
            this.valueIdx = this.minimum;
            return false;
        }
        return true;
    }

    /**
     * Checks if this counter is at its minimum value.
     * @return true if minimum value, false otherwise.
     */
    public Boolean isMinimum()  {
        return this.valueIdx <= this.minimum;
    }

    /**
     * Checks if this counter is at its maximum value.
     * @return true if maximum value, false otherwise.
     */
    public Boolean isMaximum()  {
        return this.valueIdx >= this.maximum;
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
     * @return the value index of this counter.
     */
    public int getValueIdx() {
        return this.valueIdx;
    }

    /**
     * @return the value of this counter.
     */
    public int getValue() {
        if (values != null) return values[valueIdx];
        return valueIdx;
    }

    /**
     * @return the value array of this counter.
     */
    public int[] getValues() {
        return values;
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
        this.valueIdx = i;
    }

    public void setToMax() {
        this.valueIdx = maximum;
    }

    public void setToMin() {
        this.valueIdx = minimum;
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
            this.valueIdx = this.minimum;
        else
            this.valueIdx = ((Long) ( (JSONArray) counter.get("count")).get(1)).intValue();

        parseComponent(this, counter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Counter)) return false;
        if (!super.equals(o)) return false;
        Counter counter = (Counter) o;
        return valueIdx == counter.valueIdx && minimum == counter.minimum && maximum == counter.maximum && Arrays.equals(values, counter.values);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), valueIdx, minimum, maximum);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public String toString() {
        return "" + getValue();
    }
}
