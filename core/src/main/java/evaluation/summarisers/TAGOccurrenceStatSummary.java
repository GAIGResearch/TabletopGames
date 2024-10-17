package evaluation.summarisers;

import org.jetbrains.annotations.NotNull;
import utilities.Pair;

import java.util.*;
/**
 * This class is used to model the statistics of several numbers.  For the statistics
 * we choose here it is not necessary to store all the numbers - just keeping a running total
 * of how many, the sum and the sum of the squares is sufficient (plus max and min, for max and min).
 */

public class TAGOccurrenceStatSummary extends TAGStatSummary {

    private HashMap<Object, Integer> elements;  // Map from element to count of how many times it appeared

    public TAGOccurrenceStatSummary() {
        this("unnamed");
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

    private void addSingle(Object o)
    {
        if (!elements.containsKey(o)) elements.put(o, 0);
        elements.put(o, elements.get(o) + 1);
        n++;
    }

    public void add(String s) {
        String[] els = s.split(",");
        for (String e: els) {
            //add(e); //This creates Stack Overflow.
            if (!e.equals("")) {
                addSingle(e);
            }
        }
    }

    public void add(Object o) {
        if (o instanceof String) {
            add((String)o);
        } else {
            addSingle(o);
        }
    }

    public void add(Object... xa) {
        for (Object x : xa) {
            add(x);
        }
    }

    public void add(double[] xa) {
        for (double x : xa) {
            add(x);
        }
    }
    public void add(int[] xa) {
        for (int x : xa) {
            add(x);
        }
    }

    public Pair<Object, Integer> getHighestOccurrence() {
        Object maxO = null;
        int max = 0;
        for (Object o: elements.keySet()) {
            if (elements.get(o) > max) {
                max = elements.get(o);
                maxO = o;
            }
        }
        if (maxO != null) return new Pair<>(maxO, max);
        return null;
    }

    public Pair<Object, Integer> getLowestOccurrence() {
        Object minO = null;
        int min = Integer.MAX_VALUE;
        for (Object o: elements.keySet()) {
            if (elements.get(o) < min) {
                min = elements.get(o);
                minO = o;
            }
        }
        if (minO != null) return new Pair<>(minO, min);
        return null;
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

    public String stringSummary()
    {
        TreeSet<DataMeasure> sortedByVal = new TreeSet<>();

        StringBuilder stb = new StringBuilder();

        //header
//        stb.append(name).append("\n");
        stb.append("\tCount - Measure\n");
        for(Map.Entry<Object, Integer> k: elements.entrySet())
            sortedByVal.add(new DataMeasure(k.getKey().toString(), k.getValue()));

        for(DataMeasure d : sortedByVal)
            stb.append("\t").append(d.toString()).append("\n");

        return stb.toString();
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
        for (Object k: elements.keySet()) {
            data.put(k.toString(), elements.get(k));
        }
        return data;
    }

    private static class DataMeasure implements Comparable
    {
        private final String data;
        private final int count;

        DataMeasure(String data, int count)
        {
            this.data = data;
            this.count = count;
        }

        @Override
        public int compareTo(@NotNull Object o) {
            if(!(o instanceof DataMeasure)) return 0;
            int comparison = Integer.compare(this.count, ((DataMeasure)o).count);
            if(comparison == 0) return this.data.compareTo(((DataMeasure)o).data);
            return -comparison; //This is to sort from high count to lower count.
        }

        public boolean equals(Object o)
        {
            if(!(o instanceof DataMeasure)) return false;
            return this.data.equals(((DataMeasure)o).data) && this.count == ((DataMeasure)o).count;
        }

        public String toString()
        {
            return count + " - " + data;
        }
    }

}