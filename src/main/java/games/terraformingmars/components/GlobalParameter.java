package games.terraformingmars.components;

import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.effects.Bonus;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.effects.GlobalParameterEffect;
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

    public boolean increment(int amount, TMGameState gs) {
        boolean success = true;
        for (int i = 0; i < amount; i++) {  // TODO: could be reducing instead
            boolean s = super.increment(1);
            success &= s;
            if (s) {
                int player = gs.getCurrentPlayer();
                increases.add(new Pair<>(gs.getGeneration(), player));

                // Player gets TR
                gs.getPlayerResources()[player].get(TMTypes.Resource.TR).increment(1);
                gs.getPlayerResourceIncreaseGen()[player].put(TMTypes.Resource.TR, true);

                // Params increase, check bonuses
                for (Bonus b : gs.getBonuses()) {
                    b.checkBonus(gs);
                }
            }
        }
        return success;
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

    public ArrayList<Pair<Integer, Integer>> getIncreases() {
        return increases;
    }

    public String getIncreasesString() {
        String s = "[";
        for (Pair<Integer, Integer> p: increases) {
            s += "(" + p.a + "," + p.b + "),";
        }
        s += "]";
        return s.replace(",]", "]");
    }
}
