package utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to model the statistics of several numbers.  For the statistics
 * we choose here it is not necessary to store all the numbers - just keeping a running total
 * of how many, the sum and the sum of the squares is sufficient (plus max and min, for max and min).
 */

public class TAGStringStatSummary {

    public String name; // defaults to ""

    private int n; // Total number of elements (sum of values in map)
    private HashMap<String, Integer> elements;  // Map from element to count of how many times it appeared

    public TAGStringStatSummary() {
        this("");
    }

    public TAGStringStatSummary(String name) {
        this.name = name;
        reset();
    }

    public final void reset() {
        n = 0;
        elements = new HashMap<>();
    }

    public int n() {
        return n;
    }

    public void add(TAGStringStatSummary ss) {
        n += ss.n;
        for (Map.Entry<String, Integer> els: ss.elements.entrySet()) {
            if (elements.containsKey(els.getKey())) {
                elements.put(els.getKey(), elements.get(els.getKey()) + els.getValue());
            } else {
                elements.put(els.getKey(), els.getValue());
            }
        }
    }

    public void add(String s) {
        String[] els = s.split(",");
        n += els.length;
        for (String e: els) {
            if (!elements.containsKey(e)) elements.put(e, 0);
            elements.put(e, elements.get(e) + 1);
        }
    }

    public void add(String... xa) {
        for (String x : xa) {
            add(x);
        }
    }

    @Override
    public String toString() {
        String s = (name == null) ? "" : (name + "\n");
        s += " n     = " + n + "\n" + elements.toString();
        return s;
    }

    public String shortString() {
        return (name == null) ? "[" : (name + ": [") + elements.toString() + "]";
    }

    public HashMap<String, Integer> getElements() {
        return elements;
    }

    public TAGStringStatSummary copy() {
        TAGStringStatSummary ss = new TAGStringStatSummary();

        ss.name = this.name;
        ss.n = this.n;
        ss.elements = new HashMap<>(elements);

        return ss;
    }
}