package games.terraformingmars.components;

import core.components.Counter;
import utilities.Pair;

import java.util.ArrayList;

public class GlobalParameter extends Counter {
    ArrayList<Pair<Integer, Integer>> increases;

    public GlobalParameter() {
        super();
        increases = new ArrayList<>();
    }

    public GlobalParameter(int valueIdx, int minimum, int maximum, String name) {
        super(valueIdx, minimum, maximum, name);
        increases = new ArrayList<>();
    }

    public GlobalParameter(int[] values, String name) {
        super(values, name);
        increases = new ArrayList<>();
    }

    protected GlobalParameter(int[] values, int valueIdx, int minimum, int maximum, String name, int ID) {
        super(values, valueIdx, minimum, maximum, name, ID);
        increases = new ArrayList<>();
    }

    public boolean increment(int amount, int generation, int player) {
        boolean s = super.increment(amount);
        if (s) {
            increases.add(new Pair<>(generation, player));
        }
        return s;
    }

    @Override
    public GlobalParameter copy() {
        GlobalParameter copy = new GlobalParameter(values != null? values.clone() : null, valueIdx, minimum, maximum, componentName, componentID);
        for (Pair<Integer, Integer> p: increases) {
            copy.increases.add(p.copy());
        }
        copyComponentTo(copy);
        return copy;
    }

    public String getIncreases() {
        String s = "[";
        for (Pair<Integer, Integer> p: increases) {
            s += "(" + p.a + "," + p.b + "),";
        }
        s += "]";
        return s.replace(",]", "]");
    }
}
