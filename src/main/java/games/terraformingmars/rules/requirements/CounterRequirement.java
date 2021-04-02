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
        int value = calculate(gs);
        int discount = discount(gs);

        if (max && (value - discount <= threshold)) return true;
        return !max && (value + discount >= threshold);
    }

    private int calculate(TMGameState gs) {
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

        if (max && threshold == -1) {
            threshold = c.getMaximum();
        }

        return c.getValue();
    }

    private int discount(TMGameState gs) {
        // Apply discounts for current player
        int discount = 0;
        int player = gs.getCurrentPlayer();
        for (Map.Entry<Requirement, Integer> e : gs.getPlayerDiscountEffects()[player].entrySet()) {
            if (e.getKey() instanceof CounterRequirement && ((CounterRequirement) e.getKey()).counterCode.equalsIgnoreCase(counterCode)) {
                discount = e.getValue();
                break;
            }
        }
        return discount;
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
            if (c.getComponentName().equalsIgnoreCase("temperature")) {
                // Turn to index
                t = c.getValues()[threshold];
            }
        } else {
            c = (Counter) gs.getComponentById(counterID);
        }
        String text;
        TMTypes.GlobalParameter p = Utils.searchEnum(TMTypes.GlobalParameter.class, c.getComponentName());
        if (p != null) {
            text = t + " " + p.getShortString();
        } else {
            text = t + " " + c.getComponentName();
        }
        return text;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        int value = calculate(gs);
        int discount = discount(gs);

        if (max) {
            return "value " + (value - discount) + " when max " + getDisplayText(gs);
        } else {
            return "value " + (value + discount) + " when min " + getDisplayText(gs);
        }
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

    @Override
    public String toString() {
        return "Counter Value";
    }
}
