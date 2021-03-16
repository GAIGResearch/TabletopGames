package games.terraformingmars.rules;

import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;

import java.util.Objects;

public class Bonus {
    public final int counterID;
    public final int threshold;
    public final AbstractAction effect;
    public final String effectString;

    public boolean executed;  // Can only execute once

    public Bonus(int counter, int threshold, AbstractAction effect, String effectString) {
        this.counterID = counter;
        this.threshold = threshold;
        this.effect = effect;
        this.effectString = effectString;
    }

    public void checkBonus(TMGameState gs) {
        if (!executed) {
            Counter c = (Counter) gs.getComponentById(counterID);
            if (c.getValue() == threshold) {
                effect.execute(gs);
                executed = true;
            }
        }
    }

    public Bonus copy() {
        Bonus b = new Bonus(counterID, threshold, effect, effectString);
        b.executed = executed;
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bonus)) return false;
        Bonus bonus = (Bonus) o;
        return counterID == bonus.counterID &&
                threshold == bonus.threshold &&
                Objects.equals(effect, bonus.effect) &&
                Objects.equals(effectString, bonus.effectString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counterID, threshold, effect, effectString);
    }
}
