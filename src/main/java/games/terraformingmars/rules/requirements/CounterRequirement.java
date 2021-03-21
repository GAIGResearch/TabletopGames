package games.terraformingmars.rules.requirements;

import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import jdk.nashorn.internal.objects.Global;
import utilities.Utils;

import java.awt.*;
import java.util.Map;

public class CounterRequirement implements Requirement<TMGameState> {

    public String counterCode;

    int counterID = -1;
    int threshold;
    public boolean max;  // if true, value of counter must be <= threshold, if false >=

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
            if (c.getComponentName().equalsIgnoreCase("temperature")) {
                // Turn to index
                threshold = Utils.indexOf(c.getValues(), threshold);
            }
        } else {
            c = (Counter) gs.getComponentById(counterID);
        }

        // Apply discounts for current player
        int discount = 0;
        int player = gs.getCurrentPlayer();
        for (Map.Entry<Requirement, Integer> e : gs.getPlayerDiscountEffects()[player].entrySet()) {
            if (e.getKey() instanceof CounterRequirement && ((CounterRequirement) e.getKey()).counterCode.equalsIgnoreCase(counterCode)) {
                discount = e.getValue();
                break;
            }
        }

        if (max && c.getValue() - discount <= threshold) return true;
        return !max && c.getValue() + discount >= threshold;
    }

    @Override
    public boolean isMax() {
        return max;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        Counter c;
        int t = threshold;
        if (counterID == -1) {
            c = setCounter(gs);
        } else {
            c = (Counter) gs.getComponentById(counterID);
            if (c.getComponentName().equalsIgnoreCase("temperature")) {
                // Turn to index
                t = c.getValues()[threshold];
            }
        }
        String text = "";
        TMTypes.GlobalParameter p = Utils.searchEnum(TMTypes.GlobalParameter.class, c.getComponentName());
        if (p != null) {
            text = max? "max " : "" + t + " " + p.getShortString();
        } else {
            text = max? "max " : "" + t + " " + c.getComponentName();
        }
        return text;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;  // TODO: if player counter, display image of resource instead
    }

    private Counter setCounter(TMGameState gs) {
        Counter which = gs.stringToGPOrPlayerResCounter(counterCode, -1);
        counterID = which.getComponentID();
        return which;
    }
}
