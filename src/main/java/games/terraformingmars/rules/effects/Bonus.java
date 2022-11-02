package games.terraformingmars.rules.effects;

import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;
import java.util.Objects;

public class Bonus {
    public final int threshold;
    public final TMTypes.GlobalParameter param;
    public final TMAction effect;

    public int claimed = -1;  // Can only execute once

    public Bonus(TMTypes.GlobalParameter p, int threshold, TMAction effect) {
        this.param = p;
        this.effect = effect;
        this.threshold = threshold;
    }

    public TMAction getEffect() {
        return effect;
    }

    public void checkBonus(TMGameState gs) {
        if (claimed == -1) {
            Counter c = gs.getGlobalParameters().get(param);
            if (c.getValueIdx() >= threshold-1) {  // -1 because this is checked right before the increase
                effect.player = gs.getCurrentPlayer();
                claimed = effect.player;
                effect.execute(gs);
            }
        }
    }

    public Bonus copy() {
        Bonus b = new Bonus(param, threshold, effect.copy());
        b.claimed = claimed;
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bonus)) return false;
        Bonus bonus = (Bonus) o;
        return threshold == bonus.threshold && claimed == bonus.claimed && param == bonus.param && Objects.equals(effect, bonus.effect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, param, effect, claimed);
    }
}
