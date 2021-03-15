package games.terraformingmars.rules;

import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import static games.terraformingmars.TMGameState.stringToGPCounter;

public class CounterRequirement implements Requirement {

    String counterCode;

    int counterID = -1;
    int threshold;
    boolean max;  // if true, value of counter must be <= threshold, if false >=

    public CounterRequirement(String code, int threshold, boolean max) {
        this.counterCode = code;
        this.threshold = threshold;
        this.max = max;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        Counter c;
        if (counterID == -1) {
            c = setCounter(gs);
        } else {
            c = (Counter) gs.getComponentById(counterID);
        }
        if (max && c.getValue() <= threshold) return true;
        return !max && c.getValue() >= threshold;
    }

    private Counter setCounter(TMGameState gs) {
        Counter which = stringToGPCounter(gs, counterCode);

        if (which == null) {
            // A resource or production instead
            TMTypes.Resource res = TMTypes.Resource.valueOf(counterCode.split("prod")[0]);
            if (counterCode.contains("prod")) {
                which = gs.getPlayerProduction()[gs.getCurrentPlayer()].get(res);
            } else {
                which = gs.getPlayerResources()[gs.getCurrentPlayer()].get(res);
            }
        }

        counterID = which.getComponentID();
        return which;
    }
}
