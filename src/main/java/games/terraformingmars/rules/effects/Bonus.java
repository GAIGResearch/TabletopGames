package games.terraformingmars.rules.effects;

import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.TMAction;
import utilities.Pair;
import java.util.Objects;

public class Bonus {
    public final int threshold;
    public int counterID;
    public String counterCode;
    public AbstractAction effect;
    public String effectString;

    public boolean executed;  // Can only execute once

    public Bonus(String counterCode, int threshold, String effect) {
        this.counterCode = counterCode;
        this.threshold = threshold;
        this.effectString = effect;
        this.counterID = -1;
    }

    private Bonus(int counterID, int threshold, AbstractAction effect) {
        this.counterID = counterID;
        this.effect = effect;
        this.threshold = threshold;
    }

    public void checkBonus(TMGameState gs) {
        if (!executed) {
            Counter c;
            if (counterID == -1) {
                c = gs.stringToGPOrPlayerResCounter(counterCode, -1);
                counterID = c.getComponentID();
            } else {
                c = (Counter) gs.getComponentById(counterID);
            }
            if (effect == null) {
                Pair<TMAction, String> effect = TMAction.parseAction(gs, effectString);
                this.effect = effect.a;
            }

            if (c != null && c.getValue() == threshold) {
                effect.execute(gs);
                executed = true;
            }
        }
    }

    public Bonus copy() {
        Bonus b = new Bonus(counterID, threshold, effect);
        b.executed = executed;
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bonus)) return false;
        Bonus bonus = (Bonus) o;
        return threshold == bonus.threshold && counterID == bonus.counterID && executed == bonus.executed && Objects.equals(counterCode, bonus.counterCode) && Objects.equals(effect, bonus.effect) && Objects.equals(effectString, bonus.effectString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, counterID, counterCode, effect, effectString, executed);
    }

    public static Bonus parseBonus(String s) {
        /*
        Bonus options implemented:
            - Increase/Decrease counter (global parameter, player resource, or player production)
            - Place ocean tile
         */
        String[] split = s.split(":");

        // First element is the counter
        // Second element is threshold, int
        // Third is effect
        return new Bonus(split[0], Integer.parseInt(split[1]), split[2]);
    }
}
