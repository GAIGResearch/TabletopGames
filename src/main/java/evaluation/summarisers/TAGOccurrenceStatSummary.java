package evaluation.summarisers;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to model the statistics of several numbers.  For the statistics
 * we choose here it is not necessary to store all the numbers - just keeping a running total
 * of how many, the sum and the sum of the squares is sufficient (plus max and min, for max and min).
 */

public class TAGOccurrenceStatSummary extends TAGStatSummary {

    private HashMap<Object, Integer> elements;  // Map from element to count of how many times it appeared

    public TAGOccurrenceStatSummary() {
        this("");
    }

    public TAGOccurrenceStatSummary(String name) {
        super(name, StatType.Occurrence);
    }

    public void reset() {
        super.reset();
        elements = new HashMap<>();
    }

    public void add(TAGOccurrenceStatSummary ss) {
        super.add(ss);
        for (Map.Entry<Object, Integer> els: ss.elements.entrySet()) {
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
            add(e);
        }
    }

    public void add(Object o) {
        if (!elements.containsKey(o)) elements.put(o, 0);
        elements.put(o, elements.get(o) + 1);
    }

    public void add(Object... xa) {
        for (Object x : xa) {
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

    public HashMap<Object, Integer> getElements() {
        return elements;
    }

    public TAGOccurrenceStatSummary copy() {
        TAGOccurrenceStatSummary ss = new TAGOccurrenceStatSummary();

        ss.name = this.name;
        ss.n = this.n;
        ss.type = this.type;

        ss.elements = new HashMap<>(elements);

        return ss;
    }

    @Override
    public Map<String, Object> getSummary() {
        Map<String, Object> data = new HashMap<>();
        for (Object key: elements.keySet()) {
            data.put(key.toString(), elements.get(key));
        }
        return data;
    }
}