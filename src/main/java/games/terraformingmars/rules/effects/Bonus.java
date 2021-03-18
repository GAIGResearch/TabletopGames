package games.terraformingmars.rules.effects;

import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import utilities.Pair;
import java.util.Objects;

public class Bonus {
    public final int threshold;
    public TMTypes.GlobalParameter param;
    public AbstractAction effect;
    public String effectString;

    public boolean executed;  // Can only execute once

    public Bonus(TMTypes.GlobalParameter p, int threshold, String effect) {
        this.threshold = threshold;
        this.effectString = effect;
        this.param = p;
    }

    private Bonus(TMTypes.GlobalParameter p, int threshold, AbstractAction effect) {
        this.param = p;
        this.effect = effect;
        this.threshold = threshold;
    }

    public void checkBonus(TMGameState gs) {
        if (!executed) {
            Counter c = gs.getGlobalParameters().get(param);
            if (effect == null) {
                Pair<TMAction, String> effect = TMAction.parseAction(gs, effectString);
                this.effect = effect.a;
            }

            if (c.getValue() == threshold) {
                effect.execute(gs);
                executed = true;
            }
        }
    }

    public Bonus copy() {
        Bonus b = new Bonus(param, threshold, effect);
        b.executed = executed;
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bonus)) return false;
        Bonus bonus = (Bonus) o;
        return threshold == bonus.threshold && executed == bonus.executed && param == bonus.param && Objects.equals(effect, bonus.effect) && Objects.equals(effectString, bonus.effectString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, param, effect, effectString, executed);
    }
}
