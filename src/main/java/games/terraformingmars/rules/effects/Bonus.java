package games.terraformingmars.rules.effects;

import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import java.util.Objects;

public class Bonus {
    public final int threshold;
    public TMTypes.GlobalParameter param;
    public TMAction effect;

    public boolean executed;  // Can only execute once

    public Bonus(TMTypes.GlobalParameter p, int threshold, TMAction effect) {
        this.param = p;
        this.effect = effect;
        this.threshold = threshold;
    }

    public TMAction getEffect() {
        return effect;
    }

    public void checkBonus(TMGameState gs) {
        if (!executed) {
            Counter c = gs.getGlobalParameters().get(param);
            if (c.getValueIdx() >= threshold-1) {  // -1 because this is checked right before the increase
                effect.player = gs.getCurrentPlayer();
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
        return threshold == bonus.threshold && executed == bonus.executed && param == bonus.param && Objects.equals(effect, bonus.effect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, param, effect, executed);
    }
}
