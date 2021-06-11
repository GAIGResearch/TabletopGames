package games.terraformingmars.rules.requirements;

import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import utilities.Utils;

import java.awt.*;
import java.util.Map;
import java.util.Objects;

public class CounterRequirement implements Requirement<TMGameState> {

    public String counterCode;

    int counterID = -1;
    int thresholdIdx;
    public boolean max;  // if true, value of counter must be <= threshold, if false >=

    public CounterRequirement(String code, int threshold, boolean max) {
        this.counterCode = code;
        this.thresholdIdx = threshold;
        this.max = max;
    }

    @Override
    public boolean testCondition(TMGameState gs) {
        int value = getCounter(gs).getValueIdx();
        int discount = discount(gs);

        if (max && (value - discount <= thresholdIdx)) return true;
        return !max && (value + discount >= thresholdIdx);
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
        Counter c = getCounter(gs);
        String text = "";
        if (c == null) return text;
        TMTypes.GlobalParameter p = Utils.searchEnum(TMTypes.GlobalParameter.class, c.getComponentName());
        if (p != null) {
            text = c.getValues()[thresholdIdx] + " " + p.getShortString();
        } else {
            text = c.getValue() + " " + c.getComponentName();
        }
        return text;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        int value = getCounter(gs).getValue();
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

    @Override
    public CounterRequirement copy() {
        CounterRequirement copy = new CounterRequirement(counterCode, thresholdIdx, max);
        copy.counterID = counterID;
        return copy;
    }

    private Counter getCounter(TMGameState gs) {
        Counter which;
        if (counterID == -1) {
            which = gs.stringToGPOrPlayerResCounter(counterCode, -1);
            counterID = which.getComponentID();
            if (which.getComponentName().equalsIgnoreCase("temperature") ||
                    which.getComponentName().equalsIgnoreCase("venus")) {
                // Turn to index
                thresholdIdx = Utils.indexOf(which.getValues(), thresholdIdx);
            }
        } else {
            gs.getAllComponents();
            which = (Counter) gs.getComponentById(counterID);
        }

        if (max && thresholdIdx == -1) {
            thresholdIdx = which.getMaximum()-1;
        }

        return which;
    }

    @Override
    public String toString() {
        return "Counter Value";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CounterRequirement)) return false;
        CounterRequirement that = (CounterRequirement) o;
        return that.counterCode.equals(counterCode) && max == that.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(counterCode, max);
    }
}
