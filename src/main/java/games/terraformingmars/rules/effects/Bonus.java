package games.terraformingmars.rules.effects;

import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.PlaceholderModifyCounter;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.actions.TMModifyCounter;
import games.terraformingmars.components.TMMapTile;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.HashSet;
import java.util.Objects;

import static games.terraformingmars.TMGameState.stringToGPCounter;

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

    public static Bonus parseBonus(TMGameState gs, String s) {
        /*
        Bonus options implemented:
            - Increase/Decrease counter (global parameter, player resource, or player production)
            - Place ocean tile
         */
        String[] split = s.split(":");

        // First element is the counter
        Counter c = stringToGPCounter(gs, split[0]);

        // Second element is threshold, int
        int threshold = Integer.parseInt(split[1]);

        Pair<TMAction, String> effect = TMAction.parseAction(gs, split[2]);
        if (c != null) {
            return new Bonus(c.getComponentID(), threshold, effect.a, effect.b);
        }
        return null;
    }
}
